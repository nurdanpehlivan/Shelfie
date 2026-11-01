import { env } from "cloudflare:workers";
import type { User } from "./auth.ts";
import { db } from "./db.ts";
import { accessToken, parseServiceAccount, type ServiceAccount } from "./google.ts";
import { error, readJson, sha256Hex } from "./http.ts";
import { premiumUntil } from "./quota.ts";

/**
 * Google Play subscriptions. The app buys through Play Billing and sends the purchase token
 * here; the Worker asks the Play Developer API whether it is a live subscription of this app,
 * and stores its expiry for the user. Premium covers the whole household (see quota.ts).
 *
 * Needs the GOOGLE_SERVICE_ACCOUNT secret: the JSON key of a service account that has access
 * to the app in Play Console (Users and permissions → "View financial data").
 */

const PLAY_API = "https://androidpublisher.googleapis.com/androidpublisher/v3";
const SCOPE = "https://www.googleapis.com/auth/androidpublisher";

/** Still entitled until expiry: cancelled means "won't renew", grace means "payment retrying". */
const ENTITLED_STATES = new Set([
	"SUBSCRIPTION_STATE_ACTIVE",
	"SUBSCRIPTION_STATE_IN_GRACE_PERIOD",
	"SUBSCRIPTION_STATE_CANCELED",
]);

interface SubscriptionV2 {
	subscriptionState?: string;
	lineItems?: Array<{ productId?: string; expiryTime?: string }>;
}

/** `POST /api/billing/verify` `{ productId, purchaseToken }`. */
export async function handleBillingVerify(request: Request, user: User): Promise<Response> {
	if (request.method !== "POST") return error(405, "Method not allowed");
	const body = await readJson<{ productId?: unknown; purchaseToken?: unknown }>(request, 8 * 1024);
	const productId = typeof body?.productId === "string" ? body.productId : "";
	const purchaseToken = typeof body?.purchaseToken === "string" ? body.purchaseToken : "";
	if (!allowedProducts().has(productId) || purchaseToken.length < 10 || purchaseToken.length > 4096) {
		return error(400, "Invalid purchase");
	}
	const account = serviceAccount();
	if (!account) return error(503, "Billing is not configured on the server");

	const d = await db();
	const tokenHash = await sha256Hex(purchaseToken);
	// One purchase belongs to one account; sharing a token would hand out free Premium.
	const owner = await d.prepare("SELECT user_id FROM subscriptions WHERE token_hash = ?").bind(tokenHash).first<{ user_id: string }>();
	if (owner && owner.user_id !== user.id) return error(409, "This purchase belongs to another account");

	let subscription: SubscriptionV2;
	try {
		subscription = await fetchSubscription(account, purchaseToken);
	} catch (e) {
		console.error("play verify failed", e);
		return error(502, "Could not reach Google Play");
	}
	const line = subscription.lineItems?.find((l) => l.productId && allowedProducts().has(l.productId));
	const expiresAt = line?.expiryTime ? Date.parse(line.expiryTime) : NaN;
	const entitled = ENTITLED_STATES.has(subscription.subscriptionState ?? "") && Number.isFinite(expiresAt) && expiresAt > Date.now();

	await d
		.prepare(
			`INSERT INTO subscriptions (token_hash, user_id, product_id, state, expires_at, updated_at)
			 VALUES (?, ?, ?, ?, ?, ?)
			 ON CONFLICT (token_hash) DO UPDATE SET state = excluded.state, expires_at = excluded.expires_at, updated_at = excluded.updated_at`,
		)
		.bind(tokenHash, user.id, line?.productId ?? productId, subscription.subscriptionState ?? "UNKNOWN", entitled ? expiresAt : 0, Date.now())
		.run();

	return Response.json({ entitled, premiumUntil: await premiumUntil(user.id) });
}

function allowedProducts(): Set<string> {
	return new Set((env.PREMIUM_PRODUCT_IDS ?? "").split(",").map((p) => p.trim()).filter(Boolean));
}

function serviceAccount(): ServiceAccount | null {
	return parseServiceAccount(env.GOOGLE_SERVICE_ACCOUNT);
}

async function fetchSubscription(account: ServiceAccount, purchaseToken: string): Promise<SubscriptionV2> {
	const url = `${PLAY_API}/applications/${encodeURIComponent(env.PLAY_PACKAGE_NAME)}/purchases/subscriptionsv2/tokens/${encodeURIComponent(purchaseToken)}`;
	const response = await fetch(url, { headers: { authorization: `Bearer ${await accessToken(account, SCOPE)}` } });
	if (!response.ok) throw new Error(`Play API ${response.status}: ${await response.text()}`);
	return (await response.json()) as SubscriptionV2;
}

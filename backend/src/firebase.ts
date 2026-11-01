import { env } from "cloudflare:workers";
import { accessToken, parseServiceAccount, type ServiceAccount } from "./google.ts";

/**
 * Firebase: Authentication keeps the email + password accounts (and sends the "forgot password"
 * email), Cloud Messaging wakes household members' phones. Calls go to Google's REST APIs:
 * Identity Toolkit with FIREBASE_API_KEY for signing in, the service account in
 * FIREBASE_SERVICE_ACCOUNT for deleting accounts and sending messages.
 *
 * The rest of the backend keeps its own users and session tokens; Firebase only answers "is this
 * the right password" (auth.ts maps a Firebase account to a user by email).
 */

const IDENTITY = "https://identitytoolkit.googleapis.com/v1";
const CLOUD_SCOPE = "https://www.googleapis.com/auth/cloud-platform";
const FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";

export function firebaseConfigured(): boolean {
	return !!env.FIREBASE_API_KEY?.startsWith("AIza");
}

function serviceAccount(): ServiceAccount | null {
	const account = parseServiceAccount(env.FIREBASE_SERVICE_ACCOUNT);
	return account?.project_id ? account : null;
}

/** Firebase's error code, e.g. "EMAIL_EXISTS" or "INVALID_LOGIN_CREDENTIALS" (details after " : " dropped). */
export class FirebaseError extends Error {
	constructor(readonly code: string) {
		super(code);
	}
}

async function identity<T>(method: string, body: Record<string, unknown>, locale?: string): Promise<T> {
	const response = await fetch(`${IDENTITY}/accounts:${method}?key=${env.FIREBASE_API_KEY}`, {
		method: "POST",
		headers: { "content-type": "application/json", ...(locale ? { "x-firebase-locale": locale } : {}) },
		body: JSON.stringify(body),
	});
	const json = (await response.json().catch(() => ({}))) as { error?: { message?: string } } & T;
	if (!response.ok) throw new FirebaseError((json.error?.message ?? `HTTP_${response.status}`).split(" ")[0]);
	return json;
}

interface FirebaseUser {
	localId: string;
	email: string;
}

/** Throws [FirebaseError] "INVALID_LOGIN_CREDENTIALS" (or the older EMAIL_NOT_FOUND / INVALID_PASSWORD) for a bad login. */
export function signInWithPassword(email: string, password: string): Promise<FirebaseUser> {
	return identity<FirebaseUser>("signInWithPassword", { email, password, returnSecureToken: true });
}

/** Throws [FirebaseError] "EMAIL_EXISTS" when the email already has a Firebase account. */
export function signUp(email: string, password: string): Promise<FirebaseUser> {
	return identity<FirebaseUser>("signUp", { email, password, returnSecureToken: true });
}

/** Emails Firebase's reset link in [locale]; succeeds silently for unknown addresses. */
export async function sendPasswordReset(email: string, locale: string): Promise<void> {
	await identity("sendOobCode", { requestType: "PASSWORD_RESET", email }, locale);
}

/** Deletes the Firebase account; without the service account it only logs, since the user row is gone anyway. */
export async function deleteFirebaseUser(uid: string): Promise<void> {
	const account = serviceAccount();
	if (!account) {
		console.error("FIREBASE_SERVICE_ACCOUNT missing; Firebase user not deleted", uid);
		return;
	}
	const response = await fetch(`${IDENTITY}/projects/${account.project_id}/accounts:delete`, {
		method: "POST",
		headers: { authorization: `Bearer ${await accessToken(account, CLOUD_SCOPE)}`, "content-type": "application/json" },
		body: JSON.stringify({ localId: uid }),
	});
	if (!response.ok) console.error("firebase delete", response.status, await response.text());
}

/**
 * Sends a data-only message to each token. Returns the tokens Firebase says are gone (app
 * uninstalled or token rotated), so the caller can forget them.
 */
export async function sendPush(tokens: string[], data: Record<string, string>): Promise<string[]> {
	const account = serviceAccount();
	if (!account || !tokens.length) return [];
	const bearer = await accessToken(account, FCM_SCOPE);
	const results = await Promise.allSettled(
		tokens.map(async (token) => {
			const response = await fetch(`https://fcm.googleapis.com/v1/projects/${account.project_id}/messages:send`, {
				method: "POST",
				headers: { authorization: `Bearer ${bearer}`, "content-type": "application/json" },
				// High priority so a dozing phone wakes up for it; the app builds the notification itself.
				body: JSON.stringify({ message: { token, data, android: { priority: "HIGH" } } }),
			});
			if (response.status === 404 || response.status === 400) return token;
			if (!response.ok) console.error("fcm", response.status, await response.text());
			return null;
		}),
	);
	return results.flatMap((r) => (r.status === "fulfilled" && r.value ? [r.value] : []));
}

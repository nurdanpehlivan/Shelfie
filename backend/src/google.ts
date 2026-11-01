/**
 * OAuth access tokens for Google APIs from a service account's JSON key (self-signed JWT flow).
 * Used by billing.ts (Play Developer API) and firebase.ts (Identity Toolkit, Cloud Messaging).
 */

const TOKEN_URL = "https://oauth2.googleapis.com/token";

export interface ServiceAccount {
	client_email: string;
	private_key: string;
	project_id?: string;
}

/** The parsed key, or `null` while the secret holds a placeholder such as "{}". */
export function parseServiceAccount(json: string | undefined): ServiceAccount | null {
	try {
		const parsed = JSON.parse(json ?? "") as Partial<ServiceAccount>;
		return parsed.client_email && parsed.private_key ? (parsed as ServiceAccount) : null;
	} catch {
		return null;
	}
}

const cache = new Map<string, { value: string; expiresAt: number }>();

/** A token for [scope], cached per isolate until a minute before it expires. */
export async function accessToken(account: ServiceAccount, scope: string): Promise<string> {
	const key = `${account.client_email} ${scope}`;
	const cached = cache.get(key);
	if (cached && cached.expiresAt > Date.now() + 60_000) return cached.value;
	const now = Math.floor(Date.now() / 1000);
	const assertion = await signJwt(account, { iss: account.client_email, scope, aud: TOKEN_URL, iat: now, exp: now + 3600 });
	const response = await fetch(TOKEN_URL, {
		method: "POST",
		headers: { "content-type": "application/x-www-form-urlencoded" },
		body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion }),
	});
	if (!response.ok) throw new Error(`token ${response.status}: ${await response.text()}`);
	const json = (await response.json()) as { access_token: string; expires_in: number };
	cache.set(key, { value: json.access_token, expiresAt: Date.now() + json.expires_in * 1000 });
	return json.access_token;
}

async function signJwt(account: ServiceAccount, claims: Record<string, unknown>): Promise<string> {
	const encode = (value: unknown) => base64Url(new TextEncoder().encode(JSON.stringify(value)));
	const unsigned = `${encode({ alg: "RS256", typ: "JWT" })}.${encode(claims)}`;
	const pem = account.private_key.replace(/-----[^-]+-----/g, "").replace(/\s+/g, "");
	const der = Uint8Array.from(atob(pem), (c) => c.charCodeAt(0));
	const key = await crypto.subtle.importKey("pkcs8", der, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["sign"]);
	const signature = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(unsigned));
	return `${unsigned}.${base64Url(new Uint8Array(signature))}`;
}

function base64Url(bytes: Uint8Array): string {
	let binary = "";
	for (const b of bytes) binary += String.fromCharCode(b);
	return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

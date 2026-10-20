/** Same envelope shape as Gemini errors, so the app's error handling reads both alike. */
export function error(status: number, message: string, headers?: HeadersInit): Response {
	return Response.json({ error: { code: status, message } }, { status, headers });
}

export function html(body: string, headers?: Record<string, string>): Response {
	return new Response(body, {
		headers: {
			"content-type": "text/html; charset=utf-8",
			"cache-control": "no-store",
			"x-frame-options": "DENY",
			"referrer-policy": "no-referrer",
			...headers,
		},
	});
}

/** Constant-time comparison so a secret can't be guessed byte by byte from response timing. */
export function safeEqual(given: string | null | undefined, expected: string | undefined): boolean {
	if (!given || !expected) return false;
	const a = new TextEncoder().encode(given);
	const b = new TextEncoder().encode(expected);
	if (a.byteLength !== b.byteLength) return false;
	return crypto.subtle.timingSafeEqual(a, b);
}

export function randomToken(bytes = 32): string {
	return toHex(crypto.getRandomValues(new Uint8Array(bytes)));
}

export async function sha256Hex(value: string): Promise<string> {
	return toHex(new Uint8Array(await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value))));
}

export function toHex(bytes: Uint8Array): string {
	return Array.from(bytes, (b) => b.toString(16).padStart(2, "0")).join("");
}

export function fromHex(hex: string): Uint8Array {
	const out = new Uint8Array(hex.length / 2);
	for (let i = 0; i < out.length; i++) out[i] = parseInt(hex.slice(i * 2, i * 2 + 2), 16);
	return out;
}

export function getCookie(request: Request, name: string): string | null {
	const header = request.headers.get("cookie");
	if (!header) return null;
	for (const part of header.split(";")) {
		const [key, ...rest] = part.trim().split("=");
		if (key === name) return decodeURIComponent(rest.join("="));
	}
	return null;
}

/** HttpOnly, Secure, SameSite=Strict, scoped to [path]; `maxAge` 0 clears it. */
export function cookie(name: string, value: string, path: string, maxAgeSeconds: number): string {
	return `${name}=${encodeURIComponent(value)}; Path=${path}; Max-Age=${maxAgeSeconds}; HttpOnly; Secure; SameSite=Strict`;
}

/** Reads a JSON body of at most [maxBytes]; `null` if it is missing, too large or not JSON. */
export async function readJson<T>(request: Request, maxBytes = 1024 * 1024): Promise<T | null> {
	const declared = Number(request.headers.get("content-length") ?? 0);
	if (declared > maxBytes) return null;
	const text = await request.text();
	if (text.length > maxBytes) return null;
	try {
		return JSON.parse(text) as T;
	} catch {
		return null;
	}
}

export function clientIp(request: Request): string {
	return request.headers.get("cf-connecting-ip") ?? "unknown";
}

/**
 * Same-origin check for cookie-authenticated writes. SameSite=Strict already blocks
 * cross-site cookies; this also rejects requests whose Origin header names another site.
 */
export function isSameOrigin(request: Request): boolean {
	const origin = request.headers.get("origin");
	return origin === null || origin === new URL(request.url).origin;
}

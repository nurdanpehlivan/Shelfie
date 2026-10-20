import { env } from "cloudflare:workers";
import { db } from "./db.ts";
import {
	clientIp,
	cookie,
	error,
	fromHex,
	getCookie,
	isSameOrigin,
	randomToken,
	readJson,
	sha256Hex,
	toHex,
} from "./http.ts";

/**
 * Email + password accounts for sync and the web panel.
 *
 * The app sends `Authorization: Bearer <token>`; the web panel gets the same kind of token in an
 * HttpOnly cookie. Only the token's SHA-256 is stored, so a database leak doesn't leak sessions.
 */

export const SESSION_COOKIE = "shelfie_session";
const DAY_MS = 24 * 60 * 60 * 1000;
const APP_SESSION_DAYS = 365;
const WEB_SESSION_DAYS = 30;

/**
 * PBKDF2 rounds for new and upgraded hashes, from the PASSWORD_ITERATIONS setting. The Free plan
 * allows 10 ms of CPU per request, which 100 000 rounds exceed; Workers caps PBKDF2 at 100 000.
 * Each user's hash records its own count, and login re-hashes to the current one.
 */
function passwordIterations(): number {
	const n = Number(env.PASSWORD_ITERATIONS);
	return Number.isInteger(n) ? Math.min(100_000, Math.max(10_000, n)) : 20_000;
}
const MIN_PASSWORD_LENGTH = 8;
const MAX_PASSWORD_LENGTH = 200;
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const MAX_LOGIN_FAILURES = 10;
const LOGIN_WINDOW_MS = 15 * 60 * 1000;

export interface User {
	id: string;
	email: string;
}

type Client = "app" | "web";

interface Credentials {
	email?: unknown;
	password?: unknown;
	installId?: unknown;
}

export async function hashPassword(password: string, saltHex: string, iterations: number): Promise<string> {
	const key = await crypto.subtle.importKey("raw", new TextEncoder().encode(password), "PBKDF2", false, [
		"deriveBits",
	]);
	const bits = await crypto.subtle.deriveBits(
		{ name: "PBKDF2", hash: "SHA-256", salt: fromHex(saltHex), iterations },
		key,
		256,
	);
	return toHex(new Uint8Array(bits));
}

/** `POST /api/auth/register` and `/api/auth/login`; [client] decides token vs. cookie. */
export async function handleAuth(request: Request, action: string, client: Client): Promise<Response> {
	if (request.method !== "POST") return error(405, "Method not allowed");
	if (client === "web" && !isSameOrigin(request)) return error(403, "Forbidden");

	const { success } = await env.RATE_LIMITER.limit({ key: `auth:${clientIp(request)}` });
	if (!success) return error(429, "Too many requests");

	if (action === "logout") return logout(request, client);

	const body = await readJson<Credentials>(request, 4096);
	const email = typeof body?.email === "string" ? body.email.trim().toLowerCase() : "";
	const password = typeof body?.password === "string" ? body.password : "";
	const installId = typeof body?.installId === "string" ? body.installId.slice(0, 64) : null;
	if (!EMAIL.test(email) || email.length > 254) return error(400, "Geçerli bir e-posta adresi gir.");
	if (password.length < MIN_PASSWORD_LENGTH || password.length > MAX_PASSWORD_LENGTH) {
		return error(400, `Şifre en az ${MIN_PASSWORD_LENGTH} karakter olmalı.`);
	}

	if (action === "register") return register(email, password, client, installId);
	if (action === "login") return login(request, email, password, client, installId);
	return error(404, "Not found");
}

async function register(email: string, password: string, client: Client, installId: string | null) {
	const d = await db();
	const existing = await d.prepare("SELECT 1 FROM users WHERE email = ?").bind(email).first();
	if (existing) return error(409, "Bu e-posta ile zaten bir hesap var.");

	const id = crypto.randomUUID();
	const salt = randomToken(16);
	const iterations = passwordIterations();
	const now = Date.now();
	await d
		.prepare(
			`INSERT INTO users (id, email, password_hash, password_salt, password_iterations, created_at, last_seen_at)
			 VALUES (?, ?, ?, ?, ?, ?, ?)`,
		)
		.bind(id, email, await hashPassword(password, salt, iterations), salt, iterations, now, now)
		.run();
	return startSession({ id, email }, client, installId, 201);
}

async function login(request: Request, email: string, password: string, client: Client, installId: string | null) {
	const d = await db();
	const failureKey = `login:${clientIp(request)}`;
	if (await tooManyFailures(failureKey)) {
		return error(429, "Çok fazla hatalı deneme. Biraz sonra tekrar dene.");
	}

	const row = await d
		.prepare("SELECT id, email, password_hash, password_salt, password_iterations FROM users WHERE email = ?")
		.bind(email)
		.first<{ id: string; email: string; password_hash: string; password_salt: string; password_iterations: number }>();
	// Hash even when the user doesn't exist, so response time doesn't reveal which emails have accounts.
	const target = passwordIterations();
	const hash = await hashPassword(password, row?.password_salt ?? "00".repeat(16), row?.password_iterations ?? target);
	if (!row || !timingSafeHexEqual(hash, row.password_hash)) {
		await recordFailure(failureKey);
		return error(401, "E-posta ya da şifre hatalı.");
	}
	if (row.password_iterations !== target) {
		// The round count was changed (e.g. after moving to a paid plan); re-hash while the password is at hand.
		const salt = randomToken(16);
		await d
			.prepare("UPDATE users SET password_hash = ?, password_salt = ?, password_iterations = ? WHERE id = ?")
			.bind(await hashPassword(password, salt, target), salt, target, row.id)
			.run();
	}
	await d.prepare("UPDATE users SET last_seen_at = ? WHERE id = ?").bind(Date.now(), row.id).run();
	return startSession({ id: row.id, email: row.email }, client, installId, 200);
}

async function startSession(user: User, client: Client, installId: string | null, status: number) {
	const token = randomToken();
	const now = Date.now();
	const days = client === "app" ? APP_SESSION_DAYS : WEB_SESSION_DAYS;
	const d = await db();
	await d
		.prepare("INSERT INTO sessions (token_hash, user_id, client, install_id, created_at, expires_at) VALUES (?, ?, ?, ?, ?, ?)")
		.bind(await sha256Hex(token), user.id, client, installId, now, now + days * DAY_MS)
		.run();

	if (client === "app") return Response.json({ token, user }, { status });
	return Response.json(
		{ user },
		{ status, headers: { "set-cookie": cookie(SESSION_COOKIE, token, "/", days * 24 * 60 * 60) } },
	);
}

async function logout(request: Request, client: Client): Promise<Response> {
	const token = sessionToken(request, client);
	if (token) {
		const d = await db();
		await d.prepare("DELETE FROM sessions WHERE token_hash = ?").bind(await sha256Hex(token)).run();
	}
	const headers = client === "web" ? { "set-cookie": cookie(SESSION_COOKIE, "", "/", 0) } : undefined;
	return Response.json({ ok: true }, { headers });
}

function sessionToken(request: Request, client: Client): string | null {
	if (client === "web") return getCookie(request, SESSION_COOKIE);
	const header = request.headers.get("authorization") ?? "";
	return header.startsWith("Bearer ") ? header.slice(7).trim() : null;
}

/** The signed-in user, or `null` when the token is missing, unknown or expired. */
export async function authenticate(request: Request, client: Client): Promise<User | null> {
	const token = sessionToken(request, client);
	if (!token) return null;
	const d = await db();
	return d
		.prepare(
			`SELECT users.id AS id, users.email AS email FROM sessions
			 JOIN users ON users.id = sessions.user_id
			 WHERE sessions.token_hash = ? AND sessions.expires_at > ?`,
		)
		.bind(await sha256Hex(token), Date.now())
		.first<User>();
}

/** `DELETE /api/account`: removes the user, their sessions and every synced record. */
export async function deleteAccount(user: User): Promise<Response> {
	const d = await db();
	await d.batch([
		d.prepare("DELETE FROM sync_records WHERE user_id = ?").bind(user.id),
		d.prepare("DELETE FROM sessions WHERE user_id = ?").bind(user.id),
		d.prepare("DELETE FROM users WHERE id = ?").bind(user.id),
	]);
	return Response.json({ ok: true }, { headers: { "set-cookie": cookie(SESSION_COOKIE, "", "/", 0) } });
}

/** Both are hex digests of equal length, so a plain constant-time loop is enough. */
function timingSafeHexEqual(a: string, b: string): boolean {
	if (a.length !== b.length) return false;
	let diff = 0;
	for (let i = 0; i < a.length; i++) diff |= a.charCodeAt(i) ^ b.charCodeAt(i);
	return diff === 0;
}

export async function tooManyFailures(key: string): Promise<boolean> {
	const d = await db();
	const row = await d
		.prepare("SELECT count, window_start FROM login_failures WHERE key = ?")
		.bind(key)
		.first<{ count: number; window_start: number }>();
	return !!row && row.window_start > Date.now() - LOGIN_WINDOW_MS && row.count >= MAX_LOGIN_FAILURES;
}

export async function recordFailure(key: string): Promise<void> {
	const d = await db();
	const now = Date.now();
	await d
		.prepare(
			`INSERT INTO login_failures (key, count, window_start) VALUES (?1, 1, ?2)
			 ON CONFLICT (key) DO UPDATE SET
			   count = CASE WHEN window_start > ?3 THEN count + 1 ELSE 1 END,
			   window_start = CASE WHEN window_start > ?3 THEN window_start ELSE ?2 END`,
		)
		.bind(key, now, now - LOGIN_WINDOW_MS)
		.run();
}

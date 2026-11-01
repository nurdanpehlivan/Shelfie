import { env } from "cloudflare:workers";
import { db } from "./db.ts";
import { firebaseConfigured, FirebaseError, sendPasswordReset, signUp } from "./firebase.ts";
import { clientIp, error, randomToken, readJson } from "./http.ts";

/**
 * "Forgot password": `POST /api/auth/forgot` `{email, lang}` has Firebase email its reset link,
 * which opens Firebase's own page for the new password. Answers the same whether or not the
 * account exists, and 503 until Firebase is set up.
 */

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const LANGS = new Set(["tr", "en", "de", "es", "fr"]);

export async function handleForgot(request: Request): Promise<Response> {
	if (request.method !== "POST") return error(405, "Method not allowed");
	if (!firebaseConfigured()) return error(503, "Password reset is not available");
	const { success } = await env.RATE_LIMITER.limit({ key: `forgot:${clientIp(request)}` });
	if (!success) return error(429, "Too many requests");

	const body = await readJson<{ email?: unknown; lang?: unknown }>(request, 1024);
	const email = typeof body?.email === "string" ? body.email.trim().toLowerCase() : "";
	if (!EMAIL.test(email) || email.length > 254) return error(400, "Invalid email");
	const lang = typeof body?.lang === "string" && LANGS.has(body.lang.slice(0, 2)) ? body.lang.slice(0, 2) : "en";

	await moveToFirebaseIfNeeded(email);
	try {
		await sendPasswordReset(email, lang);
	} catch (e) {
		if (e instanceof FirebaseError && e.code === "TOO_MANY_ATTEMPTS_TRY_LATER") return error(429, "Too many requests");
		// Unknown addresses are not an error worth showing; anything else is.
		if (!(e instanceof FirebaseError && e.code === "EMAIL_NOT_FOUND")) {
			console.error("forgot", e);
			return error(502, "Could not send the email");
		}
	}
	return Response.json({ ok: true });
}

/**
 * An account from before Firebase, which forgot its password before logging in again, has no
 * Firebase account to reset yet: create one with a random password, which the reset replaces.
 */
async function moveToFirebaseIfNeeded(email: string): Promise<void> {
	const d = await db();
	const row = await d
		.prepare("SELECT id FROM users WHERE email = ? AND id NOT IN (SELECT user_id FROM firebase_users)")
		.bind(email)
		.first<{ id: string }>();
	if (!row) return;
	try {
		const firebaseUser = await signUp(email, randomToken(24));
		await d.batch([
			d.prepare("INSERT INTO firebase_users (user_id, firebase_uid) VALUES (?, ?)").bind(row.id, firebaseUser.localId),
			d.prepare("UPDATE users SET password_hash = '', password_salt = '', password_iterations = 0 WHERE id = ?").bind(row.id),
		]);
	} catch (e) {
		// Already in Firebase (e.g. moved by a half-finished earlier request): the reset works anyway.
		if (!(e instanceof FirebaseError && e.code === "EMAIL_EXISTS")) throw e;
	}
}

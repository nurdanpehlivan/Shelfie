import { env } from "cloudflare:workers";
import { hashPassword, MAX_PASSWORD_LENGTH, MIN_PASSWORD_LENGTH, passwordIterations } from "./auth.ts";
import { db } from "./db.ts";
import { clientIp, error, randomToken, readJson, sha256Hex } from "./http.ts";

/**
 * "Forgot password": `POST /api/auth/forgot` emails a one-hour link to `/reset#<token>`, where
 * `POST /api/auth/reset` sets the new password and signs every device out.
 *
 * Mail goes out through Resend (RESEND_API_KEY, MAIL_FROM on a domain verified there). Until
 * they are real values the endpoint answers 503 and the app says the feature isn't available yet.
 */

const RESET_TTL_MS = 60 * 60 * 1000;
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

type Lang = "tr" | "en" | "de" | "es" | "fr";

const MAIL: Record<Lang, { subject: string; body: (link: string) => string }> = {
	tr: {
		subject: "Frizzy şifreni sıfırla",
		body: (link) =>
			`Merhaba,\n\nFrizzy hesabın için şifre sıfırlama istendi. Yeni şifreni belirlemek için bu bağlantıyı aç (1 saat geçerli):\n\n${link}\n\nBunu sen istemediysen bu e-postayı yok sayabilirsin; şifren değişmez.\n\nFrizzy`,
	},
	en: {
		subject: "Reset your Frizzy password",
		body: (link) =>
			`Hi,\n\nSomeone asked to reset the password of your Frizzy account. Open this link to choose a new one (valid for 1 hour):\n\n${link}\n\nIf it wasn't you, ignore this email; your password stays the same.\n\nFrizzy`,
	},
	de: {
		subject: "Frizzy-Passwort zurücksetzen",
		body: (link) =>
			`Hallo,\n\nfür dein Frizzy-Konto wurde ein neues Passwort angefordert. Öffne diesen Link, um eines festzulegen (1 Stunde gültig):\n\n${link}\n\nWarst du das nicht, ignoriere diese E-Mail; dein Passwort bleibt gleich.\n\nFrizzy`,
	},
	es: {
		subject: "Restablece tu contraseña de Frizzy",
		body: (link) =>
			`Hola:\n\nSe ha pedido restablecer la contraseña de tu cuenta de Frizzy. Abre este enlace para elegir una nueva (válido 1 hora):\n\n${link}\n\nSi no fuiste tú, ignora este correo; tu contraseña no cambia.\n\nFrizzy`,
	},
	fr: {
		subject: "Réinitialise ton mot de passe Frizzy",
		body: (link) =>
			`Bonjour,\n\nUne réinitialisation du mot de passe de ton compte Frizzy a été demandée. Ouvre ce lien pour en choisir un nouveau (valable 1 heure) :\n\n${link}\n\nSi ce n'était pas toi, ignore cet e-mail ; ton mot de passe reste le même.\n\nFrizzy`,
	},
};

function langOf(value: unknown): Lang {
	const code = typeof value === "string" ? value.slice(0, 2).toLowerCase() : "";
	return code in MAIL ? (code as Lang) : "en";
}

/** Both secrets must exist to deploy; until mail is set up they hold a placeholder such as "unset". */
export function mailConfigured(): boolean {
	return !!env.RESEND_API_KEY?.startsWith("re_") && !!env.MAIL_FROM?.includes("@");
}

/** `POST /api/auth/forgot` `{email, lang}`. Answers the same whether or not the account exists. */
export async function handleForgot(request: Request): Promise<Response> {
	if (request.method !== "POST") return error(405, "Method not allowed");
	if (!mailConfigured()) return error(503, "Password reset is not available");
	const { success } = await env.RATE_LIMITER.limit({ key: `forgot:${clientIp(request)}` });
	if (!success) return error(429, "Too many requests");

	const body = await readJson<{ email?: unknown; lang?: unknown }>(request, 1024);
	const email = typeof body?.email === "string" ? body.email.trim().toLowerCase() : "";
	if (!EMAIL.test(email) || email.length > 254) return error(400, "Invalid email");

	const d = await db();
	const user = await d.prepare("SELECT id FROM users WHERE email = ?").bind(email).first<{ id: string }>();
	if (user) {
		const token = randomToken();
		// One live link per account: a new request replaces the previous one.
		await d.batch([
			d.prepare("DELETE FROM password_resets WHERE user_id = ?").bind(user.id),
			d
				.prepare("INSERT INTO password_resets (token_hash, user_id, expires_at) VALUES (?, ?, ?)")
				.bind(await sha256Hex(token), user.id, Date.now() + RESET_TTL_MS),
		]);
		const mail = MAIL[langOf(body?.lang)];
		const link = `${new URL(request.url).origin}/reset#${token}`;
		const sent = await sendMail(email, mail.subject, mail.body(link));
		if (!sent) return error(502, "Could not send the email");
	}
	return Response.json({ ok: true });
}

/** `POST /api/auth/reset` `{token, password}`: sets the password and ends every session. */
export async function handleReset(request: Request): Promise<Response> {
	if (request.method !== "POST") return error(405, "Method not allowed");
	const { success } = await env.RATE_LIMITER.limit({ key: `reset:${clientIp(request)}` });
	if (!success) return error(429, "Too many requests");

	const body = await readJson<{ token?: unknown; password?: unknown }>(request, 1024);
	const token = typeof body?.token === "string" ? body.token.trim() : "";
	const password = typeof body?.password === "string" ? body.password : "";
	if (password.length < MIN_PASSWORD_LENGTH || password.length > MAX_PASSWORD_LENGTH) {
		return error(400, "Password too short");
	}
	const d = await db();
	const row = token
		? await d
				.prepare("SELECT user_id FROM password_resets WHERE token_hash = ? AND expires_at > ?")
				.bind(await sha256Hex(token), Date.now())
				.first<{ user_id: string }>()
		: null;
	if (!row) return error(404, "Link expired");

	const salt = randomToken(16);
	const iterations = passwordIterations();
	await d.batch([
		d
			.prepare("UPDATE users SET password_hash = ?, password_salt = ?, password_iterations = ? WHERE id = ?")
			.bind(await hashPassword(password, salt, iterations), salt, iterations, row.user_id),
		d.prepare("DELETE FROM password_resets WHERE user_id = ?").bind(row.user_id),
		// Whoever knew the old password is signed out everywhere.
		d.prepare("DELETE FROM sessions WHERE user_id = ?").bind(row.user_id),
	]);
	return Response.json({ ok: true });
}

async function sendMail(to: string, subject: string, text: string): Promise<boolean> {
	const response = await fetch("https://api.resend.com/emails", {
		method: "POST",
		headers: { authorization: `Bearer ${env.RESEND_API_KEY}`, "content-type": "application/json" },
		body: JSON.stringify({ from: env.MAIL_FROM, to: [to], subject, text }),
	});
	if (!response.ok) console.error("resend", response.status, await response.text());
	return response.ok;
}

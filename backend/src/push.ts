import { db } from "./db.ts";
import type { User } from "./auth.ts";
import { sendPush } from "./firebase.ts";
import { usersInSpace } from "./household.ts";
import { error, readJson, sha256Hex } from "./http.ts";

/**
 * Push messages through Firebase Cloud Messaging. The app registers its token per session.
 * Whenever a data space changes, every other device in it gets a data message: "household_activity"
 * when someone else did something worth telling (the app fetches the feed and posts the
 * notification in its own language, syncing too), otherwise "sync" (the app just syncs). So the
 * phones, the household and the web panel stay in step within seconds instead of hours.
 */

const TOKEN = /^[\w:-]{20,4096}$/;

/** `POST /api/push/token` `{token}`: this signed-in app's current FCM token. */
export async function handlePushToken(request: Request, user: User): Promise<Response> {
	if (request.method !== "POST") return error(405, "Method not allowed");
	const body = await readJson<{ token?: unknown }>(request, 8192);
	const token = typeof body?.token === "string" ? body.token.trim() : "";
	if (!TOKEN.test(token)) return error(400, "Invalid token");
	const session = (request.headers.get("authorization") ?? "").slice("Bearer ".length).trim();
	const d = await db();
	await d
		.prepare(
			`INSERT INTO push_tokens (token, user_id, session_hash, updated_at) VALUES (?, ?, ?, ?)
			 ON CONFLICT (token) DO UPDATE SET user_id = excluded.user_id, session_hash = excluded.session_hash, updated_at = excluded.updated_at`,
		)
		.bind(token, user.id, await sha256Hex(session), Date.now())
		.run();
	return Response.json({ ok: true });
}

/**
 * Tells the devices in [space] that its data changed. The device that made the change (its
 * session [sourceSession], a SHA-256 hex) is skipped; with [news], the other members get the
 * activity message and everyone else (the actor's other devices included) a plain "sync".
 */
export async function notifySpaceChanged(
	space: string,
	actorId: string | null,
	sourceSession: string | null,
	news: boolean,
): Promise<void> {
	const users = await usersInSpace(space);
	const d = await db();
	const { results } = await d
		.prepare(`SELECT token, user_id, session_hash FROM push_tokens WHERE user_id IN (${users.map(() => "?").join(",")})`)
		.bind(...users)
		.all<{ token: string; user_id: string; session_hash: string }>();
	const targets = results.filter((r) => r.session_hash !== sourceSession);
	const toldNews = (r: { user_id: string }) => news && r.user_id !== actorId;
	const gone = [
		...(await sendPush(targets.filter(toldNews).map((r) => r.token), { type: "household_activity" })),
		...(await sendPush(targets.filter((r) => !toldNews(r)).map((r) => r.token), { type: "sync" })),
	];
	if (gone.length) {
		await d.batch(gone.map((token) => d.prepare("DELETE FROM push_tokens WHERE token = ?").bind(token)));
	}
}

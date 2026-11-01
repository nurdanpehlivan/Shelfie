import { db } from "./db.ts";
import type { User } from "./auth.ts";
import { sendPush } from "./firebase.ts";
import { usersInSpace } from "./household.ts";
import { error, readJson, sha256Hex } from "./http.ts";

/**
 * Push messages through Firebase Cloud Messaging. The app registers its token per session;
 * when someone in a household changes something, the other members' phones get a data message
 * and fetch the activity feed themselves (so the notification is in the phone's language).
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

/** Wakes everyone in [space] except [actorId]: "the household changed something". */
export async function notifyHousehold(space: string, actorId: string): Promise<void> {
	const others = (await usersInSpace(space)).filter((id) => id !== actorId);
	if (!others.length) return;
	const d = await db();
	const { results } = await d
		.prepare(`SELECT token FROM push_tokens WHERE user_id IN (${others.map(() => "?").join(",")})`)
		.bind(...others)
		.all<{ token: string }>();
	const gone = await sendPush(
		results.map((r) => r.token),
		{ type: "household_activity" },
	);
	if (gone.length) {
		await d.batch(gone.map((token) => d.prepare("DELETE FROM push_tokens WHERE token = ?").bind(token)));
	}
}

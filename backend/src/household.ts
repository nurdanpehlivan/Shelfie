import { db } from "./db.ts";
import type { User } from "./auth.ts";
import { error, readJson } from "./http.ts";

/**
 * Households: family members share one pantry, shopping list, recipe history and meal plan.
 *
 * A household is its owner's data space. Members' synced records are read from and written to
 * the owner's `user_id`, so every query in sync.ts goes through `spaceOf`. Joining or leaving
 * changes a user's space; the sync response reports it, and the app then re-uploads what it has
 * and pulls the new space from the start (see the app's SyncManager), which merges the data.
 */

export const MAX_HOUSEHOLD_MEMBERS = 6;
const INVITE_TTL_MS = 48 * 60 * 60 * 1000;
/** No 0/O, 1/I/L: codes are read out loud and typed on phones. */
const INVITE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
const INVITE_LENGTH = 8;

/** The user id whose records this user syncs: their household's owner, or themselves. */
export async function spaceOf(userId: string): Promise<string> {
	const d = await db();
	const row = await d.prepare("SELECT owner_id FROM household_members WHERE member_id = ?").bind(userId).first<{ owner_id: string }>();
	return row?.owner_id ?? userId;
}

/** Every user in [space]: the owner and the members. */
export async function usersInSpace(space: string): Promise<string[]> {
	const d = await db();
	const { results } = await d
		.prepare("SELECT member_id AS id FROM household_members WHERE owner_id = ?")
		.bind(space)
		.all<{ id: string }>();
	return [space, ...results.map((r) => r.id)];
}

interface MemberRow {
	email: string;
	joined_at: number | null;
	is_owner: number;
}

/** `/api/household/*` for the signed-in app user. */
export async function handleHousehold(request: Request, action: string, user: User): Promise<Response> {
	if (action === "" && request.method === "GET") return Response.json(await describe(user));
	if (request.method !== "POST") return error(405, "Method not allowed");
	switch (action) {
		case "invite":
			return invite(user);
		case "join":
			return join(request, user);
		case "leave":
			return leave(user);
		case "remove":
			return remove(request, user);
		default:
			return error(404, "Not found");
	}
}

async function describe(user: User) {
	const d = await db();
	const space = await spaceOf(user.id);
	const { results } = await d
		.prepare(
			`SELECT users.email AS email, NULL AS joined_at, 1 AS is_owner FROM users WHERE users.id = ?1
			 UNION ALL
			 SELECT users.email, household_members.joined_at, 0 FROM household_members
			 JOIN users ON users.id = household_members.member_id
			 WHERE household_members.owner_id = ?1
			 ORDER BY is_owner DESC, joined_at`,
		)
		.bind(space)
		.all<MemberRow>();
	const invite =
		space === user.id
			? await d
					.prepare("SELECT code, expires_at FROM household_invites WHERE owner_id = ? AND expires_at > ?")
					.bind(user.id, Date.now())
					.first<{ code: string; expires_at: number }>()
			: null;
	return {
		// "none" until someone joins: a solo account is the owner of a household of one.
		role: space !== user.id ? "member" : results.length > 1 ? "owner" : "none",
		members: results.map((r) => ({ email: r.email, isOwner: r.is_owner === 1, joinedAt: r.joined_at })),
		invite: invite ? { code: invite.code, expiresAt: invite.expires_at } : null,
		maxMembers: MAX_HOUSEHOLD_MEMBERS,
	};
}

/** A fresh code replaces the previous one; it stays valid for anyone until it expires. */
async function invite(user: User): Promise<Response> {
	if ((await spaceOf(user.id)) !== user.id) return error(403, "Only the household owner can invite.");
	const d = await db();
	const code = randomCode();
	const expiresAt = Date.now() + INVITE_TTL_MS;
	await d.batch([
		d.prepare("DELETE FROM household_invites WHERE owner_id = ?").bind(user.id),
		d.prepare("INSERT INTO household_invites (code, owner_id, expires_at) VALUES (?, ?, ?)").bind(code, user.id, expiresAt),
	]);
	return Response.json({ code, expiresAt });
}

async function join(request: Request, user: User): Promise<Response> {
	const body = await readJson<{ code?: unknown }>(request, 1024);
	const code = typeof body?.code === "string" ? body.code.trim().toUpperCase().replace(/[\s-]/g, "") : "";
	if (code.length !== INVITE_LENGTH) return error(400, "Invalid code");

	const d = await db();
	const inviteRow = await d
		.prepare("SELECT owner_id FROM household_invites WHERE code = ? AND expires_at > ?")
		.bind(code, Date.now())
		.first<{ owner_id: string }>();
	if (!inviteRow) return error(404, "Invite not found or expired");
	const owner = inviteRow.owner_id;
	if (owner === user.id) return error(409, "That is your own household");
	// An owner with members would strand them; they have to empty their household first.
	const ownMembers = await d.prepare("SELECT COUNT(*) AS n FROM household_members WHERE owner_id = ?").bind(user.id).first<{ n: number }>();
	if ((ownMembers?.n ?? 0) > 0) return error(409, "Remove your own household members first");
	const size = await d.prepare("SELECT COUNT(*) AS n FROM household_members WHERE owner_id = ?").bind(owner).first<{ n: number }>();
	if ((size?.n ?? 0) + 1 >= MAX_HOUSEHOLD_MEMBERS) return error(409, "Household is full");

	await d
		.prepare(
			`INSERT INTO household_members (member_id, owner_id, joined_at) VALUES (?, ?, ?)
			 ON CONFLICT (member_id) DO UPDATE SET owner_id = excluded.owner_id, joined_at = excluded.joined_at`,
		)
		.bind(user.id, owner, Date.now())
		.run();
	return Response.json(await describe(user));
}

/** The member keeps a copy: their app re-uploads it to their own space on the next sync. */
async function leave(user: User): Promise<Response> {
	const d = await db();
	await d.prepare("DELETE FROM household_members WHERE member_id = ?").bind(user.id).run();
	return Response.json(await describe(user));
}

async function remove(request: Request, user: User): Promise<Response> {
	const body = await readJson<{ email?: unknown }>(request, 1024);
	const email = typeof body?.email === "string" ? body.email.trim().toLowerCase() : "";
	const d = await db();
	await d
		.prepare(
			`DELETE FROM household_members
			 WHERE owner_id = ? AND member_id = (SELECT id FROM users WHERE email = ?)`,
		)
		.bind(user.id, email)
		.run();
	return Response.json(await describe(user));
}

function randomCode(): string {
	const bytes = crypto.getRandomValues(new Uint8Array(INVITE_LENGTH));
	return Array.from(bytes, (b) => INVITE_ALPHABET[b % INVITE_ALPHABET.length]).join("");
}

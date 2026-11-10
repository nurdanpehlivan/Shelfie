import { db } from "./db.ts";
import type { User } from "./auth.ts";
import { error, readJson } from "./http.ts";
import { notifySpaceChanged } from "./push.ts";
import { premiumUntil } from "./quota.ts";

/**
 * Households: family members share one pantry, shopping list, recipe history and meal plan.
 *
 * A household is its owner's data space. Members' synced records are read from and written to
 * the owner's `user_id`, so every query in sync.ts goes through `spaceOf`. Joining or leaving
 * changes a user's space; the sync response reports it, and the app then replaces its data with
 * the new space's (see the app's SyncManager).
 *
 * Joining brings the member's own records into the household (`mergeInto`). Leaving or being
 * removed splits it again (`splitOut`): every record keeps an author (`record_authors`), the
 * member takes back what they added, and the owner keeps the rest.
 */

/** People in a household, owner included: Premium (held by anyone in it) raises the limit. */
export const MAX_HOUSEHOLD_MEMBERS = 6;
export const FREE_HOUSEHOLD_MEMBERS = 2;

async function memberLimit(owner: string): Promise<number> {
	return (await premiumUntil(owner)) ? MAX_HOUSEHOLD_MEMBERS : FREE_HOUSEHOLD_MEMBERS;
}
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

export async function describe(user: User) {
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
		maxMembers: await memberLimit(space),
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
	const limit = await memberLimit(owner);
	if ((size?.n ?? 0) + 1 >= limit) {
		// The app tells "full" from "full on the free plan" by the message.
		return error(409, limit < MAX_HOUSEHOLD_MEMBERS ? "Household is full on the free plan; Premium allows more" : "Household is full");
	}

	await d.batch([
		...mergeInto(d, owner, user.id),
		d
			.prepare(
				`INSERT INTO household_members (member_id, owner_id, joined_at) VALUES (?, ?, ?)
				 ON CONFLICT (member_id) DO UPDATE SET owner_id = excluded.owner_id, joined_at = excluded.joined_at`,
			)
			.bind(user.id, owner, Date.now()),
	]);
	await notifySpaceChanged(owner, user.id, null, false).catch((e) => console.error("push", e));
	return Response.json(await describe(user));
}

/** The member takes back what they added; the owner keeps the rest. */
async function leave(user: User): Promise<Response> {
	const owner = await spaceOf(user.id);
	if (owner !== user.id) await splitOut(owner, user.id);
	return Response.json(await describe(user));
}

/**
 * Statements that copy [member]'s own live records into [owner]'s space, authored by the member.
 * Revs are the owner's highest plus the member's own, so they are new to the owner's devices.
 */
function mergeInto(d: D1Database, owner: string, member: string): D1PreparedStatement[] {
	return [
		d
			.prepare(
				`INSERT OR IGNORE INTO record_authors (space, collection, sync_id, author_id)
				 SELECT ?1, collection, sync_id, ?2 FROM sync_records m
				 WHERE m.user_id = ?2 AND m.deleted = 0
				   AND NOT EXISTS (SELECT 1 FROM sync_records o WHERE o.user_id = ?1 AND o.collection = m.collection AND o.sync_id = m.sync_id)`,
			)
			.bind(owner, member),
		d
			.prepare(
				`INSERT INTO sync_records (user_id, collection, sync_id, data, deleted, changed_at, rev)
				 SELECT ?1, collection, sync_id, data, 0, changed_at,
				        (SELECT COALESCE(MAX(rev), 0) FROM sync_records WHERE user_id = ?1) + rev
				 FROM sync_records WHERE user_id = ?2 AND deleted = 0
				 ON CONFLICT (user_id, collection, sync_id) DO UPDATE SET
				   data = excluded.data, deleted = 0, changed_at = excluded.changed_at, rev = excluded.rev
				 WHERE excluded.changed_at >= sync_records.changed_at`,
			)
			.bind(owner, member),
	];
}

/**
 * Ends [member]'s membership in [owner]'s household and splits the data: the member's own space
 * is rebuilt from the records they added, and those leave the owner's space as deletions, so the
 * owner's devices drop them. Both sides' apps notice and pull their space again.
 */
export async function splitOut(owner: string, member: string): Promise<void> {
	const d = await db();
	const now = Date.now();
	const authored = `SELECT collection, sync_id FROM record_authors WHERE space = ?1 AND author_id = ?2`;
	await d.batch([
		// What the member had before joining is already in the household; start their space afresh.
		d.prepare("DELETE FROM sync_records WHERE user_id = ?").bind(member),
		d.prepare("DELETE FROM record_authors WHERE space = ?").bind(member),
		d
			.prepare(
				`INSERT INTO sync_records (user_id, collection, sync_id, data, deleted, changed_at, rev)
				 SELECT ?2, collection, sync_id, data, 0, changed_at, rev FROM sync_records
				 WHERE user_id = ?1 AND deleted = 0 AND (collection, sync_id) IN (${authored})`,
			)
			.bind(owner, member),
		// Newer than any write still on its way, so an old edit can't bring a record back.
		d
			.prepare(
				`UPDATE sync_records SET data = NULL, deleted = 1, changed_at = MAX(changed_at, ?3),
				   rev = (SELECT COALESCE(MAX(rev), 0) FROM sync_records WHERE user_id = ?1) + rev
				 WHERE user_id = ?1 AND deleted = 0 AND (collection, sync_id) IN (${authored})`,
			)
			.bind(owner, member, now),
		d.prepare("DELETE FROM record_authors WHERE space = ? AND author_id = ?").bind(owner, member),
		d.prepare("DELETE FROM household_members WHERE member_id = ? AND owner_id = ?").bind(member, owner),
	]);
	// Both sides' devices pull their new data now, not at their next scheduled sync.
	await notifySpaceChanged(owner, null, null, false).catch((e) => console.error("push", e));
	await notifySpaceChanged(member, null, null, false).catch((e) => console.error("push", e));
}

async function remove(request: Request, user: User): Promise<Response> {
	const body = await readJson<{ email?: unknown }>(request, 1024);
	const email = typeof body?.email === "string" ? body.email.trim().toLowerCase() : "";
	const d = await db();
	const member = await d
		.prepare(
			`SELECT member_id FROM household_members
			 WHERE owner_id = ? AND member_id = (SELECT id FROM users WHERE email = ?)`,
		)
		.bind(user.id, email)
		.first<{ member_id: string }>();
	if (member) await splitOut(user.id, member.member_id);
	return Response.json(await describe(user));
}

function randomCode(): string {
	const bytes = crypto.getRandomValues(new Uint8Array(INVITE_LENGTH));
	return Array.from(bytes, (b) => INVITE_ALPHABET[b % INVITE_ALPHABET.length]).join("");
}

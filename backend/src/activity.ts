import { db } from "./db.ts";
import type { User } from "./auth.ts";
import { spaceOf, usersInSpace } from "./household.ts";
import type { Change } from "./sync.ts";

/**
 * The household's activity feed: who added, used, threw away or froze what. Events are derived
 * from synced writes by comparing a record with its previous state, so the app needs no extra
 * calls. Only shared spaces keep a feed; a solo account has nobody to tell.
 */

/** A record first seen with an older `createdAt` is old data being merged (sign-in, joining), not news. */
const FRESH_MS = 6 * 60 * 60 * 1000;
const MAX_NAME_LENGTH = 60;
const MAX_EVENTS = 50;
export const ACTIVITY_RETENTION_DAYS = 30;

export type Action =
	| "added"
	| "used"
	| "used_some"
	| "wasted"
	| "removed"
	| "froze"
	| "thawed"
	| "list_added"
	| "list_bought"
	| "recipe_saved";

export interface PreviousState {
	data: Record<string, unknown> | null;
	deleted: boolean;
	changedAt: number;
}

interface Event {
	action: Action;
	name: string;
	emoji: string | null;
}

/** The previous state of each record in [changes], keyed by "collection/syncId"; `null` when the space isn't shared. */
export async function loadPrevious(space: string, changes: Change[]): Promise<Map<string, PreviousState> | null> {
	if (!changes.length || (await usersInSpace(space)).length < 2) return null;
	const d = await db();
	const previous = new Map<string, PreviousState>();
	// D1 binds at most 100 parameters per statement.
	for (let i = 0; i < changes.length; i += 90) {
		const ids = [...new Set(changes.slice(i, i + 90).map((c) => c.syncId))];
		const { results } = await d
			.prepare(
				`SELECT collection, sync_id, data, deleted, changed_at FROM sync_records
				 WHERE user_id = ? AND sync_id IN (${ids.map(() => "?").join(",")})`,
			)
			.bind(space, ...ids)
			.all<{ collection: string; sync_id: string; data: string | null; deleted: number; changed_at: number }>();
		for (const row of results) {
			previous.set(`${row.collection}/${row.sync_id}`, {
				data: row.data ? (JSON.parse(row.data) as Record<string, unknown>) : null,
				deleted: row.deleted === 1,
				changedAt: row.changed_at,
			});
		}
	}
	return previous;
}

/** Records the events in [changes] made by [actorId]; [previous] comes from [loadPrevious] before the write. */
export async function recordActivity(
	space: string,
	actorId: string,
	changes: Change[],
	previous: Map<string, PreviousState>,
	now = Date.now(),
): Promise<void> {
	const events = changes
		.map((c) => describeChange(c, previous.get(`${c.collection}/${c.syncId}`), now))
		.filter((e): e is Event => e !== null);
	if (!events.length) return;
	const d = await db();
	const insert = d.prepare(
		"INSERT INTO household_activity (space, actor_id, action, name, emoji, ts) VALUES (?, ?, ?, ?, ?, ?)",
	);
	await d.batch(events.map((e) => insert.bind(space, actorId, e.action, e.name, e.emoji, now)));
}

/** What a change means to the rest of the household, or `null` when it isn't worth telling. */
export function describeChange(change: Change, prev: PreviousState | undefined, now: number): Event | null {
	// The server keeps the newer copy; an older write changed nothing.
	if (prev && change.changedAt < prev.changedAt) return null;
	const before = prev && !prev.deleted ? prev.data : null;
	const after = change.deleted ? null : change.data;
	const source = after ?? before;
	const name = text(source?.name ?? source?.title)?.slice(0, MAX_NAME_LENGTH);
	if (!name) return null;
	const emoji = text(source?.emoji)?.slice(0, 16) ?? null;
	const event = (action: Action): Event => ({ action, name, emoji });
	const isFresh = (data: Record<string, unknown>) => Number(data.createdAt) > now - FRESH_MS;

	switch (change.collection) {
		case "pantry": {
			if (!after) return before ? event("removed") : null;
			if (!before) return isFresh(after) && after.isConsumed !== true ? event("added") : null;
			if (before.isConsumed !== true && after.isConsumed === true) return event(after.isWasted === true ? "wasted" : "used");
			if (before.location !== "FREEZER" && after.location === "FREEZER") return event("froze");
			if (before.location === "FREEZER" && typeof after.location === "string" && after.location !== "FREEZER") {
				return event("thawed");
			}
			if (Number(after.quantity) < Number(before.quantity)) return event("used_some");
			return null;
		}
		case "shopping": {
			// Deleting from the list is tidying up; buying already said what happened.
			if (!after) return null;
			if (!before) return isFresh(after) ? event("list_added") : null;
			if (before.isChecked !== true && after.isChecked === true) return event("list_bought");
			return null;
		}
		case "recipes":
			return after && !before && isFresh(after) ? event("recipe_saved") : null;
		default:
			return null;
	}
}

function text(value: unknown): string | null {
	return typeof value === "string" && value.trim() ? value.trim() : null;
}

/** `GET /api/household/activity`: the newest events of the user's household, newest first. */
export async function handleActivity(user: User): Promise<Response> {
	const space = await spaceOf(user.id);
	const d = await db();
	const { results } = await d
		.prepare(
			`SELECT household_activity.id AS id, household_activity.actor_id AS actor_id, users.email AS email,
			        action, name, emoji, ts
			 FROM household_activity JOIN users ON users.id = household_activity.actor_id
			 WHERE space = ? ORDER BY household_activity.id DESC LIMIT ?`,
		)
		.bind(space, MAX_EVENTS)
		.all<{ id: number; actor_id: string; email: string; action: Action; name: string; emoji: string | null; ts: number }>();
	return Response.json({
		events: results.map((r) => ({
			id: r.id,
			actor: r.email,
			isMe: r.actor_id === user.id,
			action: r.action,
			name: r.name,
			emoji: r.emoji,
			at: r.ts,
		})),
	});
}

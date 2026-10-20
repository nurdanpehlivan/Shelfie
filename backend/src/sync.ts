import { db } from "./db.ts";
import type { User } from "./auth.ts";
import { spaceOf } from "./household.ts";
import { error, readJson } from "./http.ts";

/**
 * Record sync between the app's Room database, the server and the web panel.
 *
 * Each record is identified by (collection, syncId) and carries its whole state as JSON.
 * Conflicts resolve by last write wins on the writer's `changedAt`. Every accepted write gets the
 * next per-space `rev`; a client pulls everything above the highest `rev` it has seen.
 *
 * Records live in a data space: the user's own id, or their household owner's (household.ts).
 * The sync response names the space, so a client notices when it changes and starts over.
 */

export const COLLECTIONS = ["pantry", "shopping", "recipes", "mealplan"] as const;
export type Collection = (typeof COLLECTIONS)[number];

const MAX_CHANGES_PER_REQUEST = 200;
const MAX_RECORDS_PER_PULL = 500;
const MAX_RECORD_BYTES = 64 * 1024;
const SYNC_ID = /^[A-Za-z0-9-]{8,64}$/;

export interface Change {
	collection: Collection;
	syncId: string;
	data: Record<string, unknown> | null;
	deleted: boolean;
	changedAt: number;
}

interface SyncRequest {
	cursor?: unknown;
	changes?: unknown;
}

interface RecordRow {
	collection: Collection;
	sync_id: string;
	data: string | null;
	deleted: number;
	changed_at: number;
	rev: number;
}

/** `POST /api/sync`: applies the client's changes, then returns what changed since its cursor. */
export async function handleSync(request: Request, user: User): Promise<Response> {
	if (request.method !== "POST") return error(405, "Method not allowed");
	const body = await readJson<SyncRequest>(request, 4 * 1024 * 1024);
	if (!body) return error(400, "Invalid JSON");

	const cursor = typeof body.cursor === "number" && body.cursor >= 0 ? Math.floor(body.cursor) : 0;
	const rawChanges = Array.isArray(body.changes) ? body.changes : [];
	if (rawChanges.length > MAX_CHANGES_PER_REQUEST) {
		return error(413, `At most ${MAX_CHANGES_PER_REQUEST} changes per request`);
	}
	const changes: Change[] = [];
	for (const raw of rawChanges) {
		const change = parseChange(raw, Date.now());
		if (!change) return error(400, "Invalid change");
		changes.push(change);
	}

	const space = await spaceOf(user.id);
	await applyChanges(space, changes);

	const d = await db();
	const { results } = await d
		.prepare(
			`SELECT collection, sync_id, data, deleted, changed_at, rev FROM sync_records
			 WHERE user_id = ? AND rev > ? ORDER BY rev LIMIT ?`,
		)
		.bind(space, cursor, MAX_RECORDS_PER_PULL + 1)
		.all<RecordRow>();
	const hasMore = results.length > MAX_RECORDS_PER_PULL;
	const page = results.slice(0, MAX_RECORDS_PER_PULL);

	return Response.json({
		space,
		cursor: page.length ? page[page.length - 1].rev : cursor,
		hasMore,
		records: page.map(toChange),
	});
}

/**
 * Validates one change. `changedAt` may not lie in the future (beyond a little clock skew),
 * or a device with a wrong clock would win every conflict forever.
 */
export function parseChange(raw: unknown, now: number): Change | null {
	if (typeof raw !== "object" || raw === null) return null;
	const r = raw as Record<string, unknown>;
	if (!COLLECTIONS.includes(r.collection as Collection)) return null;
	if (typeof r.syncId !== "string" || !SYNC_ID.test(r.syncId)) return null;
	if (typeof r.changedAt !== "number" || !Number.isFinite(r.changedAt)) return null;
	const deleted = r.deleted === true;
	const data = deleted ? null : r.data;
	if (!deleted && (typeof data !== "object" || data === null || Array.isArray(data))) return null;
	if (data && JSON.stringify(data).length > MAX_RECORD_BYTES) return null;
	return {
		collection: r.collection as Collection,
		syncId: r.syncId,
		data: data as Record<string, unknown> | null,
		deleted,
		changedAt: Math.min(Math.floor(r.changedAt), now + 5 * 60 * 1000),
	};
}

/** Upserts [changes] in one transaction; older writes than what's stored are ignored. */
export async function applyChanges(userId: string, changes: Change[]): Promise<void> {
	if (!changes.length) return;
	const d = await db();
	const statement = d.prepare(
		`INSERT INTO sync_records (user_id, collection, sync_id, data, deleted, changed_at, rev)
		 VALUES (?1, ?2, ?3, ?4, ?5, ?6, (SELECT COALESCE(MAX(rev), 0) + 1 FROM sync_records WHERE user_id = ?1))
		 ON CONFLICT (user_id, collection, sync_id) DO UPDATE SET
		   data = excluded.data, deleted = excluded.deleted, changed_at = excluded.changed_at, rev = excluded.rev
		 WHERE excluded.changed_at >= sync_records.changed_at`,
	);
	await d.batch(
		changes.map((c) =>
			statement.bind(userId, c.collection, c.syncId, c.data ? JSON.stringify(c.data) : null, c.deleted ? 1 : 0, c.changedAt),
		),
	);
}

function toChange(row: RecordRow): Change {
	return {
		collection: row.collection,
		syncId: row.sync_id,
		data: row.data ? (JSON.parse(row.data) as Record<string, unknown>) : null,
		deleted: row.deleted === 1,
		changedAt: row.changed_at,
	};
}

/** `GET /api/web/data`: the live records of the signed-in user's space, for the web panel. */
export async function handleWebData(user: User): Promise<Response> {
	const space = await spaceOf(user.id);
	const d = await db();
	const { results } = await d
		.prepare(
			`SELECT collection, sync_id, data, deleted, changed_at, rev FROM sync_records
			 WHERE user_id = ? AND deleted = 0`,
		)
		.bind(space)
		.all<RecordRow>();
	const grouped: Record<Collection, Array<{ syncId: string; changedAt: number } & Record<string, unknown>>> = {
		pantry: [],
		shopping: [],
		recipes: [],
		mealplan: [],
	};
	for (const row of results) {
		grouped[row.collection]?.push({ ...JSON.parse(row.data ?? "{}"), syncId: row.sync_id, changedAt: row.changed_at });
	}
	return Response.json({ user, ...grouped });
}

/** `POST /api/web/records`: one edit from the web panel, stamped with the server's time. */
export async function handleWebWrite(request: Request, user: User): Promise<Response> {
	if (request.method !== "POST") return error(405, "Method not allowed");
	const body = await readJson<Record<string, unknown>>(request, MAX_RECORD_BYTES + 1024);
	const now = Date.now();
	const change = body && parseChange({ ...body, syncId: body.syncId ?? crypto.randomUUID(), changedAt: now }, now);
	if (!change) return error(400, "Invalid record");
	await applyChanges(await spaceOf(user.id), [change]);
	return Response.json({ syncId: change.syncId, changedAt: change.changedAt });
}

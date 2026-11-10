import { loadPrevious, recordActivity } from "./activity.ts";
import { db } from "./db.ts";
import type { User } from "./auth.ts";
import { describe, spaceOf } from "./household.ts";
import { error, readJson, sha256Hex } from "./http.ts";
import { notifySpaceChanged } from "./push.ts";

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
	settings?: unknown;
}

/** Household-wide settings; the app's FreshnessThresholds bounds. */
export interface SpaceSettings {
	criticalDays: number;
	changedAt: number;
}
const MIN_CRITICAL_DAYS = 1;
const MAX_CRITICAL_DAYS = 4;

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
	const session = (request.headers.get("authorization") ?? "").slice("Bearer ".length).trim();
	const sessionHash = await sha256Hex(session);
	await applyChanges(space, changes, user.id, sessionHash);
	const settingsIn = parseSettings(body.settings, Date.now());
	if (settingsIn && (await saveSettings(space, settingsIn))) {
		await notifySpaceChanged(space, user.id, sessionHash, false).catch((e) => console.error("push", e));
	}

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
		settings: await loadSettings(space),
		cursor: page.length ? page[page.length - 1].rev : cursor,
		hasMore,
		records: page.map(toChange),
	});
}

function parseSettings(raw: unknown, now: number): SpaceSettings | null {
	if (typeof raw !== "object" || raw === null) return null;
	const r = raw as Record<string, unknown>;
	const days = r.criticalDays;
	if (typeof days !== "number" || !Number.isInteger(days) || days < MIN_CRITICAL_DAYS || days > MAX_CRITICAL_DAYS) return null;
	if (typeof r.changedAt !== "number" || !Number.isFinite(r.changedAt)) return null;
	return { criticalDays: days, changedAt: Math.min(Math.floor(r.changedAt), now + 5 * 60 * 1000) };
}

/** Stores [settings] unless the space has newer ones; returns whether they changed. */
async function saveSettings(space: string, settings: SpaceSettings): Promise<boolean> {
	const d = await db();
	const result = await d
		.prepare(
			`INSERT INTO space_settings (space, critical_days, changed_at) VALUES (?, ?, ?)
			 ON CONFLICT (space) DO UPDATE SET critical_days = excluded.critical_days, changed_at = excluded.changed_at
			 WHERE excluded.changed_at > space_settings.changed_at`,
		)
		.bind(space, settings.criticalDays, settings.changedAt)
		.run();
	return (result.meta?.changes ?? 0) > 0;
}

/** The space's shared settings, `null` while nobody has changed them (the app keeps its own). */
export async function loadSettings(space: string): Promise<SpaceSettings | null> {
	const d = await db();
	const row = await d
		.prepare("SELECT critical_days, changed_at FROM space_settings WHERE space = ?")
		.bind(space)
		.first<{ critical_days: number; changed_at: number }>();
	return row ? { criticalDays: row.critical_days, changedAt: row.changed_at } : null;
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

/**
 * Upserts [changes] in one transaction; older writes than what's stored are ignored. With an
 * [actorId], what they did is added to the household's activity feed (activity.ts), and the
 * space's other devices (all but session [sourceSession]) are told to sync (push.ts).
 */
export async function applyChanges(
	userId: string,
	changes: Change[],
	actorId?: string,
	sourceSession: string | null = null,
): Promise<void> {
	if (!changes.length) return;
	const previous = actorId ? await loadPrevious(userId, changes) : null;
	const d = await db();
	// Before the upsert: only a record new to the space gets an author, so editing someone
	// else's item never takes it over.
	const author = d.prepare(
		`INSERT OR IGNORE INTO record_authors (space, collection, sync_id, author_id)
		 SELECT ?1, ?2, ?3, ?4
		 WHERE NOT EXISTS (SELECT 1 FROM sync_records WHERE user_id = ?1 AND collection = ?2 AND sync_id = ?3)`,
	);
	const authorStatements = changes
		.filter((c) => !c.deleted)
		.map((c) => author.bind(userId, c.collection, c.syncId, actorId ?? userId));
	const statement = d.prepare(
		`INSERT INTO sync_records (user_id, collection, sync_id, data, deleted, changed_at, rev)
		 VALUES (?1, ?2, ?3, ?4, ?5, ?6, (SELECT COALESCE(MAX(rev), 0) + 1 FROM sync_records WHERE user_id = ?1))
		 ON CONFLICT (user_id, collection, sync_id) DO UPDATE SET
		   data = excluded.data, deleted = excluded.deleted, changed_at = excluded.changed_at, rev = excluded.rev
		 WHERE excluded.changed_at >= sync_records.changed_at`,
	);
	await d.batch([
		...authorStatements,
		...changes.map((c) =>
			statement.bind(userId, c.collection, c.syncId, c.data ? JSON.stringify(c.data) : null, c.deleted ? 1 : 0, c.changedAt),
		),
	]);
	const news = actorId && previous ? await recordActivity(userId, actorId, changes, previous) : false;
	// A push failure must not fail the write that caused it.
	if (actorId) await notifySpaceChanged(userId, actorId, sourceSession, news).catch((e) => console.error("push", e));
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

/**
 * `GET /api/web/data`: the live records of the signed-in user's space for the web panel, with
 * the household and, in a shared space, who added each record (`addedBy`, an email).
 */
export async function handleWebData(user: User): Promise<Response> {
	const space = await spaceOf(user.id);
	const d = await db();
	const household = await describe(user);
	const authors = new Map<string, string>();
	if (household.role !== "none") {
		const { results: rows } = await d
			.prepare(
				`SELECT record_authors.collection AS collection, record_authors.sync_id AS sync_id, users.email AS email
				 FROM record_authors JOIN users ON users.id = record_authors.author_id
				 WHERE record_authors.space = ?`,
			)
			.bind(space)
			.all<{ collection: string; sync_id: string; email: string }>();
		for (const r of rows) authors.set(`${r.collection}/${r.sync_id}`, r.email);
		// No author row means the household's owner.
		const owner = household.members.find((m) => m.isOwner)?.email;
		if (owner) authors.set("", owner);
	}
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
		const addedBy = authors.size ? (authors.get(`${row.collection}/${row.sync_id}`) ?? authors.get("")) : undefined;
		grouped[row.collection]?.push({ ...JSON.parse(row.data ?? "{}"), syncId: row.sync_id, changedAt: row.changed_at, addedBy });
	}
	return Response.json({ user, household, settings: await loadSettings(space), ...grouped });
}

/**
 * `POST /api/web/author` `{collection, syncId, email}`: the household owner corrects who added a
 * record, e.g. one from before authors were kept; it then goes with that person if they leave.
 */
export async function handleSetAuthor(request: Request, user: User): Promise<Response> {
	if (request.method !== "POST") return error(405, "Method not allowed");
	const body = await readJson<Record<string, unknown>>(request, 2048);
	const collection = body?.collection as Collection;
	const syncId = typeof body?.syncId === "string" ? body.syncId : "";
	const email = typeof body?.email === "string" ? body.email.trim().toLowerCase() : "";
	if (!COLLECTIONS.includes(collection) || !SYNC_ID.test(syncId) || !email) return error(400, "Invalid request");
	const household = await describe(user);
	if (household.role !== "owner") return error(403, "Only the household owner can change this");
	if (!household.members.some((m) => m.email.toLowerCase() === email)) return error(400, "Not a member of this household");
	const d = await db();
	const author = await d.prepare("SELECT id FROM users WHERE email = ?").bind(email).first<{ id: string }>();
	if (!author) return error(400, "Not a member of this household");
	await d
		.prepare(
			`INSERT INTO record_authors (space, collection, sync_id, author_id) VALUES (?, ?, ?, ?)
			 ON CONFLICT (space, collection, sync_id) DO UPDATE SET author_id = excluded.author_id`,
		)
		.bind(user.id, collection, syncId, author.id)
		.run();
	return Response.json({ ok: true });
}

/** `POST /api/web/records`: one edit from the web panel, stamped with the server's time. */
export async function handleWebWrite(request: Request, user: User): Promise<Response> {
	if (request.method !== "POST") return error(405, "Method not allowed");
	const body = await readJson<Record<string, unknown>>(request, MAX_RECORD_BYTES + 1024);
	const now = Date.now();
	const change = body && parseChange({ ...body, syncId: body.syncId ?? crypto.randomUUID(), changedAt: now }, now);
	if (!change) return error(400, "Invalid record");
	await applyChanges(await spaceOf(user.id), [change], user.id);
	return Response.json({ syncId: change.syncId, changedAt: change.changedAt });
}

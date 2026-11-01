import { env } from "cloudflare:workers";
import { ACTIVITY_RETENTION_DAYS } from "./activity.ts";

/**
 * D1 schema. Every statement is idempotent and runs once per isolate before the first query,
 * so a fresh database (auto-provisioned by `cf deploy`) needs no separate migration step.
 * Additive changes only: append new statements, never edit an existing one.
 */
const SCHEMA = [
	// One row per Gemini call through the proxy; feeds the admin dashboard.
	`CREATE TABLE IF NOT EXISTS usage_events (
		id INTEGER PRIMARY KEY AUTOINCREMENT,
		ts INTEGER NOT NULL,
		install_id TEXT,
		feature TEXT,
		model TEXT NOT NULL,
		status INTEGER NOT NULL,
		latency_ms INTEGER NOT NULL,
		prompt_tokens INTEGER,
		output_tokens INTEGER,
		country TEXT,
		app_version TEXT
	)`,
	`CREATE INDEX IF NOT EXISTS usage_events_ts ON usage_events (ts)`,
	`CREATE INDEX IF NOT EXISTS usage_events_install ON usage_events (install_id, ts)`,

	`CREATE TABLE IF NOT EXISTS users (
		id TEXT PRIMARY KEY,
		email TEXT NOT NULL UNIQUE COLLATE NOCASE,
		password_hash TEXT NOT NULL,
		password_salt TEXT NOT NULL,
		password_iterations INTEGER NOT NULL,
		created_at INTEGER NOT NULL,
		last_seen_at INTEGER
	)`,

	// Only the SHA-256 of a session token is stored; the token itself lives in the client.
	`CREATE TABLE IF NOT EXISTS sessions (
		token_hash TEXT PRIMARY KEY,
		user_id TEXT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
		client TEXT NOT NULL,
		install_id TEXT,
		created_at INTEGER NOT NULL,
		expires_at INTEGER NOT NULL
	)`,
	`CREATE INDEX IF NOT EXISTS sessions_user ON sessions (user_id)`,
	`CREATE INDEX IF NOT EXISTS sessions_expiry ON sessions (expires_at)`,

	/**
	 * Synced app data. `data` is the record as JSON, opaque to the server apart from the web
	 * panel. `changed_at` is the client's change time and decides conflicts (last write wins);
	 * `rev` is a per-user counter assigned here, which clients use as their pull cursor.
	 */
	`CREATE TABLE IF NOT EXISTS sync_records (
		user_id TEXT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
		collection TEXT NOT NULL,
		sync_id TEXT NOT NULL,
		data TEXT,
		deleted INTEGER NOT NULL DEFAULT 0,
		changed_at INTEGER NOT NULL,
		rev INTEGER NOT NULL,
		PRIMARY KEY (user_id, collection, sync_id)
	)`,
	`CREATE INDEX IF NOT EXISTS sync_records_rev ON sync_records (user_id, rev)`,

	// Brute-force guard for the login forms, keyed by "<scope>:<ip>".
	`CREATE TABLE IF NOT EXISTS login_failures (
		key TEXT PRIMARY KEY,
		count INTEGER NOT NULL,
		window_start INTEGER NOT NULL
	)`,

	// Households (household.ts): a member syncs in the owner's data space. One household per member.
	`CREATE TABLE IF NOT EXISTS household_members (
		member_id TEXT PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
		owner_id TEXT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
		joined_at INTEGER NOT NULL
	)`,
	`CREATE INDEX IF NOT EXISTS household_members_owner ON household_members (owner_id)`,
	`CREATE TABLE IF NOT EXISTS household_invites (
		code TEXT PRIMARY KEY,
		owner_id TEXT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
		expires_at INTEGER NOT NULL
	)`,

	// Successful AI calls per UTC day, per account ("u:<id>") or install ("i:<id>"); see quota.ts.
	`CREATE TABLE IF NOT EXISTS ai_usage (
		day TEXT NOT NULL,
		subject TEXT NOT NULL,
		feature TEXT NOT NULL,
		count INTEGER NOT NULL,
		PRIMARY KEY (day, subject, feature)
	)`,

	// Play subscriptions (billing.ts); only the SHA-256 of the purchase token is stored.
	`CREATE TABLE IF NOT EXISTS subscriptions (
		token_hash TEXT PRIMARY KEY,
		user_id TEXT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
		product_id TEXT NOT NULL,
		state TEXT NOT NULL,
		expires_at INTEGER NOT NULL,
		updated_at INTEGER NOT NULL
	)`,
	`CREATE INDEX IF NOT EXISTS subscriptions_user ON subscriptions (user_id, expires_at)`,

	// Household activity feed (activity.ts); `space` is the household owner's id.
	`CREATE TABLE IF NOT EXISTS household_activity (
		id INTEGER PRIMARY KEY AUTOINCREMENT,
		space TEXT NOT NULL,
		actor_id TEXT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
		action TEXT NOT NULL,
		name TEXT NOT NULL,
		emoji TEXT,
		ts INTEGER NOT NULL
	)`,
	`CREATE INDEX IF NOT EXISTS household_activity_space ON household_activity (space, id)`,

	// "Forgot password" links (auth.ts); only the SHA-256 of the emailed token is stored.
	`CREATE TABLE IF NOT EXISTS password_resets (
		token_hash TEXT PRIMARY KEY,
		user_id TEXT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
		expires_at INTEGER NOT NULL
	)`,
];

let ready: Promise<void> | undefined;

/** The database, with the schema applied. */
export async function db(): Promise<D1Database> {
	ready ??= env.DB.batch(SCHEMA.map((sql) => env.DB.prepare(sql)))
		.then(() => undefined)
		.catch((e) => {
			// Let the next request try again instead of caching the failure for the isolate's life.
			ready = undefined;
			throw e;
		});
	await ready;
	return env.DB;
}

/** How long rows are kept; see `cleanup`. */
export const USAGE_RETENTION_DAYS = 180;
const DAY_MS = 24 * 60 * 60 * 1000;

/** Daily housekeeping (cron): old usage rows, sessions, login counters, tombstones, invites, quota days, activity, reset links. */
export async function cleanup(now = Date.now()): Promise<void> {
	const d = await db();
	await d.batch([
		d.prepare("DELETE FROM usage_events WHERE ts < ?").bind(now - USAGE_RETENTION_DAYS * DAY_MS),
		d.prepare("DELETE FROM sessions WHERE expires_at < ?").bind(now),
		d.prepare("DELETE FROM login_failures WHERE window_start < ?").bind(now - DAY_MS),
		// A device offline for a year re-uploads everything on its next sync anyway.
		d.prepare("DELETE FROM sync_records WHERE deleted = 1 AND changed_at < ?").bind(now - 365 * DAY_MS),
		d.prepare("DELETE FROM household_invites WHERE expires_at < ?").bind(now),
		// Days are ISO strings, so they compare as text.
		d.prepare("DELETE FROM ai_usage WHERE day < ?").bind(new Date(now - 40 * DAY_MS).toISOString().slice(0, 10)),
		d.prepare("DELETE FROM household_activity WHERE ts < ?").bind(now - ACTIVITY_RETENTION_DAYS * DAY_MS),
		d.prepare("DELETE FROM password_resets WHERE expires_at < ?").bind(now),
	]);
}

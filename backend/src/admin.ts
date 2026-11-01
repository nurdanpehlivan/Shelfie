import { env } from "cloudflare:workers";
import { deleteUserData, recordFailure, tooManyFailures } from "./auth.ts";
import { db } from "./db.ts";
import { clientIp, cookie, error, getCookie, isSameOrigin, readJson, safeEqual, toHex } from "./http.ts";

/**
 * Operator dashboard at `/admin`, guarded by the ADMIN_PASSWORD secret.
 *
 * The session cookie is `<expiry>.<HMAC(expiry)>` keyed by the password itself, so there's no
 * session table and changing the password signs everyone out.
 */

const ADMIN_COOKIE = "shelfie_admin";
const ADMIN_SESSION_SECONDS = 12 * 60 * 60;
const DAY_MS = 24 * 60 * 60 * 1000;
const MAX_RANGE_DAYS = 180;

async function sign(value: string): Promise<string> {
	const key = await crypto.subtle.importKey(
		"raw",
		new TextEncoder().encode(`shelfie-admin:${env.ADMIN_PASSWORD}`),
		{ name: "HMAC", hash: "SHA-256" },
		false,
		["sign"],
	);
	return toHex(new Uint8Array(await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(value))));
}

export async function isAdmin(request: Request): Promise<boolean> {
	if (!env.ADMIN_PASSWORD) return false;
	const value = getCookie(request, ADMIN_COOKIE);
	if (!value) return false;
	const [expiry, signature] = value.split(".");
	if (!expiry || !signature || Number(expiry) < Date.now()) return false;
	return safeEqual(signature, await sign(expiry));
}

/** `POST /admin/api/login` and `/admin/api/logout`. */
export async function handleAdminAuth(request: Request, action: string): Promise<Response> {
	if (request.method !== "POST") return error(405, "Method not allowed");
	if (!isSameOrigin(request)) return error(403, "Forbidden");

	if (action === "logout") {
		return Response.json({ ok: true }, { headers: { "set-cookie": cookie(ADMIN_COOKIE, "", "/admin", 0) } });
	}
	if (!env.ADMIN_PASSWORD) return error(503, "ADMIN_PASSWORD secret is not set");

	const failureKey = `admin:${clientIp(request)}`;
	if (await tooManyFailures(failureKey)) return error(429, "Çok fazla hatalı deneme. Biraz sonra tekrar dene.");

	const body = await readJson<{ password?: unknown }>(request, 1024);
	if (!safeEqual(typeof body?.password === "string" ? body.password : null, env.ADMIN_PASSWORD)) {
		await recordFailure(failureKey);
		return error(401, "Şifre hatalı.");
	}
	const expiry = String(Date.now() + ADMIN_SESSION_SECONDS * 1000);
	return Response.json(
		{ ok: true },
		{ headers: { "set-cookie": cookie(ADMIN_COOKIE, `${expiry}.${await sign(expiry)}`, "/admin", ADMIN_SESSION_SECONDS) } },
	);
}

/**
 * `GET /admin/api/stats?days=30&tz=180`: everything the dashboard shows for the last [days]
 * days, bucketed into days of the viewer's timezone ([tz] = minutes east of UTC).
 */
export async function handleStats(url: URL): Promise<Response> {
	const days = clampInt(url.searchParams.get("days"), 1, MAX_RANGE_DAYS, 30);
	const tzMs = clampInt(url.searchParams.get("tz"), -14 * 60, 14 * 60, 0) * 60 * 1000;
	const now = Date.now();
	// Start of the viewer's local day, [days - 1] days ago.
	const todayStart = Math.floor((now + tzMs) / DAY_MS) * DAY_MS - tzMs;
	const since = todayStart - (days - 1) * DAY_MS;

	const d = await db();
	const q = (sql: string, ...params: unknown[]) => d.prepare(sql).bind(...params);
	const [totals, daily, models, features, statuses, countries, versions, recentErrors, userTotals, users, latencyRank] =
		await d.batch([
			q(
				`SELECT COUNT(*) AS requests,
				        SUM(status BETWEEN 200 AND 299) AS ok,
				        SUM(status = 429) AS rate_limited,
				        COUNT(DISTINCT install_id) AS installs,
				        AVG(CASE WHEN status BETWEEN 200 AND 299 THEN latency_ms END) AS avg_latency,
				        SUM(prompt_tokens) AS prompt_tokens,
				        SUM(output_tokens) AS output_tokens
				 FROM usage_events WHERE ts >= ?`,
				since,
			),
			q(
				`SELECT (ts + CAST(?1 AS INTEGER)) / ${DAY_MS} AS day,
				        COUNT(*) AS requests,
				        SUM(status BETWEEN 200 AND 299) AS ok,
				        COUNT(DISTINCT install_id) AS installs,
				        SUM(COALESCE(prompt_tokens, 0) + COALESCE(output_tokens, 0)) AS tokens
				 FROM usage_events WHERE ts >= ?2 GROUP BY day ORDER BY day`,
				tzMs,
				since,
			),
			q(
				`SELECT model AS name, COUNT(*) AS requests, SUM(status BETWEEN 200 AND 299) AS ok,
				        AVG(CASE WHEN status BETWEEN 200 AND 299 THEN latency_ms END) AS avg_latency,
				        SUM(COALESCE(prompt_tokens, 0) + COALESCE(output_tokens, 0)) AS tokens
				 FROM usage_events WHERE ts >= ? GROUP BY model ORDER BY requests DESC`,
				since,
			),
			q(
				`SELECT COALESCE(feature, 'bilinmiyor') AS name, COUNT(*) AS requests, SUM(status BETWEEN 200 AND 299) AS ok
				 FROM usage_events WHERE ts >= ? GROUP BY name ORDER BY requests DESC`,
				since,
			),
			q(
				`SELECT status AS name, COUNT(*) AS requests FROM usage_events WHERE ts >= ?
				 GROUP BY status ORDER BY requests DESC`,
				since,
			),
			q(
				`SELECT COALESCE(country, '??') AS name, COUNT(*) AS requests, COUNT(DISTINCT install_id) AS installs
				 FROM usage_events WHERE ts >= ? GROUP BY name ORDER BY requests DESC LIMIT 10`,
				since,
			),
			q(
				`SELECT COALESCE(app_version, 'bilinmiyor') AS name, COUNT(DISTINCT install_id) AS installs, COUNT(*) AS requests
				 FROM usage_events WHERE ts >= ? GROUP BY name ORDER BY installs DESC LIMIT 10`,
				since,
			),
			q(
				`SELECT ts, model, feature, status, latency_ms, country, app_version FROM usage_events
				 WHERE ts >= ? AND status NOT BETWEEN 200 AND 299 ORDER BY ts DESC LIMIT 25`,
				since,
			),
			q(
				`SELECT (SELECT COUNT(*) FROM users) AS users,
				        (SELECT COUNT(*) FROM users WHERE created_at >= ?1) AS new_users,
				        (SELECT COUNT(DISTINCT user_id) FROM sessions WHERE expires_at > ?2) AS signed_in_users,
				        (SELECT COUNT(*) FROM sync_records WHERE deleted = 0) AS records`,
				since,
				now,
			),
			q(
				`SELECT users.id, users.email, users.created_at, users.last_seen_at,
				        (SELECT COUNT(*) FROM sync_records r WHERE r.user_id = users.id AND r.deleted = 0) AS records,
				        (SELECT MAX(changed_at) FROM sync_records r WHERE r.user_id = users.id) AS last_sync
				 FROM users ORDER BY created_at DESC LIMIT 100`,
			),
			// SQLite has no percentile; the 95th-percentile row is fetched by rank below.
			q(
				`SELECT COUNT(*) AS n FROM usage_events WHERE ts >= ? AND status BETWEEN 200 AND 299`,
				since,
			),
		]);

	const okCount = (latencyRank.results[0] as { n: number }).n;
	const p95 = okCount
		? await q(
				`SELECT latency_ms FROM usage_events WHERE ts >= ? AND status BETWEEN 200 AND 299
				 ORDER BY latency_ms LIMIT 1 OFFSET ?`,
				since,
				Math.min(okCount - 1, Math.floor(okCount * 0.95)),
			).first<number>("latency_ms")
		: null;

	return Response.json({
		range: { days, since, tzMinutes: tzMs / 60000, generatedAt: now },
		totals: { ...(totals.results[0] as object), p95_latency: p95, ...(userTotals.results[0] as object) },
		daily: daily.results,
		models: models.results,
		features: features.results,
		statuses: statuses.results,
		countries: countries.results,
		versions: versions.results,
		recentErrors: recentErrors.results,
		users: users.results,
	});
}

/** `POST /admin/api/users/delete` `{ id }`: removes an account and its data (abuse handling). */
export async function handleDeleteUser(request: Request): Promise<Response> {
	if (request.method !== "POST") return error(405, "Method not allowed");
	if (!isSameOrigin(request)) return error(403, "Forbidden");
	const body = await readJson<{ id?: unknown }>(request, 1024);
	if (typeof body?.id !== "string") return error(400, "Missing id");
	await deleteUserData(body.id);
	return Response.json({ ok: true });
}

function clampInt(raw: string | null, min: number, max: number, fallback: number): number {
	const n = Number(raw);
	return Number.isFinite(n) ? Math.min(max, Math.max(min, Math.round(n))) : fallback;
}

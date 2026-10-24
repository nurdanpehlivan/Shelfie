import { env } from "cloudflare:workers";
import { authenticate } from "./auth.ts";
import { db } from "./db.ts";
import { spaceOf, usersInSpace } from "./household.ts";
import { APP_TOKEN_HEADER, INSTALL_HEADER } from "./proxy.ts";
import { clientIp, error, safeEqual } from "./http.ts";

/**
 * Daily AI allowance per plan. Free users get a few photo scans and recipes a day; Premium
 * (an active Play subscription anywhere in the household) gets a fair-use cap. Only successful
 * calls count, so a Gemini outage or a failed retry never eats into the allowance.
 *
 * Signed-in users are counted per account (across their phones); others per install.
 * Days are UTC days; the app shows the reset time in local time.
 */

export type Feature = "scan" | "recipe" | "recipe_daily" | "translate";
const FEATURES: Feature[] = ["scan", "recipe", "recipe_daily", "translate"];

export interface Subject {
	key: string;
	userId: string | null;
	premiumUntil: number | null;
}

function intSetting(value: string | undefined, fallback: number): number {
	const n = Number(value);
	return Number.isInteger(n) && n >= 0 ? n : fallback;
}

/**
 * Limits per feature; the morning freshness recipe (background) gets one of its own. Translating
 * item names after a language change is small and automatic, so it never eats the scan allowance.
 */
export function limitsFor(premium: boolean): Record<Feature, number> {
	const premiumLimit = intSetting(env.PREMIUM_DAILY_LIMIT, 50);
	return {
		scan: premium ? premiumLimit : intSetting(env.FREE_DAILY_SCANS, 3),
		recipe: premium ? premiumLimit : intSetting(env.FREE_DAILY_RECIPES, 3),
		recipe_daily: 1,
		translate: premium ? 200 : 30,
	};
}

export function parseFeature(raw: string | null): Feature {
	return FEATURES.includes(raw as Feature) ? (raw as Feature) : "recipe";
}

export function utcDay(now = Date.now()): string {
	return new Date(now).toISOString().slice(0, 10);
}

export function nextUtcMidnight(now = Date.now()): number {
	const d = new Date(now);
	return Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), d.getUTCDate() + 1);
}

/** Latest expiry of an active subscription held by anyone in [userId]'s household. */
export async function premiumUntil(userId: string, now = Date.now()): Promise<number | null> {
	const space = await spaceOf(userId);
	const users = await usersInSpace(space);
	const d = await db();
	const placeholders = users.map(() => "?").join(",");
	const row = await d
		.prepare(`SELECT MAX(expires_at) AS until FROM subscriptions WHERE user_id IN (${placeholders}) AND expires_at > ?`)
		.bind(...users, now)
		.first<{ until: number | null }>();
	return row?.until ?? null;
}

/** Who a proxy call is charged to. An invalid bearer token falls back to the install. */
export async function subjectOf(request: Request): Promise<Subject> {
	const user = request.headers.has("authorization") ? await authenticate(request, "app") : null;
	if (user) return { key: `u:${user.id}`, userId: user.id, premiumUntil: await premiumUntil(user.id) };
	const install = request.headers.get(INSTALL_HEADER)?.slice(0, 64);
	return { key: install ? `i:${install}` : `ip:${clientIp(request)}`, userId: null, premiumUntil: null };
}

export async function usedToday(subjectKey: string): Promise<Record<Feature, number>> {
	const d = await db();
	const { results } = await d
		.prepare("SELECT feature, count FROM ai_usage WHERE day = ? AND subject = ?")
		.bind(utcDay(), subjectKey)
		.all<{ feature: Feature; count: number }>();
	const used: Record<Feature, number> = { scan: 0, recipe: 0, recipe_daily: 0, translate: 0 };
	for (const r of results) if (r.feature in used) used[r.feature] = r.count;
	return used;
}

/** `null` if the call may go ahead, else the 402 to return. */
export async function checkQuota(subject: Subject, feature: Feature): Promise<Response | null> {
	const premium = subject.premiumUntil !== null;
	const limit = limitsFor(premium)[feature];
	const used = (await usedToday(subject.key))[feature];
	if (used < limit) return null;
	return Response.json(
		{
			error: {
				code: 402,
				status: "QUOTA_EXCEEDED",
				message: "Daily AI allowance used up",
				details: { feature, limit, plan: premium ? "premium" : "free", resetsAt: nextUtcMidnight() },
			},
		},
		{ status: 402 },
	);
}

/** Never throws: a lost increment only means one free call. */
export async function recordUse(subjectKey: string, feature: Feature): Promise<void> {
	try {
		const d = await db();
		await d
			.prepare(
				`INSERT INTO ai_usage (day, subject, feature, count) VALUES (?, ?, ?, 1)
				 ON CONFLICT (day, subject, feature) DO UPDATE SET count = count + 1`,
			)
			.bind(utcDay(), subjectKey, feature)
			.run();
	} catch (e) {
		console.error("quota count failed", e);
	}
}

/** `GET /api/quota` (app token, optional bearer): the plan and today's usage, for the app. */
export async function handleQuota(request: Request): Promise<Response> {
	if (!safeEqual(request.headers.get(APP_TOKEN_HEADER), env.APP_TOKEN)) return error(401, "Unauthorized");
	const subject = await subjectOf(request);
	const premium = subject.premiumUntil !== null;
	const limits = limitsFor(premium);
	const used = await usedToday(subject.key);
	return Response.json({
		plan: premium ? "premium" : "free",
		premiumUntil: subject.premiumUntil,
		resetsAt: nextUtcMidnight(),
		scan: { used: used.scan, limit: limits.scan },
		recipe: { used: used.recipe, limit: limits.recipe },
	});
}

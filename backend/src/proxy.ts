import { env } from "cloudflare:workers";
import { db } from "./db.ts";
import { clientIp, error, safeEqual } from "./http.ts";

/**
 * Thin proxy in front of Gemini so the Gemini API key never ships inside the app.
 *
 * Mirrors Gemini's own path (`POST /v1beta/models/{model}:generateContent`), so the app only
 * swaps the base URL and sends its app token instead of the Gemini key. Status codes and error
 * bodies are passed through unchanged; the app relies on them (429 + "PerDay", 503 retries).
 *
 * Every call is logged to D1 (no prompt or photo content, only counts) for the admin dashboard.
 */

const GEMINI_BASE_URL = "https://generativelanguage.googleapis.com";
export const APP_TOKEN_HEADER = "x-shelfie-token";
/** Random id per app install, so the dashboard can count active installs without accounts. */
const INSTALL_HEADER = "x-shelfie-install";
/** What the call is for: "scan", "recipe", … */
const FEATURE_HEADER = "x-shelfie-feature";
const VERSION_HEADER = "x-shelfie-version";

/** Compressed photos are well under 1 MB; anything near this is not a Shelfie request. */
const MAX_BODY_BYTES = 6 * 1024 * 1024;

export const PROXY_ROUTE = /^\/v1beta\/models\/([A-Za-z0-9.\-]+):generateContent$/;

interface UsageMetadata {
	promptTokenCount?: number;
	candidatesTokenCount?: number;
	thoughtsTokenCount?: number;
}

export async function handleProxy(request: Request, model: string, ctx: ExecutionContext): Promise<Response> {
	if (request.method !== "POST") return error(405, "Method not allowed");
	if (!safeEqual(request.headers.get(APP_TOKEN_HEADER), env.APP_TOKEN)) return error(401, "Unauthorized");
	if (!allowedModels().has(model)) return error(400, `Model not allowed: ${model}`);

	const { success } = await env.RATE_LIMITER.limit({ key: clientIp(request) });
	if (!success) {
		ctx.waitUntil(logUsage(request, model, 429, 0, undefined));
		return error(429, "Too many requests to the Shelfie proxy");
	}

	const declaredLength = Number(request.headers.get("content-length") ?? 0);
	if (declaredLength > MAX_BODY_BYTES) return error(413, "Request too large");
	const body = await request.arrayBuffer();
	if (body.byteLength > MAX_BODY_BYTES) return error(413, "Request too large");

	const started = Date.now();
	const upstream = await fetch(`${GEMINI_BASE_URL}/v1beta/models/${model}:generateContent`, {
		method: "POST",
		headers: {
			"content-type": "application/json",
			"x-goog-api-key": env.GEMINI_API_KEY,
		},
		body,
	});
	// Buffered rather than streamed so the token counts can be read; answers are a few KB.
	const text = await upstream.text();
	const latency = Date.now() - started;

	let usage: UsageMetadata | undefined;
	if (upstream.ok) {
		try {
			usage = (JSON.parse(text) as { usageMetadata?: UsageMetadata }).usageMetadata;
		} catch {
			// Logged without token counts; the app reports the malformed body to the user itself.
		}
	}
	ctx.waitUntil(logUsage(request, model, upstream.status, latency, usage));

	return new Response(text, {
		status: upstream.status,
		headers: { "content-type": upstream.headers.get("content-type") ?? "application/json" },
	});
}

function allowedModels(): Set<string> {
	return new Set(
		env.ALLOWED_MODELS.split(",")
			.map((m) => m.trim())
			.filter(Boolean),
	);
}

/** Never throws: losing a log row must not fail or slow down the user's request. */
async function logUsage(
	request: Request,
	model: string,
	status: number,
	latencyMs: number,
	usage: UsageMetadata | undefined,
): Promise<void> {
	try {
		const header = (name: string, max: number) => request.headers.get(name)?.slice(0, max) || null;
		const country = (request.cf?.country as string | undefined) ?? null;
		const output =
			usage?.candidatesTokenCount === undefined && usage?.thoughtsTokenCount === undefined
				? null
				: (usage?.candidatesTokenCount ?? 0) + (usage?.thoughtsTokenCount ?? 0);
		const d = await db();
		await d
			.prepare(
				`INSERT INTO usage_events (ts, install_id, feature, model, status, latency_ms, prompt_tokens, output_tokens, country, app_version)
				 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
			)
			.bind(
				Date.now(),
				header(INSTALL_HEADER, 64),
				header(FEATURE_HEADER, 32),
				model,
				status,
				latencyMs,
				usage?.promptTokenCount ?? null,
				output,
				country,
				header(VERSION_HEADER, 32),
			)
			.run();
	} catch (e) {
		console.error("usage log failed", e);
	}
}

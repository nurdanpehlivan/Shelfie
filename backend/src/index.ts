import { handleActivity } from "./activity.ts";
import { handleAdminAuth, handleDeleteUser, handleStats, isAdmin } from "./admin.ts";
import { authenticate, deleteAccount, handleAuth } from "./auth.ts";
import { handleBillingVerify } from "./billing.ts";
import { handleHousehold } from "./household.ts";
import { handleQuota } from "./quota.ts";
import { cleanup } from "./db.ts";
import { error, html, isSameOrigin } from "./http.ts";
import { LANDING_HTML } from "./landing.ts";
import { ADMIN_HTML } from "./pages/admin.ts";
import { PANEL_HTML } from "./pages/panel.ts";
import { joinPage, JOIN_ROUTE } from "./pages/join.ts";
import { PRIVACY_HTML } from "./pages/privacy.ts";
import { RESET_HTML } from "./pages/reset.ts";
import { handleProxy, PROXY_ROUTE } from "./proxy.ts";
import { handleForgot, handleReset } from "./reset.ts";
import { handleSync, handleWebData, handleWebWrite } from "./sync.ts";

/**
 * Routes:
 * - `POST /v1beta/models/{model}:generateContent`: Gemini proxy (app token), see proxy.ts
 * - `/api/auth/*`, `/api/sync`, `/api/account`: the app's account and sync API (bearer token)
 * - `/api/auth/forgot`, `/api/auth/reset` + `/reset`: "forgot password" by email, see reset.ts
 * - `/join/<code>`: landing page of a family invite link (opens the app or the Play Store)
 * - `/api/household/*`, `/api/billing/verify`: family sharing, its activity feed and Play subscriptions (bearer token)
 * - `GET /api/quota`: plan and today's AI usage (app token, optional bearer), see quota.ts
 * - `/panel` + `/api/web/*`: the user's web panel (session cookie)
 * - `/privacy`: the privacy policy (public)
 * - `/admin` + `/admin/api/*`: operator dashboard (ADMIN_PASSWORD)
 */
export default {
	async fetch(request, _env, ctx) {
		const url = new URL(request.url);
		const path = url.pathname.replace(/\/+$/, "") || "/";
		const isRead = request.method === "GET" || request.method === "HEAD";

		const proxy = PROXY_ROUTE.exec(path);
		if (proxy) return handleProxy(request, proxy[1], ctx);

		if (path === "/" && isRead) {
			return new Response(LANDING_HTML, {
				headers: { "content-type": "text/html; charset=utf-8", "cache-control": "public, max-age=300" },
			});
		}
		if (path === "/panel" && isRead) return html(PANEL_HTML);
		if (path === "/privacy" && isRead) return html(PRIVACY_HTML);
		if (path === "/reset" && isRead) return html(RESET_HTML);
		const join = JOIN_ROUTE.exec(path);
		if (join && isRead) return html(joinPage(join[1]));
		if (path === "/admin" && isRead) return html(ADMIN_HTML);

		try {
			return await route(request, url, path);
		} catch (e) {
			console.error(path, e);
			return error(500, "Internal error");
		}
	},

	async scheduled(_controller, _env, ctx) {
		ctx.waitUntil(cleanup());
	},
} satisfies ExportedHandler;

async function route(request: Request, url: URL, path: string): Promise<Response> {
	// App API (bearer token).
	const appAuth = /^\/api\/auth\/(register|login|logout)$/.exec(path);
	if (appAuth) return handleAuth(request, appAuth[1], "app");
	// Same for the app and the web panel: neither needs a session.
	if (path === "/api/auth/forgot" || path === "/api/web/auth/forgot") return handleForgot(request);
	if (path === "/api/auth/reset") return handleReset(request);
	if (path === "/api/quota" && request.method === "GET") return handleQuota(request);
	const household = /^\/api\/household(?:\/(invite|join|leave|remove|activity))?$/.exec(path);
	if (household || path === "/api/billing/verify") {
		const user = await authenticate(request, "app");
		if (!user) return error(401, "Unauthorized");
		if (household?.[1] === "activity") return request.method === "GET" ? handleActivity(user) : error(405, "Method not allowed");
		if (household) return handleHousehold(request, household[1] ?? "", user);
		return handleBillingVerify(request, user);
	}
	if (path === "/api/sync" || path === "/api/account" || path === "/api/me") {
		const user = await authenticate(request, "app");
		if (!user) return error(401, "Unauthorized");
		if (path === "/api/sync") return handleSync(request, user);
		if (path === "/api/me") return Response.json({ user });
		if (request.method !== "DELETE") return error(405, "Method not allowed");
		return deleteAccount(user);
	}

	// Web panel API (cookie).
	const webAuth = /^\/api\/web\/auth\/(register|login|logout)$/.exec(path);
	if (webAuth) return handleAuth(request, webAuth[1], "web");
	if (path.startsWith("/api/web/")) {
		const user = await authenticate(request, "web");
		if (!user) return error(401, "Unauthorized");
		if (request.method !== "GET" && !isSameOrigin(request)) return error(403, "Forbidden");
		if (path === "/api/web/data" && request.method === "GET") return handleWebData(user);
		if (path === "/api/web/records") return handleWebWrite(request, user);
		if (path === "/api/web/account" && request.method === "DELETE") return deleteAccount(user);
		return error(404, "Not found");
	}

	// Admin API.
	const adminAuth = /^\/admin\/api\/(login|logout)$/.exec(path);
	if (adminAuth) return handleAdminAuth(request, adminAuth[1]);
	if (path.startsWith("/admin/api/")) {
		if (!(await isAdmin(request))) return error(401, "Unauthorized");
		if (path === "/admin/api/stats" && request.method === "GET") return handleStats(url);
		if (path === "/admin/api/users/delete") return handleDeleteUser(request);
		return error(404, "Not found");
	}

	return error(404, "Not found");
}

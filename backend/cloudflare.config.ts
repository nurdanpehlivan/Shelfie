import { bindings, defineConfig, triggers } from "cf/config";
import * as entrypoint from "./src/index.ts" with { type: "cf-worker" };

export default defineConfig({
	worker: {
		name: "shelfie-gemini-proxy",
		compatibilityDate: "2026-10-06",
		entrypoint,
		env: {
			// Set with: cf workers secrets update GEMINI_API_KEY --worker shelfie-gemini-proxy
			GEMINI_API_KEY: bindings.secret(),
			// Shared with the app (local.properties SHELFIE_PROXY_TOKEN); rotate if it leaks.
			APP_TOKEN: bindings.secret(),
			// Password of the /admin dashboard; changing it signs every admin out.
			ADMIN_PASSWORD: bindings.secret(),
			// Usage log, accounts and synced data. Created on the first `cf deploy`; the schema
			// is applied by the Worker itself (src/db.ts).
			DB: bindings.d1({ name: "shelfie" }),
			// PBKDF2 rounds for user passwords. 20 000 fits the Free plan's 10 ms CPU per request;
			// on the Paid plan raise it to 100 000 (users are re-hashed on their next login).
			PASSWORD_ITERATIONS: bindings.text("20000"),
			// Keep in sync with GEMINI_MODEL / GEMINI_FALLBACK_MODELS in the app.
			ALLOWED_MODELS: bindings.text("gemini-3.8-flash,gemini-3.5-flash,gemini-3.5-flash-lite,gemini-3.1-flash-lite"),
			// Daily AI allowance (src/quota.ts). Premium is a live Play subscription in the household.
			FREE_DAILY_SCANS: bindings.text("3"),
			FREE_DAILY_RECIPES: bindings.text("3"),
			PREMIUM_DAILY_LIMIT: bindings.text("50"),
			// Play subscriptions (src/billing.ts). The secret is a service account's JSON key with
			// access to the app in Play Console; without it, purchases can't be verified (503).
			GOOGLE_SERVICE_ACCOUNT: bindings.secret(),
			PLAY_PACKAGE_NAME: bindings.text("com.nurdanpehlivan.shelfie"),
			// Comma-separated Play product ids that grant Premium; keep in sync with the app.
			PREMIUM_PRODUCT_IDS: bindings.text("frizzy_premium"),
			// "Forgot password" mail (src/reset.ts) through Resend: an API key and a sender on a
			// domain verified there, e.g. "Frizzy <noreply@frizzyapp.com>". Unset, resets answer 503.
			RESEND_API_KEY: bindings.secret(),
			MAIL_FROM: bindings.secret(),
			// Per client IP, per Cloudflare location. Also guards the login and register endpoints.
			RATE_LIMITER: bindings.rateLimit({
				namespace: "1001",
				simple: { limit: 20, period: 60 },
			}),
		},
		triggers: [
			// Daily housekeeping (src/db.ts cleanup), 03:17 UTC.
			triggers.scheduled({ schedule: "17 3 * * *" }),
		],
	},
});

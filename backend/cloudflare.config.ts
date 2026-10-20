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

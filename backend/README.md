# Frizzy backend

A Cloudflare Worker with a D1 database. It does four jobs:

| Path | What | Auth |
|---|---|---|
| `POST /v1beta/models/{model}:generateContent` | Gemini proxy: keeps the Gemini API key out of the app, checks the model allowlist and a per-IP rate limit, logs each call (counts only, no content) | `x-shelfie-token` (APP_TOKEN) |
| `/api/auth/*`, `/api/sync`, `/api/account` | Optional accounts and two-way sync of pantry, shopping list and recipe history | `Authorization: Bearer <session>` |
| `/panel` | Users' web panel: view and edit the synced pantry, shopping list and recipes | Session cookie |
| `/api/household/*` | Family sharing: invite codes, join, leave, remove members | `Authorization: Bearer <session>` |
| `GET /api/quota` | The caller's plan and today's AI usage | `x-shelfie-token`, optional bearer |
| `POST /api/billing/verify` | Checks a Play subscription purchase and grants Premium | `Authorization: Bearer <session>` |
| `/privacy` | Privacy policy (Turkish and English), linked from the Play listing | Public |
| `/admin` | Operator dashboard: requests, active installs, errors, models, tokens, users | ADMIN_PASSWORD |

URL: https://shelfie-gemini-proxy.shelfie-app.workers.dev/

## Deploy

```sh
cd backend
npm install
cf deploy
```

The first deploy creates the `shelfie` D1 database. The Worker creates its own tables on the
first request (`src/db.ts`), so there is no migration step. A daily cron (03:17 UTC) deletes
usage rows older than 180 days and expired sessions.

## Secrets

```sh
cf workers secrets update GEMINI_API_KEY --worker shelfie-gemini-proxy --type secret_text --text "<gemini key>"
cf workers secrets update APP_TOKEN      --worker shelfie-gemini-proxy --type secret_text --text "<new token>"
cf workers secrets update ADMIN_PASSWORD --worker shelfie-gemini-proxy --type secret_text --text "<long random password>"
cf workers secrets update GOOGLE_SERVICE_ACCOUNT --worker shelfie-gemini-proxy --type secret_text --text "$(cat play-service-account.json)"
```

`GOOGLE_SERVICE_ACCOUNT` is required by the config: set it before the first deploy with these
changes. It is the JSON key of a Google Cloud service account that has been added in Play
Console (Users and permissions) with "View financial data"; the Worker uses it to check
subscription purchases. Until the real key exists, `--text "{}"` lets the Worker deploy, and
`/api/billing/verify` answers 503.

After rotating `APP_TOKEN`, put the same value in the app's `local.properties` as
`SHELFIE_PROXY_TOKEN` and ship a new build. Changing `ADMIN_PASSWORD` signs every admin out.

`PASSWORD_ITERATIONS` (in `cloudflare.config.ts`) is 20 000 so login fits the Workers Free plan's
10 ms CPU per request. On the Paid plan, raise it to 100 000 and redeploy; each user's password
is re-hashed with the new count on their next login.

Adding a model on the app side (`GEMINI_MODEL` / `GEMINI_FALLBACK_MODELS`) also requires adding
it to `ALLOWED_MODELS` in `cloudflare.config.ts` and running `cf deploy`.

## Local development

Put test values in `.dev.vars` (git-ignored):

```
GEMINI_API_KEY="..."
APP_TOKEN="local-app-token"
ADMIN_PASSWORD="local-admin-pass"
```

Then `npx vite` serves the Worker with a local D1 at http://localhost:5173.

## Sync model

Each record is `(collection, syncId)` with its full state as JSON (`pantry`, `shopping`,
`recipes`, `mealplan`; field names in the app's `SyncRecordMapper.kt` and in `src/pages/panel.ts`).
Conflicts resolve by last write wins on the writer's change time; every accepted write gets the
next per-user `rev`, and clients pull everything above the highest `rev` they have seen.
Deletions are kept as tombstones for a year.

## Households

A household is its owner's data space: members read and write records under the owner's
`user_id` (`household_members`), so they share the pantry, shopping list, recipes and meal plan.
Owners create 8-character invite codes valid for 48 hours (`household_invites`); a household
holds up to 6 people. The sync response carries `space`; when it changes (join, leave, removal,
owner deleted), the app re-uploads what it has and pulls the new space from rev 0, which merges
the data. Leaving keeps a copy on the phone.

## Plans and AI allowance

Successful Gemini calls are counted per UTC day in `ai_usage`, per account when the app sends
its session and per install otherwise. Free: `FREE_DAILY_SCANS` scans and `FREE_DAILY_RECIPES`
recipes a day (3 each), plus one background morning recipe. Premium: `PREMIUM_DAILY_LIMIT`
(50) each. Over the limit the proxy answers 402 `QUOTA_EXCEEDED` without calling Gemini.

Premium is a live Play subscription (`PREMIUM_PRODUCT_IDS`, default `frizzy_premium`) held by
anyone in the household. The app sends the purchase token to `/api/billing/verify`; the Worker
checks it with the Play Developer API (`purchases.subscriptionsv2`) and stores the expiry in
`subscriptions`. Create the subscription in Play Console with that id and a monthly and/or
yearly base plan; the app lists whatever base plans Play returns.

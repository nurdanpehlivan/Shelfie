# Shelfie backend

A Cloudflare Worker with a D1 database. It does four jobs:

| Path | What | Auth |
|---|---|---|
| `POST /v1beta/models/{model}:generateContent` | Gemini proxy: keeps the Gemini API key out of the app, checks the model allowlist and a per-IP rate limit, logs each call (counts only, no content) | `x-shelfie-token` (APP_TOKEN) |
| `/api/auth/*`, `/api/sync`, `/api/account` | Optional accounts and two-way sync of pantry, shopping list and recipe history | `Authorization: Bearer <session>` |
| `/panel` | Users' web panel: view and edit the synced pantry, shopping list and recipes | Session cookie |
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
cf workers secrets update GEMINI_API_KEY --worker shelfie-gemini-proxy --text "<gemini key>"
cf workers secrets update APP_TOKEN      --worker shelfie-gemini-proxy --text "<new token>"
cf workers secrets update ADMIN_PASSWORD --worker shelfie-gemini-proxy --text "<long random password>"
```

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
`recipes`; field names in the app's `SyncRecordMapper.kt` and in `src/pages/panel.ts`).
Conflicts resolve by last write wins on the writer's change time; every accepted write gets the
next per-user `rev`, and clients pull everything above the highest `rev` they have seen.
Deletions are kept as tombstones for a year.

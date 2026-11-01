import { env } from "cloudflare:workers";

/** Invite codes as household.ts makes them: 8 characters, no 0/O or 1/I/L. */
export const JOIN_ROUTE = /^\/join\/([A-HJ-KM-NP-Z2-9]{8})$/;

/**
 * Served at `GET /join/<code>`: where the link in a family invite leads. On Android the button
 * opens the app straight at "join with this code" (shelfie://join?joinCode=...), or the Play
 * Store page when Frizzy isn't installed yet; elsewhere it shows the code and the steps.
 */
export function joinPage(code: string): string {
	const pkg = env.PLAY_PACKAGE_NAME ?? "com.nurdanpehlivan.shelfie";
	const play = `https://play.google.com/store/apps/details?id=${pkg}`;
	// Chrome's intent URL: the app if installed, the fallback URL otherwise.
	const open = `intent://join?joinCode=${code}#Intent;scheme=shelfie;package=${pkg};S.browser_fallback_url=${encodeURIComponent(play)};end`;
	const pretty = `${code.slice(0, 4)} ${code.slice(4)}`;
	return `<!doctype html>
<html lang="tr">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Frizzy · Aile daveti</title>
<meta name="robots" content="noindex">
<style>
	:root { --bg: #fff9f2; --card: #ffffff; --ink: #2a2420; --muted: #6b5f55; --accent: #23974f; --accent-ink: #fff; --soft: #e6f5eb; --line: #f0e6d8; }
	@media (prefers-color-scheme: dark) {
		:root { --bg: #15120f; --card: #1e1a16; --ink: #f3ece4; --muted: #b3a79b; --accent: #5bd48a; --accent-ink: #10291a; --soft: #18271d; --line: #2c2621; }
	}
	* { box-sizing: border-box; }
	body { margin: 0; background: var(--bg); color: var(--ink); font: 16px/1.55 system-ui, -apple-system, "Segoe UI", Roboto, sans-serif; }
	main { max-width: 480px; margin: 0 auto; padding: 32px 16px; text-align: center; }
	.card { background: var(--soft); border: 1px solid color-mix(in srgb, var(--accent) 22%, transparent); border-radius: 28px; padding: 28px 20px; }
	h1 { margin: 12px 0 6px; font-size: 1.6rem; }
	p { margin: 0 0 12px; color: var(--muted); }
	.code { font-size: 2.2rem; font-weight: 800; letter-spacing: 0.12em; color: var(--accent); margin: 8px 0 20px; }
	.button { display: block; padding: 14px 20px; border-radius: 999px; font-weight: 700; text-decoration: none; margin-top: 10px; }
	.primary { background: var(--accent); color: var(--accent-ink); }
	.secondary { background: var(--card); color: var(--ink); border: 1px solid var(--line); }
	ol { text-align: left; color: var(--muted); padding-left: 20px; margin: 24px 0 0; }
</style>
</head>
<body>
<main>
	<div class="card">
		<div style="font-size: 3rem">👨‍👩‍👧</div>
		<h1>Frizzy kilerine davet edildin</h1>
		<p>Aynı kileri, alışveriş listesini ve tarifleri birlikte kullanın.</p>
		<div class="code">${pretty}</div>
		<a class="button primary" href="${open}">Frizzy'de aç</a>
		<a class="button secondary" href="${play}">Google Play'den indir</a>
	</div>
	<ol>
		<li>Frizzy'yi indir ve aç.</li>
		<li>Kiler ekranının sağ üstündeki aile simgesine dokun; giriş yap ya da hesap oluştur.</li>
		<li>Aynı ekranda bu kodu girip "Katıl"a dokun.</li>
	</ol>
</main>
</body>
</html>`;
}

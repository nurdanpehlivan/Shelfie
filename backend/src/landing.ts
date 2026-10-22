/**
 * Served at `GET /`: the public front door. It says what Shelfie is and leads to the web panel
 * (`/panel`, for signed-in users) and the privacy policy. Colours follow the app's theme.
 */
export const LANDING_HTML = `<!doctype html>
<html lang="tr">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Shelfie</title>
<meta name="description" content="Shelfie: mutfağındakileri çöpe atmak yerine yemene yardım eden uygulama.">
<style>
	:root {
		--bg: #fff9f2;
		--card: #ffffff;
		--text: #2a2420;
		--muted: #6b5f55;
		--accent: #23974f;
		--accent-dark: #1b7a40;
		--accent-ink: #ffffff;
		--soft: #e6f5eb;
		--line: #f0e6d8;
	}
	@media (prefers-color-scheme: dark) {
		:root {
			--bg: #15120f;
			--card: #1e1a16;
			--text: #f3ece4;
			--muted: #b3a79b;
			--accent: #5bd48a;
			--accent-dark: #8be0a8;
			--accent-ink: #10291a;
			--soft: #18271d;
			--line: #2c2621;
		}
	}
	* { box-sizing: border-box; }
	body {
		margin: 0;
		background: var(--bg);
		color: var(--text);
		font-family: system-ui, -apple-system, "Segoe UI", Roboto, sans-serif;
		line-height: 1.55;
	}
	main { max-width: 820px; margin: 0 auto; padding: 40px 16px 32px; }
	.hero {
		display: flex; align-items: center; gap: 28px; flex-wrap: wrap;
		background: var(--soft); border: 1px solid color-mix(in srgb, var(--accent) 22%, transparent);
		border-radius: 28px; padding: 28px;
	}
	.hero svg { flex: 0 0 auto; width: 150px; height: 150px; }
	.hero div { flex: 1 1 260px; }
	h1 { margin: 0 0 6px; font-size: 2.6rem; letter-spacing: -0.02em; color: var(--accent-dark); }
	.tagline { margin: 0 0 18px; font-size: 1.12rem; color: var(--muted); }
	.actions { display: flex; gap: 10px; flex-wrap: wrap; }
	.button {
		display: inline-block; padding: 12px 20px; border-radius: 999px; font-weight: 700;
		text-decoration: none; background: var(--accent); color: var(--accent-ink);
	}
	.button.secondary { background: var(--card); color: var(--accent-dark); border: 1px solid var(--line); }
	.features { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 14px; margin: 28px 0; }
	.feature { background: var(--card); border: 1px solid var(--line); border-radius: 20px; padding: 18px; }
	.feature .icon { font-size: 1.6rem; }
	.feature h2 { margin: 8px 0 4px; font-size: 1.05rem; }
	.feature p { margin: 0; color: var(--muted); font-size: 0.95rem; }
	footer { margin-top: 12px; padding-top: 16px; border-top: 1px solid var(--line); font-size: 0.9rem; color: var(--muted); }
	footer a { color: var(--accent-dark); }
</style>
</head>
<body>
<main>
	<section class="hero">
		<!-- Frizzy as on the app icon: cheering, with a sprout. -->
		<svg viewBox="22 12 64 80" role="img" aria-label="Frizzy, Shelfie'nin buzdolabı maskotu">
			<ellipse cx="54" cy="86" rx="16" ry="2.6" fill="#000" opacity="0.12"/>
			<path d="M36.5,57Q30,55 29,48.5 M71.5,57Q78,55 79,48.5" stroke="#134d33" stroke-width="3.4" stroke-linecap="round" fill="none"/>
			<path d="M42,80h7v5.5h-7z M59,80h7v5.5h-7z" fill="#134d33"/>
			<defs><linearGradient id="body" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#fff"/><stop offset="1" stop-color="#e2f4e8"/></linearGradient></defs>
			<rect x="36" y="25" width="36" height="56" rx="10" fill="url(#body)" stroke="#134d33" stroke-width="3.2"/>
			<path d="M36,44H72" stroke="#134d33" stroke-width="3" stroke-linecap="round"/>
			<path d="M64.5,30.5h2.6v8.5h-2.6z M64.5,48.5h2.6v10h-2.6z" fill="#7ed3a6"/>
			<path d="M40.5,39.5C40.5,33.5 46.5,31.5 50,32.5C50,37.5 46,41 40.5,39.5Z" fill="#2fa85e"/>
			<path d="M54,25V20.5" stroke="#134d33" stroke-width="2" stroke-linecap="round"/>
			<path d="M54,21C50.5,21.5 47.5,19.5 47,16C50.5,15.5 53.5,17.5 54,21Z M54,21C57.5,21.5 60.5,19.5 61,16C57.5,15.5 54.5,17.5 54,21Z" fill="#8be0a8" stroke="#134d33" stroke-width="1.4"/>
			<ellipse cx="47" cy="58.5" rx="2.9" ry="3.7" fill="#1c2b22"/>
			<ellipse cx="61" cy="58.5" rx="2.9" ry="3.7" fill="#1c2b22"/>
			<circle cx="48.3" cy="57.1" r="1.1" fill="#fff"/>
			<circle cx="62.3" cy="57.1" r="1.1" fill="#fff"/>
			<ellipse cx="42.5" cy="64.5" rx="2.6" ry="1.6" fill="#ff9e9e" opacity="0.65"/>
			<ellipse cx="65.5" cy="64.5" rx="2.6" ry="1.6" fill="#ff9e9e" opacity="0.65"/>
			<path d="M49.5,63.5H58.5Q58.5,70 54,70Q49.5,70 49.5,63.5Z" fill="#1c2b22"/>
			<ellipse cx="54" cy="68" rx="2.4" ry="1.3" fill="#ff7a7a"/>
			<path d="M30,33l1.2,3.3l3.3,1.2l-3.3,1.2l-1.2,3.3l-1.2,-3.3l-3.3,-1.2l3.3,-1.2z" fill="#ffc86b"/>
		</svg>
		<div>
			<h1>Shelfie</h1>
			<p class="tagline">Merhaba, ben Frizzy! Aldıklarını çöpe atmak yerine yemene yardım ederim.</p>
			<div class="actions">
				<a class="button" href="/panel">Web panele gir</a>
				<a class="button secondary" href="/privacy">Gizlilik</a>
			</div>
		</div>
	</section>

	<section class="features">
		<article class="feature">
			<div class="icon">📸</div>
			<h2>Fotoğrafla ekle</h2>
			<p>Buzdolabının, kilerinin ya da fişinin tek fotoğrafı yeter; bozulmuş ürünleri de fark ederim.</p>
		</article>
		<article class="feature">
			<div class="icon">⏳</div>
			<h2>Tazeliği takip et</h2>
			<p>Son günü gelenleri hatırlatırım; dondur, çöz ya da "açtım" de, tarihi ben ayarlarım.</p>
		</article>
		<article class="feature">
			<div class="icon">🍳</div>
			<h2>Kurtarma tarifleri</h2>
			<p>Elindekilerle, gerçek ve hızlı tarifler; haftalık plana ve alışveriş listesine eklenir.</p>
		</article>
		<article class="feature">
			<div class="icon">👨‍👩‍👧</div>
			<h2>Aileyle paylaş</h2>
			<p>Davet koduyla tüm hane aynı kileri, listeyi ve tarifleri görür.</p>
		</article>
	</section>

	<footer>Shelfie bir Android uygulamasıdır. Verilerini web panelden de görebilir, düzenleyebilir ya da silebilirsin. <a href="/privacy">Gizlilik politikası</a></footer>
</main>
</body>
</html>`;

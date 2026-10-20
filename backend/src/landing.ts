/** Served at `GET /`: a small page about the app, so the Worker URL isn't a bare 404. */
export const LANDING_HTML = `<!doctype html>
<html lang="tr">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Shelfie</title>
<meta name="description" content="Shelfie: mutfağındakileri çöpe atmak yerine yemene yardım eden uygulama.">
<style>
	:root {
		--bg: #f6fbf8;
		--card: #ffffff;
		--text: #1c2b22;
		--muted: #4f6458;
		--accent: #43a36f;
		--accent-dark: #1f5c43;
		--line: #d3eedd;
		--warm: #f2a65a;
	}
	@media (prefers-color-scheme: dark) {
		:root {
			--bg: #101a14;
			--card: #17241c;
			--text: #e6f2ea;
			--muted: #9db5a6;
			--accent: #6cc495;
			--accent-dark: #8fd8b0;
			--line: #24382c;
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
	main { max-width: 760px; margin: 0 auto; padding: 48px 16px 32px; }
	.hero { display: flex; align-items: center; gap: 28px; flex-wrap: wrap; }
	.hero svg { flex: 0 0 auto; width: 132px; height: 160px; }
	.hero div { flex: 1 1 280px; }
	h1 { margin: 0 0 6px; font-size: 2.6rem; letter-spacing: -0.02em; color: var(--accent-dark); }
	.tagline { margin: 0; font-size: 1.15rem; color: var(--muted); }
	.features { display: grid; grid-template-columns: repeat(auto-fit, minmax(210px, 1fr)); gap: 14px; margin: 40px 0; }
	.feature { background: var(--card); border: 1px solid var(--line); border-radius: 16px; padding: 18px; }
	.feature .icon { font-size: 1.6rem; }
	.feature h2 { margin: 8px 0 4px; font-size: 1.05rem; }
	.feature p { margin: 0; color: var(--muted); font-size: 0.95rem; }
	.status { display: inline-flex; align-items: center; gap: 8px; font-size: 0.9rem; color: var(--muted); }
	.dot { width: 9px; height: 9px; border-radius: 50%; background: var(--accent); box-shadow: 0 0 0 4px color-mix(in srgb, var(--accent) 25%, transparent); }
	footer { margin-top: 28px; padding-top: 16px; border-top: 1px solid var(--line); font-size: 0.85rem; color: var(--muted); }
</style>
</head>
<body>
<main>
	<section class="hero">
		<svg viewBox="0 0 132 160" role="img" aria-label="Frizzy, Shelfie'nin buzdolabı maskotu">
			<path d="M66 6 C60 -2 48 2 50 12 C56 12 62 10 66 6Z" fill="#43a36f"/>
			<rect x="14" y="14" width="104" height="138" rx="26" fill="#eaf7ef" stroke="#1f5c43" stroke-width="4"/>
			<line x1="14" y1="64" x2="118" y2="64" stroke="#1f5c43" stroke-width="4"/>
			<rect x="98" y="28" width="8" height="24" rx="4" fill="#8fd8b0"/>
			<rect x="98" y="78" width="8" height="34" rx="4" fill="#8fd8b0"/>
			<circle cx="48" cy="96" r="6" fill="#1c2b22"/>
			<circle cx="80" cy="96" r="6" fill="#1c2b22"/>
			<circle cx="50" cy="94" r="2" fill="#fff"/>
			<circle cx="82" cy="94" r="2" fill="#fff"/>
			<ellipse cx="38" cy="110" rx="7" ry="4" fill="#ff9e9e"/>
			<ellipse cx="90" cy="110" rx="7" ry="4" fill="#ff9e9e"/>
			<path d="M54 112 Q64 126 74 112Z" fill="#1c2b22"/>
			<path d="M59 118 Q64 124 69 118Z" fill="#ff7a7a"/>
			<path d="M118 22 l3 7 7 3 -7 3 -3 7 -3 -7 -7 -3 7 -3Z" fill="#f2a65a"/>
		</svg>
		<div>
			<h1>Shelfie</h1>
			<p class="tagline">Merhaba, ben Frizzy! Aldıklarını çöpe atmak yerine yemene yardım ederim.</p>
		</div>
	</section>

	<section class="features">
		<article class="feature">
			<div class="icon">📸</div>
			<h2>Rafının fotoğrafını çek</h2>
			<p>Buzdolabının, kilerinin ya da fişinin tek fotoğrafı yeter; ya da barkod okut.</p>
		</article>
		<article class="feature">
			<div class="icon">⏳</div>
			<h2>Tazeliği takip et</h2>
			<p>Bozulmak üzere olan ürünleri Frizzy senin yerine hatırlar.</p>
		</article>
		<article class="feature">
			<div class="icon">🍳</div>
			<h2>Kurtarma tarifleri</h2>
			<p>Tarihi yaklaşan ürünleri değerlendiren hızlı tarifler.</p>
		</article>
		<article class="feature">
			<div class="icon">🔔</div>
			<h2>Günlük hatırlatma</h2>
			<p>Her sabah pişirmeye hazır bir tarifle dostça bir dürtme.</p>
		</article>
	</section>

	<span class="status"><span class="dot"></span>Shelfie sunucusu çalışıyor</span>

	<footer>Bu adres Shelfie Android uygulamasının yapay zekâ hizmetine aracılık eder.</footer>
</main>
</body>
</html>`;

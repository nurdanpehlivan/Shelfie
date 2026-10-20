/** Served at `GET /admin`: login form, then the usage dashboard (data from /admin/api/stats). */
export const ADMIN_HTML = `<!doctype html>
<html lang="tr">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="robots" content="noindex">
<title>Shelfie Yönetim</title>
<style>
	:root {
		color-scheme: light;
		--page: #f9f9f7;
		--surface: #fcfcfb;
		--ink: #0b0b0b;
		--ink-2: #52514e;
		--muted: #898781;
		--grid: #e1e0d9;
		--axis: #c3c2b7;
		--border: rgba(11,11,11,0.10);
		--series-1: #2a78d6;
		--series-1-wash: rgba(42,120,214,0.10);
		--critical: #d03b3b;
		--good-text: #006300;
		--brand: #1f5c43;
		--focus: #2a78d6;
	}
	@media (prefers-color-scheme: dark) {
		:root:not([data-theme="light"]) {
			color-scheme: dark;
			--page: #0d0d0d;
			--surface: #1a1a19;
			--ink: #f0efec;
			--ink-2: #c3c2b7;
			--grid: #2c2c2a;
			--axis: #383835;
			--border: rgba(255,255,255,0.10);
			--series-1: #3987e5;
			--series-1-wash: rgba(57,135,229,0.12);
			--good-text: #0ca30c;
			--brand: #8fd8b0;
			--focus: #3987e5;
		}
	}
	:root[data-theme="dark"] {
		color-scheme: dark;
		--page: #0d0d0d;
		--surface: #1a1a19;
		--ink: #f0efec;
		--ink-2: #c3c2b7;
		--grid: #2c2c2a;
		--axis: #383835;
		--border: rgba(255,255,255,0.10);
		--series-1: #3987e5;
		--series-1-wash: rgba(57,135,229,0.12);
		--good-text: #0ca30c;
		--brand: #8fd8b0;
		--focus: #3987e5;
	}
	* { box-sizing: border-box; }
	body { margin: 0; background: var(--page); color: var(--ink); font: 15px/1.5 system-ui, -apple-system, "Segoe UI", Roboto, sans-serif; }
	main { max-width: 1180px; margin: 0 auto; padding: 24px 16px 48px; }
	header { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; margin-bottom: 20px; }
	header h1 { margin: 0; font-size: 1.35rem; color: var(--brand); }
	header .spacer { flex: 1; }
	.muted { color: var(--muted); }
	.secondary { color: var(--ink-2); }
	button, input { font: inherit; }
	button { cursor: pointer; border-radius: 8px; border: 1px solid var(--border); background: var(--surface); color: var(--ink); padding: 6px 12px; }
	button:hover { background: color-mix(in srgb, var(--ink) 5%, var(--surface)); }
	button:focus-visible, input:focus-visible, [tabindex]:focus-visible { outline: 2px solid var(--focus); outline-offset: 2px; }
	.filters { display: flex; gap: 6px; flex-wrap: wrap; margin-bottom: 20px; }
	.filters button[aria-pressed="true"] { background: var(--ink); color: var(--surface); border-color: var(--ink); }
	.card { background: var(--surface); border: 1px solid var(--border); border-radius: 14px; padding: 18px; min-width: 0; }
	.card h2 { margin: 0 0 2px; font-size: 1rem; }
	.card .sub { margin: 0 0 14px; font-size: 0.85rem; color: var(--ink-2); }
	.hero { display: grid; grid-template-columns: minmax(220px, 1fr) 3fr; gap: 16px; margin-bottom: 16px; }
	.hero-value { font-size: 3.4rem; font-weight: 650; line-height: 1.05; letter-spacing: -0.02em; margin: 6px 0; }
	.tiles { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 10px; }
	.tile { border: 1px solid var(--border); border-radius: 12px; padding: 12px 14px; }
	.tile .label { font-size: 0.82rem; color: var(--ink-2); }
	.tile .value { font-size: 1.45rem; font-weight: 600; margin-top: 2px; }
	.tile .note { font-size: 0.78rem; color: var(--muted); }
	.grid-2 { display: grid; grid-template-columns: repeat(auto-fit, minmax(340px, 1fr)); gap: 16px; margin-bottom: 16px; }
	.full { margin-bottom: 16px; }
	.legend { display: flex; gap: 14px; font-size: 0.82rem; color: var(--ink-2); margin: -6px 0 8px; flex-wrap: wrap; }
	.legend i { display: inline-block; width: 10px; height: 10px; border-radius: 2px; margin-right: 6px; vertical-align: -1px; }
	.chart { position: relative; }
	.chart svg { display: block; width: 100%; height: auto; overflow: visible; }
	.chart text { font-size: 11px; fill: var(--muted); font-variant-numeric: tabular-nums; }
	.tooltip { position: absolute; pointer-events: none; background: var(--surface); border: 1px solid var(--border); border-radius: 8px; padding: 8px 10px; font-size: 0.82rem; box-shadow: 0 4px 16px rgba(0,0,0,0.12); white-space: nowrap; z-index: 2; display: none; }
	.tooltip .t-title { color: var(--ink-2); margin-bottom: 4px; }
	.tooltip .t-row { display: flex; align-items: center; gap: 8px; }
	.tooltip .t-row b { font-weight: 650; color: var(--ink); }
	.tooltip .key { width: 12px; height: 2px; border-radius: 1px; display: inline-block; }
	.bars { display: grid; gap: 8px; }
	.bar-row { display: grid; grid-template-columns: minmax(90px, 34%) 1fr auto; gap: 10px; align-items: center; font-size: 0.88rem; }
	.bar-row .name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--ink-2); }
	.bar-track { height: 14px; position: relative; }
	.bar-fill { position: absolute; left: 0; top: 0; bottom: 0; background: var(--series-1); border-radius: 0 4px 4px 0; min-width: 2px; }
	.bar-row .num { font-variant-numeric: tabular-nums; color: var(--ink); text-align: right; min-width: 48px; }
	table { width: 100%; border-collapse: collapse; font-size: 0.86rem; }
	th, td { text-align: left; padding: 7px 8px; border-bottom: 1px solid var(--grid); }
	th { color: var(--ink-2); font-weight: 600; }
	td.num, th.num { text-align: right; font-variant-numeric: tabular-nums; }
	.table-wrap { overflow-x: auto; }
	details { margin-top: 10px; font-size: 0.85rem; }
	summary { cursor: pointer; color: var(--ink-2); }
	.status-pill { display: inline-flex; align-items: center; gap: 5px; }
	.status-pill::before { content: "!"; display: inline-grid; place-items: center; width: 14px; height: 14px; border-radius: 50%; background: var(--critical); color: #fff; font-size: 10px; font-weight: 700; }
	.ok-pill::before { content: "✓"; background: var(--good-text); }
	.empty { padding: 28px 0; text-align: center; color: var(--muted); }
	.loading { opacity: 0.5; transition: opacity 0.2s; }
	#login { max-width: 360px; margin: 12vh auto; }
	#login form { display: grid; gap: 10px; }
	#login input { padding: 10px 12px; border-radius: 8px; border: 1px solid var(--axis); background: var(--page); color: var(--ink); }
	#login button { background: var(--brand); color: var(--surface); border: 0; padding: 10px; }
	.error-msg { color: var(--critical); font-size: 0.88rem; min-height: 1.2em; }
	.danger { color: var(--critical); }
	@media (max-width: 720px) {
		.hero { grid-template-columns: 1fr; }
		.hero-value { font-size: 2.8rem; }
	}
</style>
</head>
<body>
<main>
	<section id="login" class="card" hidden>
		<h1 style="margin:0 0 4px;font-size:1.3rem;color:var(--brand)">Shelfie Yönetim</h1>
		<p class="secondary" style="margin:0 0 16px">Panele girmek için yönetici şifresini gir.</p>
		<form id="login-form">
			<input type="password" id="password" autocomplete="current-password" placeholder="Yönetici şifresi" required aria-label="Yönetici şifresi">
			<button type="submit">Giriş yap</button>
			<div class="error-msg" id="login-error" role="alert"></div>
		</form>
	</section>

	<section id="dashboard" hidden>
		<header>
			<h1>Shelfie Yönetim</h1>
			<span class="muted" id="generated"></span>
			<span class="spacer"></span>
			<button id="refresh" type="button">Yenile</button>
			<button id="logout" type="button">Çıkış</button>
		</header>

		<div class="filters" role="group" aria-label="Tarih aralığı">
			<button type="button" data-days="1">Bugün</button>
			<button type="button" data-days="7">Son 7 gün</button>
			<button type="button" data-days="30">Son 30 gün</button>
			<button type="button" data-days="90">Son 90 gün</button>
		</div>

		<div id="content">
			<div class="hero">
				<div class="card">
					<h2>Yapay zekâ istekleri</h2>
					<div class="hero-value" id="hero-requests">–</div>
					<div class="secondary" id="hero-sub"></div>
				</div>
				<div class="card">
					<div class="tiles" id="tiles"></div>
				</div>
			</div>

			<div class="card full">
				<h2>Günlük istekler</h2>
				<p class="sub">Başarılı ve başarısız Gemini çağrıları, gün gün</p>
				<div class="legend"><span><i style="background:var(--series-1)"></i>Başarılı</span><span><i style="background:var(--critical)"></i>Başarısız</span></div>
				<div class="chart" id="chart-requests"></div>
				<details><summary>Tablo olarak göster</summary><div class="table-wrap" id="table-requests"></div></details>
			</div>

			<div class="grid-2">
				<div class="card">
					<h2>Günlük aktif kurulumlar</h2>
					<p class="sub">O gün en az bir istek gönderen cihaz sayısı</p>
					<div class="chart" id="chart-installs"></div>
				</div>
				<div class="card">
					<h2>Günlük token tüketimi</h2>
					<p class="sub">Girdi + çıktı (düşünme dahil) token</p>
					<div class="chart" id="chart-tokens"></div>
				</div>
			</div>

			<div class="grid-2">
				<div class="card">
					<h2>Özellikler</h2>
					<p class="sub">İsteklerin hangi ekrandan geldiği</p>
					<div class="bars" id="bars-features"></div>
				</div>
				<div class="card">
					<h2>Yanıt kodları</h2>
					<p class="sub">429 = kota ya da hız sınırı, 5xx = Gemini tarafı</p>
					<div class="bars" id="bars-statuses"></div>
				</div>
			</div>

			<div class="card full">
				<h2>Modeller</h2>
				<p class="sub">Uygulama sırayla dener; yedek modellere düşüş kota sıkıntısına işaret eder</p>
				<div class="table-wrap" id="table-models"></div>
			</div>

			<div class="grid-2">
				<div class="card">
					<h2>Ülkeler</h2>
					<p class="sub">İstek sayısına göre ilk 10</p>
					<div class="bars" id="bars-countries"></div>
				</div>
				<div class="card">
					<h2>Uygulama sürümleri</h2>
					<p class="sub">Kurulum sayısına göre</p>
					<div class="bars" id="bars-versions"></div>
				</div>
			</div>

			<div class="card full">
				<h2>Kullanıcılar</h2>
				<p class="sub">Senkronizasyon için hesap açanlar (son 100)</p>
				<div class="table-wrap" id="table-users"></div>
			</div>

			<div class="card full">
				<h2>Son hatalar</h2>
				<p class="sub">Seçili aralıktaki son 25 başarısız istek</p>
				<div class="table-wrap" id="table-errors"></div>
			</div>
		</div>
	</section>
</main>

<script>
(() => {
	const $ = (id) => document.getElementById(id);
	const fmt = new Intl.NumberFormat("tr-TR");
	const compact = new Intl.NumberFormat("tr-TR", { notation: "compact", maximumFractionDigits: 1 });
	const pct = new Intl.NumberFormat("tr-TR", { style: "percent", maximumFractionDigits: 1 });
	const DAY = 86400000;
	const tz = -new Date().getTimezoneOffset();
	let days = 30;
	try { days = Number(localStorage.getItem("shelfie-admin-days")) || 30; } catch {}

	const el = (tag, attrs = {}, ...children) => {
		const node = document.createElement(tag);
		for (const [k, v] of Object.entries(attrs)) {
			if (k === "class") node.className = v;
			else if (k === "style") node.style.cssText = v;
			else node.setAttribute(k, v);
		}
		for (const c of children) node.append(c instanceof Node ? c : document.createTextNode(c ?? ""));
		return node;
	};
	const svgEl = (tag, attrs = {}) => {
		const node = document.createElementNS("http://www.w3.org/2000/svg", tag);
		for (const [k, v] of Object.entries(attrs)) node.setAttribute(k, v);
		return node;
	};
	const dateLabel = (day, opts = { day: "numeric", month: "short" }) =>
		new Date(day * DAY).toLocaleDateString("tr-TR", { ...opts, timeZone: "UTC" });
	const dateTime = (ms) => ms ? new Date(ms).toLocaleString("tr-TR", { dateStyle: "short", timeStyle: "short" }) : "–";
	const ms = (v) => v == null ? "–" : v >= 1000 ? (v / 1000).toLocaleString("tr-TR", { maximumFractionDigits: 1 }) + " sn" : Math.round(v) + " ms";

	async function api(path, options) {
		const res = await fetch(path, { credentials: "same-origin", ...options });
		if (res.status === 401) { showLogin(); throw new Error("unauthorized"); }
		const body = await res.json().catch(() => ({}));
		if (!res.ok) throw new Error(body?.error?.message || "İstek başarısız");
		return body;
	}

	function showLogin() { $("dashboard").hidden = true; $("login").hidden = false; $("password").focus(); }

	$("login-form").addEventListener("submit", async (e) => {
		e.preventDefault();
		$("login-error").textContent = "";
		const res = await fetch("/admin/api/login", {
			method: "POST", headers: { "content-type": "application/json" },
			body: JSON.stringify({ password: $("password").value }),
		});
		if (!res.ok) {
			const body = await res.json().catch(() => ({}));
			$("login-error").textContent = body?.error?.message || "Giriş başarısız";
			return;
		}
		$("password").value = "";
		load();
	});
	$("logout").addEventListener("click", async () => { await fetch("/admin/api/logout", { method: "POST" }); showLogin(); });
	$("refresh").addEventListener("click", () => load());
	document.querySelectorAll(".filters button").forEach((b) => b.addEventListener("click", () => {
		days = Number(b.dataset.days);
		try { localStorage.setItem("shelfie-admin-days", String(days)); } catch {}
		load();
	}));

	async function load() {
		document.querySelectorAll(".filters button").forEach((b) => b.setAttribute("aria-pressed", String(Number(b.dataset.days) === days)));
		$("content").classList.add("loading");
		try {
			const data = await api("/admin/api/stats?days=" + days + "&tz=" + tz);
			$("login").hidden = true;
			$("dashboard").hidden = false;
			render(data);
		} catch (e) {
			if (e.message !== "unauthorized") alert(e.message);
		} finally {
			$("content").classList.remove("loading");
		}
	}

	/** Fills missing days with zeros so the x axis is continuous. */
	function series(data) {
		const first = Math.floor((data.range.since + tz * 60000) / DAY);
		const byDay = new Map(data.daily.map((r) => [r.day, r]));
		const out = [];
		for (let i = 0; i < data.range.days; i++) {
			const r = byDay.get(first + i) || {};
			out.push({ day: first + i, requests: r.requests || 0, ok: r.ok || 0, installs: r.installs || 0, tokens: r.tokens || 0 });
		}
		return out;
	}

	let lastData = null;

	function render(data) {
		lastData = data;
		const t = data.totals;
		const requests = t.requests || 0;
		$("generated").textContent = "Güncellendi: " + dateTime(data.range.generatedAt);
		$("hero-requests").textContent = fmt.format(requests);
		$("hero-sub").textContent = days === 1 ? "bugün" : "son " + days + " günde";

		const okRate = requests ? (t.ok || 0) / requests : null;
		const tiles = [
			["Aktif kurulum", fmt.format(t.installs || 0), "en az bir istek atan cihaz"],
			["Başarı oranı", okRate == null ? "–" : pct.format(okRate), fmt.format(requests - (t.ok || 0)) + " başarısız"],
			["Hız sınırı / kota", fmt.format(t.rate_limited || 0), "429 yanıtı"],
			["Ortalama süre", ms(t.avg_latency), "p95: " + ms(t.p95_latency)],
			["Token", compact.format((t.prompt_tokens || 0) + (t.output_tokens || 0)), compact.format(t.prompt_tokens || 0) + " girdi · " + compact.format(t.output_tokens || 0) + " çıktı"],
			["Kullanıcı hesabı", fmt.format(t.users || 0), "+" + fmt.format(t.new_users || 0) + " bu aralıkta"],
			["Oturum açık", fmt.format(t.signed_in_users || 0), "geçerli oturumu olan hesap"],
			["Senkronize kayıt", compact.format(t.records || 0), "kiler, liste ve tarif"],
		];
		$("tiles").replaceChildren(...tiles.map(([label, value, note]) =>
			el("div", { class: "tile" }, el("div", { class: "label" }, label), el("div", { class: "value" }, value), el("div", { class: "note" }, note))));

		const s = series(data);
		columnChart($("chart-requests"), s, [
			{ key: "ok", label: "Başarılı", color: "var(--series-1)" },
			{ key: "failed", label: "Başarısız", color: "var(--critical)" },
		], s.map((r) => ({ ...r, failed: r.requests - r.ok })));
		$("table-requests").replaceChildren(table(
			["Gün", "İstek", "Başarılı", "Başarısız", "Kurulum", "Token"],
			s.slice().reverse().map((r) => [dateLabel(r.day, { day: "numeric", month: "short", year: "numeric" }), fmt.format(r.requests), fmt.format(r.ok), fmt.format(r.requests - r.ok), fmt.format(r.installs), fmt.format(r.tokens)]),
			[1, 2, 3, 4, 5]));
		lineChart($("chart-installs"), s, "installs", "Kurulum");
		lineChart($("chart-tokens"), s, "tokens", "Token");

		const featureNames = { scan: "Fotoğraf tarama", recipe: "Kurtarma tarifi", bilinmiyor: "Bilinmiyor (eski sürüm)" };
		barList($("bars-features"), data.features.map((r) => [featureNames[r.name] || r.name, r.requests]));
		barList($("bars-statuses"), data.statuses.map((r) => [String(r.name) + " " + statusText(r.name), r.requests]));
		barList($("bars-countries"), data.countries.map((r) => [countryName(r.name), r.requests]));
		barList($("bars-versions"), data.versions.map((r) => [r.name, r.installs]), "kurulum");

		$("table-models").replaceChildren(data.models.length ? table(
			["Model", "İstek", "Başarı", "Ort. süre", "Token"],
			data.models.map((m) => [m.name, fmt.format(m.requests), pct.format(m.requests ? m.ok / m.requests : 0), ms(m.avg_latency), compact.format(m.tokens || 0)]),
			[1, 2, 3, 4]) : empty());

		$("table-users").replaceChildren(data.users.length ? usersTable(data.users) : empty("Henüz hesap yok"));
		$("table-errors").replaceChildren(data.recentErrors.length ? table(
			["Zaman", "Kod", "Model", "Özellik", "Süre", "Ülke", "Sürüm"],
			data.recentErrors.map((e) => [dateTime(e.ts), statusPill(e.status), e.model, featureNames[e.feature] || e.feature || "–", ms(e.latency_ms), e.country || "–", e.app_version || "–"]),
			[4]) : empty("Bu aralıkta hata yok"));
	}

	function statusText(code) {
		return ({ 200: "Başarılı", 400: "Geçersiz istek", 401: "Yetkisiz", 403: "Yasak", 404: "Bulunamadı", 413: "Çok büyük", 429: "Kota / hız sınırı", 500: "Sunucu hatası", 502: "Ağ geçidi", 503: "Aşırı yoğun", 504: "Zaman aşımı" })[code] || "";
	}
	function statusPill(code) {
		return el("span", { class: "status-pill" + (code >= 200 && code < 300 ? " ok-pill" : "") }, String(code));
	}
	function countryName(code) {
		try { return new Intl.DisplayNames(["tr"], { type: "region" }).of(code) || code; } catch { return code; }
	}
	function empty(text = "Bu aralıkta veri yok") { return el("div", { class: "empty" }, text); }

	function table(headers, rows, numericCols = []) {
		const thead = el("thead", {}, el("tr", {}, ...headers.map((h, i) => el("th", { class: numericCols.includes(i) ? "num" : "" }, h))));
		const tbody = el("tbody", {}, ...rows.map((r) => el("tr", {}, ...r.map((c, i) => el("td", { class: numericCols.includes(i) ? "num" : "" }, c)))));
		return el("table", {}, thead, tbody);
	}

	function usersTable(users) {
		const t = table(
			["E-posta", "Kayıt", "Son giriş", "Son senkron", "Kayıt sayısı", ""],
			users.map((u) => {
				const del = el("button", { type: "button", class: "danger" }, "Sil");
				del.addEventListener("click", async () => {
					if (!confirm(u.email + " hesabı ve tüm senkronize verisi silinsin mi? Bu geri alınamaz.")) return;
					try {
						await api("/admin/api/users/delete", { method: "POST", headers: { "content-type": "application/json" }, body: JSON.stringify({ id: u.id }) });
						load();
					} catch (e) { if (e.message !== "unauthorized") alert(e.message); }
				});
				return [u.email, dateTime(u.created_at), dateTime(u.last_seen_at), dateTime(u.last_sync), fmt.format(u.records), del];
			}),
			[4]);
		return t;
	}

	function barList(container, rows, unit = "istek") {
		if (!rows.length) { container.replaceChildren(empty()); return; }
		const max = Math.max(...rows.map((r) => r[1]), 1);
		container.replaceChildren(...rows.map(([name, value]) =>
			el("div", { class: "bar-row", title: name + ": " + fmt.format(value) + " " + unit },
				el("span", { class: "name" }, name),
				el("span", { class: "bar-track" }, el("span", { class: "bar-fill", style: "width:" + (value / max * 100) + "%" })),
				el("span", { class: "num" }, compact.format(value)))));
	}

	function niceMax(v) {
		if (v <= 4) return 4;
		const pow = Math.pow(10, Math.floor(Math.log10(v)));
		for (const m of [1, 2, 2.5, 5, 10]) if (m * pow >= v) return m * pow;
		return 10 * pow;
	}

	/** Shared frame: y gridlines with compact ticks, sparse x date labels, hover tooltip. */
	function frame(container, rows, maxValue, height = 220) {
		const width = Math.max(container.clientWidth || 600, 280);
		const m = { top: 8, right: 8, bottom: 24, left: 40 };
		const w = width - m.left - m.right, h = height - m.top - m.bottom;
		const svg = svgEl("svg", { viewBox: "0 0 " + width + " " + height, role: "img" });
		const max = niceMax(maxValue);
		for (let i = 0; i <= 4; i++) {
			const y = m.top + h - (h * i) / 4;
			svg.append(svgEl("line", { x1: m.left, x2: m.left + w, y1: y, y2: y, stroke: i === 0 ? "var(--axis)" : "var(--grid)", "stroke-width": 1 }));
			const label = svgEl("text", { x: m.left - 6, y: y + 4, "text-anchor": "end" });
			label.textContent = compact.format((max * i) / 4);
			svg.append(label);
		}
		const step = w / rows.length;
		const every = Math.ceil(rows.length / Math.max(2, Math.floor(w / 64)));
		rows.forEach((r, i) => {
			if ((rows.length - 1 - i) % every !== 0) return;
			const label = svgEl("text", { x: m.left + step * i + step / 2, y: height - 6, "text-anchor": "middle" });
			label.textContent = dateLabel(r.day);
			svg.append(label);
		});
		const tip = el("div", { class: "tooltip" });
		container.replaceChildren(svg, tip);
		return { svg, tip, m, w, h, step, max, width, height };
	}

	function showTip(f, container, i, title, rows) {
		f.tip.replaceChildren(el("div", { class: "t-title" }, title),
			...rows.map(([label, value, color]) => el("div", { class: "t-row" }, el("span", { class: "key", style: "background:" + color }), el("b", {}, value), el("span", { class: "secondary" }, label))));
		f.tip.style.display = "block";
		const scale = container.clientWidth / f.width;
		const x = (f.m.left + f.step * i + f.step / 2) * scale;
		const tipW = f.tip.offsetWidth;
		f.tip.style.left = Math.min(Math.max(0, x + 12), container.clientWidth - tipW) + "px";
		f.tip.style.top = "4px";
	}

	function columnChart(container, base, keys, rows) {
		const maxValue = Math.max(...rows.map((r) => keys.reduce((s, k) => s + r[k.key], 0)), 0);
		const f = frame(container, rows, maxValue);
		const barW = Math.max(2, Math.min(24, f.step - 4));
		const radius = Math.min(4, barW / 2);
		rows.forEach((r, i) => {
			const x = f.m.left + f.step * i + (f.step - barW) / 2;
			let y0 = f.m.top + f.h;
			const visible = keys.filter((k) => r[k.key] > 0);
			visible.forEach((k, j) => {
				const hgt = (r[k.key] / f.max) * f.h;
				const gap = j > 0 ? 2 : 0;
				const top = y0 - hgt;
				const isTop = j === visible.length - 1;
				const hh = Math.max(0, hgt - gap);
				const path = isTop && hh > radius
					? "M" + x + "," + (y0 - gap) + "V" + (top + radius) + "Q" + x + "," + top + " " + (x + radius) + "," + top + "H" + (x + barW - radius) + "Q" + (x + barW) + "," + top + " " + (x + barW) + "," + (top + radius) + "V" + (y0 - gap) + "Z"
					: "M" + x + "," + (y0 - gap) + "V" + (y0 - gap - hh) + "H" + (x + barW) + "V" + (y0 - gap) + "Z";
				f.svg.append(svgEl("path", { d: path, fill: k.color }));
				y0 = top;
			});
			const hit = svgEl("rect", { x: f.m.left + f.step * i, y: f.m.top, width: f.step, height: f.h, fill: "transparent", tabindex: 0 });
			const show = () => {
				hit.setAttribute("fill", "var(--series-1-wash)");
				showTip(f, container, i, dateLabel(r.day, { weekday: "short", day: "numeric", month: "long" }),
					keys.map((k) => [k.label, fmt.format(r[k.key]), k.color]));
			};
			const hide = () => { hit.setAttribute("fill", "transparent"); f.tip.style.display = "none"; };
			hit.addEventListener("pointerenter", show);
			hit.addEventListener("focus", show);
			hit.addEventListener("pointerleave", hide);
			hit.addEventListener("blur", hide);
			f.svg.append(hit);
		});
	}

	function lineChart(container, rows, key, label) {
		const f = frame(container, rows, Math.max(...rows.map((r) => r[key]), 0), 180);
		const pts = rows.map((r, i) => [f.m.left + f.step * i + f.step / 2, f.m.top + f.h - (r[key] / f.max) * f.h]);
		const line = pts.map((p, i) => (i ? "L" : "M") + p[0].toFixed(1) + "," + p[1].toFixed(1)).join("");
		f.svg.append(svgEl("path", { d: line + "L" + pts[pts.length - 1][0] + "," + (f.m.top + f.h) + "L" + pts[0][0] + "," + (f.m.top + f.h) + "Z", fill: "var(--series-1-wash)" }));
		f.svg.append(svgEl("path", { d: line, fill: "none", stroke: "var(--series-1)", "stroke-width": 2, "stroke-linejoin": "round", "stroke-linecap": "round" }));
		const last = pts[pts.length - 1];
		f.svg.append(svgEl("circle", { cx: last[0], cy: last[1], r: 4, fill: "var(--series-1)", stroke: "var(--surface)", "stroke-width": 2 }));
		const endLabel = svgEl("text", { x: last[0] - 8, y: Math.max(f.m.top + 10, last[1] - 10), "text-anchor": "end", style: "fill:var(--ink);font-weight:600" });
		endLabel.textContent = compact.format(rows[rows.length - 1][key]);
		f.svg.append(endLabel);

		const cross = svgEl("line", { y1: f.m.top, y2: f.m.top + f.h, stroke: "var(--axis)", "stroke-width": 1, visibility: "hidden" });
		const dot = svgEl("circle", { r: 4, fill: "var(--series-1)", stroke: "var(--surface)", "stroke-width": 2, visibility: "hidden" });
		const overlay = svgEl("rect", { x: f.m.left, y: f.m.top, width: f.w, height: f.h, fill: "transparent", tabindex: 0 });
		f.svg.append(cross, dot, overlay);
		let current = rows.length - 1;
		const show = (i) => {
			current = i;
			const [x, y] = pts[i];
			cross.setAttribute("x1", x); cross.setAttribute("x2", x); cross.setAttribute("visibility", "visible");
			dot.setAttribute("cx", x); dot.setAttribute("cy", y); dot.setAttribute("visibility", "visible");
			showTip(f, container, i, dateLabel(rows[i].day, { weekday: "short", day: "numeric", month: "long" }), [[label, fmt.format(rows[i][key]), "var(--series-1)"]]);
		};
		const hide = () => { cross.setAttribute("visibility", "hidden"); dot.setAttribute("visibility", "hidden"); f.tip.style.display = "none"; };
		overlay.addEventListener("pointermove", (e) => {
			const rect = f.svg.getBoundingClientRect();
			const x = (e.clientX - rect.left) * (f.width / rect.width);
			show(Math.min(rows.length - 1, Math.max(0, Math.floor((x - f.m.left) / f.step))));
		});
		overlay.addEventListener("pointerleave", hide);
		overlay.addEventListener("focus", () => show(current));
		overlay.addEventListener("blur", hide);
		overlay.addEventListener("keydown", (e) => {
			if (e.key === "ArrowLeft") show(Math.max(0, current - 1));
			if (e.key === "ArrowRight") show(Math.min(rows.length - 1, current + 1));
		});
	}

	let resizeTimer;
	addEventListener("resize", () => { clearTimeout(resizeTimer); resizeTimer = setTimeout(() => lastData && render(lastData), 150); });

	load();
})();
</script>
</body>
</html>`;

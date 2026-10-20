/**
 * Served at `GET /panel`: the user's pantry, shopping list and recipes on the web.
 *
 * Reads `/api/web/data` and writes whole records to `/api/web/records`; the app picks the
 * changes up on its next sync. Record shapes match the app's sync mapper (SyncRecordMapper.kt).
 */
export const PANEL_HTML = `<!doctype html>
<html lang="tr">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="robots" content="noindex">
<title>Shelfie Panel</title>
<style>
	:root {
		color-scheme: light;
		--bg: #f6fbf8;
		--card: #ffffff;
		--text: #1c2b22;
		--muted: #4f6458;
		--accent: #2e7d5b;
		--accent-ink: #ffffff;
		--line: #d3eedd;
		--soft: #eaf7ef;
		--critical: #c62828;
		--critical-bg: #fdecea;
		--warning: #8a5300;
		--warning-bg: #fff3dc;
		--fresh: #1f6b45;
		--fresh-bg: #e3f5ea;
	}
	@media (prefers-color-scheme: dark) {
		:root:not([data-theme="light"]) {
			color-scheme: dark;
			--bg: #101a14; --card: #17241c; --text: #e6f2ea; --muted: #9db5a6; --accent: #6cc495; --accent-ink: #0d1a12;
			--line: #24382c; --soft: #1d2e24; --critical: #ff8a80; --critical-bg: #3a1d1b; --warning: #ffc66b; --warning-bg: #352a14;
			--fresh: #8fd8b0; --fresh-bg: #183326;
		}
	}
	:root[data-theme="dark"] {
		color-scheme: dark;
		--bg: #101a14; --card: #17241c; --text: #e6f2ea; --muted: #9db5a6; --accent: #6cc495; --accent-ink: #0d1a12;
		--line: #24382c; --soft: #1d2e24; --critical: #ff8a80; --critical-bg: #3a1d1b; --warning: #ffc66b; --warning-bg: #352a14;
		--fresh: #8fd8b0; --fresh-bg: #183326;
	}
	* { box-sizing: border-box; }
	body { margin: 0; background: var(--bg); color: var(--text); font: 15px/1.5 system-ui, -apple-system, "Segoe UI", Roboto, sans-serif; }
	main { max-width: 860px; margin: 0 auto; padding: 24px 16px 56px; }
	h1 { margin: 0; font-size: 1.5rem; color: var(--accent); }
	button, input, select { font: inherit; color: inherit; }
	button { cursor: pointer; border-radius: 10px; border: 1px solid var(--line); background: var(--card); padding: 7px 12px; }
	button:hover { background: var(--soft); }
	button.primary { background: var(--accent); color: var(--accent-ink); border-color: var(--accent); }
	button.link { border: 0; background: none; padding: 4px 6px; color: var(--muted); }
	button:focus-visible, input:focus-visible, select:focus-visible { outline: 2px solid var(--accent); outline-offset: 2px; }
	input, select { border: 1px solid var(--line); border-radius: 10px; padding: 9px 11px; background: var(--bg); min-width: 0; }
	.card { background: var(--card); border: 1px solid var(--line); border-radius: 16px; padding: 18px; }
	.muted { color: var(--muted); }
	.error-msg { color: var(--critical); min-height: 1.3em; font-size: 0.9rem; }
	#auth { max-width: 400px; margin: 10vh auto 0; }
	#auth form { display: grid; gap: 10px; margin-top: 14px; }
	.switch { margin-top: 10px; font-size: 0.9rem; }
	header { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: 18px; }
	header .spacer { flex: 1; }
	.summary { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 10px; margin-bottom: 18px; }
	.summary .card { padding: 14px; }
	.summary .value { font-size: 1.7rem; font-weight: 650; }
	.summary .label { color: var(--muted); font-size: 0.88rem; }
	.tabs { display: flex; gap: 6px; margin-bottom: 14px; flex-wrap: wrap; }
	.tabs button[aria-selected="true"] { background: var(--accent); color: var(--accent-ink); border-color: var(--accent); }
	.list { display: grid; gap: 8px; }
	.item { display: flex; align-items: center; gap: 12px; background: var(--card); border: 1px solid var(--line); border-radius: 14px; padding: 12px 14px; }
	.item .main { flex: 1; min-width: 0; }
	.item .title { font-weight: 600; overflow-wrap: anywhere; }
	.item .meta { font-size: 0.86rem; color: var(--muted); }
	.item .actions { display: flex; gap: 4px; flex-wrap: wrap; justify-content: flex-end; }
	.badge { display: inline-block; font-size: 0.78rem; font-weight: 600; padding: 2px 8px; border-radius: 999px; white-space: nowrap; }
	.badge.critical { background: var(--critical-bg); color: var(--critical); }
	.badge.warning { background: var(--warning-bg); color: var(--warning); }
	.badge.fresh { background: var(--fresh-bg); color: var(--fresh); }
	.checked .title { text-decoration: line-through; color: var(--muted); }
	.add-form { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 14px; }
	.add-form input[name="item"] { flex: 2 1 180px; }
	.add-form input, .add-form select { flex: 1 1 110px; }
	.empty { text-align: center; padding: 36px 12px; color: var(--muted); }
	.section-title { margin: 18px 0 8px; font-size: 0.95rem; color: var(--muted); }
	dialog { border: 1px solid var(--line); border-radius: 18px; background: var(--card); color: var(--text); max-width: 560px; width: calc(100% - 32px); padding: 22px; }
	dialog::backdrop { background: rgba(0,0,0,0.4); }
	dialog h2 { margin-top: 0; }
	dialog ol { padding-left: 22px; }
	dialog li { margin-bottom: 8px; }
	.fav { font-size: 1.15rem; }
	.toolbar { display: flex; gap: 8px; margin-bottom: 10px; align-items: center; flex-wrap: wrap; }
	.toolbar .spacer { flex: 1; }
	footer { margin-top: 32px; font-size: 0.85rem; color: var(--muted); display: flex; gap: 12px; flex-wrap: wrap; align-items: center; }
	footer .spacer { flex: 1; }
	@media (max-width: 560px) {
		.item { flex-wrap: wrap; }
		.item .actions { width: 100%; justify-content: flex-start; }
	}
</style>
</head>
<body>
<main>
	<section id="auth" class="card" hidden>
		<h1>Shelfie</h1>
		<p class="muted" id="auth-intro" style="margin:4px 0 0">Kilerini, alışveriş listeni ve tariflerini her yerden gör. Uygulamadaki hesabınla giriş yap.</p>
		<form id="auth-form">
			<input type="email" name="email" autocomplete="email" placeholder="E-posta" required aria-label="E-posta">
			<input type="password" name="password" autocomplete="current-password" placeholder="Şifre (en az 8 karakter)" minlength="8" required aria-label="Şifre">
			<button type="submit" class="primary" id="auth-submit">Giriş yap</button>
			<div class="error-msg" id="auth-error" role="alert"></div>
		</form>
		<div class="switch muted"><span id="switch-text">Hesabın yok mu?</span> <button type="button" class="link" id="switch-mode">Hesap oluştur</button></div>
	</section>

	<section id="app" hidden>
		<header>
			<h1>Shelfie</h1>
			<span class="muted" id="who"></span>
			<span class="spacer"></span>
			<button type="button" id="reload">Yenile</button>
			<button type="button" id="logout">Çıkış</button>
		</header>

		<div class="summary" id="summary"></div>

		<div class="tabs" role="tablist">
			<button type="button" role="tab" data-tab="pantry">Kiler</button>
			<button type="button" role="tab" data-tab="shopping">Alışveriş listesi</button>
			<button type="button" role="tab" data-tab="recipes">Tarifler</button>
		</div>

		<div id="tab-pantry" role="tabpanel" hidden>
			<form class="add-form" id="add-pantry">
				<input name="item" placeholder="Ürün adı" maxlength="60" required aria-label="Ürün adı">
				<select name="category" aria-label="Kategori"></select>
				<input name="expiry" type="date" required aria-label="Son kullanma tarihi">
				<button type="submit" class="primary">Ekle</button>
			</form>
			<div class="toolbar"><input type="search" id="pantry-search" placeholder="Kilerde ara" aria-label="Kilerde ara"></div>
			<div class="list" id="pantry-list"></div>
			<h3 class="section-title" id="history-title" hidden>Son 30 günde kilerden çıkanlar</h3>
			<div class="list" id="history-list"></div>
		</div>

		<div id="tab-shopping" role="tabpanel" hidden>
			<form class="add-form" id="add-shopping">
				<input name="item" placeholder="Ne alınacak?" maxlength="60" required aria-label="Ürün">
				<input name="amount" placeholder="Miktar (isteğe bağlı)" maxlength="30" aria-label="Miktar">
				<button type="submit" class="primary">Ekle</button>
			</form>
			<div class="toolbar"><span class="spacer"></span><button type="button" id="clear-checked">Alınanları temizle</button></div>
			<div class="list" id="shopping-list"></div>
		</div>

		<div id="tab-recipes" role="tabpanel" hidden>
			<div class="toolbar">
				<label><input type="checkbox" id="only-favs"> Sadece favoriler</label>
			</div>
			<div class="list" id="recipe-list"></div>
		</div>

		<footer>
			<span>Değişiklikler uygulamaya bir sonraki senkronizasyonda geçer.</span>
			<span class="spacer"></span>
			<button type="button" class="link" id="delete-account">Hesabımı sil</button>
		</footer>
	</section>

	<dialog id="recipe-dialog"><div id="recipe-body"></div><form method="dialog" style="text-align:right"><button>Kapat</button></form></dialog>
</main>

<script>
(() => {
	const $ = (id) => document.getElementById(id);
	const DAY = 86400000;
	const CRITICAL_DAYS = 2, WARNING_DAYS = 5;
	const CATEGORIES = { DAIRY: "Süt ürünleri", MEAT: "Et", SEAFOOD: "Deniz ürünleri", FRUITS: "Meyve", VEGETABLES: "Sebze", BAKERY: "Fırın", GRAINS: "Tahıl", CANNED: "Konserve", FROZEN: "Dondurulmuş", BEVERAGES: "İçecek", CONDIMENTS: "Sos ve baharat", SNACKS: "Atıştırmalık", OTHER: "Diğer" };
	const SHELF_LIFE = { DAIRY: 7, MEAT: 3, SEAFOOD: 2, FRUITS: 7, VEGETABLES: 7, BAKERY: 4, GRAINS: 180, CANNED: 365, FROZEN: 90, BEVERAGES: 30, CONDIMENTS: 90, SNACKS: 60, OTHER: 14 };
	const UNITS = { PIECE: "adet", PACKAGE: "paket", GRAM: "g", KILOGRAM: "kg", MILLILITER: "ml", LITER: "L" };
	let data = { pantry: [], shopping: [], recipes: [] };
	let mode = "login";
	let tab = "pantry";
	try { tab = localStorage.getItem("shelfie-panel-tab") || "pantry"; } catch {}

	const el = (tag, attrs = {}, ...children) => {
		const node = document.createElement(tag);
		for (const [k, v] of Object.entries(attrs)) {
			if (k === "class") node.className = v;
			else if (k.startsWith("on")) node.addEventListener(k.slice(2), v);
			else if (v === true) node.setAttribute(k, "");
			else if (v !== false && v != null) node.setAttribute(k, v);
		}
		for (const c of children) if (c != null) node.append(c instanceof Node ? c : document.createTextNode(String(c)));
		return node;
	};
	const todayIso = () => { const d = new Date(); d.setMinutes(d.getMinutes() - d.getTimezoneOffset()); return d.toISOString().slice(0, 10); };
	const addDays = (iso, n) => { const d = new Date(iso + "T00:00:00Z"); d.setUTCDate(d.getUTCDate() + n); return d.toISOString().slice(0, 10); };
	const daysUntil = (iso) => Math.round((Date.parse(iso + "T00:00:00Z") - Date.parse(todayIso() + "T00:00:00Z")) / DAY);
	const longDate = (iso) => new Date(iso + "T00:00:00Z").toLocaleDateString("tr-TR", { day: "numeric", month: "long", timeZone: "UTC" });
	const qty = (n) => Number(n).toLocaleString("tr-TR", { maximumFractionDigits: 2 });

	async function api(path, options = {}) {
		const res = await fetch(path, { credentials: "same-origin", ...options, headers: { "content-type": "application/json", ...(options.headers || {}) } });
		const body = await res.json().catch(() => ({}));
		if (res.status === 401 && !path.startsWith("/api/web/auth")) { showAuth(); throw new Error("unauthorized"); }
		if (!res.ok) throw new Error(body?.error?.message || "Bir şeyler ters gitti, tekrar dene.");
		return body;
	}

	// region Auth

	function showAuth() { $("app").hidden = true; $("auth").hidden = false; }
	function setMode(next) {
		mode = next;
		const register = mode === "register";
		$("auth-submit").textContent = register ? "Hesap oluştur" : "Giriş yap";
		$("switch-text").textContent = register ? "Zaten hesabın var mı?" : "Hesabın yok mu?";
		$("switch-mode").textContent = register ? "Giriş yap" : "Hesap oluştur";
		$("auth-form").password.autocomplete = register ? "new-password" : "current-password";
		$("auth-error").textContent = "";
	}
	$("switch-mode").addEventListener("click", () => setMode(mode === "login" ? "register" : "login"));
	$("auth-form").addEventListener("submit", async (e) => {
		e.preventDefault();
		const f = e.target;
		$("auth-error").textContent = "";
		try {
			await api("/api/web/auth/" + mode, { method: "POST", body: JSON.stringify({ email: f.email.value, password: f.password.value }) });
			f.password.value = "";
			await load();
		} catch (err) { $("auth-error").textContent = err.message; }
	});
	$("logout").addEventListener("click", async () => { await api("/api/web/auth/logout", { method: "POST" }).catch(() => {}); showAuth(); });
	$("delete-account").addEventListener("click", async () => {
		if (!confirm("Hesabın ve sunucudaki tüm verilerin silinsin mi? Telefonundaki veriler kalır. Bu işlem geri alınamaz.")) return;
		try { await api("/api/web/account", { method: "DELETE" }); showAuth(); } catch (err) { if (err.message !== "unauthorized") alert(err.message); }
	});

	// endregion

	async function load() {
		const body = await api("/api/web/data");
		data = { pantry: body.pantry, shopping: body.shopping, recipes: body.recipes };
		$("who").textContent = body.user.email;
		$("auth").hidden = true;
		$("app").hidden = false;
		render();
	}
	$("reload").addEventListener("click", () => load().catch((e) => e.message !== "unauthorized" && alert(e.message)));

	/** Writes the whole record; the server stamps the change time. */
	async function save(collection, record, deleted = false) {
		const { syncId, changedAt, ...fields } = record;
		const body = deleted ? { collection, syncId, deleted: true } : { collection, syncId, data: fields };
		const res = await api("/api/web/records", { method: "POST", body: JSON.stringify(body) });
		const list = data[collection];
		const i = list.findIndex((r) => r.syncId === res.syncId);
		if (deleted) { if (i >= 0) list.splice(i, 1); }
		else if (i >= 0) list[i] = { ...fields, syncId: res.syncId, changedAt: res.changedAt };
		else list.push({ ...fields, syncId: res.syncId, changedAt: res.changedAt });
		render();
	}
	const run = (p) => p.catch((e) => e.message !== "unauthorized" && alert(e.message));

	// region Rendering

	document.querySelectorAll("[data-tab]").forEach((b) => b.addEventListener("click", () => {
		tab = b.dataset.tab;
		try { localStorage.setItem("shelfie-panel-tab", tab); } catch {}
		render();
	}));

	function render() {
		const active = data.pantry.filter((p) => !p.isConsumed);
		const critical = active.filter((p) => daysUntil(p.expiryDate) <= CRITICAL_DAYS).length;
		const toBuy = data.shopping.filter((s) => !s.isChecked).length;
		const summary = [
			[active.length, "kilerdeki ürün"],
			[critical, "acil tüketilmeli"],
			[toBuy, "alınacak ürün"],
			[data.recipes.length, "kayıtlı tarif"],
		];
		$("summary").replaceChildren(...summary.map(([v, l]) => el("div", { class: "card" }, el("div", { class: "value" }, v), el("div", { class: "label" }, l))));

		document.querySelectorAll("[data-tab]").forEach((b) => b.setAttribute("aria-selected", String(b.dataset.tab === tab)));
		for (const name of ["pantry", "shopping", "recipes"]) $("tab-" + name).hidden = name !== tab;
		renderPantry(active);
		renderShopping();
		renderRecipes();
	}

	function freshnessBadge(iso) {
		const days = daysUntil(iso);
		const cls = days <= CRITICAL_DAYS ? "critical" : days <= WARNING_DAYS ? "warning" : "fresh";
		const text = days < 0 ? (-days) + " gün geçti" : days === 0 ? "Bugün bitiyor" : days === 1 ? "Yarın bitiyor" : days + " gün kaldı";
		return el("span", { class: "badge " + cls, title: "Son kullanma: " + longDate(iso) }, (cls === "critical" ? "⚠ " : "") + text);
	}

	function renderPantry(active) {
		const query = $("pantry-search").value.trim().toLocaleLowerCase("tr");
		const items = active
			.filter((p) => !query || p.name.toLocaleLowerCase("tr").includes(query))
			.sort((a, b) => a.expiryDate.localeCompare(b.expiryDate) || a.name.localeCompare(b.name, "tr"));
		const change = (item, patch) => run(save("pantry", { ...item, ...patch, updatedAt: Date.now() }));
		$("pantry-list").replaceChildren(...(items.length ? items.map((p) =>
			el("div", { class: "item" },
				el("div", { class: "main" },
					el("div", { class: "title" }, p.name),
					el("div", { class: "meta" }, qty(p.quantity) + " " + (UNITS[p.unit] || "") + " · " + (CATEGORIES[p.category] || "Diğer"))),
				freshnessBadge(p.expiryDate),
				el("div", { class: "actions" },
					el("button", { type: "button", onclick: () => change(p, { isConsumed: true, isWasted: false }) }, "Yedim"),
					el("button", { type: "button", onclick: () => change(p, { isConsumed: true, isWasted: true }) }, "Attım"),
					el("button", { type: "button", class: "link", "aria-label": p.name + " sil", onclick: () => confirm(p.name + " silinsin mi?") && run(save("pantry", p, true)) }, "Sil"))))
			: [el("div", { class: "empty" }, query ? "Aramana uyan ürün yok." : "Kilerin boş. Uygulamadan bir fotoğraf çek ya da buradan ekle.")]));

		const since = Date.now() - 30 * DAY;
		const removed = data.pantry.filter((p) => p.isConsumed && (p.updatedAt || 0) >= since).sort((a, b) => (b.updatedAt || 0) - (a.updatedAt || 0));
		$("history-title").hidden = !removed.length;
		$("history-list").replaceChildren(...removed.map((p) =>
			el("div", { class: "item" },
				el("div", { class: "main" },
					el("div", { class: "title" }, p.name),
					el("div", { class: "meta" }, (p.isWasted ? "Atıldı" : "Yendi") + " · " + new Date(p.updatedAt).toLocaleDateString("tr-TR", { day: "numeric", month: "long" }))),
				el("div", { class: "actions" },
					el("button", { type: "button", onclick: () => change(p, { isConsumed: false, isWasted: false }) }, "Kilere geri koy")))));
	}

	function renderShopping() {
		const items = data.shopping.slice().sort((a, b) => (a.isChecked - b.isChecked) || (b.createdAt - a.createdAt));
		$("clear-checked").hidden = !items.some((s) => s.isChecked);
		$("shopping-list").replaceChildren(...(items.length ? items.map((s) =>
			el("label", { class: "item" + (s.isChecked ? " checked" : "") },
				el("input", { type: "checkbox", checked: s.isChecked, onchange: (e) => run(save("shopping", { ...s, isChecked: e.target.checked })) }),
				el("div", { class: "main" }, el("div", { class: "title" }, s.name), s.amount ? el("div", { class: "meta" }, s.amount) : null),
				el("button", { type: "button", class: "link", "aria-label": s.name + " sil", onclick: (e) => { e.preventDefault(); run(save("shopping", s, true)); } }, "Sil")))
			: [el("div", { class: "empty" }, "Alışveriş listen boş.")]));
	}

	function parseRecipe(r) { try { return JSON.parse(r.recipeJson); } catch { return null; } }

	function renderRecipes() {
		const onlyFavs = $("only-favs").checked;
		const items = data.recipes.filter((r) => !onlyFavs || r.isFavorite).sort((a, b) => b.createdAt - a.createdAt);
		$("recipe-list").replaceChildren(...(items.length ? items.map((r) => {
			const recipe = parseRecipe(r) || {};
			const meta = [recipe.cooking_time_minutes ? recipe.cooking_time_minutes + " dk" : null, recipe.servings ? recipe.servings + " kişilik" : null,
				new Date(r.createdAt).toLocaleDateString("tr-TR", { day: "numeric", month: "long" }), r.cookedAt ? "Pişirildi ✓" : null].filter(Boolean).join(" · ");
			return el("div", { class: "item" },
				el("button", { type: "button", class: "link fav", "aria-label": r.isFavorite ? "Favorilerden çıkar" : "Favorilere ekle", "aria-pressed": String(!!r.isFavorite), onclick: () => run(save("recipes", { ...r, isFavorite: !r.isFavorite })) }, r.isFavorite ? "★" : "☆"),
				el("div", { class: "main" }, el("div", { class: "title" }, r.title || recipe.title || "Tarif"), el("div", { class: "meta" }, meta)),
				el("div", { class: "actions" },
					el("button", { type: "button", onclick: () => openRecipe(r, recipe) }, "Tarifi aç"),
					el("button", { type: "button", class: "link", onclick: () => confirm("Tarif silinsin mi?") && run(save("recipes", r, true)) }, "Sil")));
		}) : [el("div", { class: "empty" }, onlyFavs ? "Favori tarifin yok." : "Henüz tarif yok. Uygulamadan bir kurtarma tarifi oluştur.")]));
	}

	function openRecipe(r, recipe) {
		const ingredients = (recipe.ingredients || []).map((i) => el("li", {}, [i.emoji, i.name].filter(Boolean).join(" ") + (i.amount ? " — " + i.amount : "") + (i.from_pantry ? " (kilerden)" : "")));
		const steps = (recipe.steps || []).map((s) => el("li", {}, s.instruction + (s.duration_minutes ? " (" + s.duration_minutes + " dk)" : "")));
		$("recipe-body").replaceChildren(
			el("h2", {}, r.title || recipe.title || "Tarif"),
			el("h3", {}, "Malzemeler"), el("ul", {}, ...ingredients),
			el("h3", {}, "Hazırlanışı"), el("ol", {}, ...steps));
		$("recipe-dialog").showModal();
	}

	// endregion

	// region Forms

	const categorySelect = $("add-pantry").category;
	for (const [key, label] of Object.entries(CATEGORIES)) categorySelect.append(el("option", { value: key }, label));
	const suggestExpiry = () => { $("add-pantry").expiry.value = addDays(todayIso(), SHELF_LIFE[categorySelect.value] || 14); };
	categorySelect.addEventListener("change", suggestExpiry);
	categorySelect.value = "OTHER";
	suggestExpiry();

	$("add-pantry").addEventListener("submit", (e) => {
		e.preventDefault();
		const f = e.target;
		const now = Date.now();
		run(save("pantry", {
			name: f.item.value.trim().slice(0, 60), category: f.category.value, quantity: 1, unit: "PIECE",
			purchaseDate: todayIso(), expiryDate: f.expiry.value, isConsumed: false, isWasted: false, createdAt: now, updatedAt: now,
		}).then(() => { f.item.value = ""; f.item.focus(); }));
	});
	$("add-shopping").addEventListener("submit", (e) => {
		e.preventDefault();
		const f = e.target;
		const name = f.item.value.trim().slice(0, 60);
		if (data.shopping.some((s) => !s.isChecked && s.name.toLocaleLowerCase("tr") === name.toLocaleLowerCase("tr"))) { f.item.value = ""; return; }
		run(save("shopping", { name, amount: f.amount.value.trim().slice(0, 30), isChecked: false, createdAt: Date.now() })
			.then(() => { f.item.value = ""; f.amount.value = ""; f.item.focus(); }));
	});
	$("clear-checked").addEventListener("click", () => run(Promise.all(data.shopping.filter((s) => s.isChecked).map((s) => save("shopping", s, true)))));
	$("pantry-search").addEventListener("input", render);
	$("only-favs").addEventListener("change", render);

	// endregion

	setMode("login");
	load().catch((e) => { if (e.message !== "unauthorized") { showAuth(); $("auth-error").textContent = e.message; } });
})();
</script>
</body>
</html>`;

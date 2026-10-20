/**
 * Served at `GET /privacy`: the privacy policy linked from the Play listing and the app.
 * Turkish first (the app's main audience), English below. Keep it in step with what the Worker
 * and the app actually store; see README "Sync model" and src/db.ts.
 */
export const PRIVACY_HTML = `<!doctype html>
<html lang="tr">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Shelfie · Gizlilik politikası</title>
<style>
	:root { --bg: #fff9f2; --ink: #2a2420; --muted: #7a6f66; --accent: #23974f; --line: #efe5d8; }
	@media (prefers-color-scheme: dark) { :root { --bg: #15120f; --ink: #f6eee6; --muted: #b5a99c; --accent: #5bd48a; --line: #3a332c; } }
	body { margin: 0; background: var(--bg); color: var(--ink); font: 16px/1.6 system-ui, sans-serif; }
	main { max-width: 720px; margin: 0 auto; padding: 32px 20px 64px; }
	h1 { font-size: 28px; margin: 0 0 4px; } h2 { font-size: 19px; margin: 28px 0 8px; }
	p, li { color: var(--ink); } .muted { color: var(--muted); } a { color: var(--accent); }
	hr { border: 0; border-top: 1px solid var(--line); margin: 40px 0; }
</style>
</head>
<body>
<main>
<h1>Gizlilik politikası</h1>
<p class="muted">Son güncelleme: 20 Ekim 2026</p>

<p>Shelfie, evdeki yiyeceklerini takip edip israfı azaltman için var. Bu sayfa hangi verileri neden işlediğimizi anlatır. Kısaca: hesap açmadan kullanırsan verilerin yalnızca telefonunda kalır; reklam ya da veri satışı yoktur.</p>

<h2>Telefonunda kalanlar</h2>
<p>Kilerin, alışveriş listen, tariflerin, haftalık planın ve ayarların telefonundaki uygulama veritabanında tutulur. Çektiğin fotoğraflar uygulamanın kendi klasöründe saklanır; tarih okutmak için çekilenler okunduktan sonra silinir. Paketteki tarih telefonun içinde (Google ML Kit ile) okunur, fotoğraf hiçbir yere gönderilmez.</p>

<h2>Yapay zekâ ile tarama ve tarif</h2>
<p>Bir fotoğrafı yapay zekâyla taradığında ya da tarif istediğinde fotoğraf veya ürün adları, sunucumuz üzerinden Google Gemini'ye gönderilir ve yanıt sana döner. Sunucumuz fotoğrafları ve istek içeriğini saklamaz; yalnızca istek sayısı, model, süre, yaklaşık ülke ve uygulama sürümü gibi sayıları kaydeder (180 gün). Google'ın bu verileri nasıl işlediği <a href="https://ai.google.dev/gemini-api/terms">Gemini API koşullarında</a> anlatılır.</p>

<h2>Hesap ve senkronizasyon (isteğe bağlı)</h2>
<p>Hesap açarsan e-posta adresin ve şifrenin tek yönlü özeti (şifrenin kendisi değil) saklanır. Kilerin, alışveriş listen, tariflerin ve planın diğer telefonlarınla, web panelinle ve katıldığın hanenin üyeleriyle eşitlenmek için sunucumuzda tutulur. Hanene katılan herkes bu verileri görür ve değiştirebilir.</p>

<h2>Premium</h2>
<p>Ödemeyi Google Play alır; kart bilgilerini biz görmeyiz. Aboneliğini doğrulamak için Google'dan satın alma durumunu ve bitiş tarihini alırız ve satın alma anahtarının yalnızca özetini saklarız. Günlük yapay zekâ kullanım sayıların (kaç tarama ve tarif) gün gün tutulur ve 40 günde silinir.</p>

<h2>Bildirimler</h2>
<p>Günlük hatırlatma telefonunda hazırlanır. Kapatmak için Ayarlar'ı kullanabilirsin.</p>

<h2>Silme</h2>
<p>Uygulamada Ayarlar → Hesap ve senkronizasyon → Hesabı sil ya da web panelindeki "Hesabımı sil" ile hesabın ve sunucudaki tüm verilerin hemen silinir. Telefondaki veriler uygulamayı kaldırınca silinir. Silinen kayıtların izleri en geç bir yılda temizlenir.</p>

<h2>Çocuklar</h2>
<p>Shelfie 13 yaşından küçükler için tasarlanmamıştır ve bilerek onlardan veri toplamayız.</p>

<h2>İletişim</h2>
<p>Sorular ve silme talepleri için Google Play sayfamızdaki geliştirici e-posta adresine yazabilirsin.</p>

<hr>

<h1 lang="en">Privacy policy</h1>
<p class="muted" lang="en">Last updated: 20 October 2026</p>
<div lang="en">
<p>Shelfie helps you track the food at home and waste less. Without an account your data stays on your phone. There are no ads and no data sales.</p>
<h2>On your phone</h2>
<p>Your pantry, shopping list, recipes, weekly plan and settings live in the app's database on your phone. Photos stay in the app's own folder; photos taken to read a date are deleted once read. Printed dates are read on the device (Google ML Kit) and never uploaded.</p>
<h2>AI scans and recipes</h2>
<p>When you scan a photo with AI or ask for a recipe, the photo or the product names go through our server to Google Gemini and the answer comes back. Our server does not store photos or request content; it logs only counts such as the number of requests, model, latency, approximate country and app version (kept 180 days). See the <a href="https://ai.google.dev/gemini-api/terms">Gemini API terms</a> for how Google handles them.</p>
<h2>Account and sync (optional)</h2>
<p>With an account we store your email and a one-way hash of your password (not the password). Your pantry, shopping list, recipes and plan are kept on our server to sync with your other phones, the web panel and the members of a household you join, who can see and change them.</p>
<h2>Premium</h2>
<p>Google Play takes the payment; we never see card details. To confirm a subscription we get its status and expiry from Google and store only a hash of the purchase token. Daily AI usage counts are kept per day and deleted after 40 days.</p>
<h2>Deletion</h2>
<p>Settings → Account & sync → Delete account in the app, or "Delete my account" in the web panel, deletes your account and all its server data at once. Data on the phone is removed when you uninstall the app. Traces of deleted records are purged within a year.</p>
<h2>Children</h2>
<p>Shelfie is not designed for children under 13 and we do not knowingly collect their data.</p>
<h2>Contact</h2>
<p>For questions or deletion requests, write to the developer email on our Google Play page.</p>
</div>
</main>
</body>
</html>`;

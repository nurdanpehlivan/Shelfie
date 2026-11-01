/**
 * Served at `GET /privacy`: the privacy policy linked from the Play listing and the app.
 * Turkish first, English below. Keep it in step with what the Worker and the app actually
 * store (README "Sync model", src/db.ts) and with the Play Console data safety form.
 * `#silme` / `#deletion` are the account deletion anchors given to Play.
 */
export const PRIVACY_HTML = `<!doctype html>
<html lang="tr">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Frizzy · Gizlilik politikası</title>
<style>
	:root { --bg: #fff9f2; --ink: #2a2420; --muted: #7a6f66; --accent: #23974f; --line: #efe5d8; }
	@media (prefers-color-scheme: dark) { :root { --bg: #15120f; --ink: #f6eee6; --muted: #b5a99c; --accent: #5bd48a; --line: #3a332c; } }
	body { margin: 0; background: var(--bg); color: var(--ink); font: 16px/1.6 system-ui, sans-serif; }
	main { max-width: 720px; margin: 0 auto; padding: 32px 20px 64px; }
	h1 { font-size: 28px; margin: 0 0 4px; } h2 { font-size: 19px; margin: 28px 0 8px; }
	p, li { color: var(--ink); } .muted { color: var(--muted); } a { color: var(--accent); }
	hr { border: 0; border-top: 1px solid var(--line); margin: 40px 0; }
	nav { font-size: 15px; }
</style>
</head>
<body>
<main>
<nav class="muted"><a href="#tr">Türkçe</a> · <a href="#en">English</a></nav>

<h1 id="tr">Gizlilik politikası</h1>
<p class="muted">Son güncelleme: 1 Kasım 2026</p>

<p>Frizzy, evdeki yiyeceklerini takip edip israfı azaltman için var. Bu sayfa hangi verileri, neden ve ne kadar süre işlediğimizi anlatır. Kısaca: hesap açmadan kullanırsan verilerin telefonunda kalır; reklam yok, veri satışı yok, verilerini kimseyle pazarlama için paylaşmayız.</p>

<h2>Biz kimiz</h2>
<p>Frizzy, bağımsız geliştirici Nurdan Pehlivan tarafından geliştirilir ve bu politikadaki verilerin sorumlusudur. İletişim bilgileri aşağıda, "İletişim" bölümündedir.</p>

<h2>Telefonunda kalanlar</h2>
<p>Kilerin, alışveriş listen, tariflerin, haftalık planın ve ayarların (dil, para birimi, tema, hatırlatma saati) telefonundaki uygulama veritabanında tutulur. Çektiğin fotoğraflar uygulamanın kendi klasöründe saklanır; tarih okutmak için çekilenler okunduktan sonra silinir. Paketin üstündeki tarih telefonun içinde (Google ML Kit ile) okunur ve fotoğraf hiçbir yere gönderilmez. Barkod da telefonda okunur; ürün adını bulmak için yalnızca barkod numarası Open Food Facts'e sorulur.</p>

<h2>Yapay zekâ: tarama, tarifler ve çeviriler</h2>
<p>Bir fotoğrafı yapay zekâyla taradığında fotoğraf; tarif istediğinde seçtiğin ürünlerin adları; uygulamanın dilini değiştirdiğinde ise kilerindeki ürün adları ve kayıtlı tariflerin, sunucumuz üzerinden Google Gemini'ye gönderilir ve yanıt sana döner. Fotoğraf taramasında Gemini ürünleri, tahmini son kullanma sürelerini ve görünür bozulma belirtilerini bulur. Çeviriler yalnızca telefonunda saklanır. Sunucumuz fotoğrafları ve istek içeriğini saklamaz; yalnızca istek sayısı, özellik, model, süre, yaklaşık ülke, uygulama sürümü ve uygulamanın rastgele kurulum kimliği gibi kullanım kayıtlarını tutar (180 gün). Google'ın bu verileri nasıl işlediği Gemini API koşullarında anlatılır.</p>

<h2>Hesap ve senkronizasyon (isteğe bağlı)</h2>
<p>Hesap açarsan e-posta adresin ve şifrenin tek yönlü özeti (şifrenin kendisi değil) saklanır. Kilerin, alışveriş listen, tariflerin ve haftalık planın diğer telefonlarınla ve web panelinle eşitlenmek için sunucumuzda tutulur. Web paneli oturumunu açık tutmak için yalnızca zorunlu bir oturum çerezi kullanır; reklam ya da takip çerezi yoktur.</p>

<h2>Aile paylaşımı</h2>
<p>Bir haneye davet koduyla katılırsan, hanedeki herkes ortak kileri, listeyi, tarifleri ve planı görür ve değiştirebilir; üyeler birbirinin e-posta adresini görür. Hanedeki hareketler (kimin hangi ürünü eklediği, bitirdiği, attığı, dondurduğu ya da listeye eklediği) hanedeki herkese bir akışta ve bildirim olarak gösterilir; bu kayıt ürün adını, kişiyi ve zamanı içerir ve 30 gün tutulur. Davet linkiyle uygulamayı yüklediğinde Google Play'in bize ilettiği yükleme bilgisi, yalnızca davet kodunu otomatik doldurmak için telefonunda bir kez okunur ve başka bir yere gönderilmez.</p>

<h2>Premium</h2>
<p>Ödemeyi Google Play alır; kart bilgilerini biz görmeyiz. Aboneliğini doğrulamak için Google'dan satın alma durumunu ve bitiş tarihini alırız ve satın alma anahtarının yalnızca özetini saklarız. Premium hanendeki herkes için geçerlidir. Günlük yapay zekâ kullanım sayıların gün gün tutulur ve 40 günde silinir.</p>

<h2>Bildirimler</h2>
<p>Günlük hatırlatma telefonunda hazırlanır. Kapatmak için Ayarlar'ı ya da telefonunun bildirim ayarlarını kullanabilirsin.</p>

<h2>Hizmet sağlayıcılar ve yurt dışına aktarım</h2>
<p>Sunucumuz Cloudflare üzerinde çalışır; yapay zekâ işlemleri Google (Gemini API) tarafından, ödemeler Google Play tarafından yapılır; şifre sıfırlama e-postaları Resend aracılığıyla gönderilir (yalnızca e-posta adresin ve bağlantı). Bu sağlayıcıların sunucuları Türkiye dışında, örneğin AB ve ABD'de olabilir. Verilerini yalnızca hizmeti sunmak için ve onların koşulları çerçevesinde işlerler.</p>

<h2>Hukuki dayanak</h2>
<p>Hesap, senkronizasyon, aile paylaşımı, yapay zekâ özellikleri ve Premium için verilerini hizmeti sunmak (sözleşmenin ifası) amacıyla; kullanım kayıtlarını ve sınırları ise hizmetin güvenliği ve kötüye kullanımın önlenmesi (meşru menfaat) amacıyla işleriz.</p>

<h2>Saklama süreleri</h2>
<ul>
<li>Hesap ve senkron verileri: hesabını silene kadar.</li>
<li>Kullanım kayıtları (istek sayıları, model, ülke, sürüm): 180 gün.</li>
<li>Günlük yapay zekâ kullanım sayıları: 40 gün.</li>
<li>Aile hareketleri: 30 gün.</li>
<li>Şifre sıfırlama bağlantıları: 1 saat (kullanılınca hemen silinir).</li>
<li>Silinen kayıtların izleri: en geç 1 yıl.</li>
</ul>

<h2 id="silme">Hesabını ve verilerini silme</h2>
<p>Hesabını ve sunucudaki tüm verilerini (e-posta, kiler, liste, tarifler, plan, abonelik kaydı) iki yoldan silebilirsin; silme anında gerçekleşir:</p>
<ol>
<li>Uygulamada: <strong>Ayarlar → Hesap ve senkronizasyon → Hesabı sil</strong>.</li>
<li>Web'de: <a href="/panel">web paneline</a> giriş yap → <strong>Hesabımı sil</strong>.</li>
</ol>
<p>Hesabını silmeden tek tek ürünleri, tarifleri ve liste maddelerini uygulamada silebilirsin; silinenler sunucudan da kaldırılır. Telefondaki veriler uygulamayı kaldırınca silinir. Google Play aboneliğin varsa onu Play Store → Ödemeler ve abonelikler bölümünden ayrıca iptal etmelisin.</p>

<h2>Hakların</h2>
<p>KVKK ve GDPR kapsamında verilerine erişme, düzeltme, silme, işlemeye itiraz etme ve verilerini taşıma hakkın vardır. Verilerinin çoğunu web panelinde görebilir ve düzeltebilirsin; diğer talepler için bize yazabilirsin. Ayrıca bulunduğun ülkedeki veri koruma kurumuna şikâyette bulunabilirsin.</p>

<h2>Çocuklar</h2>
<p>Frizzy 16 yaşından küçükler için tasarlanmamıştır ve bilerek onlardan veri toplamayız.</p>

<h2>Değişiklikler</h2>
<p>Bu politikayı güncellersek bu sayfadaki tarihi değiştiririz; önemli değişikliklerde Google Play sayfamızdaki sürüm notlarında da belirtiriz.</p>

<h2>İletişim</h2>
<p>Sorular ve talepler için Google Play'deki Frizzy sayfasında yer alan geliştirici e-posta adresine yazabilirsin.</p>

<hr>

<h1 id="en">Privacy policy</h1>
<p class="muted">Last updated: 1 November 2026</p>

<p>Frizzy helps you track the food at home and waste less. This page explains what data we process, why and for how long. In short: without an account your data stays on your phone; there are no ads, no data sales, and nothing is shared for marketing.</p>

<h2>Who we are</h2>
<p>Frizzy is developed by independent developer Nurdan Pehlivan, the controller of the data described here. See "Contact" below.</p>

<h2>On your phone</h2>
<p>Your pantry, shopping list, recipes, weekly plan and settings (language, currency, theme, reminder time) live in the app's database on your phone. Photos stay in the app's own folder; photos taken to read a date are deleted once read. Printed dates are read on the device (Google ML Kit) and never uploaded. Barcodes are read on the device too; only the barcode number is looked up on Open Food Facts to find the product name.</p>

<h2>AI: scans, recipes and translations</h2>
<p>When you scan a photo with AI, the photo; when you ask for a recipe, the names of the chosen products; and when you change the app language, the names of your pantry items and your saved recipes go through our server to Google Gemini and the answer comes back. In photo scans Gemini identifies products, estimates how long they keep and spots visible signs of spoilage. Translations are stored only on your phone. Our server does not store photos or request content; it logs only usage records such as the number of requests, feature, model, latency, approximate country, app version and the app's random install ID (kept 180 days). See the Gemini API terms for how Google handles this data.</p>

<h2>Account and sync (optional)</h2>
<p>With an account we store your email and a one-way hash of your password (not the password). Your pantry, shopping list, recipes and weekly plan are kept on our server to sync with your other phones and the web panel. The web panel uses only a necessary session cookie to keep you signed in; no advertising or tracking cookies.</p>

<h2>Family sharing</h2>
<p>If you join a household with an invite code, everyone in it sees and can change the shared pantry, list, recipes and plan, and members see each other's email addresses. What happens in the household (who added, used up, threw away, froze or put on the list which item) is shown to everyone in it as a feed and as notifications; each entry holds the item name, the person and the time and is kept for 30 days. When you install the app from an invite link, the install referrer Google Play passes to the app is read once on your phone, only to fill in the invite code, and is not sent anywhere.</p>

<h2>Premium</h2>
<p>Google Play takes the payment; we never see card details. To confirm a subscription we get its status and expiry from Google and store only a hash of the purchase token. Premium covers everyone in your household. Daily AI usage counts are kept per day and deleted after 40 days.</p>

<h2>Notifications</h2>
<p>The daily reminder is prepared on your phone. Turn it off in Settings or in your phone's notification settings.</p>

<h2>Service providers and international transfers</h2>
<p>Our server runs on Cloudflare; AI processing is done by Google (Gemini API) and payments by Google Play; password reset emails are sent through Resend (only your email address and the link). Their servers may be outside your country, for example in the EU and the US. They process your data only to provide the service and under their own terms.</p>

<h2>Legal basis</h2>
<p>We process data for accounts, sync, family sharing, AI features and Premium to provide the service you asked for (performance of a contract), and usage records and limits to keep the service secure and prevent abuse (legitimate interest).</p>

<h2>Retention</h2>
<ul>
<li>Account and synced data: until you delete your account.</li>
<li>Usage records (request counts, model, country, version): 180 days.</li>
<li>Daily AI usage counts: 40 days.</li>
<li>Household activity: 30 days.</li>
<li>Password reset links: 1 hour (deleted as soon as used).</li>
<li>Traces of deleted records: purged within 1 year.</li>
</ul>

<h2 id="deletion">Deleting your account and data</h2>
<p>You can delete your account and all its server data (email, pantry, list, recipes, plan, subscription record) in two ways; deletion is immediate:</p>
<ol>
<li>In the app: <strong>Settings → Account &amp; sync → Delete account</strong>.</li>
<li>On the web: sign in to the <a href="/panel">web panel</a> → <strong>Delete my account</strong>.</li>
</ol>
<p>Without deleting your account you can delete single items, recipes and list entries in the app; they are removed from the server too. Data on your phone is removed when you uninstall the app. If you have a Google Play subscription, cancel it separately in Play Store → Payments &amp; subscriptions.</p>

<h2>Your rights</h2>
<p>Under the GDPR and similar laws you can access, correct, delete and export your data and object to its processing. You can see and correct most of it in the web panel; for anything else, write to us. You may also complain to the data protection authority in your country.</p>

<h2>Children</h2>
<p>Frizzy is not designed for children under 16 and we do not knowingly collect their data.</p>

<h2>Changes</h2>
<p>If we update this policy we change the date on this page and mention important changes in the release notes on Google Play.</p>

<h2>Contact</h2>
<p>For questions and requests, write to the developer email on Frizzy's Google Play page.</p>
</main>
</body>
</html>`;

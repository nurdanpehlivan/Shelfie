/**
 * Served at `GET /reset`: where the "forgot password" email leads. The token is in the URL
 * fragment, so it never reaches server logs or the Referer header; the page posts it to
 * `/api/auth/reset` with the new password. Text follows the browser's language.
 */
export const RESET_HTML = `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Frizzy · Password</title>
<meta name="robots" content="noindex">
<style>
	:root { --bg: #fff9f2; --card: #ffffff; --ink: #2a2420; --muted: #6b5f55; --accent: #23974f; --accent-ink: #fff; --soft: #e6f5eb; --line: #e3d8c9; --error: #c0392b; }
	@media (prefers-color-scheme: dark) {
		:root { --bg: #15120f; --card: #1e1a16; --ink: #f3ece4; --muted: #b3a79b; --accent: #5bd48a; --accent-ink: #10291a; --soft: #18271d; --line: #3a322b; --error: #ff8a7a; }
	}
	* { box-sizing: border-box; }
	body { margin: 0; background: var(--bg); color: var(--ink); font: 16px/1.55 system-ui, -apple-system, "Segoe UI", Roboto, sans-serif; }
	main { max-width: 440px; margin: 0 auto; padding: 40px 16px; }
	.card { background: var(--soft); border-radius: 28px; padding: 28px 20px; text-align: center; }
	h1 { margin: 8px 0 6px; font-size: 1.5rem; }
	p { margin: 0 0 16px; color: var(--muted); }
	input { width: 100%; padding: 14px 16px; border-radius: 14px; border: 1px solid var(--line); background: var(--card); color: var(--ink); font: inherit; margin-bottom: 10px; }
	button { width: 100%; padding: 14px 20px; border: 0; border-radius: 999px; background: var(--accent); color: var(--accent-ink); font: inherit; font-weight: 700; cursor: pointer; }
	button:disabled { opacity: .6; cursor: default; }
	.error { color: var(--error); min-height: 1.5em; margin: 4px 0 10px; }
</style>
</head>
<body>
<main>
	<div class="card">
		<div style="font-size: 3rem">🔑</div>
		<h1 id="title"></h1>
		<p id="hint"></p>
		<form id="form">
			<input id="password" type="password" autocomplete="new-password" required>
			<input id="repeat" type="password" autocomplete="new-password" required>
			<div class="error" id="error" role="alert"></div>
			<button id="submit" type="submit"></button>
		</form>
	</div>
</main>
<script>
const T = {
	tr: { title: "Yeni şifre belirle", hint: "Frizzy hesabın için en az 8 karakterlik yeni bir şifre seç.", password: "Yeni şifre", repeat: "Yeni şifre (tekrar)", submit: "Şifreyi kaydet", short: "Şifre en az 8 karakter olmalı.", mismatch: "Şifreler aynı değil.", expired: "Bu bağlantının süresi dolmuş. Uygulamadan yeni bir bağlantı iste.", failed: "Bir sorun oldu, tekrar dene.", done: "Şifren değişti", doneHint: "Uygulamaya dönüp yeni şifrenle giriş yapabilirsin." },
	en: { title: "Choose a new password", hint: "Pick a new password of at least 8 characters for your Frizzy account.", password: "New password", repeat: "Repeat new password", submit: "Save password", short: "The password needs at least 8 characters.", mismatch: "The passwords don't match.", expired: "This link has expired. Ask for a new one in the app.", failed: "Something went wrong, try again.", done: "Password changed", doneHint: "Go back to the app and sign in with your new password." },
	de: { title: "Neues Passwort festlegen", hint: "Wähle ein neues Passwort mit mindestens 8 Zeichen für dein Frizzy-Konto.", password: "Neues Passwort", repeat: "Neues Passwort wiederholen", submit: "Passwort speichern", short: "Das Passwort braucht mindestens 8 Zeichen.", mismatch: "Die Passwörter stimmen nicht überein.", expired: "Dieser Link ist abgelaufen. Fordere in der App einen neuen an.", failed: "Etwas ist schiefgelaufen, versuch es noch einmal.", done: "Passwort geändert", doneHint: "Geh zurück zur App und melde dich mit dem neuen Passwort an." },
	es: { title: "Elige una contraseña nueva", hint: "Elige una contraseña nueva de al menos 8 caracteres para tu cuenta de Frizzy.", password: "Contraseña nueva", repeat: "Repite la contraseña", submit: "Guardar contraseña", short: "La contraseña necesita al menos 8 caracteres.", mismatch: "Las contraseñas no coinciden.", expired: "Este enlace ha caducado. Pide uno nuevo desde la app.", failed: "Algo salió mal, inténtalo de nuevo.", done: "Contraseña cambiada", doneHint: "Vuelve a la app e inicia sesión con tu contraseña nueva." },
	fr: { title: "Choisis un nouveau mot de passe", hint: "Choisis un nouveau mot de passe d'au moins 8 caractères pour ton compte Frizzy.", password: "Nouveau mot de passe", repeat: "Répète le mot de passe", submit: "Enregistrer", short: "Le mot de passe doit faire au moins 8 caractères.", mismatch: "Les mots de passe ne correspondent pas.", expired: "Ce lien a expiré. Demandes-en un nouveau dans l'app.", failed: "Un problème est survenu, réessaie.", done: "Mot de passe modifié", doneHint: "Retourne dans l'app et connecte-toi avec ton nouveau mot de passe." },
};
const lang = (navigator.language || "en").slice(0, 2).toLowerCase();
const t = T[lang] || T.en;
document.documentElement.lang = T[lang] ? lang : "en";
const $ = (id) => document.getElementById(id);
$("title").textContent = t.title;
$("hint").textContent = t.hint;
$("password").placeholder = t.password;
$("repeat").placeholder = t.repeat;
$("submit").textContent = t.submit;
const token = location.hash.slice(1);
// Keep the token out of the history and anything the page might share.
history.replaceState(null, "", location.pathname);
if (!token) { $("error").textContent = t.expired; $("submit").disabled = true; }
$("form").addEventListener("submit", async (e) => {
	e.preventDefault();
	const password = $("password").value;
	$("error").textContent = "";
	if (password.length < 8) { $("error").textContent = t.short; return; }
	if (password !== $("repeat").value) { $("error").textContent = t.mismatch; return; }
	$("submit").disabled = true;
	try {
		const res = await fetch("/api/auth/reset", { method: "POST", headers: { "content-type": "application/json" }, body: JSON.stringify({ token, password }) });
		if (res.ok) {
			$("title").textContent = t.done;
			$("hint").textContent = t.doneHint;
			$("form").remove();
			return;
		}
		$("error").textContent = res.status === 404 ? t.expired : t.failed;
	} catch { $("error").textContent = t.failed; }
	$("submit").disabled = false;
});
</script>
</body>
</html>`;

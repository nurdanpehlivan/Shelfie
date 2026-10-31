package com.shelfie.app.presentation.common

import androidx.compose.runtime.staticCompositionLocalOf
import java.net.URLEncoder

/**
 * Build facts the UI shows or links to. Provided once by the app module, which owns the
 * BuildConfig, so feature modules don't depend on it.
 */
data class AppInfo(
    val versionName: String,
    /** The application id, i.e. the Play Store listing's package name. */
    val packageName: String,
    /** Base URL of the Shelfie backend, which also serves the web panel and privacy page. */
    val webBaseUrl: String,
) {
    val webPanelUrl: String get() = webBaseUrl.trimEnd('/') + "/panel"
    val privacyPolicyUrl: String get() = webBaseUrl.trimEnd('/') + "/privacy"

    /** The page a family invite links to: it opens the app at "join", or the Play Store. */
    fun inviteUrl(code: String): String = webBaseUrl.trimEnd('/') + "/join/" + code

    /**
     * The Play Store page, carrying [code] as the install referrer: installed from this link,
     * the app fills the code in by itself on first launch (InstallReferrerSource).
     */
    fun playStoreInviteUrl(code: String): String =
        "https://play.google.com/store/apps/details?id=$packageName&referrer=" +
            URLEncoder.encode("joinCode=$code", Charsets.UTF_8.name())
}

val LocalAppInfo = staticCompositionLocalOf { AppInfo(versionName = "", packageName = "", webBaseUrl = "") }

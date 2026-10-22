package com.shelfie.app.presentation.common

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Build facts the UI shows or links to. Provided once by the app module, which owns the
 * BuildConfig, so feature modules don't depend on it.
 */
data class AppInfo(
    val versionName: String,
    /** Base URL of the Shelfie backend, which also serves the web panel and privacy page. */
    val webBaseUrl: String,
) {
    val webPanelUrl: String get() = webBaseUrl.trimEnd('/') + "/panel"
    val privacyPolicyUrl: String get() = webBaseUrl.trimEnd('/') + "/privacy"
}

val LocalAppInfo = staticCompositionLocalOf { AppInfo(versionName = "", webBaseUrl = "") }

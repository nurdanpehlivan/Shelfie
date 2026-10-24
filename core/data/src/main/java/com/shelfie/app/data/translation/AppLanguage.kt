package com.shelfie.app.data.translation

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The language the app is shown in: the one picked in Settings, else the device's. Everything
 * that asks Gemini for text (scans, recipes, translations) and the barcode lookup reads it
 * here, so they all follow the in-app picker on every Android version.
 */
@Singleton
class AppLanguage @Inject constructor(@ApplicationContext context: Context) {

    /** The app's [Locale]; an in-app choice wins over the device language. */
    val locale: Locale
        get() = AppCompatDelegate.getApplicationLocales()[0] ?: Locale.getDefault()

    private val _language = MutableStateFlow(locale.language)

    /** ISO 639 code of [locale], e.g. "tr"; changes when the user picks another language. */
    val language: StateFlow<String> = _language.asStateFlow()

    init {
        // Picking a language recreates the activity, so a new activity is the moment to look.
        (context as Application).registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    _language.value = locale.language
                }

                override fun onActivityStarted(activity: Activity) = Unit
                override fun onActivityResumed(activity: Activity) = Unit
                override fun onActivityPaused(activity: Activity) = Unit
                override fun onActivityStopped(activity: Activity) = Unit
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
                override fun onActivityDestroyed(activity: Activity) = Unit
            },
        )
    }
}

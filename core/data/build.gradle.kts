import java.util.Properties

// Implementations of the domain's repositories: Room, the Shelfie backend, Gemini via the
// proxy, Play Billing, on-device OCR and sync. Only the app module depends on it.
plugins {
    alias(libs.plugins.shelfie.android.library)
    alias(libs.plugins.shelfie.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.room)
}

// Settings live in the untracked local.properties, e.g. SHELFIE_PROXY_TOKEN=your-token.
// The Gemini API key itself is never in the app; it is a secret of the Cloudflare Worker in backend/.
val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}

android {
    namespace = "com.shelfie.app.core.data"

    defaultConfig {
        buildConfigField(
            "String",
            "SHELFIE_PROXY_URL",
            "\"${localProperties.getProperty("SHELFIE_PROXY_URL", "https://shelfie-gemini-proxy.shelfie-app.workers.dev/").trim()}\"",
        )
        buildConfigField(
            "String",
            "SHELFIE_PROXY_TOKEN",
            "\"${localProperties.getProperty("SHELFIE_PROXY_TOKEN", "").trim()}\"",
        )
        buildConfigField(
            "String",
            "GEMINI_MODEL",
            "\"${localProperties.getProperty("GEMINI_MODEL", "gemini-3.8-flash").trim()}\"",
        )
        // Comma-separated; each model has its own free-tier quota.
        buildConfigField(
            "String",
            "GEMINI_FALLBACK_MODELS",
            "\"${localProperties.getProperty("GEMINI_FALLBACK_MODELS", "gemini-3.5-flash-lite,gemini-3.1-flash-lite").trim()}\"",
        )
    }
    buildFeatures {
        buildConfig = true
    }
    sourceSets {
        // Exported Room schemas are used by MigrationTestHelper in instrumented tests.
        getByName("androidTest").assets.directories.add("$projectDir/schemas")
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    api(project(":core:domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    // Shelfie Premium subscription; purchases are verified by the backend (backend/src/billing.ts).
    implementation(libs.play.billing)
    // On-device reading of printed expiry dates; the model comes from Play services.
    implementation(libs.play.services.mlkit.text.recognition)

    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.turbine)
}

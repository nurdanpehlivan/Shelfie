import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
}

// Settings live in the untracked local.properties, e.g. SHELFIE_PROXY_TOKEN=your-token.
// The Gemini API key itself is never in the app; it is a secret of the Cloudflare Worker in backend/.
val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}

android {
    namespace = "com.shelfie.app"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.shelfie.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

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

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    androidResources {
        // Lists en, tr, de, es, fr for the per-app language setting (Android 13+).
        generateLocaleConfig = true
    }
    buildFeatures {
        compose = true
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
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // AndroidX & Compose
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    // Shelfie Premium subscription; purchases are verified by the backend (backend/src/billing.ts).
    implementation(libs.play.billing)
    implementation(libs.play.services.code.scanner)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Networking & serialization
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Image loading
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Unit tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)

    // Instrumented tests
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.turbine)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// The design system and shared UI: theme, components, Frizzy, the item editor, and all strings.
plugins {
    alias(libs.plugins.shelfie.android.library)
    alias(libs.plugins.shelfie.android.compose)
    alias(libs.plugins.shelfie.hilt)
}

android {
    namespace = "com.shelfie.app.core.ui"
}

dependencies {
    api(project(":core:domain"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
}

// Settings, account, household and Premium.
plugins {
    alias(libs.plugins.shelfie.android.feature)
}

android {
    namespace = "com.shelfie.app.feature.settings"
}

dependencies {
    // The in-app language picker.
    implementation(libs.androidx.appcompat)
}


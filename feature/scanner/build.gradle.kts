// Photo scan: Gemini reads the products in a photo, the user reviews them.
plugins {
    alias(libs.plugins.shelfie.android.feature)
}

android {
    namespace = "com.shelfie.app.feature.scanner"
}

dependencies {
    implementation(libs.coil.compose)
}

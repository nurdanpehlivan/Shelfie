// The pantry: what's in the fridge, freezer and cupboard, and how fresh it is.
plugins {
    alias(libs.plugins.shelfie.android.feature)
}

android {
    namespace = "com.shelfie.app.feature.pantry"
}

dependencies {
    // Barcode scanning for "quick add".
    implementation(libs.play.services.code.scanner)
}

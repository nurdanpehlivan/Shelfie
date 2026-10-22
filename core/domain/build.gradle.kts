// Business rules and contracts in plain Kotlin: models, repository interfaces, use cases.
// No Android here, so it is fast to test and can't reach into the UI or the database.
plugins {
    alias(libs.plugins.shelfie.jvm.library)
}

dependencies {
    implementation(libs.javax.inject)
    api(libs.kotlinx.coroutines.core)
}

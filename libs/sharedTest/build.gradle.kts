plugins {
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.android.library)
}

// not really an Android library, but index is not publishing for JVM at the moment
android {
    namespace = "org.fdroid.test"
    @Suppress("ktlint:standard:chain-method-continuation")
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig {
        minSdk = 21
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    implementation(project(":libs:download"))
    implementation(project(":libs:index"))

    implementation(libs.kotlin.test)
    implementation(libs.kotlinx.serialization.json)
}

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.android.ksp)
    alias(libs.plugins.android.hilt)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.kotlin.plugin.serialization)
    alias(libs.plugins.jetbrains.compose.compiler)
    alias(libs.plugins.screenshot)
}

android {
    namespace = "org.fdroid"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.fdroid"
        minSdk = 24
        targetSdk = 36
        versionCode = 2000002
        versionName = "2.0-alpha2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        all {
            buildConfigField("String", "ACRA_REPORT_EMAIL", "\"reports@f-droid.org\"")
        }
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isDebuggable = true
        }
    }
    flavorDimensions += listOf("variant", "release")
    productFlavors {
        create("basic") {
            dimension = "variant"
            applicationIdSuffix = ".basic"
        }
        create("full") {
            dimension = "variant"
            applicationIdSuffix = ".fdroid"
        }
        create("default") {
            dimension = "release"
        }
        create("nightly") {
            dimension = "release"
            versionCode = (System.currentTimeMillis() / 1000 / 60).toInt()
            versionNameSuffix = "-$gitHash"
            applicationIdSuffix = ".nightly"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += listOf("META-INF/LICENSE.md", "META-INF/LICENSE-notice.md")
        }
    }
    lint {
        lintConfig = file("lint.xml")
    }
    @Suppress("UnstableApiUsage")
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
}

androidComponents {
    beforeVariants { variantBuilder ->
        if (variantBuilder.buildType == "debug" &&
            variantBuilder.productFlavors.contains("release" to "nightly")
        ) {
            // no debug builds for nightly version,
            // so we can test proguard minification in production
            variantBuilder.enable = false
        }
    }
}

dependencies {
    implementation(project(":libs:index"))
    implementation(project(":libs:database"))
    implementation(project(":libs:download"))
    implementation(libs.kotlinx.serialization.json)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.material3.adaptive.navigation3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlinx.serialization.core)

    implementation(libs.molecule.runtime)
    implementation(libs.coil.compose)
    implementation(libs.compose.hints)
    implementation(libs.compose.preference)

    implementation(libs.slf4j.api)
    implementation(libs.logback.android)
    implementation(libs.microutils.kotlin.logging)

    implementation(libs.acra.mail)
    implementation(libs.acra.dialog)
    implementation("com.journeyapps:zxing-android-embedded:4.3.0") { isTransitive = false }
    implementation(libs.zxing.core)

    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.android.compiler)
    ksp(libs.androidx.hilt.compiler)
    // https://github.com/google/dagger/issues/5001
    ksp("org.jetbrains.kotlin:kotlin-metadata-jvm:2.3.0")

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.slf4j.simple)

    androidTestImplementation(libs.kotlin.test)
    androidTestImplementation(libs.kotlin.reflect)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.androidx.ui.tooling)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    metricsDestination = layout.buildDirectory.dir("compose_compiler")
    stabilityConfigurationFiles.add(layout.projectDirectory.file("compose-stability.conf"))
}

val gitHash: String
    get() {
        val process = ProcessBuilder("git", "rev-parse", "--short=8", "HEAD")
            .directory(rootDir)
            .redirectErrorStream(true)
            .start()
        process.waitFor() // Ensure the command completes
        return process.inputStream.use { it.readBytes().decodeToString().trim() }
    }

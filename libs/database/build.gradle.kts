plugins {
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.android.library)
    alias(libs.plugins.android.ksp)
    alias(libs.plugins.jetbrains.dokka)
    alias(libs.plugins.vanniktech.maven.publish)
}

android {
    namespace = "org.fdroid.database"
    @Suppress("ktlint:standard:chain-method-continuation")
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = 23
        consumerProguardFiles("consumer-rules.pro")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["disableAnalytics"] = "true"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    sourceSets {
        getByName("androidTest") {
            java.srcDirs("src/dbTest/java")
            // Adds exported schema location as test app assets.
            assets.srcDirs(files("$projectDir/schemas"))
        }
        getByName("test") {
            java.srcDirs("src/dbTest/java")
            // Adds exported schema location as test app assets.
            assets.srcDirs(files("$projectDir/schemas"))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions {
        targetSdk = 34 // relevant for instrumentation tests (targetSdk 21 fails on Android 14)
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    androidResources {
        // needed only for instrumentation tests: assets.openFd()
        noCompress += "json"
    }
    packaging {
        resources {
            excludes.add("META-INF/AL2.0")
            excludes.add("META-INF/LGPL2.1")
            excludes.add("META-INF/LICENSE.md")
            excludes.add("META-INF/LICENSE-notice.md")
        }
    }
}

kotlin {
    explicitApi()
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        optIn.add("kotlin.RequiresOptIn")
    }
}

dependencies {
    implementation(project(":libs:download"))
    implementation(project(":libs:index"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.microutils.kotlin.logging)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(project(":libs:sharedTest"))
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.core.testing)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.commons.io)
    testImplementation(libs.logback.classic)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.okhttp)

    androidTestImplementation(project(":libs:sharedTest"))
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.kotlin.test)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.core.testing)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.commons.io)
}

ksp {
    arg(RoomSchemaArgProvider(File(projectDir, "schemas")))
}

signing {
    useGpgCmd()
}

tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
    pluginsMapConfiguration.set(
        mapOf(
            "org.jetbrains.dokka.base.DokkaBase" to """{
                "customAssets": ["${file("${rootProject.rootDir}/logo-icon.svg")}"],
                "footerMessage": "© 2010-2025 F-Droid Limited and Contributors",
        }""",
        ),
    )
}

class RoomSchemaArgProvider(
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val schemaDir: File,
) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> = listOf("room.schemaLocation=${schemaDir.path}")
}

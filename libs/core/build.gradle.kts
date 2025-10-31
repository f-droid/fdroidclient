plugins {
    alias(libs.plugins.jetbrains.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.dokka)
    alias(libs.plugins.vanniktech.maven.publish)
}
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        }
        publishLibraryVariants("release")
    }
    explicitApi()
    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation {
        enabled = true
    }
    compilerOptions {
        optIn.add("kotlin.RequiresOptIn")
    }
    sourceSets {
        commonMain {
            dependencies {
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

android {
    namespace = "org.fdroid.core"
    @Suppress("ktlint:standard:chain-method-continuation")
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["disableAnalytics"] = "true"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
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

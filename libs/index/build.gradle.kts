plugins {
    alias(libs.plugins.jetbrains.kotlin.multiplatform)
    alias(libs.plugins.jetbrains.kotlin.plugin.serialization)
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.dokka)
    alias(libs.plugins.vanniktech.maven.publish)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        }
        publishLibraryVariants("release")
    }
    compilerOptions {
        optIn.add("kotlin.RequiresOptIn")
    }
    explicitApi()
    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation {
        enabled = true
    }
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":libs:core"))
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.microutils.kotlin.logging)
            }
        }
        commonTest {
            dependencies {
                implementation(project(":libs:sharedTest"))
                implementation(kotlin("test"))
                implementation(libs.goncalossilva.resources)
            }
        }
        // JVM is disabled for now, because Android app is including it instead of Android library
        jvmMain {
            dependencies {
            }
        }
        jvmTest {
            dependencies {
                implementation(libs.junit)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.kotlin.reflect)
                implementation(libs.androidx.core.ktx)
            }
        }
        androidUnitTest {
            dependencies {
                implementation(libs.junit)
                implementation(libs.mockk)
            }
        }
        androidInstrumentedTest {
            dependencies {
                implementation(project(":libs:sharedTest"))
                implementation(kotlin("test"))
                implementation(libs.androidx.test.runner)
                implementation(libs.androidx.test.ext.junit)
            }
        }
    }
}

android {
    namespace = "org.fdroid.index"
    @Suppress("ktlint:standard:chain-method-continuation")
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig {
        minSdk = 21
        consumerProguardFiles("consumer-rules.pro")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["disableAnalytics"] = "true"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

signing {
    useGpgCmd()
}

dokka {
    pluginsConfiguration.html {
        customAssets.from("${file("${rootProject.rootDir}/logo-icon.svg")}")
        footerMessage.set("© 2010-2025 F-Droid Limited and Contributors")
    }
}

plugins {
    alias(libs.plugins.jetbrains.kotlin.multiplatform)
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
                api(project(":libs:core"))
                api(libs.ktor.client.core)
                implementation(libs.microutils.kotlin.logging)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.ktor.client.mock)
                implementation(libs.mockk)
            }
        }
        // JVM is disabled for now, because Android app is including it instead of Android library
        jvmMain {
            dependencies {
                implementation(libs.ktor.client.cio)
            }
        }
        jvmTest {
            dependencies {
                implementation(libs.junit)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.ktor.client.okhttp)
                //noinspection UseTomlInstead
                implementation("com.github.bumptech.glide:glide:4.16.0") {
                    isTransitive = false // we don't need all that it pulls in, just the basics
                }
                implementation(libs.glide.annotations)
                implementation("io.coil-kt.coil3:coil-core:3.3.0") {
                    isTransitive = false // we don't need all that it pulls in, just the basics
                }
                implementation("javax.inject:javax.inject:1")
            }
        }
        androidUnitTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.json)
                implementation(libs.junit)
                implementation(libs.logback.classic)
            }
        }
        val commonTest by getting
        androidInstrumentedTest {
            dependsOn(commonTest)
            dependencies {
                implementation(project(":libs:sharedTest"))
                implementation(libs.androidx.test.runner)
                implementation(libs.androidx.test.ext.junit)
                implementation(libs.mockk.android)
            }
        }
    }
}

android {
    namespace = "org.fdroid.download"
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
    lint {
        checkReleaseBuilds = false
        abortOnError = true

        htmlReport = true
        xmlReport = false
        textReport = true

        lintConfig = file("lint.xml")
    }
    testOptions {
        targetSdk = 34 // needed for instrumentation tests
        packaging {
            resources.excludes.add("META-INF/*")
        }
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

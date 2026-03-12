plugins {
  alias(libs.plugins.jetbrains.kotlin.multiplatform)
  alias(libs.plugins.android.multiplatform.library)
  alias(libs.plugins.jetbrains.dokka)
  alias(libs.plugins.vanniktech.maven.publish)
  alias(libs.plugins.ktfmt)
}

kotlin {
  compilerOptions { optIn.add("kotlin.RequiresOptIn") }
  explicitApi()
  @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
  abiValidation { enabled = true }

  jvm()
  android {
    namespace = "org.fdroid.download"
    compileSdk = libs.versions.compileSdk.get().toInt()
    minSdk = 21

    compilerOptions { jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17 }

    withHostTest { packaging { resources.excludes.add("META-INF/*") } }
    withDeviceTest {
      instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
      instrumentationRunnerArguments["disableAnalytics"] = "true"
    }
    androidResources { enable = true }
    lint {
      checkReleaseBuilds = false
      abortOnError = true

      htmlReport = true
      xmlReport = false
      textReport = true

      lintConfig = file("lint.xml")
    }
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
    jvmMain { dependencies { implementation(libs.ktor.client.cio) } }
    jvmTest { dependencies { implementation(libs.junit) } }
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
    getByName("androidHostTest") {
      dependencies {
        implementation(kotlin("test"))
        implementation(libs.json)
        implementation(libs.junit)
        implementation(libs.logback.classic)
      }
    }
    val commonTest by getting
    getByName("androidDeviceTest") {
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

ktfmt { googleStyle() }

signing { useGpgCmd() }

dokka {
  pluginsConfiguration.html {
    customAssets.from("${file("${rootProject.rootDir}/logo-icon.svg")}")
    footerMessage.set("© 2010-2025 F-Droid Limited and Contributors")
  }
}

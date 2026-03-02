plugins {
  alias(libs.plugins.android.multiplatform.library)
  alias(libs.plugins.jetbrains.kotlin.multiplatform)
  alias(libs.plugins.jetbrains.kotlin.plugin.serialization)
  alias(libs.plugins.jetbrains.dokka)
  alias(libs.plugins.vanniktech.maven.publish)
  alias(libs.plugins.ktfmt)
}

kotlin {
  compilerOptions { optIn.add("kotlin.RequiresOptIn") }
  explicitApi()
  @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
  abiValidation { enabled = true }

  android {
    namespace = "org.fdroid.index"
    compileSdk = libs.versions.compileSdk.get().toInt()
    minSdk = 21

    @Suppress("UnstableApiUsage")
    optimization {
      consumerKeepRules.apply {
        publish = true
        file("consumer-rules.pro")
      }
    }

    withJava()
    compilerOptions { jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17 }

    withHostTest {
      isIncludeAndroidResources = true
      packaging { resources.excludes.add("META-INF/*") }
    }
    withDeviceTest {
      instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
      instrumentationRunnerArguments["disableAnalytics"] = "true"
    }
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
    jvmMain { dependencies {} }
    jvmTest { dependencies { implementation(libs.junit) } }
    androidMain {
      dependencies {
        implementation(libs.kotlin.reflect)
        implementation(libs.androidx.core.ktx)
      }
    }
    getByName("androidHostTest") {
      dependencies {
        implementation(libs.junit)
        implementation(libs.mockk)
      }
    }
    getByName("androidDeviceTest") {
      dependencies {
        implementation(project(":libs:sharedTest"))
        implementation(kotlin("test"))
        implementation(libs.androidx.test.runner)
        implementation(libs.androidx.test.ext.junit)
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

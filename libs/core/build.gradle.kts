plugins {
  alias(libs.plugins.jetbrains.kotlin.multiplatform)
  alias(libs.plugins.android.library)
  alias(libs.plugins.jetbrains.dokka)
  alias(libs.plugins.vanniktech.maven.publish)
  alias(libs.plugins.ktfmt)
}

kotlin {
  androidTarget {
    compilerOptions { jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17 }
    publishLibraryVariants("release")
  }
  explicitApi()
  @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
  abiValidation { enabled = true }
  compilerOptions { optIn.add("kotlin.RequiresOptIn") }
  sourceSets {
    commonMain { dependencies {} }
    commonTest { dependencies { implementation(kotlin("test")) } }
  }
}

android {
  namespace = "org.fdroid.core"
  compileSdk = libs.versions.compileSdk.get().toInt()
  defaultConfig {
    minSdk = 21
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    testInstrumentationRunnerArguments["disableAnalytics"] = "true"
  }
  buildTypes {
    getByName("release") {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
}

ktfmt { googleStyle() }

signing { useGpgCmd() }

mavenPublishing {
  configure(
    com.vanniktech.maven.publish.KotlinMultiplatform(
      javadocJar = com.vanniktech.maven.publish.JavadocJar.Dokka("dokkaHtml"),
      sourcesJar = true,
      androidVariantsToPublish = listOf("release"),
    )
  )
}

dokka {
  pluginsConfiguration.html {
    customAssets.from("${file("${rootProject.rootDir}/logo-icon.svg")}")
    footerMessage.set("© 2010-2025 F-Droid Limited and Contributors")
  }
}

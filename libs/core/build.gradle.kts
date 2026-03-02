plugins {
  alias(libs.plugins.jetbrains.kotlin.multiplatform)
  alias(libs.plugins.android.multiplatform.library)
  alias(libs.plugins.jetbrains.dokka)
  alias(libs.plugins.vanniktech.maven.publish)
  alias(libs.plugins.ktfmt)
}

kotlin {
  explicitApi()
  @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
  abiValidation { enabled = true }
  compilerOptions { optIn.add("kotlin.RequiresOptIn") }

  jvm()
  android {
    namespace = "org.fdroid.core"
    compileSdk = libs.versions.compileSdk.get().toInt()
    minSdk = 21
    withHostTestBuilder {}.configure {}
    withDeviceTestBuilder { sourceSetTreeName = "test" }
    compilerOptions { jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17 }
  }

  sourceSets {
    commonMain { dependencies {} }
    commonTest { dependencies { implementation(kotlin("test")) } }
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

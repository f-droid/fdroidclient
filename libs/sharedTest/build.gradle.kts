plugins {
  alias(libs.plugins.jetbrains.kotlin.multiplatform)
  alias(libs.plugins.android.multiplatform.library)
  alias(libs.plugins.ktfmt)
}

kotlin {
  jvm()
  android {
    namespace = "org.fdroid.test"
    compileSdk = libs.versions.compileSdk.get().toInt()
    minSdk = 21
    compilerOptions { jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17 }
  }
  sourceSets {
    commonMain {
      resources.srcDir("src/commonMain/resources")
      dependencies {
        implementation(project(":libs:download"))
        implementation(project(":libs:index"))

        implementation(libs.kotlin.test)
        implementation(libs.kotlinx.serialization.json)
      }
    }
  }
}

ktfmt { googleStyle() }

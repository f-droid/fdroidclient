package org.fdroid.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.reflect.full.primaryConstructor
import kotlin.test.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class PrimaryConstructorTest {

  private val classes =
    listOf(
      AntiFeature::class,
      Category::class,
      ReleaseChannel::class,
      // recent minification removes the primary constructor of CoreRepository
      // so we need to ensure it is still there for our reflection diffing
      Class.forName("org.fdroid.database.CoreRepository").kotlin,
    )

  @Test
  fun testPrimaryConstructor() {
    classes.forEach {
      assertNotNull(
        actual = it.primaryConstructor,
        message = "${it.simpleName} has no primary constructor",
      )
    }
  }
}

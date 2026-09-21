package org.fdroid.settings

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.setMain
import org.fdroid.settings.SettingsConstants.PREF_KEY_PROXY
import org.junit.Test

class SettingsManagerTest {

  private val context: Context = mockk(relaxed = true)
  private val prefs: SharedPreferences = mockk(relaxed = true)
  private val editor: SharedPreferences.Editor = mockk(relaxed = true)

  init {
    @OptIn(ExperimentalCoroutinesApi::class) Dispatchers.setMain(Dispatchers.Unconfined)

    // We mock the SharedPreferences and its Editor, because we can't get a real SharedPreferences.
    // Another option would be creating a custom fake implementation of both,
    // but then the test would test that fake implementation and also not the real one.
    every { context.getSharedPreferences(any(), any()) } returns prefs
    every { prefs.edit() } returns editor
  }

  @Test
  fun testTorToggleMigration() {
    // mockk old settings
    every { prefs.getBoolean("useTor", false) } returns true

    // init new settings manager auto-runs migrations
    SettingsManager(context)

    // verify that the migration has been applied and old setting removed
    verify {
      editor.putString(PREF_KEY_PROXY, "127.0.0.1:9050")
      editor.remove("useTor")
    }
  }
}

package org.fdroid.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.setMain
import org.fdroid.settings.SettingsConstants.AutoUpdateValues
import org.fdroid.settings.SettingsConstants.MirrorChooserValues
import org.fdroid.settings.SettingsConstants.PREF_KEY_AUTO_UPDATES
import org.fdroid.settings.SettingsConstants.PREF_KEY_MIRROR_CHOOSER
import org.fdroid.settings.SettingsConstants.PREF_KEY_PROXY
import org.fdroid.settings.SettingsConstants.PREF_KEY_REPO_UPDATES
import org.junit.Test

class SettingsManagerTest {

  private val context: Context = mockk(relaxed = true)
  private val prefs: SharedPreferences = FakeSharedPreferences()

  // 1.x shared preferences keys and values
  private val overWifi = "overWifi"
  private val overData = "overData"
  private val updateAutoDownload = "updateAutoDownload"
  private val never = 0
  private val manual = 1
  private val always = 2

  init {
    @OptIn(ExperimentalCoroutinesApi::class) Dispatchers.setMain(Dispatchers.Unconfined)

    every { context.getSharedPreferences(any(), any()) } returns prefs
  }

  @Test
  fun testTorToggleMigration() {
    // old installation had useTor on
    prefs.edit { putBoolean("useTor", true) }

    // init new settings manager auto-runs migrations
    SettingsManager(context)

    // verify that the migration has been applied and old setting removed
    assertEquals("127.0.0.1:9050", prefs.getString(PREF_KEY_PROXY, null))
    assertFalse(prefs.contains("useTor"))
  }

  @Test
  fun testUpdateDefaultsMigration() {
    prefs.edit {
      putInt(overWifi, always)
      putInt(overData, manual)
      putBoolean(updateAutoDownload, true)
    }
    SettingsManager(context)
    assertEquals(AutoUpdateValues.OnlyWifi.name, prefs.getString(PREF_KEY_REPO_UPDATES, null))
    assertEquals(AutoUpdateValues.OnlyWifi.name, prefs.getString(PREF_KEY_AUTO_UPDATES, null))
    assertFalse(prefs.contains(overWifi))
    assertFalse(prefs.contains(overData))
    assertFalse(prefs.contains(updateAutoDownload))
  }

  @Test
  fun testUpdateDefaultsMigrationWithPrivilegedExtension() {
    prefs.edit {
      putInt(overWifi, always)
      putInt(overData, manual)
      putBoolean(updateAutoDownload, false)
    }
    SettingsManager(context)
    assertEquals(AutoUpdateValues.OnlyWifi.name, prefs.getString(PREF_KEY_REPO_UPDATES, null))
    assertEquals(AutoUpdateValues.OnlyWifi.name, prefs.getString(PREF_KEY_AUTO_UPDATES, null))
    assertFalse(prefs.contains(overWifi))
    assertFalse(prefs.contains(overData))
    assertFalse(prefs.contains(updateAutoDownload))
  }

  @Test
  fun testExerciseAllUpdateMigrationOptions() {
    for (ow in listOf(always, manual, never)) {
      for (od in listOf(always, manual, never)) {
        for (uad in listOf(true, false)) {
          prefs.edit {
            putInt(overWifi, ow)
            putInt(overData, od)
            putBoolean(updateAutoDownload, uad)
          }
          SettingsManager(context)
          assertNotEquals(null, prefs.getString(PREF_KEY_REPO_UPDATES, null))
          assertNotEquals(null, prefs.getString(PREF_KEY_AUTO_UPDATES, null))
          assertFalse(prefs.contains(overWifi))
          assertFalse(prefs.contains(overData))
          assertFalse(prefs.contains(updateAutoDownload))
        }
      }
    }
  }

  @Test
  fun testNoPreferForeignMigration() {
    SettingsManager(context)
    assertNull(prefs.getString(PREF_KEY_MIRROR_CHOOSER, null))
    assertFalse(prefs.contains("preferForeign"))
  }

  @Test
  fun testPreferForeignFalseMigration() {
    prefs.edit { putBoolean("preferForeign", false) }
    SettingsManager(context)
    assertNull(prefs.getString(PREF_KEY_MIRROR_CHOOSER, null))
  }

  @Test
  fun testPreferForeignTrueMigration() {
    prefs.edit { putBoolean("preferForeign", true) }
    SettingsManager(context)
    assertEquals(
      MirrorChooserValues.PreferForeign.name,
      prefs.getString(PREF_KEY_MIRROR_CHOOSER, null),
    )
    assertFalse(prefs.contains("preferForeign"))
  }
}

private class FakeSharedPreferences : SharedPreferences {
  private val data = mutableMapOf<String, Any?>()

  override fun getAll(): Map<String, *> = data

  override fun getString(key: String?, defValue: String?): String? =
    data[key] as? String ?: defValue

  override fun getStringSet(key: String?, defValues: Set<String>?): Set<String>? =
    data[key] as? Set<String> ?: defValues

  override fun getInt(key: String?, defValue: Int): Int = data[key] as? Int ?: defValue

  override fun getLong(key: String?, defValue: Long): Long = data[key] as? Long ?: defValue

  override fun getFloat(key: String?, defValue: Float): Float = data[key] as? Float ?: defValue

  override fun getBoolean(key: String?, defValue: Boolean): Boolean =
    data[key] as? Boolean ?: defValue

  override fun contains(key: String?): Boolean = data.containsKey(key)

  override fun edit(): SharedPreferences.Editor = FakeEditor()

  override fun registerOnSharedPreferenceChangeListener(
    listener: SharedPreferences.OnSharedPreferenceChangeListener?
  ) = TODO()

  override fun unregisterOnSharedPreferenceChangeListener(
    listener: SharedPreferences.OnSharedPreferenceChangeListener?
  ) = TODO()

  inner class FakeEditor : SharedPreferences.Editor {
    private val tempData = mutableMapOf<String, Any?>()

    override fun putString(key: String, value: String?): SharedPreferences.Editor {
      tempData[key] = value
      return this
    }

    override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor {
      tempData[key] = values
      return this
    }

    override fun remove(key: String): SharedPreferences.Editor {
      tempData.remove(key)
      return this
    }

    override fun putInt(key: String, value: Int): SharedPreferences.Editor {
      tempData[key] = value
      return this
    }

    override fun putLong(key: String, value: Long): SharedPreferences.Editor {
      tempData[key] = value
      return this
    }

    override fun apply() {
      data.clear()
      data.putAll(tempData)
    }

    override fun clear(): SharedPreferences.Editor {
      data.clear()
      return this
    }

    override fun commit(): Boolean {
      data.clear()
      data.putAll(tempData)
      return true
    }

    override fun putBoolean(
      key: String,
      value: Boolean,
    ): SharedPreferences.Editor {
      tempData[key] = value
      return this
    }

    override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
      tempData[key] = value
      return this
    }
  }
}

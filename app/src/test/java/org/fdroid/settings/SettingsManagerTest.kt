package org.fdroid.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.setMain
import org.fdroid.settings.SettingsConstants.PREF_KEY_PROXY
import org.junit.Test

class SettingsManagerTest {

  private val context: Context = mockk(relaxed = true)
  private val prefs: SharedPreferences = FakeSharedPreferences()

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

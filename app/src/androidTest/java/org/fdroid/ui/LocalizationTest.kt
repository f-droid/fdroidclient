package org.fdroid.ui

import android.content.res.Resources
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.IllegalFormatException
import java.util.Locale
import java.util.regex.Pattern
import org.fdroid.R
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs through all the translated strings and tests them with the same format values that the
 * source strings expect. This is to ensure that the formats in the translations are correct in
 * number and in type (e.g. `s` or `s`). It reads the source formats and then builds `formats` to
 * represent the position and type of the formats. Then it runs through all the translations with
 * formats of the correct number and type.
 */
@Suppress("DEPRECATION")
@RunWith(AndroidJUnit4::class)
class LocalizationTest {
  private val androidFormat: Pattern = Pattern.compile("(%[a-z0-9]\\$?[a-z]?)")
  private val locales: Array<Locale> = Locale.getAvailableLocales()
  private val localeNames = HashSet<String?>(locales.size)

  private val context = InstrumentationRegistry.getInstrumentation().targetContext
  private var assets = context.assets
  private var config =
    context.resources.configuration.apply {
      locale = Locale.ENGLISH
    }
  private var resources = Resources(assets, DisplayMetrics(), config)

  @Before
  fun setUp() {
    for (locale in LOCALES_TO_TEST) {
      localeNames.add(locale.toString())
    }
    for (locale in locales) {
      localeNames.add(locale.toString())
    }
  }

  @Test
  @Throws(IllegalAccessException::class)
  fun testLoadAllPlural() {
    val fields = R.plurals::class.java.declaredFields

    val haveFormats = HashMap<String?, String?>()
    for (field in fields) {
      // Log.i(TAG, field.getName());
      val resId = field.getInt(Int::class.javaPrimitiveType)
      val string = resources.getQuantityText(resId, 4)
      // Log.i(TAG, field.getName() + ": '" + string + "'");
      val matcher = androidFormat.matcher(string)
      var matches = 0
      val formats = CharArray(5)
      while (matcher.find()) {
        val match = matcher.group(0)
        val formatType = match!!.get(match.length - 1)
        when (match.length) {
          2 -> {
            formats[matches] = formatType
            matches++
          }
          4 -> formats[match.substring(1, 2).toInt() - 1] = formatType
          5 -> formats[match.substring(1, 3).toInt() - 1] = formatType
          else -> throw IllegalStateException(field.getName() + " has bad format: " + match)
        }
      }
      haveFormats[field.getName()] = String(formats).trim { it <= ' ' }
    }

    for (locale in locales) {
      config.locale = locale
      // Resources() requires DisplayMetrics, but they are only needed for drawables
      resources = Resources(assets, DisplayMetrics(), config)
      for (field in fields) {
        var formats: String? = null
        try {
          val resId = field.getInt(Int::class.javaPrimitiveType)
          for (quantity in 0..566) {
            resources.getQuantityString(resId, quantity)
          }

          formats = haveFormats[field.getName()]
          when (formats) {
            "d" -> resources.getQuantityString(resId, 1, 1)
            "s" -> resources.getQuantityString(resId, 1, "ONE")
            "ds" -> resources.getQuantityString(resId, 2, 1, "TWO")
            else ->
              check(TextUtils.isEmpty(formats)) { "Pattern not included in tests: " + formats }
          }
        } catch (e: IllegalFormatException) {
          Log.i(TAG, locale.toString() + " " + field.getName())
          throw IllegalArgumentException(
            ("Bad '" + formats + "' format in " + locale + " " + field.getName() + ": " + e.message)
          )
        } catch (e: Resources.NotFoundException) {
          Log.i(TAG, locale.toString() + " " + field.getName())
          throw IllegalArgumentException(
            ("Bad '" + formats + "' format in " + locale + " " + field.getName() + ": " + e.message)
          )
        }
      }
    }
  }

  @Test
  @Throws(IllegalAccessException::class)
  fun testLoadAllStrings() {
    val fields = R.string::class.java.declaredFields

    val haveFormats = HashMap<String?, String?>()
    for (field in fields) {
      val string = resources.getString(field.getInt(Int::class.javaPrimitiveType))
      val matcher = androidFormat.matcher(string)
      var matches = 0
      val formats = CharArray(5)
      while (matcher.find()) {
        val match = matcher.group(0)
        val formatType = match!!.get(match.length - 1)
        when (match.length) {
          2 -> {
            formats[matches] = formatType
            matches++
          }
          4 -> formats[match.substring(1, 2).toInt() - 1] = formatType
          5 -> formats[match.substring(1, 3).toInt() - 1] = formatType
          else -> throw IllegalStateException(field.getName() + " has bad format: " + match)
        }
      }
      haveFormats[field.getName()] = String(formats).trim { it <= ' ' }
    }

    for (locale in locales) {
      config!!.locale = locale
      // Resources() requires DisplayMetrics, but they are only needed for drawables
      resources = Resources(assets, DisplayMetrics(), config)
      for (field in fields) {
        val resId = field.getInt(Int::class.javaPrimitiveType)
        resources.getString(resId)

        val formats = haveFormats.get(field.getName())
        try {
          when (formats) {
            "d" -> resources.getString(resId, 1)
            "dd" -> resources.getString(resId, 1, 2)
            "ds" -> resources.getString(resId, 1, "TWO")
            "dds" -> resources.getString(resId, 1, 2, "THREE")
            "sds" -> resources.getString(resId, "ONE", 2, "THREE")
            "s" -> resources.getString(resId, "ONE")
            "ss" -> resources.getString(resId, "ONE", "TWO")
            "sss" -> resources.getString(resId, "ONE", "TWO", "THREE")
            "ssss" -> resources.getString(resId, "ONE", "TWO", "THREE", "FOUR")
            "ssd" -> resources.getString(resId, "ONE", "TWO", 3)
            "sssd" -> resources.getString(resId, "ONE", "TWO", "THREE", 4)
            else ->
              check(TextUtils.isEmpty(formats)) { "Pattern not included in tests: " + formats }
          }
        } catch (e: Exception) {
          Log.i(TAG, locale.toString() + " " + field.getName())
          throw IllegalArgumentException(
            ("Bad format in '" + locale + "' '" + field.getName() + "': " + e.message)
          )
        }
      }
    }
  }

  companion object {
    const val TAG: String = "LocalizationTest"

    val LOCALES_TO_TEST: Array<Locale> =
      arrayOf(
        Locale.ENGLISH,
        Locale.FRENCH,
        Locale.GERMAN,
        Locale.ITALIAN,
        Locale.JAPANESE,
        Locale.KOREAN,
        Locale.SIMPLIFIED_CHINESE,
        Locale.TRADITIONAL_CHINESE,
        Locale("zh", "HK"),
        Locale("bo"),
        Locale("af"),
        Locale("ar"),
        Locale("be"),
        Locale("bg"),
        Locale("ca"),
        Locale("cs"),
        Locale("da"),
        Locale("el"),
        Locale("es"),
        Locale("eo"),
        Locale("et"),
        Locale("eu"),
        Locale("fa"),
        Locale("fi"),
        Locale("he"),
        Locale("hi"),
        Locale("hu"),
        Locale("hy"),
        Locale("id"),
        Locale("is"),
        Locale("it"),
        Locale("ml"),
        Locale("my"),
        Locale("nb"),
        Locale("nl"),
        Locale("pl"),
        Locale("pt"),
        Locale("ro"),
        Locale("ru"),
        Locale("sc"),
        Locale("sk"),
        Locale("sn"),
        Locale("sr"),
        Locale("sv"),
        Locale("th"),
        Locale("tr"),
        Locale("uk"),
        Locale("vi"),
      )
  }
}

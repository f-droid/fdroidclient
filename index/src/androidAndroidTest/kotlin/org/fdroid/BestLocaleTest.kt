package org.fdroid

import androidx.core.os.LocaleListCompat
import androidx.core.os.LocaleListCompat.getEmptyLocaleList
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.fdroid.LocaleChooser.getBestLocale
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Needs to run on-device to get access to real [LocaleListCompat.getFirstMatch].
 */
@RunWith(AndroidJUnit4::class)
internal class BestLocaleTest {

    @Test
    fun testEmptyLocalesReturnsNull() {
        assertNull(emptyMap<String, String>().getBestLocale(getLocaleList("en-US, de-DE")))
        assertNull(getMap("en-US, de-DE").getBestLocale(getEmptyLocaleList()))
    }

    @Test
    fun testFallbackToEn() {
        assertEquals(
            "en",
            getMap("de-AT", "de-DE", "en").getBestLocale(getLocaleList("fr-FR")),
        )
    }

    /**
     * Ported from old LocaleSelectionTest.
     */
    @Test
    fun testListConversion() {
        // just select the matching en-US locale, nothing special here
        assertEquals(
            "en-US",
            getMap("de-AT", "de-DE", "en-US").getBestLocale(getLocaleList("en-US, de-DE")),
        )

        // fall back to another en locale before de
        assertEquals(
            "en-US",
            getMap("de-AT", "de-DE", "en-US").getBestLocale(getLocaleList("en-SE,de-DE")),
        )

        // full match against a non-default locale
        assertEquals(
            "de-AT",
            getMap("de-AT", "de-DE", "en-GB", "en-US").getBestLocale(getLocaleList("de-AT,de-DE")),
        )
        assertEquals(
            "de",
            getMap("de-AT", "de", "en-GB", "en-US").getBestLocale(getLocaleList("de-CH,en-US")),
        )

        // no match at all, fall back to an english locale
        assertEquals(
            "en-US",
            getMap("en", "en-US").getBestLocale(getLocaleList("zh-Hant-TW,zh-Hans-CN")),
        )

        // handle stripped script (Hans/Hant)
        assertEquals(
            "zh-TW",
            getMap("en-US",
                "zh-CN",
                "zh-HK",
                "zh-TW").getBestLocale(getLocaleList("zh-Hant-TW,zh-Hans-CN")),
        )
        assertEquals(
            "zh-CN",
            getMap("en-US", "zh-CN").getBestLocale(getLocaleList("zh-Hant-TW,zh-Hans-CN")),
        )

        // https://developer.android.com/guide/topics/resources/multilingual-support#resource-resolution-examples
        assertEquals(
            "fr-FR",
            getMap("en-US", "de-DE", "es-ES", "fr-FR", "it-IT")
                .getBestLocale(getLocaleList("fr-CH")),
        )

        // https://developer.android.com/guide/topics/resources/multilingual-support#t-2d-choice
        assertEquals(
            "it-IT",
            getMap("en-US", "de-DE", "es-ES", "it-IT")
                .getBestLocale(getLocaleList("fr-CH,it-CH")),
        )
    }

    private fun getLocaleList(tags: String): LocaleListCompat {
        return LocaleListCompat.forLanguageTags(tags)
    }

    private fun getMap(vararg locales: String): Map<String, String> {
        return locales.associate { Pair(it, it) }
    }

}

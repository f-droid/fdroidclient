package org.fdroid

import android.os.Build
import androidx.core.os.LocaleListCompat
import androidx.core.os.LocaleListCompat.getEmptyLocaleList
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.fdroid.LocaleChooser.getBestLocale
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Needs to run on-device to get access to real [LocaleListCompat.getFirstMatch].
 */
@RunWith(AndroidJUnit4::class)
internal class BestLocaleTest {

    @Before
    fun check() {
        // Locale lists were introduced in SDK 24
        assumeTrue(Build.VERSION.SDK_INT >= 24)
    }

    @Test
    fun testEmptyLocalesReturnsNull() {
        assertNull(emptyMap<String, String>().getBestLocale(getLocaleList("en-US,de-DE")))
    }

    @Test
    fun testFallbackToEn() {
        assertEquals(
            "en-US",
            getMap("fr-FR", "en-US", "de-DE").getBestLocale(getEmptyLocaleList())
        )

        assertEquals(
            "en",
            getMap("de-AT", "de-DE", "en").getBestLocale(getLocaleList("fr-FR")),
        )
    }

    @Test
    fun testFallbackToFirst() {
        assertEquals(
            "de-AT",
            getMap("de-AT", "de-DE", "uk").getBestLocale(getLocaleList("fr-FR")),
        )
    }

    @Test
    fun testMatchLanguageAndScript() {
        assertEquals(
            "en",
            getMap("en-Shaw", "en-Shaw-US", "en-GB", "en").getBestLocale(getLocaleList("en-NL")),
        )

        assertEquals(
            "sr-Cyrl",
            getMap("en", "sr-Cyrl", "sr-Latn").getBestLocale(getLocaleList("sr-RS")),
        )

        assertEquals(
            "uz-Latn",
            getMap("en", "uz-Cyrl", "uz-Latn").getBestLocale(getLocaleList("uz")),
        )

        assertEquals(
            "zh-Hant",
            getMap("en", "zh-Hans", "zh-Hant").getBestLocale(getLocaleList("zh-TW")),
        )

        assertEquals(
            "sr-Latn",
            getMap("en", "sr", "sr-RS", "sr-Latn").getBestLocale(getLocaleList("sr-Latn-RS")),
        )

        assertEquals(
            "uz-Cyrl",
            getMap("en", "uz", "uz-Cyrl").getBestLocale(getLocaleList("uz-Cyrl-UZ")),
        )

        assertEquals(
            "zh-Hant",
            getMap("en", "zh", "zh-Hant").getBestLocale(getLocaleList("zh-TW")),
        )

        assertEquals(
            "zh-TW",
            getMap("zh", "zh-CN", "zh-TW", "en").getBestLocale(getLocaleList("zh-HK,de")),
        )

        assertEquals(
            "zh-Hans",
            getMap("en", "zh-Hant", "zh-Hans").getBestLocale(getLocaleList("zh")),
        )

        assertEquals(
            "zh-Hant",
            getMap("en", "zh-Hans", "zh-Hant").getBestLocale(getLocaleList("zh-HK")),
        )

        assertEquals(
            "de",
            getMap("zh", "de", "en").getBestLocale(getLocaleList("zh-HK,de")),
        )

        assertEquals(
            "zh-HK",
            getMap("zh", "zh-CN", "zh-TW", "zh-HK").getBestLocale(getLocaleList("zh-Hant-HK")),
        )

        assertEquals(
            "zh-Hant-HK",
            getMap(
                "zh",
                "zh-Hans-CN",
                "zh-Hant-TW",
                "zh-Hant-HK"
            ).getBestLocale(getLocaleList("zh-HK")),
        )
    }

    @Test
    fun testRankingPriority() {
        // an exact match is the best match (and calls it a day)
        assertEquals(
            "en-US",
            getMap(
                "en-Shaw-US",
                "en-Latn",
                "en",
                "en-US",
                "en-Latn-US"
            ).getBestLocale(getLocaleList("en-US")),
        )

        assertEquals(
            "zh-TW",
            getMap(
                "zh",
                "zh-CN",
                "zh-Hant",
                "zh-Hant-HK",
                "zh-TW"
            ).getBestLocale(getLocaleList("zh-TW")),
        )

        // else dive into the haystack in reverse order of specificity -- from specific to generic,
        // starting from the most specific form: language-script-country
        assertEquals(
            "zh-Hant-HK",
            getMap("zh", "zh-CN", "zh-Hant", "zh-Hant-HK").getBestLocale(getLocaleList("zh-HK")),
        )

        // followed by language-country and language-script
        assertEquals(
            "zh-TW",
            getMap("zh", "zh-CN", "zh-Hant", "zh-TW").getBestLocale(getLocaleList("zh-Hant-TW")),
        )

        assertEquals(
            "sr-RS",
            getMap("en", "sr", "sr-Latn", "sr-RS").getBestLocale(getLocaleList("sr-Cyrl-RS")),
        )

        assertEquals(
            "zh-MO",
            getMap("en", "zh", "zh-Hant", "zh-MO").getBestLocale(getLocaleList("zh-Hant-MO")),
        )

        assertEquals(
            "zh-Hans",
            getMap("en", "zh", "zh-Hans", "zh-MO").getBestLocale(getLocaleList("zh-Hans-MO")),
        )

        assertEquals(
            "zh-Hant",
            getMap("zh", "zh-CN", "zh-Hant", "zh-Hant-HK").getBestLocale(getLocaleList("zh-TW")),
        )

        assertEquals(
            "sr-Latn",
            getMap("en", "sr", "sr-Latn", "sr-RS").getBestLocale(getLocaleList("sr-Latn-RS")),
        )

        assertEquals(
            "en-Latn",
            getMap(
                "en-Shaw-US",
                "en",
                "en-US",
                "en-Latn",
                "en-Latn-US"
            ).getBestLocale(getLocaleList("en-GB")),
        )

        // finally language only if script matches
        assertEquals(
            "de",
            getMap("zh", "zh-CN", "en", "de").getBestLocale(getLocaleList("zh-HK,de")),
        )

        assertEquals(
            "fr",
            getMap("zh", "en", "fr").getBestLocale(getLocaleList("en-Shaw-GB,fr")),
        )

        assertEquals(
            "en",
            getMap("fr", "en", "sr").getBestLocale(getLocaleList("sr-Latn-RS,en")),
        )

        // failing which the first one with same script wins
        assertEquals(
            "en-GB",
            getMap("en-Shaw-US", "en-GB", "en-US").getBestLocale(getLocaleList("en-NL")),
        )

        assertEquals(
            "en-AR",
            getMap("en-AR", "en-GB", "en-US").getBestLocale(getLocaleList("en-NL")),
        )

        assertEquals(
            "zh-HK",
            getMap("en", "zh", "zh-CN", "zh-HK", "zh-TW").getBestLocale(getLocaleList("zh-MO")),
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
            getMap("de-AT", "de-DE", "en-US").getBestLocale(getLocaleList("en-US,de-DE")),
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
            getMap(
                "en-US",
                "zh-CN",
                "zh-HK",
                "zh-TW",
            ).getBestLocale(getLocaleList("zh-Hant-TW,zh-Hans-CN")),
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

    @Test
    fun testInvalidLocales() {
        // underscores
        assertEquals(
            "en-US",
            getMap("de_AT", "de_DE", "en-US").getBestLocale(getLocaleList("en-US,de-DE")),
        )

        // different case
        assertEquals(
            "en-US",
            getMap("DE_at", "dE_De", "en-US").getBestLocale(getLocaleList("en-US,de-DE")),
        )

        // garbage in given locales
        assertEquals(
            "en-US",
            getMap(
                "foo-Bar",
                "324;kfj4297h4c2oj",
                "2342142143",
                "de_DE",
                "#$%#!$^#&^%#*",
                "en-US",
            ).getBestLocale(getLocaleList("en-US,de-DE")),
        )
    }

    private fun getLocaleList(tags: String): LocaleListCompat {
        return LocaleListCompat.forLanguageTags(tags)
    }

    private fun getMap(vararg locales: String): Map<String, String> {
        return locales.associate { Pair(it, it) }
    }

}

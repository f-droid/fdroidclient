package org.fdroid

import androidx.core.os.LocaleListCompat
import androidx.core.text.ICUCompat
import org.fdroid.index.v2.LocalizedFileListV2
import org.fdroid.index.v2.LocalizedFileV2
import org.fdroid.index.v2.LocalizedTextV2
import java.util.Locale

public object LocaleChooser {

    /**
     * Gets the best localization for the given [localeList]
     * from collections like [LocalizedTextV2], [LocalizedFileV2], or [LocalizedFileListV2].
     */
    public fun <T> Map<String, T>?.getBestLocale(localeList: LocaleListCompat): T? {
        if (isNullOrEmpty()) return null
        if (size == 1) return values.first()
        return when (localeList.size()) {
            0 -> null
            1 -> localeList.get(0)
            else -> localeList.getFirstMatch(keys.toTypedArray())
        }?.let { firstMatch ->
            // try first matched tag first (usually has region tag, e.g. de-DE)
            get(firstMatch.toLanguageTag()) ?: run {
                // search by ranking priority if no exact match is found,
                // determining its script if not supplied
                val tried = (if (firstMatch.script.isNullOrEmpty()) 0 else 1) +
                    (if (firstMatch.country.isNullOrEmpty()) 0 else 2)
                if (firstMatch.script.isNullOrEmpty()) {
                    ICUCompat.maximizeAndGetScript(firstMatch)?.takeUnless { it.isEmpty() }
                        ?.let { script -> getInRankingOrder(firstMatch, tried + 1, script, tried) }
                } else {
                    if (tried > 1) {
                        getInRankingOrder(firstMatch, tried - 1, firstMatch.script, tried)
                    } else {
                        null
                    }
                }
                    // then language and other countries if script matches
                    ?: (if (tried == 0) null else get(firstMatch.language)?.takeIf { _ ->
                        LocaleListCompat.matchesLanguageAndScript(
                            getLocale(firstMatch.language),
                            firstMatch
                        )
                    }) ?: getFirstSameScript(firstMatch)
            }
        }
            // or English and then just take the first of the list
            ?: get("en-US") ?: get("en") ?: values.first()
    }

    private tailrec fun <T> Map<String, T>.getInRankingOrder(
        locale: Locale,
        rank: Int,
        script: String?,
        tried: Int
    ): T? {
        if (rank <= 0) return null
        if (rank != tried) getRankingTag(locale, rank, script)?.let { get(it) }?.let { return it }
        return getInRankingOrder(locale, rank - 1, script, tried)
    }

    private fun <T> Map<String, T>.getFirstSameScript(locale: Locale): T? {
        val langLen = locale.language.length
        entries.forEach { (key, value) ->
            if (key.length > langLen &&
                key.startsWith(locale.language) &&
                key[langLen] == '-' &&
                LocaleListCompat.matchesLanguageAndScript(Locale.forLanguageTag(key), locale)
            ) return value
        }
        return null
    }

    private fun getRankingTag(locale: Locale, rank: Int, script: String?): String? {
        if (rank >= 2 && locale.country.isNullOrEmpty()) return null
        if (rank != 2 && script.isNullOrEmpty()) return null
        return when (rank) {
            3 -> "${locale.language}-$script-${locale.country}"
            2 -> if (script.isNullOrEmpty() ||
                script.equals(
                    ICUCompat.maximizeAndGetScript(getLocale(locale.language, locale.country)),
                    true
                )
            ) "${locale.language}-${locale.country}" else null

            1 -> "${locale.language}-$script"
            else -> null
        }
    }

    private fun getLocale(language: String, country: String = "") =
        if (android.os.Build.VERSION.SDK_INT >= 36) {
            Locale.of(language, country)
        } else {
            @Suppress("DEPRECATION")
            Locale(language, country)
        }

}

package org.fdroid.ui.search

import java.text.Normalizer
import java.text.Normalizer.Form.NFKD

object SearchHelper {
  private val normalizerRegex = "\\p{M}".toRegex()

  /** Normalizes the string by removing any diacritics that may appear. */
  fun String.normalize(): String {
    if (Normalizer.isNormalized(this, NFKD)) return this
    return Normalizer.normalize(this, NFKD).replace(normalizerRegex, "")
  }

  /** Removes zero-width spaces from the string. Useful when string is copy and pasted. */
  fun String.removeZeroWhiteSpace(): String = this.replace("\u200B", "")

  /**
   * Normalize the query by removing diacritics and adding zero-width spaces after ideographic
   * characters.
   */
  fun fixQuery(query: String): String = addZeroWhiteSpaceIfNeeded(query.normalize())

  /**
   * Inserts a zero-width space after each ideographic character in the query. This is needed,
   * because for Fts4 search in the DB, we insert zero white space characters between tokens to fake
   * tokenization. However, when doing more naive [String.contains] searches, we need to add those
   * also to the query, otherwise nothing will be found.
   */
  private fun addZeroWhiteSpaceIfNeeded(query: String): String = buildString {
    query.forEachIndexed { i, char ->
      if (Character.isIdeographic(char.code) && i + 1 < query.length) {
        append(char)
        append("\u200B")
      } else {
        append(char)
      }
    }
  }
}

package org.fdroid.database

/**
 * Rewrites search queries so that best results with sqlite Fts4 are obtained. Uses prefix searches
 * and camel case searches for latin chars, and separate character searches for CJK chars. Also
 * employing our zero whitespace hack.
 *
 * Attention: Quotes should be removed from the query before passing it in.
 *
 * see https://www.sqlite.org/fts3.html#full_text_index_queries
 */
public object SearchQueryRewriter {

  public fun rewriteQuery(query: String): String {
    val splits = query.split(' ').filter { it.isNotBlank() }
    var hasAnyCjk = false
    return splits
      .joinToString(" ") { word ->
        var isCjk = false
        // go through word and separate CJK chars (if needed)
        val newString =
          word.toList().joinToString("") {
            if (Character.isIdeographic(it.code)) {
              isCjk = true
              hasAnyCjk = true
              "$it* "
            } else "$it"
          }
        // add * to enable prefix matches
        if (isCjk) newString.trimEnd() else "$newString*"
      }
      .let { firstPassQuery ->
        // if we had more than one word, make a more complex query
        if (splits.size > 1 && !hasAnyCjk) {
          "$firstPassQuery " + // search* term* (implicit AND and prefix search)
            "OR ${splits.joinToString("")}* " + // camel case prefix
            "OR \"${splits.joinToString("* ")}*\"" // phrase query
        } else if (hasAnyCjk) {
          val zeroSplits =
            splits.map { word ->
              if (word.any { Character.isIdeographic(it.code) }) {
                // separate CJK chars with zero-width
                word.toList().joinToString("\u200B")
              } else {
                word
              }
            }
          // query using zero-width concatenation needs to be quoted as a phrase query
          val zeroQuery = zeroSplits.joinToString(" ") { "\"$it*\"" }
          "$firstPassQuery " + // search* term* (implicit AND and prefix search)
            "OR $zeroQuery " + // zero whitespace concat as in DB
            "OR ${splits.joinToString("* ")}*" // verbatim prefix for authorName searches
        } else {
          firstPassQuery
        }
      }
  }
}

package org.fdroid.database

import kotlin.test.assertEquals
import org.junit.Test

internal class SearchQueryRewriterTest {

  @Test
  fun rewritesBlankQueryToBlank() {
    assertEquals("", SearchQueryRewriter.rewriteQuery("   "))
  }

  @Test
  fun rewritesSingleLatinWordToPrefixQuery() {
    assertEquals("foo*", SearchQueryRewriter.rewriteQuery("foo"))
  }

  @Test
  fun rewritesMultipleLatinWordsWithCamelCaseAndPhrase() {
    assertEquals(
      "foo* bar* OR foobar* OR \"foo* bar*\"",
      SearchQueryRewriter.rewriteQuery("foo bar"),
    )
  }

  @Test
  fun rewritesMultipleLatinWordsAndIgnoresExtraWhitespace() {
    assertEquals(
      "foo* bar* OR foobar* OR \"foo* bar*\"",
      SearchQueryRewriter.rewriteQuery("  foo   bar  "),
    )
  }

  @Test
  fun rewritesSingleCjkWordWithZeroWidthAndVerbatimAlternatives() {
    assertEquals("測* 試* OR \"測\u200B試*\" OR 測試*", SearchQueryRewriter.rewriteQuery("測試"))
  }

  @Test
  fun rewritesMultiWordCjkQuery() {
    assertEquals(
      "測* 試* 艾* 星* OR \"測\u200B試*\" \"艾\u200B星*\" OR 測試* 艾星*",
      SearchQueryRewriter.rewriteQuery("測試 艾星"),
    )
  }

  @Test
  fun rewritesMixedLatinAndCjkWordsUsingCjkBranch() {
    assertEquals(
      "foo* 測* 試* OR \"foo*\" \"測\u200B試*\" OR foo* 測試*",
      SearchQueryRewriter.rewriteQuery("foo 測試"),
    )
  }
}

package org.fdroid.ui.details

import org.junit.Test
import kotlin.test.assertEquals

/**
 * Tests modifications to app details descriptions done in [getHtmlDescription].
 */
class HtmlDescriptionTest {
    @Test
    fun testLinks() {
        val description = """
2. If you have experience with Java and the Android SDK, then we look forward to your contributions! More info: https://mediawiki.org/wiki/Wikimedia_Apps/Team/Android/App_hacking

3. Explanation of permissions needed by the app: https://mediawiki.org/wiki/Wikimedia_Apps/Android_FAQ#Security_and_Permissions
        """
        val expectedDescription = """<br>
2. If you have experience with Java and the Android SDK, then we look forward to your contributions! More info: <a href="https://mediawiki.org/wiki/Wikimedia_Apps/Team/Android/App_hacking">https://mediawiki.org/wiki/Wikimedia_Apps/Team/Android/App_hacking</a><br>
<br>
3. Explanation of permissions needed by the app: <a href="https://mediawiki.org/wiki/Wikimedia_Apps/Android_FAQ#Security_and_Permissions">https://mediawiki.org/wiki/Wikimedia_Apps/Android_FAQ#Security_and_Permissions</a><br>
        """
        assertEquals(expectedDescription, getHtmlDescription(description))
    }

    @Test
    fun testLinkAtTheVeryEnd() {
        val description = """
Project page: https://github.com/lukaspieper/Gcam-Services-Provider"""
        val expectedDescription = """<br>
Project page: <a href="https://github.com/lukaspieper/Gcam-Services-Provider">https://github.com/lukaspieper/Gcam-Services-Provider</a>"""
        assertEquals(expectedDescription, getHtmlDescription(description))
    }

    @Test
    fun testLinkWithDotAtTheEnd() {
        val description = """please visit our website: https://wikimediafoundation.org/."""

        @Suppress("ktlint:standard:max-line-length")
        val expectedDescription = """please visit our website: <a href="https://wikimediafoundation.org/">https://wikimediafoundation.org/</a>."""
        assertEquals(expectedDescription, getHtmlDescription(description))
    }

    @Test
    fun testLinkInRoundBrackets() {
        val description = """our link (https://wikimediafoundation.org/)."""

        @Suppress("ktlint:standard:max-line-length")
        val expectedDescription = """our link (<a href="https://wikimediafoundation.org/">https://wikimediafoundation.org/</a>)."""
        assertEquals(expectedDescription, getHtmlDescription(description))
    }

    @Test
    fun testHeadlineRemoval() {
        val description = """<h1>SimpleX - the first messaging platform that has no user identifiers, not even random numbers!</h1>
<p><a href="https://simplex.chat/blog/20221108-simplex-chat-v4.2-security-audit-new-website.html" target="_blank">Security assessment</a> was done by Trail of Bits in November 2022.</p>
<p>SimpleX Chat features:</p>
<ul>
  <li>end-to-end encrypted messages, with editing, replies and deletion of messages.</li>
  <li>sending end-to-end encrypted images and files.</li>"""

        val expectedDescription = """SimpleX - the first messaging platform that has no user identifiers, not even random numbers!<br>
<p><a href="https://simplex.chat/blog/20221108-simplex-chat-v4.2-security-audit-new-website.html" target="_blank">Security assessment</a> was done by Trail of Bits in November 2022.</p>
<p>SimpleX Chat features:</p>
<ul>
  <li>end-to-end encrypted messages, with editing, replies and deletion of messages.</li>
  <li>sending end-to-end encrypted images and files.</li>"""
        assertEquals(expectedDescription, getHtmlDescription(description))
    }
}

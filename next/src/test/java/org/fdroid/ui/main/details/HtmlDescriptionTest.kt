package org.fdroid.ui.main.details

import org.fdroid.ui.details.getHtmlDescription
import org.junit.Test
import kotlin.test.assertEquals

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
    fun testLinkWithDotAtTheEnd() {
        val description = """please visit our website: https://wikimediafoundation.org/."""
        val expectedDescription = """please visit our website: <a href="https://wikimediafoundation.org/">https://wikimediafoundation.org/</a>."""
        assertEquals(expectedDescription, getHtmlDescription(description))
    }
}

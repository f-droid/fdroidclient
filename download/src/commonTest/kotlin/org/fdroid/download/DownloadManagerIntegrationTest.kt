package org.fdroid.download

import kotlinx.coroutines.runBlocking
import org.fdroid.getRandomString
import kotlin.test.Test
import kotlin.test.assertEquals

class DownloadManagerIntegrationTest {

    private val userAgent = getRandomString()
    private val mirrors = listOf(Mirror("http://example.org"), Mirror("http://example.net/"))
    private val downloadRequest = DownloadRequest("", mirrors)

    @Test
    fun testResumeOnExample() = runBlocking {
        val downloadManager = DownloadManager(userAgent, null)

        val lastLine = downloadManager.getBytes(downloadRequest, 1248).decodeToString()
        assertEquals("</html>\n", lastLine)
    }
}

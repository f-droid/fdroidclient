package org.fdroid.download

import io.ktor.util.toByteArray
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

        val lastLine = downloadManager.get(downloadRequest, 1248).toByteArray().decodeToString()
        assertEquals("</html>\n", lastLine)
    }
}

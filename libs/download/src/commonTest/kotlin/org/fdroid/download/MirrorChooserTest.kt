package org.fdroid.download

import io.mockk.every
import io.mockk.mockk
import kotlinx.io.IOException
import org.fdroid.getIndexFile
import org.fdroid.runSuspend
import java.net.SocketTimeoutException
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MirrorChooserTest {

    private val mirrors = listOf(
        Mirror("foo"),
        Mirror("bar"),
        Mirror("42"),
        Mirror("1337"))
    private val mirrorsLocation = listOf(
        Mirror(baseUrl = "unknown_1", countryCode = null),
        Mirror(baseUrl = "unknown_2", countryCode = null),
        Mirror(baseUrl = "unknown_3", countryCode = null),
        Mirror(baseUrl = "local_1", countryCode = "HERE"),
        Mirror(baseUrl = "local_2", countryCode = "HERE"),
        Mirror(baseUrl = "local_3", countryCode = "HERE"),
        Mirror(baseUrl = "remote_1", countryCode = "THERE"),
        Mirror(baseUrl = "remote_2", countryCode = "THERE"),
        Mirror(baseUrl = "remote_3", countryCode = "THERE"))
    private val downloadRequest = DownloadRequest("foo", mirrors)
    private val downloadRequestLocation = DownloadRequest("location", mirrorsLocation)
    private val downloadRequestTryFIrst = DownloadRequest(
        path = "location",
        mirrors = mirrorsLocation,
        tryFirstMirror = Mirror(baseUrl = "remote_1", countryCode = "THERE"))

    private val ipfsIndexFile = getIndexFile(name = "foo", ipfsCidV1 = "CIDv1")

    @Test
    fun testMirrorChooserDefaultImpl() = runSuspend {
        val mirrorChooser = MirrorChooserRandom()
        val expectedResult = Random.nextInt()

        val result = mirrorChooser.mirrorRequest(downloadRequest) { mirror, url ->
            assertTrue { mirrors.contains(mirror) }
            assertEquals(mirror.getUrl(downloadRequest.indexFile.name), url)
            expectedResult
        }
        assertEquals(expectedResult, result)
    }

    @Test
    fun testFallbackToNextMirrorWithIOException() = runSuspend {
        val mirrorChooser = MirrorChooserRandom()
        val expectedResult = Random.nextInt()

        val result = mirrorChooser.mirrorRequest(downloadRequest) { mirror, url ->
            assertEquals(mirror.getUrl(downloadRequest.indexFile.name), url)
            // fails with all except last mirror
            if (mirror != downloadRequest.mirrors.last()) throw IOException("foo")
            expectedResult
        }
        assertEquals(expectedResult, result)
    }

    @Test
    fun testFallbackToNextMirrorWithSocketTimeoutException() = runSuspend {
        val mirrorChooser = MirrorChooserRandom()
        val expectedResult = Random.nextInt()

        val result = mirrorChooser.mirrorRequest(downloadRequest) { mirror, url ->
            assertEquals(mirror.getUrl(downloadRequest.indexFile.name), url)
            // fails with all except last mirror
            if (mirror != downloadRequest.mirrors.last()) throw SocketTimeoutException("foo")
            expectedResult
        }
        assertEquals(expectedResult, result)
    }

    @Test
    fun testFallbackToNextMirrorWithNoResumeException() = runSuspend {
        val mirrorChooser = MirrorChooserRandom()
        val expectedResult = Random.nextInt()

        val result = mirrorChooser.mirrorRequest(downloadRequest) { mirror, url ->
            assertEquals(mirror.getUrl(downloadRequest.indexFile.name), url)
            // fails with all except last mirror
            if (mirror != downloadRequest.mirrors.last()) throw NoResumeException()
            expectedResult
        }
        assertEquals(expectedResult, result)
    }

    @Test
    fun testMirrorChooserRandom() {
        val mirrorChooser = MirrorChooserRandom()

        val orderedMirrors = mirrorChooser.orderMirrors(downloadRequest)

        // set of input mirrors is equal to set of output mirrors
        assertEquals(mirrors.toSet(), orderedMirrors.toSet())
    }

    @Test
    fun testMirrorChooserRandomRespectsTryFirstMirror() {
        val mirrorChooser = MirrorChooserRandom()

        val tryFirstRequest = downloadRequest.copy(tryFirstMirror = Mirror("42"))
        val orderedMirrors = mirrorChooser.orderMirrors(tryFirstRequest)

        // try-first mirror is first in list
        assertEquals(tryFirstRequest.tryFirstMirror, orderedMirrors[0])
        // set of input mirrors is equal to set of output mirrors
        assertEquals(mirrors.toSet(), orderedMirrors.toSet())
    }

    @Test
    fun testMirrorChooserRandomIgnoresMissingTryFirstMirror() {
        val mirrorChooser = MirrorChooserRandom()

        val tryFirstRequest = downloadRequest.copy(tryFirstMirror = Mirror("missing"))
        val orderedMirrors = mirrorChooser.orderMirrors(tryFirstRequest)

        // set of input mirrors is equal to set of output mirrors
        assertEquals(mirrors.toSet(), orderedMirrors.toSet())
    }

    @Test
    fun testMirrorChooserIgnoresIpfsGatewayIfNoCid() = runSuspend {
        val mirrorChooser = object : MirrorChooserImpl() {
            override fun orderMirrors(downloadRequest: DownloadRequest): List<Mirror> {
                return downloadRequest.mirrors // keep mirror list stable, no random please
            }
        }
        val mirrors = listOf(
            Mirror("http://ipfs.com", isIpfsGateway = true),
            Mirror("http://example.com", isIpfsGateway = false),
        )
        val ipfsRequest = downloadRequest.copy(mirrors = mirrors)

        val result = mirrorChooser.mirrorRequest(ipfsRequest) { _, url ->
            url.toString()
        }
        assertEquals("http://example.com/foo", result)
    }

    @Test
    fun testMirrorChooserThrowsIfOnlyIpfsGateways() = runSuspend {
        val mirrorChooser = MirrorChooserRandom()
        val mirrors = listOf(
            Mirror("foo/bar", isIpfsGateway = true),
            Mirror("bar/foo", isIpfsGateway = true),
        )
        val ipfsRequest = downloadRequest.copy(mirrors = mirrors)

        val e = assertFailsWith<IOException> {
            mirrorChooser.mirrorRequest(ipfsRequest) { _, _ ->
            }
        }
        assertEquals("Got IPFS gateway without CID", e.message)
    }

    @Test
    fun testMirrorChooserDomesticLocation() {
        val mockManager = mockk<MirrorParameterManager>(relaxed = true)
        every { mockManager.getCurrentLocation() } returns "HERE"
        every { mockManager.preferForeignMirrors() } returns false

        val mirrorChooser = MirrorChooserWithParameters(mockManager)

        // test domestic mirror preference
        val domesticList = mirrorChooser.orderMirrors(downloadRequestLocation)
        // confirm the list contains all mirrors
        assertEquals(9, domesticList.size)
        // mirrors that are local should be included first
        assertEquals("HERE", domesticList[0].countryCode)
        assertEquals("HERE", domesticList[1].countryCode)
        assertEquals("HERE", domesticList[2].countryCode)
        assertEquals(null, domesticList[3].countryCode)
    }

    @Test
    fun testMirrorChooserForeignLocation() {
        val mockManager = mockk<MirrorParameterManager>(relaxed = true)
        every { mockManager.getCurrentLocation() } returns "HERE"
        every { mockManager.preferForeignMirrors() } returns true

        val mirrorChooser = MirrorChooserWithParameters(mockManager)

        // test foreign mirror preference
        val foreignList = mirrorChooser.orderMirrors(downloadRequestLocation)
        // confirm the list contains all mirrors
        assertEquals(9, foreignList.size)
        // mirrors that are remote should be included first
        assertEquals("THERE", foreignList[0].countryCode)
        assertEquals("THERE", foreignList[1].countryCode)
        assertEquals("THERE", foreignList[2].countryCode)
        assertEquals(null, foreignList[3].countryCode)
    }

    @Test
    fun testMirrorChooserErrorSort() {
        val mockManager = mockk<MirrorParameterManager>(relaxed = true)
        every { mockManager.getCurrentLocation() } returns "HERE"
        every { mockManager.preferForeignMirrors() } returns false
        every { mockManager.getMirrorErrorCount("local_1") } returns 5
        every { mockManager.getMirrorErrorCount("local_2") } returns 3
        every { mockManager.getMirrorErrorCount("local_3") } returns 1

        val mirrorChooser = MirrorChooserWithParameters(mockManager)

        // test error sorting with domestic mirror preference
        val orderedList = mirrorChooser.orderMirrors(downloadRequestLocation)
        // confirm the list contains all mirrors
        assertEquals(9, orderedList.size)
        // mirrors that have fewer errors should be included first
        assertEquals("local_3", orderedList[0].baseUrl)
        assertEquals("local_2", orderedList[1].baseUrl)
        assertEquals("local_1", orderedList[2].baseUrl)
    }

    @Test
    fun testMirrorChooserDomesticWithTryFirst() {
        val mockManager = mockk<MirrorParameterManager>(relaxed = true)
        every { mockManager.getCurrentLocation() } returns "HERE"
        every { mockManager.preferForeignMirrors() } returns false

        val mirrorChooser = MirrorChooserWithParameters(mockManager)

        // test tryfirst mirror parameter
        val tryFirstList = mirrorChooser.orderMirrors(downloadRequestTryFIrst)
        // confirm the list contains all mirrors
        assertEquals(9, tryFirstList.size)
        // tryfirst mirror should be included before local mirrors
        assertEquals("remote_1", tryFirstList[0].baseUrl)
        assertEquals("HERE", tryFirstList[1].countryCode)
        assertEquals("HERE", tryFirstList[2].countryCode)
        assertEquals("HERE", tryFirstList[3].countryCode)
    }

    @Test
    fun testMirrorChooserRandomization() {
        val mockManager = mockk<MirrorParameterManager>(relaxed = true)
        every { mockManager.getCurrentLocation() } returns "HERE"
        every { mockManager.preferForeignMirrors() } returns false

        val mirrorChooser = MirrorChooserWithParameters(mockManager)

        // repeat test to verify that if error count is equal the order isn't always the same
        var count1 = 0
        var count2 = 0
        var count3 = 0
        var countX = 0
        repeat(100) {
            // test error sorting with domestic mirror preference
            val orderedList = mirrorChooser.orderMirrors(downloadRequestLocation)
            if (orderedList[0].baseUrl.equals("local_1")) {
                count1++
            } else if (orderedList[0].baseUrl.equals("local_2")) {
                count2++
            } else if (orderedList[0].baseUrl.equals("local_3")) {
                count3++
            } else {
                countX++
            }
        }
        // all domestic urls should have appeared first in the list at least once
        assertTrue { count1 > 0 }
        assertTrue { count2 > 0 }
        assertTrue { count3 > 0 }
        // no foreign urls should should have appeared first in the list
        assertEquals(0, countX)
    }
}

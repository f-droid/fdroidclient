package org.fdroid.download

import io.mockk.every
import io.mockk.mockk
import java.net.SocketTimeoutException
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.io.IOException
import org.fdroid.runSuspend

class MirrorChooserTest {

  private val mirrors = listOf(Mirror("foo"), Mirror("bar"), Mirror("42"), Mirror("1337"))
  private val mirrorsLocation =
    listOf(
      Mirror(baseUrl = "http://test.unknown1.org", countryCode = null),
      Mirror(baseUrl = "http://test.unknown2.org", countryCode = null),
      Mirror(baseUrl = "http://test.unknown3.org", countryCode = null),
      Mirror(baseUrl = "http://test.local1.org", countryCode = "HERE"),
      Mirror(baseUrl = "http://test.local2.org", countryCode = "HERE"),
      Mirror(baseUrl = "http://test.local3.org", countryCode = "HERE"),
      Mirror(baseUrl = "http://test.remote1.org", countryCode = "THERE"),
      Mirror(baseUrl = "http://test.remote2.org", countryCode = "THERE"),
      Mirror(baseUrl = "http://test.remote3.org", countryCode = "THERE"),
    )
  private val downloadRequest = DownloadRequest("foo", mirrors)
  private val downloadRequestLocation = DownloadRequest("location", mirrorsLocation)
  private val downloadRequestTryFirst =
    DownloadRequest(
      path = "location",
      mirrors = mirrorsLocation,
      tryFirstMirror = Mirror(baseUrl = "http://test.remote1.org", countryCode = "THERE"),
    )

  @Test
  fun testMirrorChooserDefaultImpl() = runSuspend {
    val mirrorChooser = MirrorChooserRandom()
    val expectedResult = Random.nextInt()

    val result =
      mirrorChooser.mirrorRequest(downloadRequest) { mirror, url ->
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

    val result =
      mirrorChooser.mirrorRequest(downloadRequest) { mirror, url ->
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

    val result =
      mirrorChooser.mirrorRequest(downloadRequest) { mirror, url ->
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

    val result =
      mirrorChooser.mirrorRequest(downloadRequest) { mirror, url ->
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
    val mirrorChooser =
      object : MirrorChooserImpl() {
        override fun orderMirrors(downloadRequest: DownloadRequest): List<Mirror> {
          return downloadRequest.mirrors // keep mirror list stable, no random please
        }
      }
    val mirrors =
      listOf(
        Mirror("http://ipfs.com", isIpfsGateway = true),
        Mirror("http://example.com", isIpfsGateway = false),
      )
    val ipfsRequest = downloadRequest.copy(mirrors = mirrors)

    val result = mirrorChooser.mirrorRequest(ipfsRequest) { _, url -> url.toString() }
    assertEquals("http://example.com/foo", result)
  }

  @Test
  fun testMirrorChooserThrowsIfOnlyIpfsGateways() = runSuspend {
    val mirrorChooser = MirrorChooserRandom()
    val mirrors =
      listOf(Mirror("foo/bar", isIpfsGateway = true), Mirror("bar/foo", isIpfsGateway = true))
    val ipfsRequest = downloadRequest.copy(mirrors = mirrors)

    val e = assertFailsWith<IOException> { mirrorChooser.mirrorRequest(ipfsRequest) { _, _ -> } }
    assertEquals("Got IPFS gateway without CID", e.message)
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
  }

  @Test
  fun testMirrorChooserErrorSort() {
    val mockManager = mockk<MirrorParameterManager>(relaxed = true)
    every { mockManager.getCurrentLocation() } returns "HERE"
    every { mockManager.preferForeignMirrors() } returns false
    every { mockManager.getMirrorErrorCount(any()) } returns 99
    every { mockManager.getMirrorErrorCount("test.local1.org") } returns 5
    every { mockManager.getMirrorErrorCount("test.local2.org") } returns 3
    every { mockManager.getMirrorErrorCount("test.local3.org") } returns 1

    val mirrorChooser = MirrorChooserWithParameters(mockManager)

    // test error sorting with domestic mirror preference
    val orderedList = mirrorChooser.orderMirrors(downloadRequestLocation)
    // confirm the list contains all mirrors
    assertEquals(9, orderedList.size)
    // mirrors that have fewer errors should be included first
    assertEquals("http://test.local3.org", orderedList[0].baseUrl)
    assertEquals("http://test.local2.org", orderedList[1].baseUrl)
    assertEquals("http://test.local1.org", orderedList[2].baseUrl)
  }

  @Test
  fun testMirrorChooserWithTryFirst() {
    val mockManager = mockk<MirrorParameterManager>(relaxed = true)
    every { mockManager.getCurrentLocation() } returns "HERE"
    every { mockManager.preferForeignMirrors() } returns false

    val mirrorChooser = MirrorChooserWithParameters(mockManager)

    // test tryfirst mirror parameter
    val tryFirstList = mirrorChooser.orderMirrors(downloadRequestTryFirst)
    // confirm the list contains all mirrors
    assertEquals(9, tryFirstList.size)
    // tryfirst mirror should be included before other mirrors
    assertEquals("http://test.remote1.org", tryFirstList[0].baseUrl)
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
      // test error sorting without foreign mirror preference
      val orderedList = mirrorChooser.orderMirrors(downloadRequestLocation)
      if (orderedList[0].baseUrl.equals("http://test.local1.org")) {
        count1++
      } else if (orderedList[0].baseUrl.equals("http://test.remote1.org")) {
        count2++
      } else if (orderedList[0].baseUrl.equals("http://test.unknown1.org")) {
        count3++
      } else {
        countX++
      }
    }
    // the tested urls should have each appeared first in the list at least once
    assertTrue { count1 > 0 }
    assertTrue { count2 > 0 }
    assertTrue { count3 > 0 }
  }
}

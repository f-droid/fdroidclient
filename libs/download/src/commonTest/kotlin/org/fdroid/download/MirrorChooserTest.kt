package org.fdroid.download

import io.ktor.utils.io.errors.IOException
import org.fdroid.runSuspend
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MirrorChooserTest {

    private val mirrors = listOf(Mirror("foo"), Mirror("bar"), Mirror("42"), Mirror("1337"))
    private val downloadRequest = DownloadRequest("foo", mirrors)

    @Test
    fun testMirrorChooserDefaultImpl() = runSuspend {
        val mirrorChooser = MirrorChooserRandom()
        val expectedResult = Random.nextInt()

        val result = mirrorChooser.mirrorRequest(downloadRequest) { mirror, url ->
            assertTrue { mirrors.contains(mirror) }
            assertEquals(mirror.getUrl(downloadRequest.path), url)
            expectedResult
        }
        assertEquals(expectedResult, result)
    }

    @Test
    fun testFallbackToNextMirror() = runSuspend {
        val mirrorChooser = MirrorChooserRandom()
        val expectedResult = Random.nextInt()

        val result = mirrorChooser.mirrorRequest(downloadRequest) { mirror, url ->
            assertEquals(mirror.getUrl(downloadRequest.path), url)
            // fails with all except last mirror
            if (mirror != downloadRequest.mirrors.last()) throw IOException("foo")
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

}

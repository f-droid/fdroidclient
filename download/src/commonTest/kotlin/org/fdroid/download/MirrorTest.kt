package org.fdroid.download

import io.ktor.http.Url
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * This class is not to test actual mirror behavior (done elsewhere), but to test the [Mirror] class.
 */
class MirrorTest {

    @Test
    fun testInvalidUrlDoesNotCrash() {
        val fallbackInvalidUrl = Url("http://127.0.0.1:64335")
        assertEquals(fallbackInvalidUrl, Mirror(":/foo/bar").url)
        assertEquals(fallbackInvalidUrl, Mirror("http://192.168.0.1:6465161/foo").url)
        assertEquals(fallbackInvalidUrl, Mirror("mailto:x").url)
        assertEquals(fallbackInvalidUrl, Mirror("file:/root").url)
    }

    @Test
    fun testIsOnion() {
        assertTrue(Mirror("http://ftpfaudev4triw2vxiwzf4334e3mynz7osqgtozhbc77fixncqzbyoyd.onion/fdroid/repo").isOnion())
        assertFalse(Mirror("https://www.f-droid.org/fdroid/repo").isOnion())
        assertFalse(Mirror("http://192.168.0.1/fdroid/repo").isOnion())
    }
}

package org.fdroid.download

import io.ktor.http.Url
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * This class is not to test actual mirror behavior (done elsewhere), but to test the [Mirror] class.
 */
internal class MirrorTest {

    @Test
    fun testGetUrl() {
        assertEquals(
            "https://example.org/entry.jar",
            Mirror("https://example.org/").getUrl("/entry.jar").toString()
        )
        assertEquals(
            "https://example.org/entry.jar",
            Mirror("https://example.org").getUrl("/entry.jar").toString()
        )
        assertEquals(
            "https://gitlab.com/fdroidclient/fdroid/repo/entry.jar",
            Mirror("https://gitlab.com/fdroidclient/fdroid/repo").getUrl("/entry.jar").toString()
        )
    }

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
        assertTrue(
            Mirror(
                "http://ftpfaudev4triw2vxiwzf4334e3mynz7osqgtozhbc77fixncqzbyoyd.onion/fdroid/repo"
            ).isOnion()
        )
        assertFalse(Mirror("https://www.f-droid.org/fdroid/repo").isOnion())
        assertFalse(Mirror("http://192.168.0.1/fdroid/repo").isOnion())
    }

    @Test
    fun testIsLocal() {
        assertTrue(Url("http://127.0.0.1/foo/bar").isLocal())

        assertTrue(Url("http://10.0.0.0").isLocal())
        assertTrue(Url("http://10.1.2.3").isLocal())
        assertTrue(Url("http://10.255.255.255").isLocal())

        assertTrue(Url("http://169.254.0.0").isLocal())
        assertTrue(Url("http://169.254.255.255").isLocal())
        assertFalse(Url("http://169.253.255.255").isLocal())
        assertFalse(Url("http://169.255.255.255").isLocal())

        assertTrue(Url("http://172.16.0.0:8888").isLocal())
        assertTrue(Url("http://172.16.255.255").isLocal())
        assertFalse(Url("http://172.161.0.0:8888").isLocal())
        assertTrue(Url("http://172.27.1.255").isLocal())
        assertTrue(Url("http://172.31.255.255").isLocal())
        assertFalse(Url("http://172.32.0.0").isLocal())

        assertFalse(Url("http://192.168.0.example.org").isLocal())
        assertTrue(Url("http://192.168.0.112:8888").isLocal())
        assertTrue(Url("http://192.168.1.112:8888/foo/bar").isLocal())
        assertTrue(Url("http://192.168.0.112:80").isLocal())
        assertTrue(Url("http://192.168.255.255:8041").isLocal())

        assertFalse(Url("https://malware.com:8888").isLocal())
        assertFalse(Url("https://www.google.com").isLocal())
    }
}

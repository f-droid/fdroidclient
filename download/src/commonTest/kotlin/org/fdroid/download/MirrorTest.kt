package org.fdroid.download

/**
 * This class is not to test actual mirror behavior (done elsewhere), but to test the [Mirror] class.
 */
/*
class MirrorTest {
    @Test
    fun testIsSwapUri() {
        FDroidApp.subnetInfo = SubnetUtils("192.168.0.112/24").getInfo()
        val urlString =
            "http://192.168.0.112:8888/fdroid/repo?fingerprint=113F56CBFA967BA825DD13685A06E35730E0061C6BB046DF88A"
        assertTrue(HttpDownloader.isSwapUrl("192.168.0.112", 8888))
        assertTrue(HttpDownloader.isSwapUrl(Uri.parse(urlString)))
        assertTrue(HttpDownloader.isSwapUrl(URL(urlString)))
        assertFalse(HttpDownloader.isSwapUrl("192.168.1.112", 8888))
        assertFalse(HttpDownloader.isSwapUrl("192.168.0.112", 80))
        assertFalse(HttpDownloader.isSwapUrl(Uri.parse("https://malware.com:8888")))
        assertFalse(HttpDownloader.isSwapUrl(URL("https://www.google.com")))
    }
}
*/

package org.fdroid.download

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.fdroid.settings.SettingsManager
import org.junit.runner.RunWith
import java.net.InetAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class DnsCacheTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val settings = SettingsManager(context)

    private val url1 = "locaihost"
    private val url2 = "fdroid.org"
    private val url3 = "fdroid.net"

    private val ip1 = InetAddress.getByName("127.0.0.1")
    private val ip2 = InetAddress.getByName("127.0.0.2")
    private val ip3 = InetAddress.getByName("127.0.0.3")

    private val list1 = listOf(ip1, ip2, ip3)
    private val list2 = listOf(ip2)
    private val list3 = listOf(ip3)

    @Test
    fun basicCacheTest() {
        // test setup
        settings.useDnsCache = true
        val testObject = DnsCache(settings)

        // populate cache
        testObject.insert(url1, list1)
        testObject.insert(url2, list2)
        testObject.insert(url3, list3)

        // check for cached lookup results
        val testList1 = testObject.lookup(url1)
        assertNotNull(testList1)
        assertEquals(3, testList1.size.toLong())
        assertEquals(ip1.hostAddress, testList1[0].hostAddress)
        assertEquals(ip2.hostAddress, testList1[1].hostAddress)
        assertEquals(ip3.hostAddress, testList1[2].hostAddress)

        // toggle preference (false)
        settings.useDnsCache = false

        // attempt non-cached lookup
        val testList2 = testObject.lookup(url1)
        assertNull(testList2)

        // toggle preference (true)
        settings.useDnsCache = true

        // confirm lookup results remain in cache
        val testList3 = testObject.lookup(url2)
        assertNotNull(testList3)
        assertEquals(1, testList3.size.toLong())
        assertEquals(ip2.hostAddress, testList3[0].hostAddress)

        // test removal
        testObject.remove(url2)

        // confirm result was removed from cache
        val testList4 = testObject.lookup(url2)
        assertNull(testList4)
    }

    @Test
    fun dnsRetryTest() {
        // test setup
        settings.useDnsCache = true
        val testCache = DnsCache(settings)
        val testObject = DnsWithCache(settings, testCache)

        // insert dummy value into cache
        testCache.insert(url2, list2)

        // check initial status
        val testList1 = testObject.lookup(url2)
        assertEquals(1, testList1.size.toLong())
        assertEquals(ip2.hostAddress, testList1[0].hostAddress)

        // mismatch with dummy value should require retry and clear cache
        val testFlag = testObject.shouldRetryRequest(url2)
        assertTrue(testFlag)
        val testList2 = testCache.lookup(url2)
        assertNull(testList2)

        // subsequent lookup should cache actual dns result (not testing actual values)
        val testList3 = testObject.lookup(url2)
        assertNotNull(testList3)
        val testList4 = testCache.lookup(url2)
        assertNotNull(testList4)
    }
}

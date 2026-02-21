package org.fdroid.fdroid.net

import org.fdroid.fdroid.Preferences
import org.junit.Assert
import java.net.InetAddress
import java.util.Arrays
import kotlin.test.Test

class DnsCacheTest {

    private val url1: String = "locaihost"
    private val url2: String = "fdroid.org"
    private val url3: String = "fdroid.net"

    private val ip1: InetAddress = InetAddress.getByName("127.0.0.1")
    private val ip2: InetAddress = InetAddress.getByName("127.0.0.2")
    private val ip3: InetAddress = InetAddress.getByName("127.0.0.3")

    private val list1: MutableList<InetAddress> = Arrays.asList<InetAddress>(ip1, ip2, ip3)
    private val list2: MutableList<InetAddress> = Arrays.asList<InetAddress>(ip2)
    private val list3: MutableList<InetAddress> = Arrays.asList<InetAddress>(ip3)

    @Test
    fun basicCacheTest() {
        // test setup
        val prefs = Preferences.get()
        prefs.setDnsCacheEnabledValue(true)
        val testObject = DnsCache.get()

        // populate cache
        testObject.insert(url1, list1)
        testObject.insert(url2, list2)
        testObject.insert(url3, list3)

        // check for cached lookup results
        val testList1 = testObject.lookup(url1)
        Assert.assertEquals(3, testList1.size.toLong())
        Assert.assertEquals(ip1.hostAddress, testList1[0]!!.hostAddress)
        Assert.assertEquals(ip2.hostAddress, testList1[1]!!.hostAddress)
        Assert.assertEquals(ip3.hostAddress, testList1[2]!!.hostAddress)

        // toggle preference (false)
        prefs.setDnsCacheEnabledValue(false)

        // attempt non-cached lookup
        val testList2 = testObject.lookup(url1)
        Assert.assertNull(testList2)

        // toggle preference (true)
        prefs.setDnsCacheEnabledValue(true)

        // confirm lookup results remain in cache
        val testList3 = testObject.lookup(url2)
        Assert.assertEquals(1, testList3.size.toLong())
        Assert.assertEquals(ip2.hostAddress, testList3[0].hostAddress)

        // test removal
        val testList4 = testObject.remove(url2)
        Assert.assertEquals(1, testList4.size.toLong())
        Assert.assertEquals(ip2.hostAddress, testList4[0].hostAddress)
        val testList5 = testObject.lookup(url2)
        Assert.assertNull(testList5)
    }

    @Test
    fun dnsRetryTest() {
        // test setup
        val prefs = Preferences.get()
        prefs.setDnsCacheEnabledValue(true)
        val testObject = DnsWithCache.get()
        val testCache = DnsCache.get()

        // insert dummy value into cache
        testCache.insert(url2, list2)

        // check initial status
        val testList1 = testObject.lookup(url2)
        Assert.assertEquals(1, testList1.size.toLong())
        Assert.assertEquals(ip2.hostAddress, testList1[0].hostAddress)

        // mismatch with dummy value should require retry and clear cache
        val testFlag = testObject.shouldRetryRequest(url2)
        Assert.assertTrue(testFlag)
        val testList2 = testCache.lookup(url2)
        Assert.assertNull(testList2)

        // subsequent lookup should cache actual dns result (not testing actual values)
        val testList3 = testObject.lookup(url2)
        Assert.assertNotNull(testList3)
        val testList4 = testCache.lookup(url2)
        Assert.assertNotNull(testList4)
    }
}

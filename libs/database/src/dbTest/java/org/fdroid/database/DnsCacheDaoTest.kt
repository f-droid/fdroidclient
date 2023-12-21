package org.fdroid.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
internal class DnsCacheDaoTest : AppTest() {

    @Test
    fun insertAndQueryList() {

        // check initial state
        val cache1 = dnsCacheDao.getDnsCache()
        assertNotNull(cache1)
        assertEquals(0, cache1.size)

        // insert and check state
        dnsCacheDao.insert(dns1a)
        dnsCacheDao.insert(dns2)
        dnsCacheDao.insert(dns3)
        val cache2 = dnsCacheDao.getDnsCache()
        assertNotNull(cache2)
        assertEquals(3, cache2.size)

        // check final state
        dnsCacheDao.clearAll()
        val cache3 = dnsCacheDao.getDnsCache()
        assertNotNull(cache3)
        assertEquals(0, cache3.size)
    }

    @Test
    fun insertAndReplaceItem() {

        // check test conditions
        assertEquals(dns1a.hostName, dns1b.hostName)
        val key = dns1a.hostName

        // insert and then replace data
        dnsCacheDao.insert(dns1a)
        val insertedList = dnsCacheDao.getDnsCacheItemList(key)
        dnsCacheDao.insert(dns1b)
        val replacedList = dnsCacheDao.getDnsCacheItemList(key)

        // check test results
        assertNotNull(insertedList)
        assertEquals(dns1a.addressList.size, insertedList.size)
        assertNotNull(replacedList)
        assertEquals(dns1b.addressList.size, replacedList.size)

        // clear data
        dnsCacheDao.clearAll()
    }
}

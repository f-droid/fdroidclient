package org.fdroid.database

import androidx.room.*

public interface DnsCacheDao {

    /**
     * Inserts a new host name/address list pair into the database.
     */
    public fun insert(dnsCache: DnsCache): Long

    /**
     * Returns the host name/address list pair for the given [hostName], or null if none was found.
     */
    public fun getDnsCacheItem(hostName: String): DnsCache?

    /**
     * Returns a list of all host name/address list pairs in the database.
     */
    public fun getDnsCache(): List<DnsCache>

}

@Dao
internal interface DnsCacheDaoInt : DnsCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplace(dnsCache: DnsCacheDb): Long

    @Transaction
    override fun insert(dnsCache: DnsCache): Long {
        val dnsCacheDb = DnsCacheDb(hostName = dnsCache.hostName, addressList = dnsCache.addressList)
        return insertOrReplace(dnsCacheDb)
    }

    @Transaction
    fun insert(hostName: String, addressList: List<String>): Long {
        val dnsCacheDb = DnsCacheDb(hostName = hostName, addressList = addressList)
        return insertOrReplace(dnsCacheDb)
    }

    @Transaction
    @Query("SELECT * FROM ${DnsCacheDb.TABLE} WHERE hostName = :hostName")
    override fun getDnsCacheItem(hostName: String): DnsCache?

    @Transaction
    fun getDnsCacheItemList(hostName: String): List<String>? {
        val dnsCache = getDnsCacheItem(hostName)
        return dnsCache?.addressList
    }

    @Transaction
    @Query("SELECT * FROM ${DnsCacheDb.TABLE}")
    override fun getDnsCache(): List<DnsCache>

    @Query("DELETE FROM ${DnsCacheDb.TABLE}")
    fun clearAll()
}

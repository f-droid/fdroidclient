package org.fdroid.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = DnsCacheDb.TABLE)
internal data class DnsCacheDb(
    @PrimaryKey val hostName: String,
    val addressList: List<String>
) {
    internal companion object {
        const val TABLE = "DnsCacheDb"
    }
}

public data class DnsCache internal constructor(
    @Embedded internal val dnsCache: DnsCacheDb
) {
    public constructor(
        hostName: String,
        addressList: List<String>
    ) : this(
        dnsCache = DnsCacheDb(
            hostName = hostName,
            addressList = addressList
        )
    )

    public val hostName: String get() = dnsCache.hostName
    public val addressList: List<String> get() = dnsCache.addressList
}

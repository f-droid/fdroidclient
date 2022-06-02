package org.fdroid.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.fdroid.PackagePreference

@Entity
public data class AppPrefs(
    @PrimaryKey
    val packageId: String,
    override val ignoreVersionCodeUpdate: Long = 0,
    // This is named like this, because it hit a Room bug when joining with Version table
    // which had exactly the same field.
    internal val appPrefReleaseChannels: List<String>? = null,
) : PackagePreference {
    public val ignoreAllUpdates: Boolean get() = ignoreVersionCodeUpdate == Long.MAX_VALUE
    public override val releaseChannels: List<String> get() = appPrefReleaseChannels ?: emptyList()
    public fun shouldIgnoreUpdate(versionCode: Long): Boolean =
        ignoreVersionCodeUpdate >= versionCode

    public fun toggleIgnoreAllUpdates(): AppPrefs = copy(
        ignoreVersionCodeUpdate = if (ignoreAllUpdates) 0 else Long.MAX_VALUE,
    )

    public fun toggleIgnoreVersionCodeUpdate(versionCode: Long): AppPrefs = copy(
        ignoreVersionCodeUpdate = if (shouldIgnoreUpdate(versionCode)) 0 else versionCode,
    )

    public fun toggleReleaseChannel(releaseChannel: String): AppPrefs = copy(
        appPrefReleaseChannels = if (appPrefReleaseChannels?.contains(releaseChannel) == true) {
            appPrefReleaseChannels.toMutableList().apply { remove(releaseChannel) }
        } else {
            (appPrefReleaseChannels?.toMutableList() ?: ArrayList()).apply { add(releaseChannel) }
        },
    )
}

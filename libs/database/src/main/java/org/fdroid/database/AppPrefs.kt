package org.fdroid.database

import androidx.room.DatabaseView
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.fdroid.PackagePreference

/**
 * User-defined preferences related to [App]s that get stored in the database,
 * so they can be used for queries.
 */
@Entity(tableName = AppPrefs.TABLE)
public data class AppPrefs(
    @PrimaryKey
    val packageName: String,
    override val ignoreVersionCodeUpdate: Long = 0,
    val preferredRepoId: Long? = null,
    // This is named like this, because it hit a Room bug when joining with Version table
    // which had exactly the same field.
    internal val appPrefReleaseChannels: List<String>? = null,
) : PackagePreference {
    internal companion object {
        const val TABLE = "AppPrefs"
    }

    public val ignoreAllUpdates: Boolean get() = ignoreVersionCodeUpdate == Long.MAX_VALUE
    public override val releaseChannels: List<String> get() = appPrefReleaseChannels ?: emptyList()
    public fun shouldIgnoreUpdate(versionCode: Long): Boolean =
        ignoreVersionCodeUpdate >= versionCode

    /**
     * Returns a new instance of [AppPrefs] toggling [ignoreAllUpdates].
     */
    public fun toggleIgnoreAllUpdates(): AppPrefs = copy(
        ignoreVersionCodeUpdate = if (ignoreAllUpdates) 0 else Long.MAX_VALUE,
    )

    /**
     * Returns a new instance of [AppPrefs] ignoring the given [versionCode] or stop ignoring it
     * if it was already ignored.
     */
    public fun toggleIgnoreVersionCodeUpdate(versionCode: Long): AppPrefs = copy(
        ignoreVersionCodeUpdate = if (shouldIgnoreUpdate(versionCode)) 0 else versionCode,
    )

    /**
     * Returns a new instance of [AppPrefs] enabling the given [releaseChannel] or disabling it
     * if it was already enabled.
     */
    public fun toggleReleaseChannel(releaseChannel: String): AppPrefs = copy(
        appPrefReleaseChannels = if (appPrefReleaseChannels?.contains(releaseChannel) == true) {
            appPrefReleaseChannels.toMutableList().apply { remove(releaseChannel) }
        } else {
            (appPrefReleaseChannels?.toMutableList() ?: ArrayList()).apply { add(releaseChannel) }
        },
    )
}

@DatabaseView("""SELECT packageName, repoId AS preferredRepoId FROM ${AppMetadata.TABLE}
    JOIN ${RepositoryPreferences.TABLE} AS pref USING (repoId)
    LEFT JOIN ${AppPrefs.TABLE} USING (packageName)
    WHERE repoId = COALESCE(preferredRepoId, repoId)
    GROUP BY packageName HAVING MAX(pref.weight)""")
internal class PreferredRepo(
    val packageName: String,
    val preferredRepoId: Long,
)

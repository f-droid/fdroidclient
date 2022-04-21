package org.fdroid.database

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import org.fdroid.database.FDroidDatabaseHolder.dispatcher
import org.fdroid.index.v2.PackageVersionV2

public interface VersionDao {
    public fun insert(
        repoId: Long,
        packageId: String,
        packageVersions: Map<String, PackageVersionV2>,
        checkIfCompatible: (PackageVersionV2) -> Boolean,
    )

    public fun getAppVersions(packageId: String): LiveData<List<AppVersion>>
    public fun getAppVersions(repoId: Long, packageId: String): List<AppVersion>
}

@Dao
internal interface VersionDaoInt : VersionDao {

    @Transaction
    override fun insert(
        repoId: Long,
        packageId: String,
        packageVersions: Map<String, PackageVersionV2>,
        checkIfCompatible: (PackageVersionV2) -> Boolean,
    ) {
        // TODO maybe the number of queries here can be reduced
        packageVersions.entries.iterator().forEach { (versionId, packageVersion) ->
            val isCompatible = checkIfCompatible(packageVersion)
            insert(repoId, packageId, versionId, packageVersion, isCompatible)
        }
    }

    @Transaction
    fun insert(
        repoId: Long,
        packageId: String,
        versionId: String,
        packageVersion: PackageVersionV2,
        isCompatible: Boolean,
    ) {
        val version = packageVersion.toVersion(repoId, packageId, versionId, isCompatible)
        insert(version)
        insert(packageVersion.manifest.getVersionedStrings(version))
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(version: Version)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(versionedString: List<VersionedString>)

    override fun getAppVersions(
        packageId: String,
    ): LiveData<List<AppVersion>> = liveData(dispatcher) {
        // TODO we should probably react to changes of versioned strings as well
        val versionedStrings = getVersionedStrings(packageId)
        val liveData = getVersions(packageId).distinctUntilChanged().map { versions ->
            versions.map { version -> version.toAppVersion(versionedStrings) }
        }
        emitSource(liveData)
    }

    @Transaction
    override fun getAppVersions(repoId: Long, packageId: String): List<AppVersion> {
        val versionedStrings = getVersionedStrings(repoId, packageId)
        return getVersions(repoId, packageId).map { version ->
            version.toAppVersion(versionedStrings)
        }
    }

    @RewriteQueriesToDropUnusedColumns
    @Query("""SELECT * FROM Version
        JOIN RepositoryPreferences AS pref USING (repoId)
        WHERE pref.enabled = 1 AND packageId = :packageId
        ORDER BY manifest_versionCode DESC""")
    fun getVersions(packageId: String): LiveData<List<Version>>

    @Query("SELECT * FROM Version WHERE repoId = :repoId AND packageId = :packageId")
    fun getVersions(repoId: Long, packageId: String): List<Version>

    /**
     * Used for finding versions that are an update,
     * so takes [AppPrefs.ignoreVersionCodeUpdate] into account.
     */
    @RewriteQueriesToDropUnusedColumns
    @Query("""SELECT * FROM Version
        JOIN RepositoryPreferences USING (repoId)
        LEFT JOIN AppPrefs USING (packageId)
        WHERE RepositoryPreferences.enabled = 1 AND
              manifest_versionCode > COALESCE(AppPrefs.ignoreVersionCodeUpdate, 0) AND
              packageId IN (:packageNames)
        ORDER BY manifest_versionCode DESC, RepositoryPreferences.weight DESC""")
    fun getVersions(packageNames: List<String>): List<Version>

    @RewriteQueriesToDropUnusedColumns
    @Query("""SELECT * FROM VersionedString
        JOIN RepositoryPreferences AS pref USING (repoId)
        WHERE pref.enabled = 1 AND packageId = :packageId""")
    fun getVersionedStrings(packageId: String): List<VersionedString>

    @Query("SELECT * FROM VersionedString WHERE repoId = :repoId AND packageId = :packageId")
    fun getVersionedStrings(repoId: Long, packageId: String): List<VersionedString>

    @Query("""SELECT * FROM VersionedString
        WHERE repoId = :repoId AND packageId = :packageId AND versionId = :versionId""")
    fun getVersionedStrings(
        repoId: Long,
        packageId: String,
        versionId: String,
    ): List<VersionedString>

    @VisibleForTesting
    @Query("""DELETE FROM Version
        WHERE repoId = :repoId AND packageId = :packageId AND versionId = :versionId""")
    fun deleteAppVersion(repoId: Long, packageId: String, versionId: String)

    @Query("SELECT COUNT(*) FROM Version")
    fun countAppVersions(): Int

    @Query("SELECT COUNT(*) FROM VersionedString")
    fun countVersionedStrings(): Int

}

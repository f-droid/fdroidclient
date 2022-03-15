package org.fdroid.database

import androidx.annotation.VisibleForTesting
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import org.fdroid.index.v2.PackageVersionV2

public interface VersionDao {
    fun insert(repoId: Long, packageId: String, packageVersions: Map<String, PackageVersionV2>)
    fun insert(repoId: Long, packageId: String, versionId: String, packageVersion: PackageVersionV2)
    fun getAppVersions(repoId: Long, packageId: String): List<AppVersion>
}

@Dao
internal interface VersionDaoInt : VersionDao {

    @Transaction
    override fun insert(
        repoId: Long,
        packageId: String,
        packageVersions: Map<String, PackageVersionV2>,
    ) {
        // TODO maybe the number of queries here can be reduced
        packageVersions.entries.forEach { (versionId, packageVersion) ->
            insert(repoId, packageId, versionId, packageVersion)
        }
    }

    @Transaction
    override fun insert(
        repoId: Long,
        packageId: String,
        versionId: String,
        packageVersion: PackageVersionV2,
    ) {
        val version = packageVersion.toVersion(repoId, packageId, versionId)
        insert(version)
        insert(packageVersion.manifest.getVersionedStrings(version))
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(version: Version)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(versionedString: List<VersionedString>)

    @Transaction
    override fun getAppVersions(repoId: Long, packageId: String): List<AppVersion> {
        val versionedStrings = getVersionedStrings(repoId, packageId)
        return getVersions(repoId, packageId).map { version ->
            AppVersion(
                version = version,
                usesPermission = versionedStrings.getPermissions(version),
                usesPermissionSdk23 = versionedStrings.getPermissionsSdk23(version),
                features = versionedStrings.getFeatures(version),
            )
        }
    }

    @Query("SELECT * FROM Version WHERE repoId = :repoId AND packageId = :packageId")
    fun getVersions(repoId: Long, packageId: String): List<Version>

    @Query("SELECT * FROM VersionedString WHERE repoId = :repoId AND packageId = :packageId")
    fun getVersionedStrings(repoId: Long, packageId: String): List<VersionedString>

    @VisibleForTesting
    @Query("DELETE FROM Version WHERE repoId = :repoId AND packageId = :packageId AND versionId = :versionId")
    fun deleteAppVersion(repoId: Long, packageId: String, versionId: String)

    @Query("SELECT COUNT(*) FROM Version")
    fun countAppVersions(): Int

    @Query("SELECT COUNT(*) FROM VersionedString")
    fun countVersionedStrings(): Int

}

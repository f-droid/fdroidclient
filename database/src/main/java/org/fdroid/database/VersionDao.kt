package org.fdroid.database

import androidx.lifecycle.LiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import org.fdroid.database.FDroidDatabaseHolder.dispatcher
import org.fdroid.database.VersionedStringType.PERMISSION
import org.fdroid.database.VersionedStringType.PERMISSION_SDK_23
import org.fdroid.index.IndexParser.json
import org.fdroid.index.v2.ManifestV2
import org.fdroid.index.v2.PackageVersionV2
import org.fdroid.index.v2.PermissionV2
import org.fdroid.index.v2.ReflectionDiffer

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

/**
 * A list of unknown fields in [PackageVersionV2] that we don't allow for [Version].
 *
 * We are applying reflection diffs against internal database classes
 * and need to prevent the untrusted external JSON input to modify internal fields in those classes.
 * This list must always hold the names of all those internal FIELDS for [Version].
 */
private val DENY_LIST = listOf("packageId", "repoId", "versionId")

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

    @Insert(onConflict = REPLACE)
    fun insert(version: Version)

    @Insert(onConflict = REPLACE)
    fun insert(versionedString: List<VersionedString>)

    @Update
    fun update(version: Version)

    fun update(
        repoId: Long,
        packageId: String,
        versionsDiffMap: Map<String, JsonObject?>?,
        checkIfCompatible: (ManifestV2) -> Boolean,
    ) {
        if (versionsDiffMap == null) { // no more versions, delete all
            deleteAppVersion(repoId, packageId)
        } else versionsDiffMap.forEach { (versionId, jsonObject) ->
            if (jsonObject == null) { // delete individual version
                deleteAppVersion(repoId, packageId, versionId)
            } else {
                val version = getVersion(repoId, packageId, versionId)
                if (version == null) { // new version, parse normally
                    val packageVersionV2: PackageVersionV2 =
                        json.decodeFromJsonElement(jsonObject)
                    val isCompatible = checkIfCompatible(packageVersionV2.manifest)
                    insert(repoId, packageId, versionId, packageVersionV2, isCompatible)
                } else { // diff against existing version
                    diffVersion(version, jsonObject, checkIfCompatible)
                }
            }
        } // end forEach
    }

    private fun diffVersion(
        version: Version,
        jsonObject: JsonObject,
        checkIfCompatible: (ManifestV2) -> Boolean,
    ) {
        // ensure that diff does not include internal keys
        DENY_LIST.forEach { forbiddenKey ->
            println("$forbiddenKey ${jsonObject.keys}")
            if (jsonObject.containsKey(forbiddenKey)) {
                throw SerializationException(forbiddenKey)
            }
        }
        // diff version
        val diffedVersion = ReflectionDiffer.applyDiff(version, jsonObject)
        val isCompatible = checkIfCompatible(diffedVersion.manifest.toManifestV2())
        update(diffedVersion.copy(isCompatible = isCompatible))
        // diff versioned strings
        val manifest = jsonObject["manifest"]
        if (manifest is JsonNull) { // no more manifest, delete all versionedStrings
            deleteVersionedStrings(version.repoId, version.packageId, version.versionId)
        } else if (manifest is JsonObject) {
            diffVersionedStrings(version, manifest, "usesPermission", PERMISSION)
            diffVersionedStrings(version, manifest, "usesPermissionSdk23",
                PERMISSION_SDK_23)
        }
    }

    private fun diffVersionedStrings(
        version: Version,
        jsonObject: JsonObject,
        key: String,
        type: VersionedStringType,
    ) = DbDiffUtils.diffAndUpdateListTable(
        jsonObject = jsonObject,
        jsonObjectKey = key,
        listParser = { permissionArray ->
            val list: List<PermissionV2> = json.decodeFromJsonElement(permissionArray)
            list.toVersionedString(version, type)
        },
        deleteList = {
            deleteVersionedStrings(version.repoId, version.packageId, version.versionId, type)
        },
        insertNewList = { versionedStrings -> insert(versionedStrings) },
    )

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

    @Query("""SELECT * FROM Version
        WHERE repoId = :repoId AND packageId = :packageId AND versionId = :versionId""")
    fun getVersion(repoId: Long, packageId: String, versionId: String): Version?

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

    @Query("""DELETE FROM Version WHERE repoId = :repoId AND packageId = :packageId""")
    fun deleteAppVersion(repoId: Long, packageId: String)

    @Query("""DELETE FROM Version
        WHERE repoId = :repoId AND packageId = :packageId AND versionId = :versionId""")
    fun deleteAppVersion(repoId: Long, packageId: String, versionId: String)

    @Query("""DELETE FROM VersionedString
        WHERE repoId = :repoId AND packageId = :packageId AND versionId = :versionId""")
    fun deleteVersionedStrings(repoId: Long, packageId: String, versionId: String)

    @Query("""DELETE FROM VersionedString WHERE repoId = :repoId
        AND packageId = :packageId AND versionId = :versionId AND type = :type""")
    fun deleteVersionedStrings(
        repoId: Long,
        packageId: String,
        versionId: String,
        type: VersionedStringType,
    )

    @Query("SELECT COUNT(*) FROM Version")
    fun countAppVersions(): Int

    @Query("SELECT COUNT(*) FROM VersionedString")
    fun countVersionedStrings(): Int

}

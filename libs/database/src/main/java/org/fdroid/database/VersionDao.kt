package org.fdroid.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import org.fdroid.database.VersionedStringType.PERMISSION
import org.fdroid.database.VersionedStringType.PERMISSION_SDK_23
import org.fdroid.index.IndexParser.json
import org.fdroid.index.v2.PackageManifest
import org.fdroid.index.v2.PackageVersionV2
import org.fdroid.index.v2.PermissionV2
import org.fdroid.index.v2.ReflectionDiffer

public interface VersionDao {
    /**
     * Inserts new versions for a given [packageName] from a full index.
     */
    public fun insert(
        repoId: Long,
        packageName: String,
        packageVersions: Map<String, PackageVersionV2>,
        checkIfCompatible: (PackageVersionV2) -> Boolean,
    )

    /**
     * Returns a list of versions for the given [packageName] sorting by highest version code first.
     */
    public fun getAppVersions(packageName: String): LiveData<List<AppVersion>>

    /**
     * Returns a list of versions from the repo identified by the given [repoId]
     * for the given [packageName] sorting by highest version code first.
     */
    public fun getAppVersions(repoId: Long, packageName: String): LiveData<List<AppVersion>>
}

/**
 * A list of unknown fields in [PackageVersionV2] that we don't allow for [Version].
 *
 * We are applying reflection diffs against internal database classes
 * and need to prevent the untrusted external JSON input to modify internal fields in those classes.
 * This list must always hold the names of all those internal FIELDS for [Version].
 */
private val DENY_LIST = listOf("packageName", "repoId", "versionId")

@Dao
internal interface VersionDaoInt : VersionDao {

    @Transaction
    override fun insert(
        repoId: Long,
        packageName: String,
        packageVersions: Map<String, PackageVersionV2>,
        checkIfCompatible: (PackageVersionV2) -> Boolean,
    ) {
        packageVersions.entries.iterator().forEach { (versionId, packageVersion) ->
            val isCompatible = checkIfCompatible(packageVersion)
            insert(repoId, packageName, versionId, packageVersion, isCompatible)
        }
    }

    @Transaction
    fun insert(
        repoId: Long,
        packageName: String,
        versionId: String,
        packageVersion: PackageVersionV2,
        isCompatible: Boolean,
    ) {
        val version = packageVersion.toVersion(repoId, packageName, versionId, isCompatible)
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
        packageName: String,
        versionsDiffMap: Map<String, JsonObject?>?,
        checkIfCompatible: (PackageManifest) -> Boolean,
    ) {
        if (versionsDiffMap == null) { // no more versions, delete all
            deleteAppVersion(repoId, packageName)
        } else versionsDiffMap.forEach { (versionId, jsonObject) ->
            if (jsonObject == null) { // delete individual version
                deleteAppVersion(repoId, packageName, versionId)
            } else {
                val version = getVersion(repoId, packageName, versionId)
                if (version == null) { // new version, parse normally
                    val packageVersionV2: PackageVersionV2 =
                        json.decodeFromJsonElement(jsonObject)
                    val isCompatible = checkIfCompatible(packageVersionV2.packageManifest)
                    insert(repoId, packageName, versionId, packageVersionV2, isCompatible)
                } else { // diff against existing version
                    diffVersion(version, jsonObject, checkIfCompatible)
                }
            }
        } // end forEach
    }

    private fun diffVersion(
        version: Version,
        jsonObject: JsonObject,
        checkIfCompatible: (PackageManifest) -> Boolean,
    ) {
        // ensure that diff does not include internal keys
        DENY_LIST.forEach { forbiddenKey ->
            if (jsonObject.containsKey(forbiddenKey)) {
                throw SerializationException(forbiddenKey)
            }
        }
        // diff version
        val diffedVersion = ReflectionDiffer.applyDiff(version, jsonObject)
        val isCompatible = checkIfCompatible(diffedVersion.packageManifest)
        update(diffedVersion.copy(isCompatible = isCompatible))
        // diff versioned strings
        val manifest = jsonObject["manifest"]
        if (manifest is JsonNull) { // no more manifest, delete all versionedStrings
            deleteVersionedStrings(version.repoId, version.packageName, version.versionId)
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
            deleteVersionedStrings(version.repoId, version.packageName, version.versionId, type)
        },
        insertNewList = { versionedStrings -> insert(versionedStrings) },
    )

    /**
     * The `ASC` sort is to handle the rare corner case when a
     * compatible version with the right signer is available with the
     * same version code from the same repo.  For example, if there are
     * APKs with different ABIs, but same Version Code.  Both Google
     * and F-Droid recommend using different Version Codes for each ABI.
     * `ASC` isn't quite right, but works fine for this rare case that
     * happens when app devs do strange things.  The 100% correct ABI
     * sort order would be: `arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86`.
     *
     * For more info, see:
     * https://gitlab.com/fdroid/fdroidclient/-/merge_requests/1394#note_1896148332
     */
    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query("""SELECT * FROM ${Version.TABLE}
        JOIN ${RepositoryPreferences.TABLE} AS pref USING (repoId)
        WHERE pref.enabled = 1 AND packageName = :packageName
        ORDER BY manifest_versionCode DESC, pref.weight DESC, manifest_nativecode ASC""")
    override fun getAppVersions(packageName: String): LiveData<List<AppVersion>>

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query("""SELECT * FROM ${Version.TABLE}
        WHERE repoId = :repoId AND packageName = :packageName
        ORDER BY manifest_versionCode DESC, manifest_nativecode ASC""")
    override fun getAppVersions(repoId: Long, packageName: String): LiveData<List<AppVersion>>

    @Query("""SELECT * FROM ${Version.TABLE}
        WHERE repoId = :repoId AND packageName = :packageName AND versionId = :versionId""")
    fun getVersion(repoId: Long, packageName: String, versionId: String): Version?

    /**
     * Used for finding versions that are an update,
     * so takes [AppPrefs.ignoreVersionCodeUpdate] into account.
     */
    fun getVersions(packageNames: List<String>): List<Version> {
        return if (packageNames.size <= 999) getVersionsInternal(packageNames)
        else packageNames.chunked(999).flatMap { getVersionsInternal(it) }
    }

    @RewriteQueriesToDropUnusedColumns
    @Query("""SELECT * FROM ${Version.TABLE}
        JOIN ${RepositoryPreferences.TABLE} AS pref USING (repoId)
        LEFT JOIN ${AppPrefs.TABLE} AS appPrefs USING (packageName)
        WHERE pref.enabled = 1 AND
              manifest_versionCode > COALESCE(appPrefs.ignoreVersionCodeUpdate, 0) AND
              packageName IN (:packageNames)
        ORDER BY manifest_versionCode DESC, pref.weight DESC""")
    fun getVersionsInternal(packageNames: List<String>): List<Version>

    @Query("""SELECT * FROM ${VersionedString.TABLE}
        WHERE repoId = :repoId AND packageName = :packageName""")
    fun getVersionedStrings(repoId: Long, packageName: String): List<VersionedString>

    @Query("""SELECT * FROM ${VersionedString.TABLE}
        WHERE repoId = :repoId AND packageName = :packageName AND versionId = :versionId""")
    fun getVersionedStrings(
        repoId: Long,
        packageName: String,
        versionId: String,
    ): List<VersionedString>

    @Query("""DELETE FROM ${Version.TABLE} WHERE repoId = :repoId AND packageName = :packageName""")
    fun deleteAppVersion(repoId: Long, packageName: String)

    @Query("""DELETE FROM ${Version.TABLE}
        WHERE repoId = :repoId AND packageName = :packageName AND versionId = :versionId""")
    fun deleteAppVersion(repoId: Long, packageName: String, versionId: String)

    @Query("""DELETE FROM ${VersionedString.TABLE}
        WHERE repoId = :repoId AND packageName = :packageName AND versionId = :versionId""")
    fun deleteVersionedStrings(repoId: Long, packageName: String, versionId: String)

    @Query("""DELETE FROM ${VersionedString.TABLE} WHERE repoId = :repoId
        AND packageName = :packageName AND versionId = :versionId AND type = :type""")
    fun deleteVersionedStrings(
        repoId: Long,
        packageName: String,
        versionId: String,
        type: VersionedStringType,
    )

    @Query("SELECT COUNT(*) FROM ${Version.TABLE}")
    fun countAppVersions(): Int

    @Query("SELECT COUNT(*) FROM ${VersionedString.TABLE}")
    fun countVersionedStrings(): Int

}

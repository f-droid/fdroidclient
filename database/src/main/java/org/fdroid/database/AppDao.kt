package org.fdroid.database

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import androidx.annotation.VisibleForTesting
import androidx.core.os.ConfigurationCompat.getLocales
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.RoomWarnings.CURSOR_MISMATCH
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import org.fdroid.database.AppListSortOrder.LAST_UPDATED
import org.fdroid.database.AppListSortOrder.NAME
import org.fdroid.database.DbDiffUtils.diffAndUpdateListTable
import org.fdroid.database.DbDiffUtils.diffAndUpdateTable
import org.fdroid.index.IndexParser.json
import org.fdroid.index.v2.FileV2
import org.fdroid.index.v2.LocalizedFileListV2
import org.fdroid.index.v2.LocalizedFileV2
import org.fdroid.index.v2.MetadataV2
import org.fdroid.index.v2.ReflectionDiffer.applyDiff

public interface AppDao {
    /**
     * Inserts an app into the DB.
     * This is usually from a full index v2 via [MetadataV2].
     *
     * Note: The app is considered to be not compatible until [Version]s are added
     * and [updateCompatibility] was called.
     *
     * @param locales supported by the current system configuration.
     */
    public fun insert(
        repoId: Long,
        packageName: String,
        app: MetadataV2,
        locales: LocaleListCompat = getLocales(Resources.getSystem().configuration),
    )

    /**
     * Updates the [AppMetadata.isCompatible] flag
     * based on whether at least one [AppVersion] is compatible.
     * This needs to run within the transaction that adds [AppMetadata] to the DB (e.g. [insert]).
     * Otherwise the compatibility is wrong.
     */
    public fun updateCompatibility(repoId: Long)

    /**
     * Gets the app from the DB. If more than one app with this [packageName] exists,
     * the one from the repository with the highest weight is returned.
     */
    public fun getApp(packageName: String): LiveData<App?>

    /**
     * Gets an app from a specific [Repository] or null,
     * if none is found with the given [packageName],
     */
    public fun getApp(repoId: Long, packageName: String): App?

    /**
     * Returns a limited number of apps with limited data.
     * Apps without name, icon or summary are at the end (or excluded if limit is too small).
     * Includes anti-features from the version with the highest version code.
     */
    public fun getAppOverviewItems(limit: Int = 200): LiveData<List<AppOverviewItem>>

    /**
     * Returns a limited number of apps with limited data within the given [category].
     */
    public fun getAppOverviewItems(
        category: String,
        limit: Int = 50,
    ): LiveData<List<AppOverviewItem>>

    public fun getAppListItems(
        packageManager: PackageManager,
        sortOrder: AppListSortOrder,
    ): LiveData<List<AppListItem>>

    public fun getAppListItems(
        packageManager: PackageManager,
        category: String,
        sortOrder: AppListSortOrder,
    ): LiveData<List<AppListItem>>

    public fun getInstalledAppListItems(packageManager: PackageManager): LiveData<List<AppListItem>>

    public fun getNumberOfAppsInCategory(category: String): Int

    public fun getNumberOfAppsInRepository(repoId: Long): Int
}

public enum class AppListSortOrder {
    LAST_UPDATED, NAME
}

/**
 * A list of unknown fields in [MetadataV2] that we don't allow for [AppMetadata].
 *
 * We are applying reflection diffs against internal database classes
 * and need to prevent the untrusted external JSON input to modify internal fields in those classes.
 * This list must always hold the names of all those internal FIELDS for [AppMetadata].
 */
private val DENY_LIST = listOf("packageId", "repoId")

/**
 * A list of unknown fields in [LocalizedFileV2] or [LocalizedFileListV2]
 * that we don't allow for [LocalizedFile] or [LocalizedFileList].
 *
 * Similar to [DENY_LIST].
 */
private val DENY_FILE_LIST = listOf("packageId", "repoId", "type")

@Dao
internal interface AppDaoInt : AppDao {

    @Transaction
    override fun insert(
        repoId: Long,
        packageName: String,
        app: MetadataV2,
        locales: LocaleListCompat,
    ) {
        insert(app.toAppMetadata(repoId, packageName, false, locales))
        app.icon.insert(repoId, packageName, "icon")
        app.featureGraphic.insert(repoId, packageName, "featureGraphic")
        app.promoGraphic.insert(repoId, packageName, "promoGraphic")
        app.tvBanner.insert(repoId, packageName, "tvBanner")
        app.screenshots?.let {
            it.phone.insert(repoId, packageName, "phone")
            it.sevenInch.insert(repoId, packageName, "sevenInch")
            it.tenInch.insert(repoId, packageName, "tenInch")
            it.wear.insert(repoId, packageName, "wear")
            it.tv.insert(repoId, packageName, "tv")
        }
    }

    private fun LocalizedFileV2?.insert(repoId: Long, packageId: String, type: String) {
        this?.toLocalizedFile(repoId, packageId, type)?.let { files ->
            insert(files)
        }
    }

    @JvmName("insertLocalizedFileListV2")
    private fun LocalizedFileListV2?.insert(repoId: Long, packageId: String, type: String) {
        this?.toLocalizedFileList(repoId, packageId, type)?.let { files ->
            insertLocalizedFileLists(files)
        }
    }

    @Insert(onConflict = REPLACE)
    fun insert(appMetadata: AppMetadata)

    @Insert(onConflict = REPLACE)
    fun insert(localizedFiles: List<LocalizedFile>)

    @Insert(onConflict = REPLACE)
    fun insertLocalizedFileLists(localizedFiles: List<LocalizedFileList>)

    @Transaction
    fun updateApp(
        repoId: Long,
        packageId: String,
        jsonObject: JsonObject?,
        locales: LocaleListCompat,
    ) {
        if (jsonObject == null) {
            // this app is gone, we need to delete it
            deleteAppMetadata(repoId, packageId)
            return
        }
        val metadata = getAppMetadata(repoId, packageId)
        if (metadata == null) { // new app
            val metadataV2: MetadataV2 = json.decodeFromJsonElement(jsonObject)
            insert(repoId, packageId, metadataV2)
        } else { // diff against existing app
            // ensure that diff does not include internal keys
            DENY_LIST.forEach { forbiddenKey ->
                if (jsonObject.containsKey(forbiddenKey)) throw SerializationException(forbiddenKey)
            }
            // diff metadata
            val diffedApp = applyDiff(metadata, jsonObject)
            val updatedApp =
                if (jsonObject.containsKey("name") || jsonObject.containsKey("summary")) {
                    diffedApp.copy(
                        localizedName = diffedApp.name.getBestLocale(locales),
                        localizedSummary = diffedApp.summary.getBestLocale(locales),
                    )
                } else diffedApp
            updateAppMetadata(updatedApp)
            // diff localizedFiles
            val localizedFiles = getLocalizedFiles(repoId, packageId)
            localizedFiles.diffAndUpdate(repoId, packageId, "icon", jsonObject)
            localizedFiles.diffAndUpdate(repoId, packageId, "featureGraphic", jsonObject)
            localizedFiles.diffAndUpdate(repoId, packageId, "promoGraphic", jsonObject)
            localizedFiles.diffAndUpdate(repoId, packageId, "tvBanner", jsonObject)
            // diff localizedFileLists
            val screenshots = jsonObject["screenshots"]
            if (screenshots is JsonNull) {
                deleteLocalizedFileLists(repoId, packageId)
            } else if (screenshots is JsonObject) {
                diffAndUpdateLocalizedFileList(repoId, packageId, "phone", screenshots)
                diffAndUpdateLocalizedFileList(repoId, packageId, "sevenInch", screenshots)
                diffAndUpdateLocalizedFileList(repoId, packageId, "tenInch", screenshots)
                diffAndUpdateLocalizedFileList(repoId, packageId, "wear", screenshots)
                diffAndUpdateLocalizedFileList(repoId, packageId, "tv", screenshots)
            }
        }
    }

    private fun List<LocalizedFile>.diffAndUpdate(
        repoId: Long,
        packageId: String,
        type: String,
        jsonObject: JsonObject,
    ) = diffAndUpdateTable(
        jsonObject = jsonObject,
        jsonObjectKey = type,
        itemList = filter { it.type == type },
        itemFinder = { locale, item -> item.locale == locale },
        newItem = { locale -> LocalizedFile(repoId, packageId, type, locale, "") },
        deleteAll = { deleteLocalizedFiles(repoId, packageId, type) },
        deleteOne = { locale -> deleteLocalizedFile(repoId, packageId, type, locale) },
        insertReplace = { list -> insert(list) },
        isNewItemValid = { it.name.isNotEmpty() },
        keyDenyList = DENY_FILE_LIST,
    )

    private fun diffAndUpdateLocalizedFileList(
        repoId: Long,
        packageId: String,
        type: String,
        jsonObject: JsonObject,
    ) {
        diffAndUpdateListTable(
            jsonObject = jsonObject,
            jsonObjectKey = type,
            listParser = { locale, jsonArray ->
                json.decodeFromJsonElement<List<FileV2>>(jsonArray).map {
                    it.toLocalizedFileList(repoId, packageId, type, locale)
                }
            },
            deleteAll = { deleteLocalizedFileLists(repoId, packageId, type) },
            deleteList = { locale -> deleteLocalizedFileList(repoId, packageId, type, locale) },
            insertNewList = { _, fileLists -> insertLocalizedFileLists(fileLists) },
        )
    }

    /**
     * This is needed to support v1 streaming and shouldn't be used for something else.
     */
    @Deprecated("Only for v1 index")
    @Query("""UPDATE AppMetadata SET preferredSigner = :preferredSigner
        WHERE repoId = :repoId AND packageId = :packageId""")
    fun updatePreferredSigner(repoId: Long, packageId: String, preferredSigner: String?)

    @Query("""UPDATE AppMetadata
        SET isCompatible = (
            SELECT TOTAL(isCompatible) > 0 FROM Version
            WHERE repoId = :repoId AND AppMetadata.packageId = Version.packageId
        )
        WHERE repoId = :repoId""")
    override fun updateCompatibility(repoId: Long)

    @Query("""UPDATE AppMetadata SET localizedName = :name, localizedSummary = :summary
        WHERE repoId = :repoId AND packageId = :packageId""")
    fun updateAppMetadata(repoId: Long, packageId: String, name: String?, summary: String?)

    @Update
    fun updateAppMetadata(appMetadata: AppMetadata): Int

    @Transaction
    @Query("""SELECT AppMetadata.* FROM AppMetadata
        JOIN RepositoryPreferences AS pref USING (repoId)
        WHERE packageId = :packageName
        ORDER BY pref.weight DESC LIMIT 1""")
    override fun getApp(packageName: String): LiveData<App?>

    @Transaction
    @Query("""SELECT * FROM AppMetadata
        WHERE repoId = :repoId AND packageId = :packageName""")
    override fun getApp(repoId: Long, packageName: String): App?

    /**
     * Used for diffing.
     */
    @Query("SELECT * FROM AppMetadata WHERE repoId = :repoId AND packageId = :packageName")
    fun getAppMetadata(repoId: Long, packageName: String): AppMetadata?

    /**
     * Used for updating best locales.
     */
    @Query("SELECT * FROM AppMetadata")
    fun getAppMetadata(): List<AppMetadata>

    /**
     * used for diffing
     */
    @Query("SELECT * FROM LocalizedFile WHERE repoId = :repoId AND packageId = :packageId")
    fun getLocalizedFiles(repoId: Long, packageId: String): List<LocalizedFile>

    @Transaction
    @Query("""SELECT repoId, packageId, app.added, app.lastUpdated, localizedName,
            localizedSummary, version.antiFeatures
        FROM AppMetadata AS app
        JOIN RepositoryPreferences AS pref USING (repoId)
        LEFT JOIN HighestVersion AS version USING (repoId, packageId)
        LEFT JOIN LocalizedIcon AS icon USING (repoId, packageId)
        WHERE pref.enabled = 1
        GROUP BY packageId HAVING MAX(pref.weight)
        ORDER BY localizedName IS NULL ASC, icon.packageId IS NULL ASC,
            localizedSummary IS NULL ASC, app.lastUpdated DESC
        LIMIT :limit""")
    override fun getAppOverviewItems(limit: Int): LiveData<List<AppOverviewItem>>

    @Transaction
    @Query("""SELECT repoId, packageId, app.added, app.lastUpdated, localizedName,
             localizedSummary, version.antiFeatures
        FROM AppMetadata AS app
        JOIN RepositoryPreferences AS pref USING (repoId)
        LEFT JOIN HighestVersion AS version USING (repoId, packageId)
        LEFT JOIN LocalizedIcon AS icon USING (repoId, packageId)
        WHERE pref.enabled = 1 AND categories  LIKE '%,' || :category || ',%'
        GROUP BY packageId HAVING MAX(pref.weight)
        ORDER BY localizedName IS NULL ASC, icon.packageId IS NULL ASC,
            localizedSummary IS NULL ASC, app.lastUpdated DESC
        LIMIT :limit""")
    override fun getAppOverviewItems(category: String, limit: Int): LiveData<List<AppOverviewItem>>

    /**
     * Used by [DbUpdateChecker] to get specific apps with available updates.
     */
    @Transaction
    @SuppressWarnings(CURSOR_MISMATCH) // no anti-features needed here
    @Query("""SELECT repoId, packageId, added, app.lastUpdated, localizedName,
             localizedSummary
        FROM AppMetadata AS app WHERE repoId = :repoId AND packageId = :packageId""")
    fun getAppOverviewItem(repoId: Long, packageId: String): AppOverviewItem?

    //
    // AppListItems
    //

    override fun getAppListItems(
        packageManager: PackageManager,
        sortOrder: AppListSortOrder,
    ): LiveData<List<AppListItem>> = when (sortOrder) {
        LAST_UPDATED -> getAppListItemsByLastUpdated().map(packageManager)
        NAME -> getAppListItemsByName().map(packageManager)
    }

    override fun getAppListItems(
        packageManager: PackageManager,
        category: String,
        sortOrder: AppListSortOrder,
    ): LiveData<List<AppListItem>> = when (sortOrder) {
        LAST_UPDATED -> getAppListItemsByLastUpdated(category).map(packageManager)
        NAME -> getAppListItemsByName(category).map(packageManager)
    }

    private fun LiveData<List<AppListItem>>.map(
        packageManager: PackageManager,
        installedPackages: Map<String, PackageInfo> = packageManager.getInstalledPackages(0)
            .associateBy { packageInfo -> packageInfo.packageName },
    ) = map { items ->
        items.map { item ->
            val packageInfo = installedPackages[item.packageId]
            if (packageInfo == null) item else item.copy(
                installedVersionName = packageInfo.versionName,
                installedVersionCode = packageInfo.getVersionCode(),
            )
        }
    }

    @Transaction
    @Query("""
        SELECT repoId, packageId, localizedName, localizedSummary, version.antiFeatures,
               app.isCompatible
        FROM AppMetadata AS app
        LEFT JOIN HighestVersion AS version USING (repoId, packageId)
        JOIN RepositoryPreferences AS pref USING (repoId)
        WHERE pref.enabled = 1
        GROUP BY packageId HAVING MAX(pref.weight)
        ORDER BY localizedName COLLATE NOCASE ASC""")
    fun getAppListItemsByName(): LiveData<List<AppListItem>>

    @Transaction
    @Query("""
        SELECT repoId, packageId, localizedName, localizedSummary, version.antiFeatures,
               app.isCompatible
        FROM AppMetadata AS app
        JOIN RepositoryPreferences AS pref USING (repoId)
        LEFT JOIN HighestVersion AS version USING (repoId, packageId)
        WHERE pref.enabled = 1
        GROUP BY packageId HAVING MAX(pref.weight)
        ORDER BY app.lastUpdated DESC""")
    fun getAppListItemsByLastUpdated(): LiveData<List<AppListItem>>

    @Transaction
    @Query("""
        SELECT repoId, packageId, localizedName, localizedSummary, version.antiFeatures,
               app.isCompatible
        FROM AppMetadata AS app
        JOIN RepositoryPreferences AS pref USING (repoId)
        LEFT JOIN HighestVersion AS version USING (repoId, packageId)
        WHERE pref.enabled = 1 AND categories LIKE '%,' || :category || ',%'
        GROUP BY packageId HAVING MAX(pref.weight)
        ORDER BY app.lastUpdated DESC""")
    fun getAppListItemsByLastUpdated(category: String): LiveData<List<AppListItem>>

    @Transaction
    @Query("""
        SELECT repoId, packageId, localizedName, localizedSummary, version.antiFeatures,
               app.isCompatible
        FROM AppMetadata AS app
        JOIN RepositoryPreferences AS pref USING (repoId)
        LEFT JOIN HighestVersion AS version USING (repoId, packageId)
        WHERE pref.enabled = 1 AND categories LIKE '%,' || :category || ',%'
        GROUP BY packageId HAVING MAX(pref.weight)
        ORDER BY localizedName COLLATE NOCASE ASC""")
    fun getAppListItemsByName(category: String): LiveData<List<AppListItem>>

    @Transaction
    @SuppressWarnings(CURSOR_MISMATCH) // no anti-features needed here
    @Query("""SELECT repoId, packageId, localizedName, localizedSummary, app.isCompatible
        FROM AppMetadata AS app
        JOIN RepositoryPreferences AS pref USING (repoId)
        WHERE pref.enabled = 1 AND packageId IN (:packageNames)
        GROUP BY packageId HAVING MAX(pref.weight)
        ORDER BY localizedName COLLATE NOCASE ASC""")
    fun getAppListItems(packageNames: List<String>): LiveData<List<AppListItem>>

    override fun getInstalledAppListItems(
        packageManager: PackageManager,
    ): LiveData<List<AppListItem>> {
        val installedPackages = packageManager.getInstalledPackages(0)
            .associateBy { packageInfo -> packageInfo.packageName }
        val packageNames = installedPackages.keys.toList()
        return getAppListItems(packageNames).map(packageManager, installedPackages)
    }

    @Query("""SELECT COUNT(DISTINCT packageId) FROM AppMetadata
        JOIN RepositoryPreferences AS pref USING (repoId)
        WHERE pref.enabled = 1 AND categories LIKE '%,' || :category || ',%'""")
    override fun getNumberOfAppsInCategory(category: String): Int

    @Query("SELECT COUNT(*) FROM AppMetadata WHERE repoId = :repoId")
    override fun getNumberOfAppsInRepository(repoId: Long): Int

    @Query("DELETE FROM AppMetadata WHERE repoId = :repoId AND packageId = :packageId")
    fun deleteAppMetadata(repoId: Long, packageId: String)

    @Query("""DELETE FROM LocalizedFile
        WHERE repoId = :repoId AND packageId = :packageId AND type = :type""")
    fun deleteLocalizedFiles(repoId: Long, packageId: String, type: String)

    @Query("""DELETE FROM LocalizedFile
        WHERE repoId = :repoId AND packageId = :packageId AND type = :type AND locale = :locale""")
    fun deleteLocalizedFile(repoId: Long, packageId: String, type: String, locale: String)

    @Query("""DELETE FROM LocalizedFileList
        WHERE repoId = :repoId AND packageId = :packageId""")
    fun deleteLocalizedFileLists(repoId: Long, packageId: String)

    @Query("""DELETE FROM LocalizedFileList
        WHERE repoId = :repoId AND packageId = :packageId AND type = :type""")
    fun deleteLocalizedFileLists(repoId: Long, packageId: String, type: String)

    @Query("""DELETE FROM LocalizedFileList
        WHERE repoId = :repoId AND packageId = :packageId AND type = :type AND locale = :locale""")
    fun deleteLocalizedFileList(repoId: Long, packageId: String, type: String, locale: String)

    @VisibleForTesting
    @Query("SELECT COUNT(*) FROM AppMetadata")
    fun countApps(): Int

    @VisibleForTesting
    @Query("SELECT COUNT(*) FROM LocalizedFile")
    fun countLocalizedFiles(): Int

    @VisibleForTesting
    @Query("SELECT COUNT(*) FROM LocalizedFileList")
    fun countLocalizedFileLists(): Int

}

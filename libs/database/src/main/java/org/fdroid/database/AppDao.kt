package org.fdroid.database

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import androidx.annotation.VisibleForTesting
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.os.ConfigurationCompat.getLocales
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.map
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import androidx.room.RoomWarnings.Companion.CURSOR_MISMATCH
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import org.fdroid.LocaleChooser.getBestLocale
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

    /**
     * Returns a list of all [AppListItem] sorted by the given [sortOrder],
     * or a subset of [AppListItem]s filtered by the given [searchQuery] if it is non-null.
     * In the later case, the [sortOrder] gets ignored.
     */
    public fun getAppListItems(
        packageManager: PackageManager,
        searchQuery: String?,
        sortOrder: AppListSortOrder,
    ): LiveData<List<AppListItem>>

    /**
     * Like [getAppListItems], but further filter items by the given [category].
     */
    public fun getAppListItems(
        packageManager: PackageManager,
        category: String,
        searchQuery: String?,
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
private val DENY_LIST = listOf("packageName", "repoId")

/**
 * A list of unknown fields in [LocalizedFileV2] or [LocalizedFileListV2]
 * that we don't allow for [LocalizedFile] or [LocalizedFileList].
 *
 * Similar to [DENY_LIST].
 */
private val DENY_FILE_LIST = listOf("packageName", "repoId", "type")

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

    private fun LocalizedFileV2?.insert(repoId: Long, packageName: String, type: String) {
        this?.toLocalizedFile(repoId, packageName, type)?.let { files ->
            insert(files)
        }
    }

    @JvmName("insertLocalizedFileListV2")
    private fun LocalizedFileListV2?.insert(repoId: Long, packageName: String, type: String) {
        this?.toLocalizedFileList(repoId, packageName, type)?.let { files ->
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
        packageName: String,
        jsonObject: JsonObject?,
        locales: LocaleListCompat,
    ) {
        if (jsonObject == null) {
            // this app is gone, we need to delete it
            deleteAppMetadata(repoId, packageName)
            return
        }
        val metadata = getAppMetadata(repoId, packageName)
        if (metadata == null) { // new app
            val metadataV2: MetadataV2 = json.decodeFromJsonElement(jsonObject)
            insert(repoId, packageName, metadataV2)
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
            val localizedFiles = getLocalizedFiles(repoId, packageName)
            localizedFiles.diffAndUpdate(repoId, packageName, "icon", jsonObject)
            localizedFiles.diffAndUpdate(repoId, packageName, "featureGraphic", jsonObject)
            localizedFiles.diffAndUpdate(repoId, packageName, "promoGraphic", jsonObject)
            localizedFiles.diffAndUpdate(repoId, packageName, "tvBanner", jsonObject)
            // diff localizedFileLists
            val screenshots = jsonObject["screenshots"]
            if (screenshots is JsonNull) {
                deleteLocalizedFileLists(repoId, packageName)
            } else if (screenshots is JsonObject) {
                diffAndUpdateLocalizedFileList(repoId, packageName, "phone", screenshots)
                diffAndUpdateLocalizedFileList(repoId, packageName, "sevenInch", screenshots)
                diffAndUpdateLocalizedFileList(repoId, packageName, "tenInch", screenshots)
                diffAndUpdateLocalizedFileList(repoId, packageName, "wear", screenshots)
                diffAndUpdateLocalizedFileList(repoId, packageName, "tv", screenshots)
            }
        }
    }

    private fun List<LocalizedFile>.diffAndUpdate(
        repoId: Long,
        packageName: String,
        type: String,
        jsonObject: JsonObject,
    ) = diffAndUpdateTable(
        jsonObject = jsonObject,
        jsonObjectKey = type,
        itemList = filter { it.type == type },
        itemFinder = { locale, item -> item.locale == locale },
        newItem = { locale -> LocalizedFile(repoId, packageName, type, locale, "") },
        deleteAll = { deleteLocalizedFiles(repoId, packageName, type) },
        deleteOne = { locale -> deleteLocalizedFile(repoId, packageName, type, locale) },
        insertReplace = { list -> insert(list) },
        isNewItemValid = { it.name.isNotEmpty() },
        keyDenyList = DENY_FILE_LIST,
    )

    private fun diffAndUpdateLocalizedFileList(
        repoId: Long,
        packageName: String,
        type: String,
        jsonObject: JsonObject,
    ) {
        diffAndUpdateListTable(
            jsonObject = jsonObject,
            jsonObjectKey = type,
            listParser = { locale, jsonArray ->
                json.decodeFromJsonElement<List<FileV2>>(jsonArray).map {
                    it.toLocalizedFileList(repoId, packageName, type, locale)
                }
            },
            deleteAll = { deleteLocalizedFileLists(repoId, packageName, type) },
            deleteList = { locale -> deleteLocalizedFileList(repoId, packageName, type, locale) },
            insertNewList = { _, fileLists -> insertLocalizedFileLists(fileLists) },
        )
    }

    /**
     * This is needed to support v1 streaming and shouldn't be used for something else.
     */
    @Deprecated("Only for v1 index")
    @Query("""UPDATE ${AppMetadata.TABLE} SET preferredSigner = :preferredSigner
        WHERE repoId = :repoId AND packageName = :packageName""")
    fun updatePreferredSigner(repoId: Long, packageName: String, preferredSigner: String?)

    @Query("""UPDATE ${AppMetadata.TABLE} 
        SET isCompatible = (
            SELECT TOTAL(isCompatible) > 0 FROM ${Version.TABLE}
            WHERE repoId = :repoId AND ${AppMetadata.TABLE}.packageName = ${Version.TABLE}.packageName
        )
        WHERE repoId = :repoId""")
    override fun updateCompatibility(repoId: Long)

    @Query("""UPDATE ${AppMetadata.TABLE} SET localizedName = :name, localizedSummary = :summary
        WHERE repoId = :repoId AND packageName = :packageName""")
    fun updateAppMetadata(repoId: Long, packageName: String, name: String?, summary: String?)

    @Update
    fun updateAppMetadata(appMetadata: AppMetadata): Int

    @Transaction
    @Query("""SELECT ${AppMetadata.TABLE}.* FROM ${AppMetadata.TABLE}
        JOIN RepositoryPreferences AS pref USING (repoId)
        WHERE packageName = :packageName AND pref.enabled = 1
        ORDER BY pref.weight DESC LIMIT 1""")
    override fun getApp(packageName: String): LiveData<App?>

    @Transaction
    @Query("""SELECT * FROM ${AppMetadata.TABLE}
        WHERE repoId = :repoId AND packageName = :packageName""")
    override fun getApp(repoId: Long, packageName: String): App?

    /**
     * Used for diffing.
     */
    @Query("""SELECT * FROM ${AppMetadata.TABLE}
        WHERE repoId = :repoId AND packageName = :packageName""")
    fun getAppMetadata(repoId: Long, packageName: String): AppMetadata?

    /**
     * Used for updating best locales.
     */
    @Query("SELECT * FROM ${AppMetadata.TABLE}")
    fun getAppMetadata(): List<AppMetadata>

    /**
     * used for diffing
     */
    @Query("""SELECT * FROM ${LocalizedFile.TABLE}
        WHERE repoId = :repoId AND packageName = :packageName""")
    fun getLocalizedFiles(repoId: Long, packageName: String): List<LocalizedFile>

    @Transaction
    @Query("""SELECT repoId, packageName, app.added, app.lastUpdated, localizedName,
            localizedSummary, version.antiFeatures
        FROM ${AppMetadata.TABLE} AS app
        JOIN ${RepositoryPreferences.TABLE} AS pref USING (repoId)
        LEFT JOIN ${HighestVersion.TABLE} AS version USING (repoId, packageName)
        LEFT JOIN ${LocalizedIcon.TABLE} AS icon USING (repoId, packageName)
        WHERE pref.enabled = 1
        GROUP BY packageName HAVING MAX(pref.weight)
        ORDER BY localizedName IS NULL ASC, icon.packageName IS NULL ASC,
            localizedSummary IS NULL ASC, app.lastUpdated DESC
        LIMIT :limit""")
    override fun getAppOverviewItems(limit: Int): LiveData<List<AppOverviewItem>>

    @Transaction
    @Query("""SELECT repoId, packageName, app.added, app.lastUpdated, localizedName,
             localizedSummary, version.antiFeatures
        FROM ${AppMetadata.TABLE} AS app
        JOIN ${RepositoryPreferences.TABLE} AS pref USING (repoId)
        LEFT JOIN ${HighestVersion.TABLE} AS version USING (repoId, packageName)
        LEFT JOIN ${LocalizedIcon.TABLE} AS icon USING (repoId, packageName)
        WHERE pref.enabled = 1 AND categories  LIKE '%,' || :category || ',%'
        GROUP BY packageName HAVING MAX(pref.weight)
        ORDER BY localizedName IS NULL ASC, icon.packageName IS NULL ASC,
            localizedSummary IS NULL ASC, app.lastUpdated DESC
        LIMIT :limit""")
    override fun getAppOverviewItems(category: String, limit: Int): LiveData<List<AppOverviewItem>>

    /**
     * Used by [DbUpdateChecker] to get specific apps with available updates.
     */
    @Transaction
    @SuppressWarnings(CURSOR_MISMATCH) // no anti-features needed here
    @Query("""SELECT repoId, packageName, added, app.lastUpdated, localizedName,
             localizedSummary
        FROM ${AppMetadata.TABLE} AS app WHERE repoId = :repoId AND packageName = :packageName""")
    fun getAppOverviewItem(repoId: Long, packageName: String): AppOverviewItem?

    //
    // AppListItems
    //

    override fun getAppListItems(
        packageManager: PackageManager,
        searchQuery: String?,
        sortOrder: AppListSortOrder,
    ): LiveData<List<AppListItem>> {
        return if (searchQuery.isNullOrEmpty()) when (sortOrder) {
            LAST_UPDATED -> getAppListItemsByLastUpdated().map(packageManager)
            NAME -> getAppListItemsByName().map(packageManager)
        } else getAppListItems(escapeQuery(searchQuery)).map(packageManager)
    }

    override fun getAppListItems(
        packageManager: PackageManager,
        category: String,
        searchQuery: String?,
        sortOrder: AppListSortOrder,
    ): LiveData<List<AppListItem>> {
        return if (searchQuery.isNullOrEmpty()) when (sortOrder) {
            LAST_UPDATED -> getAppListItemsByLastUpdated(category).map(packageManager)
            NAME -> getAppListItemsByName(category).map(packageManager)
        } else getAppListItems(category, escapeQuery(searchQuery)).map(packageManager)
    }

    private fun escapeQuery(searchQuery: String): String {
        val sanitized = searchQuery.replace(Regex.fromLiteral("\""), "\"\"")
        return "\"*$sanitized*\""
    }

    private fun LiveData<List<AppListItem>>.map(
        packageManager: PackageManager,
        installedPackages: Map<String, PackageInfo> = packageManager.getInstalledPackages(0)
            .associateBy { packageInfo -> packageInfo.packageName },
    ) = map { items ->
        items.map { item ->
            val packageInfo = installedPackages[item.packageName]
            if (packageInfo == null) item else item.copy(
                installedVersionName = packageInfo.versionName,
                installedVersionCode = PackageInfoCompat.getLongVersionCode(packageInfo),
            )
        }
    }

    /**
     * Warning: Run [escapeQuery] on the given [searchQuery] before.
     */
    @Transaction
    @Query("""
        SELECT repoId, packageName, app.localizedName, app.localizedSummary, app.lastUpdated, 
               version.antiFeatures, app.isCompatible, app.preferredSigner
        FROM ${AppMetadata.TABLE} AS app
        JOIN ${AppMetadataFts.TABLE} USING (repoId, packageName)
        LEFT JOIN ${HighestVersion.TABLE} AS version USING (repoId, packageName)
        JOIN ${RepositoryPreferences.TABLE} AS pref USING (repoId)
        WHERE pref.enabled = 1 AND ${AppMetadataFts.TABLE} MATCH :searchQuery
        GROUP BY packageName HAVING MAX(pref.weight)""")
    fun getAppListItems(searchQuery: String): LiveData<List<AppListItem>>

    /**
     * Warning: Run [escapeQuery] on the given [searchQuery] before.
     */
    @Transaction
    @Query("""
        SELECT repoId, packageName, app.localizedName, app.localizedSummary, app.lastUpdated, 
               version.antiFeatures, app.isCompatible, app.preferredSigner
        FROM ${AppMetadata.TABLE} AS app
        JOIN ${AppMetadataFts.TABLE} USING (repoId, packageName)
        LEFT JOIN ${HighestVersion.TABLE} AS version USING (repoId, packageName)
        JOIN ${RepositoryPreferences.TABLE} AS pref USING (repoId)
        WHERE pref.enabled = 1 AND categories LIKE '%,' || :category || ',%' AND
              ${AppMetadataFts.TABLE} MATCH :searchQuery
        GROUP BY packageName HAVING MAX(pref.weight)""")
    fun getAppListItems(category: String, searchQuery: String): LiveData<List<AppListItem>>

    @Transaction
    @Query("""
        SELECT repoId, packageName, localizedName, localizedSummary, app.lastUpdated, 
               version.antiFeatures, app.isCompatible, app.preferredSigner
        FROM ${AppMetadata.TABLE} AS app
        LEFT JOIN ${HighestVersion.TABLE} AS version USING (repoId, packageName)
        JOIN ${RepositoryPreferences.TABLE} AS pref USING (repoId)
        WHERE pref.enabled = 1
        GROUP BY packageName HAVING MAX(pref.weight)
        ORDER BY localizedName COLLATE NOCASE ASC""")
    fun getAppListItemsByName(): LiveData<List<AppListItem>>

    @Transaction
    @Query("""
        SELECT repoId, packageName, localizedName, localizedSummary, app.lastUpdated,
               version.antiFeatures, app.isCompatible, app.preferredSigner
        FROM ${AppMetadata.TABLE} AS app
        JOIN ${RepositoryPreferences.TABLE} AS pref USING (repoId)
        LEFT JOIN ${HighestVersion.TABLE} AS version USING (repoId, packageName)
        WHERE pref.enabled = 1
        GROUP BY packageName HAVING MAX(pref.weight)
        ORDER BY app.lastUpdated DESC""")
    fun getAppListItemsByLastUpdated(): LiveData<List<AppListItem>>

    @Transaction
    @Query("""
        SELECT repoId, packageName, localizedName, localizedSummary, app.lastUpdated, 
               version.antiFeatures, app.isCompatible, app.preferredSigner
        FROM ${AppMetadata.TABLE} AS app
        JOIN ${RepositoryPreferences.TABLE} AS pref USING (repoId)
        LEFT JOIN ${HighestVersion.TABLE} AS version USING (repoId, packageName)
        WHERE pref.enabled = 1 AND categories LIKE '%,' || :category || ',%'
        GROUP BY packageName HAVING MAX(pref.weight)
        ORDER BY app.lastUpdated DESC""")
    fun getAppListItemsByLastUpdated(category: String): LiveData<List<AppListItem>>

    @Transaction
    @Query("""
        SELECT repoId, packageName, localizedName, localizedSummary, app.lastUpdated,
               version.antiFeatures, app.isCompatible, app.preferredSigner
        FROM ${AppMetadata.TABLE} AS app
        JOIN ${RepositoryPreferences.TABLE} AS pref USING (repoId)
        LEFT JOIN ${HighestVersion.TABLE} AS version USING (repoId, packageName)
        WHERE pref.enabled = 1 AND categories LIKE '%,' || :category || ',%'
        GROUP BY packageName HAVING MAX(pref.weight)
        ORDER BY localizedName COLLATE NOCASE ASC""")
    fun getAppListItemsByName(category: String): LiveData<List<AppListItem>>

    /**
     * Warning: Can not be called with more than 999 [packageNames].
     */
    @Transaction
    @SuppressWarnings(CURSOR_MISMATCH) // no anti-features needed here
    @Query("""SELECT repoId, packageName, localizedName, localizedSummary, app.lastUpdated, 
                     app.isCompatible, app.preferredSigner
        FROM ${AppMetadata.TABLE} AS app
        JOIN ${RepositoryPreferences.TABLE} AS pref USING (repoId)
        WHERE pref.enabled = 1 AND packageName IN (:packageNames)
        GROUP BY packageName HAVING MAX(pref.weight)
        ORDER BY localizedName COLLATE NOCASE ASC""")
    fun getAppListItems(packageNames: List<String>): LiveData<List<AppListItem>>

    override fun getInstalledAppListItems(
        packageManager: PackageManager,
    ): LiveData<List<AppListItem>> {
        val installedPackages = packageManager.getInstalledPackages(0)
            .associateBy { packageInfo -> packageInfo.packageName }
        val packageNames = installedPackages.keys.toList()
        return if (packageNames.size <= 999) {
            getAppListItems(packageNames).map(packageManager, installedPackages)
        } else {
            AppListLiveData().apply {
                packageNames.chunked(999) { addSource(getAppListItems(it)) }
            }.map(packageManager, installedPackages)
        }
    }

    private class AppListLiveData : MediatorLiveData<List<AppListItem>>() {
        private val list = ArrayList<LiveData<List<AppListItem>>>()

        /**
         * Adds the given [liveData] and updates [getValue] with a union of all lists
         * once all added [liveData]s changed to a non-null list value.
         */
        fun addSource(liveData: LiveData<List<AppListItem>>) {
            list.add(liveData)
            addSource(liveData) {
                var shouldUpdate = true
                val result = list.flatMap {
                    it.value ?: run {
                        shouldUpdate = false
                        emptyList()
                    }
                }
                if (shouldUpdate) value = result.sortedWith { i1, i2 ->
                    // we need to re-sort the result, because each liveData is only sorted in itself
                    val n1 = i1.name ?: ""
                    val n2 = i2.name ?: ""
                    n1.compareTo(n2, ignoreCase = true)
                }
            }
        }
    }

    //
    // Misc Queries
    //

    @Query("""SELECT COUNT(DISTINCT packageName) FROM ${AppMetadata.TABLE}
        JOIN ${RepositoryPreferences.TABLE} AS pref USING (repoId)
        WHERE pref.enabled = 1 AND categories LIKE '%,' || :category || ',%'""")
    override fun getNumberOfAppsInCategory(category: String): Int

    @Query("SELECT COUNT(*) FROM ${AppMetadata.TABLE} WHERE repoId = :repoId")
    override fun getNumberOfAppsInRepository(repoId: Long): Int

    @Query("DELETE FROM ${AppMetadata.TABLE} WHERE repoId = :repoId AND packageName = :packageName")
    fun deleteAppMetadata(repoId: Long, packageName: String)

    @Query("""DELETE FROM ${LocalizedFile.TABLE}
        WHERE repoId = :repoId AND packageName = :packageName AND type = :type""")
    fun deleteLocalizedFiles(repoId: Long, packageName: String, type: String)

    @Query("""DELETE FROM ${LocalizedFile.TABLE}
        WHERE repoId = :repoId AND packageName = :packageName AND type = :type
        AND locale = :locale""")
    fun deleteLocalizedFile(repoId: Long, packageName: String, type: String, locale: String)

    @Query("""DELETE FROM ${LocalizedFileList.TABLE}
        WHERE repoId = :repoId AND packageName = :packageName""")
    fun deleteLocalizedFileLists(repoId: Long, packageName: String)

    @Query("""DELETE FROM ${LocalizedFileList.TABLE}
        WHERE repoId = :repoId AND packageName = :packageName AND type = :type""")
    fun deleteLocalizedFileLists(repoId: Long, packageName: String, type: String)

    @Query("""DELETE FROM ${LocalizedFileList.TABLE}
        WHERE repoId = :repoId AND packageName = :packageName AND type = :type
        AND locale = :locale""")
    fun deleteLocalizedFileList(repoId: Long, packageName: String, type: String, locale: String)

    @VisibleForTesting
    @Query("SELECT COUNT(*) FROM ${AppMetadata.TABLE}")
    fun countApps(): Int

    @VisibleForTesting
    @Query("SELECT COUNT(*) FROM ${LocalizedFile.TABLE}")
    fun countLocalizedFiles(): Int

    @VisibleForTesting
    @Query("SELECT COUNT(*) FROM ${LocalizedFileList.TABLE}")
    fun countLocalizedFileLists(): Int

    /**
     * Removes all apps and associated data such as versions from the database.
     * Careful: Doing this without other measures such as calling [RepositoryDaoInt.resetTimestamps]
     * will cause application of diffs to fail.
     */
    @Query("DELETE FROM ${AppMetadata.TABLE}")
    fun clearAll()
}

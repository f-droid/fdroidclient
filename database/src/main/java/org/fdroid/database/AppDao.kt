package org.fdroid.database

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import androidx.annotation.VisibleForTesting
import androidx.core.os.ConfigurationCompat.getLocales
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomWarnings.CURSOR_MISMATCH
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import org.fdroid.database.DbDiffUtils.diffAndUpdateListTable
import org.fdroid.database.DbDiffUtils.diffAndUpdateTable
import org.fdroid.database.FDroidDatabaseHolder.dispatcher
import org.fdroid.index.IndexParser.json
import org.fdroid.index.v2.FileV2
import org.fdroid.index.v2.LocalizedFileListV2
import org.fdroid.index.v2.LocalizedFileV2
import org.fdroid.index.v2.MetadataV2
import org.fdroid.index.v2.ReflectionDiffer.applyDiff
import org.fdroid.index.v2.Screenshots

public interface AppDao {
    public fun insert(
        repoId: Long,
        packageId: String,
        app: MetadataV2,
        locales: LocaleListCompat = getLocales(Resources.getSystem().configuration),
    )

    /**
     * Gets the app from the DB. If more than one app with this [packageId] exists,
     * the one from the repository with the highest weight is returned.
     */
    public fun getApp(packageId: String): LiveData<App?>
    public fun getApp(repoId: Long, packageId: String): App?
    public fun getAppOverviewItems(limit: Int = 200): LiveData<List<AppOverviewItem>>
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
        packageId: String,
        app: MetadataV2,
        locales: LocaleListCompat,
    ) {
        insert(app.toAppMetadata(repoId, packageId, false))
        app.icon.insert(repoId, packageId, "icon")
        app.featureGraphic.insert(repoId, packageId, "featureGraphic")
        app.promoGraphic.insert(repoId, packageId, "promoGraphic")
        app.tvBanner.insert(repoId, packageId, "tvBanner")
        app.screenshots?.let {
            it.phone.insert(repoId, packageId, "phone")
            it.sevenInch.insert(repoId, packageId, "sevenInch")
            it.tenInch.insert(repoId, packageId, "tenInch")
            it.wear.insert(repoId, packageId, "wear")
            it.tv.insert(repoId, packageId, "tv")
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(appMetadata: AppMetadata)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(localizedFiles: List<LocalizedFile>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
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
    @Query("""UPDATE AppMetadata SET preferredSigner = :preferredSigner
        WHERE repoId = :repoId AND packageId = :packageId""")
    fun updatePreferredSigner(repoId: Long, packageId: String, preferredSigner: String?)

    /**
     * Updates the [AppMetadata.isCompatible] flag
     * based on whether at least one [AppVersion] is compatible.
     * This needs to run within the transaction that adds [AppMetadata] to the DB.
     * Otherwise the compatibility is wrong.
     */
    @Query("""UPDATE AppMetadata
        SET isCompatible = (
            SELECT TOTAL(isCompatible) > 0 FROM Version
            WHERE repoId = :repoId AND AppMetadata.packageId = Version.packageId
        )
        WHERE repoId = :repoId""")
    fun updateCompatibility(repoId: Long)

    @Query("""UPDATE AppMetadata SET localizedName = :name, localizedSummary = :summary
        WHERE repoId = :repoId AND packageId = :packageId""")
    fun updateAppMetadata(repoId: Long, packageId: String, name: String?, summary: String?)

    @Update
    fun updateAppMetadata(appMetadata: AppMetadata): Int

    override fun getApp(packageId: String): LiveData<App?> {
        return getRepoIdForPackage(packageId).distinctUntilChanged().switchMap { repoId ->
            if (repoId == null) MutableLiveData(null)
            else getLiveApp(repoId, packageId)
        }
    }

    @Query("""SELECT repoId FROM RepositoryPreferences
        JOIN AppMetadata AS app USING(repoId)
        WHERE app.packageId = :packageId AND enabled = 1 ORDER BY weight DESC LIMIT 1""")
    fun getRepoIdForPackage(packageId: String): LiveData<Long?>

    fun getLiveApp(repoId: Long, packageId: String): LiveData<App?> = liveData(dispatcher) {
        // TODO maybe observe those as well?
        val localizedFiles = getLocalizedFiles(repoId, packageId)
        val localizedFileList = getLocalizedFileLists(repoId, packageId)
        val liveData: LiveData<App?> =
            getLiveAppMetadata(repoId, packageId).distinctUntilChanged().map {
                getApp(it, localizedFiles, localizedFileList)
            }
        emitSource(liveData)
    }

    @Transaction
    override fun getApp(repoId: Long, packageId: String): App? {
        val metadata = getAppMetadata(repoId, packageId) ?: return null
        val localizedFiles = getLocalizedFiles(repoId, packageId)
        val localizedFileList = getLocalizedFileLists(repoId, packageId)
        return getApp(metadata, localizedFiles, localizedFileList)
    }

    private fun getApp(
        metadata: AppMetadata,
        localizedFiles: List<LocalizedFile>?,
        localizedFileList: List<LocalizedFileList>?,
    ) = App(
        metadata = metadata,
        icon = localizedFiles?.toLocalizedFileV2("icon"),
        featureGraphic = localizedFiles?.toLocalizedFileV2("featureGraphic"),
        promoGraphic = localizedFiles?.toLocalizedFileV2("promoGraphic"),
        tvBanner = localizedFiles?.toLocalizedFileV2("tvBanner"),
        screenshots = if (localizedFileList.isNullOrEmpty()) null else Screenshots(
            phone = localizedFileList.toLocalizedFileListV2("phone"),
            sevenInch = localizedFileList.toLocalizedFileListV2("sevenInch"),
            tenInch = localizedFileList.toLocalizedFileListV2("tenInch"),
            wear = localizedFileList.toLocalizedFileListV2("wear"),
            tv = localizedFileList.toLocalizedFileListV2("tv"),
        )
    )

    @Query("SELECT * FROM AppMetadata WHERE repoId = :repoId AND packageId = :packageId")
    fun getLiveAppMetadata(repoId: Long, packageId: String): LiveData<AppMetadata>

    @Query("SELECT * FROM AppMetadata WHERE repoId = :repoId AND packageId = :packageId")
    fun getAppMetadata(repoId: Long, packageId: String): AppMetadata?

    @Query("SELECT * FROM AppMetadata")
    fun getAppMetadata(): List<AppMetadata>

    @Query("SELECT * FROM LocalizedFile WHERE repoId = :repoId AND packageId = :packageId")
    fun getLocalizedFiles(repoId: Long, packageId: String): List<LocalizedFile>

    @Query("SELECT * FROM LocalizedFileList WHERE repoId = :repoId AND packageId = :packageId")
    fun getLocalizedFileLists(repoId: Long, packageId: String): List<LocalizedFileList>

    @Query("SELECT * FROM LocalizedFile")
    fun getLocalizedFiles(): List<LocalizedFile>

    @Query("SELECT * FROM LocalizedFileList")
    fun getLocalizedFileLists(): List<LocalizedFileList>

    @Transaction
    @Query("""SELECT repoId, packageId, app.added, app.lastUpdated, localizedName,
             localizedSummary, version.antiFeatures
        FROM AppMetadata AS app
        JOIN RepositoryPreferences AS pref USING (repoId)
        JOIN Version AS version USING (repoId, packageId)
        JOIN LocalizedIcon AS icon USING (repoId, packageId)
        WHERE pref.enabled = 1
        GROUP BY packageId HAVING MAX(pref.weight) AND MAX(version.manifest_versionCode)
        ORDER BY localizedName IS NULL ASC, localizedSummary IS NULL ASC, app.lastUpdated DESC, app.added ASC
        LIMIT :limit""")
    override fun getAppOverviewItems(limit: Int): LiveData<List<AppOverviewItem>>

    @Transaction
    // TODO maybe it makes sense to split categories into their own table for this?
    @Query("""SELECT repoId, packageId, app.added, app.lastUpdated, localizedName,
             localizedSummary, version.antiFeatures
        FROM AppMetadata AS app
        JOIN RepositoryPreferences AS pref USING (repoId)
        JOIN Version AS version USING (repoId, packageId)
        JOIN LocalizedIcon AS icon USING (repoId, packageId)
        WHERE pref.enabled = 1 AND categories  LIKE '%,' || :category || ',%'
        GROUP BY packageId HAVING MAX(pref.weight) AND MAX(version.manifest_versionCode)
        ORDER BY localizedName IS NULL ASC, localizedSummary IS NULL ASC, app.lastUpdated DESC, app.added ASC
        LIMIT :limit""")
    override fun getAppOverviewItems(category: String, limit: Int): LiveData<List<AppOverviewItem>>

    override fun getAppListItems(
        packageManager: PackageManager,
        sortOrder: AppListSortOrder,
    ): LiveData<List<AppListItem>> {
        return when (sortOrder) {
            AppListSortOrder.LAST_UPDATED -> getAppListItemsByLastUpdated().map(packageManager)
            AppListSortOrder.NAME -> getAppListItemsByName().map(packageManager)
        }
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
        JOIN Version AS version USING (repoId, packageId)
        JOIN RepositoryPreferences AS pref USING (repoId)
        WHERE pref.enabled = 1
        GROUP BY packageId HAVING MAX(pref.weight) AND MAX(version.manifest_versionCode)
        ORDER BY localizedName COLLATE NOCASE ASC""")
    fun getAppListItemsByName(): LiveData<List<AppListItem>>

    @Transaction
    @Query("""
        SELECT repoId, packageId, localizedName, localizedSummary, version.antiFeatures,
               app.isCompatible
        FROM AppMetadata AS app
        JOIN Version AS version USING (repoId, packageId)
        JOIN RepositoryPreferences AS pref USING (repoId)
        WHERE pref.enabled = 1
        GROUP BY packageId HAVING MAX(pref.weight) AND MAX(version.manifest_versionCode)
        ORDER BY app.lastUpdated DESC""")
    fun getAppListItemsByLastUpdated(): LiveData<List<AppListItem>>

    override fun getAppListItems(
        packageManager: PackageManager,
        category: String,
        sortOrder: AppListSortOrder,
    ): LiveData<List<AppListItem>> {
        return when (sortOrder) {
            AppListSortOrder.LAST_UPDATED -> {
                getAppListItemsByLastUpdated(category).map(packageManager)
            }
            AppListSortOrder.NAME -> getAppListItemsByName(category).map(packageManager)
        }
    }

    // TODO maybe it makes sense to split categories into their own table for this?
    @Transaction
    @Query("""
        SELECT repoId, packageId, localizedName, localizedSummary, version.antiFeatures,
               app.isCompatible
        FROM AppMetadata AS app
        JOIN Version AS version USING (repoId, packageId)
        JOIN RepositoryPreferences AS pref USING (repoId)
        WHERE pref.enabled = 1 AND categories  LIKE '%,' || :category || ',%'
        GROUP BY packageId HAVING MAX(pref.weight) AND MAX(version.manifest_versionCode)
        ORDER BY app.lastUpdated DESC""")
    fun getAppListItemsByLastUpdated(category: String): LiveData<List<AppListItem>>

    // TODO maybe it makes sense to split categories into their own table for this?
    @Transaction
    @Query("""
        SELECT repoId, packageId, localizedName, localizedSummary, version.antiFeatures,
               app.isCompatible
        FROM AppMetadata AS app
        JOIN Version AS version USING (repoId, packageId)
        JOIN RepositoryPreferences AS pref USING (repoId)
        WHERE pref.enabled = 1 AND categories  LIKE '%,' || :category || ',%'
        GROUP BY packageId HAVING MAX(pref.weight) AND MAX(version.manifest_versionCode)
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

    /**
     * Used by [UpdateChecker] to get specific apps with available updates.
     */
    @Transaction
    @SuppressWarnings(CURSOR_MISMATCH) // no anti-features needed here
    @Query("""SELECT repoId, packageId, added, app.lastUpdated, localizedName,
             localizedSummary
        FROM AppMetadata AS app WHERE repoId = :repoId AND packageId = :packageId""")
    fun getAppOverviewItem(repoId: Long, packageId: String): AppOverviewItem?

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

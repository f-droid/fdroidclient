package org.fdroid.database

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import org.fdroid.index.v2.LocalizedFileListV2
import org.fdroid.index.v2.LocalizedFileV2
import org.fdroid.index.v2.MetadataV2
import org.fdroid.index.v2.Screenshots

public interface AppDao {
    fun insert(repoId: Long, packageId: String, app: MetadataV2)
    fun getApp(repoId: Long, packageId: String): App
    fun getAppOverviewItems(limit: Int = 200): LiveData<List<AppOverviewItem>>
    fun getAppOverviewItems(category: String, limit: Int = 50): LiveData<List<AppOverviewItem>>
    fun getNumberOfAppsInCategory(category: String): Int
}

@Dao
internal interface AppDaoInt : AppDao {

    @Transaction
    override fun insert(repoId: Long, packageId: String, app: MetadataV2) {
        insert(app.toAppMetadata(repoId, packageId))
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

    /**
     * This is needed to support v1 streaming and shouldn't be used for something else.
     */
    @Query("UPDATE AppMetadata SET preferredSigner = :preferredSigner WHERE repoId = :repoId AND packageId = :packageId")
    fun updatePreferredSigner(repoId: Long, packageId: String, preferredSigner: String?)

    @Transaction
    override fun getApp(repoId: Long, packageId: String): App {
        val localizedFiles = getLocalizedFiles(repoId, packageId)
        val localizedFileList = getLocalizedFileLists(repoId, packageId)
        return App(
            metadata = getAppMetadata(repoId, packageId),
            icon = localizedFiles.toLocalizedFileV2("icon"),
            featureGraphic = localizedFiles.toLocalizedFileV2("featureGraphic"),
            promoGraphic = localizedFiles.toLocalizedFileV2("promoGraphic"),
            tvBanner = localizedFiles.toLocalizedFileV2("tvBanner"),
            screenshots = if (localizedFileList.isEmpty()) null else Screenshots(
                phone = localizedFileList.toLocalizedFileListV2("phone"),
                sevenInch = localizedFileList.toLocalizedFileListV2("sevenInch"),
                tenInch = localizedFileList.toLocalizedFileListV2("tenInch"),
                wear = localizedFileList.toLocalizedFileListV2("wear"),
                tv = localizedFileList.toLocalizedFileListV2("tv"),
            )
        )
    }

    @Query("SELECT * FROM AppMetadata WHERE repoId = :repoId AND packageId = :packageId")
    fun getAppMetadata(repoId: Long, packageId: String): AppMetadata

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

    // sort order from F-Droid
    //table + "." + Cols.IS_LOCALIZED + " DESC"
    //+ ", " + table + "." + Cols.NAME + " IS NULL ASC"
    //+ ", CASE WHEN " + table + "." + Cols.ICON + " IS NULL"
    //+ "        AND " + table + "." + Cols.ICON_URL + " IS NULL"
    //+ "        THEN 1 ELSE 0 END"
    //+ ", " + table + "." + Cols.SUMMARY + " IS NULL ASC"
    //+ ", " + table + "." + Cols.DESCRIPTION + " IS NULL ASC"
    //+ ", CASE WHEN " + table + "." + Cols.PHONE_SCREENSHOTS + " IS NULL"
    //+ "        AND " + table + "." + Cols.SEVEN_INCH_SCREENSHOTS + " IS NULL"
    //+ "        AND " + table + "." + Cols.TEN_INCH_SCREENSHOTS + " IS NULL"
    //+ "        AND " + table + "." + Cols.TV_SCREENSHOTS + " IS NULL"
    //+ "        AND " + table + "." + Cols.WEAR_SCREENSHOTS + " IS NULL"
    //+ "        AND " + table + "." + Cols.FEATURE_GRAPHIC + " IS NULL"
    //+ "        AND " + table + "." + Cols.PROMO_GRAPHIC + " IS NULL"
    //+ "        AND " + table + "." + Cols.TV_BANNER + " IS NULL"
    //+ "        THEN 1 ELSE 0 END"
    //+ ", CASE WHEN date(" + added + ")  >= date(" + lastUpdated + ")"
    //+ "        AND date((SELECT " + RepoTable.Cols.LAST_UPDATED + " FROM " + RepoTable.NAME
    //+ "                  WHERE _id=" + table + "." + Cols.REPO_ID
    //+ "                  ),'-" + AppCardController.DAYS_TO_CONSIDER_NEW + " days') "
    //+ "          < date(" + lastUpdated + ")"
    //+ "        THEN 0 ELSE 1 END"
    //+ ", " + table + "." + Cols.WHATSNEW + " IS NULL ASC"
    //+ ", " + lastUpdated + " DESC"
    //+ ", " + added + " ASC");
    @Transaction
    @Query("""SELECT repoId, packageId, added, lastUpdated, name, summary FROM AppMetadata
        ORDER BY name IS NULL ASC, summary IS NULL ASC, lastUpdated DESC, added ASC LIMIT :limit""")
    override fun getAppOverviewItems(limit: Int): LiveData<List<AppOverviewItem>>

    @Transaction
    // TODO maybe it makes sense to split categories into their own table for this?
    @Query("""SELECT repoId, packageId, added, lastUpdated, name, summary FROM AppMetadata
        WHERE categories  LIKE '%' || :category || '%'
        ORDER BY name IS NULL ASC, summary IS NULL ASC, lastUpdated DESC, added ASC LIMIT :limit""")
    override fun getAppOverviewItems(category: String, limit: Int): LiveData<List<AppOverviewItem>>

    // FIXME don't over report the same app twice (e.g. in several repos)
    @Query("SELECT COUNT(*) FROM AppMetadata WHERE categories  LIKE '%' || :category || '%'")
    override fun getNumberOfAppsInCategory(category: String): Int

    @VisibleForTesting
    @Query("DELETE FROM AppMetadata WHERE repoId = :repoId AND packageId = :packageId")
    fun deleteAppMetadata(repoId: Long, packageId: String)

    @Query("SELECT COUNT(*) FROM AppMetadata")
    fun countApps(): Int

    @Query("SELECT COUNT(*) FROM LocalizedFile")
    fun countLocalizedFiles(): Int

    @Query("SELECT COUNT(*) FROM LocalizedFileList")
    fun countLocalizedFileLists(): Int

}

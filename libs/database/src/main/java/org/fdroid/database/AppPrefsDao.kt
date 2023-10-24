package org.fdroid.database

import androidx.lifecycle.LiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.MapInfo
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query

public interface AppPrefsDao {
    public fun getAppPrefs(packageName: String): LiveData<AppPrefs>
    public fun update(appPrefs: AppPrefs)
}

@Dao
internal interface AppPrefsDaoInt : AppPrefsDao {

    override fun getAppPrefs(packageName: String): LiveData<AppPrefs> {
        return getLiveAppPrefs(packageName).distinctUntilChanged().map { data ->
            data ?: AppPrefs(packageName)
        }
    }

    @Query("SELECT * FROM ${AppPrefs.TABLE} WHERE packageName = :packageName")
    fun getLiveAppPrefs(packageName: String): LiveData<AppPrefs?>

    @Query("SELECT * FROM ${AppPrefs.TABLE} WHERE packageName = :packageName")
    fun getAppPrefsOrNull(packageName: String): AppPrefs?

    fun getPreferredRepos(packageNames: List<String>): Map<String, Long> {
        return if (packageNames.size <= 999) getPreferredReposInternal(packageNames)
        else HashMap<String, Long>(packageNames.size).also { map ->
            packageNames.chunked(999).forEach { map.putAll(getPreferredReposInternal(it)) }
        }
    }

    /**
     * Use [getPreferredRepos] instead as this handles more than 1000 package names.
     */
    @MapInfo(keyColumn = "packageName", valueColumn = "preferredRepoId")
    @Query(
        """SELECT packageName, preferredRepoId FROM PreferredRepo 
             WHERE packageName IN (:packageNames)"""
    )
    fun getPreferredReposInternal(packageNames: List<String>): Map<String, Long>

    @Insert(onConflict = REPLACE)
    override fun update(appPrefs: AppPrefs)
}

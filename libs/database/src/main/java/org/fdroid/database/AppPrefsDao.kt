package org.fdroid.database

import android.os.Build.VERSION.SDK_INT
import androidx.lifecycle.LiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.MapColumn
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
        // since sqlite 3.32.0 the max variables number was increased to 32766
        return if (packageNames.size <= 999 || SDK_INT >= 31) {
            getPreferredReposInternal(packageNames)
        } else {
            HashMap<String, Long>(packageNames.size).also { map ->
                packageNames.chunked(999).forEach { map.putAll(getPreferredReposInternal(it)) }
            }
        }
    }

    /**
     * Use [getPreferredRepos] instead as this handles more than 1000 package names.
     */
    @Query(
        """SELECT packageName, preferredRepoId FROM PreferredRepo 
             WHERE packageName IN (:packageNames)"""
    )
    fun getPreferredReposInternal(
        packageNames: List<String>,
    ): Map<
        @MapColumn("packageName")
        String,
        @MapColumn("preferredRepoId")
        Long
        >

    @Insert(onConflict = REPLACE)
    override fun update(appPrefs: AppPrefs)

}

package org.fdroid.database

import androidx.lifecycle.LiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.room.Dao
import androidx.room.Insert
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

    @Insert(onConflict = REPLACE)
    override fun update(appPrefs: AppPrefs)
}

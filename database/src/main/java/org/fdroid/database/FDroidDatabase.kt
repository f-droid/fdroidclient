package org.fdroid.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [
    // repo
    CoreRepository::class,
    Mirror::class,
    AntiFeature::class,
    Category::class,
    ReleaseChannel::class,
    RepositoryPreferences::class,
    // packages
    AppMetadata::class,
    LocalizedFile::class,
    LocalizedFileList::class,
    // versions
    Version::class,
    VersionedString::class,
], views = [
    LocalizedIcon::class,
], version = 1)
@TypeConverters(Converters::class)
internal abstract class FDroidDatabaseInt internal constructor() : RoomDatabase(), FDroidDatabase {
    abstract override fun getRepositoryDao(): RepositoryDaoInt
    abstract override fun getAppDao(): AppDaoInt
    abstract fun getVersionDaoInt(): VersionDaoInt
}

public interface FDroidDatabase {
    fun getRepositoryDao(): RepositoryDao
    fun getAppDao(): AppDao
    fun runInTransaction(body: Runnable)
}

public object FDroidDatabaseHolder {
    // Singleton prevents multiple instances of database opening at the same time.
    @Volatile
    private var INSTANCE: FDroidDatabaseInt? = null

    @JvmStatic
    public fun getDb(context: Context): FDroidDatabase {
        return getDb(context, "test")
    }

    internal fun getDb(context: Context, name: String = "fdroid_db"): FDroidDatabase {
        // if the INSTANCE is not null, then return it,
        // if it is, then create the database
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                FDroidDatabaseInt::class.java,
                name,
            )//.allowMainThreadQueries() // TODO remove before release
                .build()
            INSTANCE = instance
            // return instance
            instance
        }
    }
}

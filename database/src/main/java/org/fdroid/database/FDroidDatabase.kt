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
    // packages
    AppMetadata::class,
    LocalizedFile::class,
    LocalizedFileList::class,
    // versions
    Version::class,
    VersionedString::class,
], version = 1)
@TypeConverters(Converters::class)
internal abstract class FDroidDatabase internal constructor() : RoomDatabase() {
    abstract fun getRepositoryDaoInt(): RepositoryDaoInt
    abstract fun getAppDaoInt(): AppDaoInt
    abstract fun getVersionDaoInt(): VersionDaoInt

    companion object {
        // Singleton prevents multiple instances of database opening at the same time.
        @Volatile
        private var INSTANCE: FDroidDatabase? = null

        fun getDb(context: Context, name: String = "fdroid_db"): FDroidDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FDroidDatabase::class.java,
                    name,
                ).build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }

}

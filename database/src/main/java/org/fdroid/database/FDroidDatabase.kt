package org.fdroid.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Database(
    version = 1, // TODO set version to 1 before release and wipe old schemas
    entities = [
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
    ],
    views = [
        LocalizedIcon::class,
    ],
    autoMigrations = [
        // AutoMigration (from = 1, to = 2) // seems to require Java 11
    ],
)
@TypeConverters(Converters::class)
internal abstract class FDroidDatabaseInt internal constructor() : RoomDatabase(), FDroidDatabase {
    abstract override fun getRepositoryDao(): RepositoryDaoInt
    abstract override fun getAppDao(): AppDaoInt
    abstract override fun getVersionDao(): VersionDaoInt
}

public interface FDroidDatabase {
    public fun getRepositoryDao(): RepositoryDao
    public fun getAppDao(): AppDao
    public fun getVersionDao(): VersionDao
    public fun runInTransaction(body: Runnable)
}

public fun interface FDroidFixture {
    public fun prePopulateDb(db: FDroidDatabase)
}

public object FDroidDatabaseHolder {
    // Singleton prevents multiple instances of database opening at the same time.
    @Volatile
    private var INSTANCE: FDroidDatabaseInt? = null

    internal val TAG = FDroidDatabase::class.simpleName
    internal val dispatcher get() = Dispatchers.IO

    @JvmStatic
    public fun getDb(context: Context, fixture: FDroidFixture?): FDroidDatabase {
        return getDb(context, "test", fixture)
    }

    internal fun getDb(
        context: Context,
        name: String = "fdroid_db",
        fixture: FDroidFixture? = null,
    ): FDroidDatabase {
        // if the INSTANCE is not null, then return it,
        // if it is, then create the database
        return INSTANCE ?: synchronized(this) {
            val builder = Room.databaseBuilder(
                context.applicationContext,
                FDroidDatabaseInt::class.java,
                name,
            ).fallbackToDestructiveMigration()
            // .allowMainThreadQueries() // TODO remove before release
            if (fixture != null) builder.addCallback(FixtureCallback(fixture))
            val instance = builder.build()
            INSTANCE = instance
            // return instance
            instance
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private class FixtureCallback(private val fixture: FDroidFixture) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            GlobalScope.launch(dispatcher) {
                synchronized(this) {
                    val database = INSTANCE ?: error("DB not yet initialized")
                    fixture.prePopulateDb(database)
                    Log.d(TAG, "Loaded fixtures")
                }
            }
        }

        // TODO remove before release
        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
            onCreate(db)
        }
    }

}

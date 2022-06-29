package org.fdroid.database

import android.content.Context
import android.content.res.Resources
import android.util.Log
import androidx.core.os.ConfigurationCompat.getLocales
import androidx.core.os.LocaleListCompat
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.fdroid.LocaleChooser.getBestLocale

@Database(
    version = 9, // TODO set version to 1 before release and wipe old schemas
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
        AppMetadataFts::class,
        LocalizedFile::class,
        LocalizedFileList::class,
        // versions
        Version::class,
        VersionedString::class,
        // app user preferences
        AppPrefs::class,
    ],
    views = [
        LocalizedIcon::class,
        HighestVersion::class,
    ],
    exportSchema = true,
    autoMigrations = [
        // TODO remove auto-migrations
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 1, to = 3),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9),
    ],
)
@TypeConverters(Converters::class)
internal abstract class FDroidDatabaseInt internal constructor() : RoomDatabase(), FDroidDatabase {
    abstract override fun getRepositoryDao(): RepositoryDaoInt
    abstract override fun getAppDao(): AppDaoInt
    abstract override fun getVersionDao(): VersionDaoInt
    abstract override fun getAppPrefsDao(): AppPrefsDaoInt
    fun afterUpdatingRepo(repoId: Long) {
        getAppDao().updateCompatibility(repoId)
    }
}

public interface FDroidDatabase {
    public fun getRepositoryDao(): RepositoryDao
    public fun getAppDao(): AppDao
    public fun getVersionDao(): VersionDao
    public fun getAppPrefsDao(): AppPrefsDao
    public fun afterLocalesChanged(
        locales: LocaleListCompat = getLocales(Resources.getSystem().configuration),
    ) {
        val appDao = getAppDao() as AppDaoInt
        runInTransaction {
            appDao.getAppMetadata().forEach { appMetadata ->
                appDao.updateAppMetadata(
                    repoId = appMetadata.repoId,
                    packageId = appMetadata.packageId,
                    name = appMetadata.name.getBestLocale(locales),
                    summary = appMetadata.summary.getBestLocale(locales),
                )
            }
        }
    }

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
            ).fallbackToDestructiveMigration() // TODO remove before release
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

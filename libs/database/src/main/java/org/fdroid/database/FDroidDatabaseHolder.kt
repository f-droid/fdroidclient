package org.fdroid.database

import android.content.Context
import android.util.Log
import androidx.annotation.GuardedBy
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * A way to pre-populate the database with a fixture.
 * This can be supplied to [FDroidDatabaseHolder.getDb]
 * and will then be called when a new database is created.
 */
public fun interface FDroidFixture {
    /**
     * Called when a new database gets created.
     * Multiple DB operations should use [FDroidDatabase.runInTransaction].
     */
    public fun prePopulateDb(db: FDroidDatabase)
}

/**
 * A database holder using a singleton pattern to ensure
 * that only one database is open at the same time.
 */
public object FDroidDatabaseHolder {
    // Singleton prevents multiple instances of database opening at the same time.
    @Volatile
    @GuardedBy("lock")
    private var INSTANCE: FDroidDatabaseInt? = null
    private val lock = Object()

    internal val TAG = FDroidDatabase::class.simpleName
    internal val dispatcher get() = Dispatchers.IO

    /**
     * Give you an existing instance of [FDroidDatabase] or creates/opens a new one if none exists.
     * Note: The given [name] is only used when calling this for the first time.
     * Subsequent calls with a different name will return the instance created by the first call.
     */
    @JvmStatic
    @JvmOverloads
    public fun getDb(
        context: Context,
        name: String = "fdroid_db",
        fixture: FDroidFixture? = null,
    ): FDroidDatabase {
        // if the INSTANCE is not null, then return it,
        // if it is, then create the database
        return INSTANCE ?: synchronized(lock) {
            val builder = Room.databaseBuilder(
                context.applicationContext,
                FDroidDatabaseInt::class.java,
                name,
            ).apply {
                // We allow destructive migration (if no real migration was provided),
                // so we have the option to nuke the DB in production (if that will ever be needed).
                fallbackToDestructiveMigration()
                // Add our [FixtureCallback] if a fixture was provided
                if (fixture != null) addCallback(FixtureCallback(fixture))
            }
            val instance = builder.build()
            INSTANCE = instance
            // return instance
            instance
        }
    }

    private class FixtureCallback(private val fixture: FDroidFixture) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            @OptIn(DelicateCoroutinesApi::class)
            GlobalScope.launch(dispatcher) {
                val database: FDroidDatabase
                synchronized(lock) {
                    database = INSTANCE ?: error("DB not yet initialized")
                }
                fixture.prePopulateDb(database)
                Log.d(TAG, "Loaded fixtures")
            }
        }

        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
            onCreate(db)
        }
    }

}

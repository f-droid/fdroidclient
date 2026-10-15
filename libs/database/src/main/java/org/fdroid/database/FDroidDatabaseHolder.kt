package org.fdroid.database

import android.content.Context
import android.util.Log
import androidx.annotation.GuardedBy
import androidx.room.Room
import androidx.room.withTransaction
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * A way to pre-populate the database with a fixture. This can be supplied to
 * [FDroidDatabaseHolder.getDb] and will then be called when a new database is created.
 */
public fun interface FDroidFixture {
  /** Called when a new database gets created. This gets run inside a transaction. */
  public fun prePopulateDb(db: FDroidDatabase)
}

/**
 * A database holder using a singleton pattern to ensure that only one database is open at the same
 * time.
 */
public object FDroidDatabaseHolder {
  // Singleton prevents multiple instances of database opening at the same time.
  @Volatile @GuardedBy("lock") private var instance: FDroidDatabaseInt? = null
  private val lock = Any()

  internal val TAG = FDroidDatabase::class.simpleName
  internal val dispatcher
    get() = Dispatchers.IO

  /**
   * Give you an existing instance of [FDroidDatabase] or creates/opens a new one if none exists.
   * Note: The given [name] is only used when calling this for the first time. Subsequent calls with
   * a different name will return the instance created by the first call.
   */
  @JvmStatic
  @JvmOverloads
  public fun getDb(
    context: Context,
    name: String = "fdroid_db",
    fixture: FDroidFixture? = null,
    logSlowQueries: Boolean = false,
  ): FDroidDatabase {
    // if the INSTANCE is not null, then return it,
    // if it is, then create the database
    return instance
      ?: synchronized(lock) {
        val builder =
          Room.databaseBuilder(context.applicationContext, FDroidDatabaseInt::class.java, name)
            .apply {
              addMigrations(MIGRATION_2_3, MIGRATION_5_6, MIGRATION_8_9, MIGRATION_13_14)
              // We allow destructive migration (if no real migration was provided),
              // so we have the option to nuke the DB in production (if that will ever be needed).
              fallbackToDestructiveMigration(false)

              if (logSlowQueries) {
                openHelperFactory(
                  TimingOpenHelperFactory(
                    FrameworkSQLiteOpenHelperFactory(),
                    slowQueryThresholdMs = 500,
                  )
                )
              }
            }
        val db = builder.build()
        runBlocking { runFixtureIfNeeded(db, fixture) }
        instance = db
        db
      }
  }

  /**
   * Inserts the given [fixture] into the DB if it has not been run before. We are using a flag in
   * the [DbMetadata] table set in the same transaction to ensure fixtures get inserted only once
   * when the DB is created.
   *
   * This is an ugly hack allowing us to insert fixtures into the DB while it is being constructed
   * on the UiThread without having to allow general UiThread access to the DB.
   */
  private suspend fun runFixtureIfNeeded(db: FDroidDatabaseInt, fixture: FDroidFixture?) {
    withContext(dispatcher) {
      db.withTransaction {
        val isSetup = db.getDbMetadataDao().get("setup")
        if (isSetup != "true") {
          fixture?.prePopulateDb(db)
          db.getDbMetadataDao().set(DbMetadata("setup", "true"))
          Log.d(TAG, "Finished initial setup")
        } else {
          Log.d(TAG, "DB already set up")
        }
      }
    }
  }
}

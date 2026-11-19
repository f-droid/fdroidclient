package org.fdroid.database

import android.content.Context
import androidx.annotation.GuardedBy
import androidx.room.Room
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import kotlinx.coroutines.Dispatchers

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
              if (fixture != null) {
                addCallback(FDroidFixtureCallback(context, name, fixture))
              }
              // We allow destructive migration (if no real migration was provided),
              // so we have the option to nuke the DB in production (if that will ever be needed).
              fallbackToDestructiveMigration(false)

              if (logSlowQueries) {
                openHelperFactory(
                  TimingOpenHelperFactory(
                    FrameworkSQLiteOpenHelperFactory(),
                    slowQueryThresholdMs = 500,
                  ),
                )
              }
            }
        val db = builder.build()
        instance = db
        db
      }
  }
}

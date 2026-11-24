package org.fdroid.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper

/**
 * A way to pre-populate the database with a fixture. This can be supplied to
 * [FDroidDatabaseHolder.getDb] and will then be called when a new database is created.
 */
public fun interface FDroidFixture {
  /** Called when a new database gets created. This gets run inside a transaction. */
  public fun prePopulateDb(db: FDroidDatabase)
}

internal class FDroidFixtureCallback(
  private val context: Context,
  private val name: String,
  private val fixture: FDroidFixture,
) : RoomDatabase.Callback() {
  override fun onCreate(db: SupportSQLiteDatabase) {
    super.onCreate(db)
    val tempDb =
      Room.databaseBuilder(context.applicationContext, FDroidDatabaseInt::class.java, name)
        .openHelperFactory(ExistingConnectionOpenHelperFactory(db))
        .build()
    fixture.prePopulateDb(tempDb)
  }
}

/**
 * This is a DB factory that re-uses an existing connection without ever closing it. It is used to
 * create an instance of [FDroidDatabaseInt] while it is still being opened, so we can insert
 * fixtures such as the initial repos. Without it, we'd need to write raw SQL queries to insert
 * data.
 */
internal class ExistingConnectionOpenHelperFactory(
  private val existingDb: SupportSQLiteDatabase,
) : SupportSQLiteOpenHelper.Factory {
  override fun create(
    configuration: SupportSQLiteOpenHelper.Configuration,
  ): SupportSQLiteOpenHelper {
    return object : SupportSQLiteOpenHelper {
      override val databaseName: String? = existingDb.path
      override val writableDatabase: SupportSQLiteDatabase = existingDb
      override val readableDatabase: SupportSQLiteDatabase = existingDb

      override fun setWriteAheadLoggingEnabled(enabled: Boolean) {}

      override fun close() {
        // Do NOT close existingDb; the primary Room instance owns its lifecycle
      }
    }
  }
}

package org.fdroid.database

import android.database.Cursor
import android.os.CancellationSignal
import android.os.SystemClock
import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteQuery

/**
 * Only use this in debug mode or for local builds, not for release builds.
 *
 * If you are auditing the full app and want to investigate all queries, you can set [logAllQueries]
 * and/or [explainAllQueries] in which case [slowQueryThresholdMs] will be ignored.
 *
 * The typical process is:
 * * Set [logAllQueries] which logs the time each query takes along with the actual query.
 * * If you wish to be more verbose, also add [explainAllQueries].
 * * Otherwise, just set a sensible [slowQueryThresholdMs] (e.g. 2000ms).
 */
internal class TimingOpenHelperFactory(
  private val delegate: SupportSQLiteOpenHelper.Factory,
  private val slowQueryThresholdMs: Long = Long.MAX_VALUE,
  private val logAllQueries: Boolean = false,
  private val explainAllQueries: Boolean = false,
) : SupportSQLiteOpenHelper.Factory {

  override fun create(
    configuration: SupportSQLiteOpenHelper.Configuration
  ): SupportSQLiteOpenHelper {
    val helper = delegate.create(configuration)
    return TimingOpenHelper(helper, slowQueryThresholdMs, logAllQueries, explainAllQueries)
  }
}

internal class TimingOpenHelper(
  private val delegate: SupportSQLiteOpenHelper,
  private val thresholdMs: Long,
  private val logAllQueries: Boolean = false,
  private val explainAllQueries: Boolean = false,
) : SupportSQLiteOpenHelper by delegate {

  override val writableDatabase: SupportSQLiteDatabase
    get() = TimingDatabase(delegate.writableDatabase, thresholdMs, logAllQueries, explainAllQueries)

  override val readableDatabase: SupportSQLiteDatabase
    get() = TimingDatabase(delegate.readableDatabase, thresholdMs, logAllQueries, explainAllQueries)
}

internal class TimingDatabase(
  private val delegate: SupportSQLiteDatabase,
  private val thresholdMs: Long,
  private val logAllQueries: Boolean = false,
  private val explainAllQueries: Boolean = false,
) : SupportSQLiteDatabase by delegate {

  private fun logQueryIfRequired(query: String, closure: () -> Cursor): Cursor {
    val start = SystemClock.elapsedRealtime()
    val cursor = closure()
    val count = cursor.count
    println(count)
    val duration = SystemClock.elapsedRealtime() - start
    if (duration > thresholdMs) {
      logSlowQuery(query, duration)
      explainQueryPlan(query)
    } else {
      if (logAllQueries) {
        logSlowQuery(query, duration)
      } else {
        Log.d(
          "TimingDatabase",
          "Query took $duration but threshold for logging is $thresholdMs. Skipping.",
        )
      }

      if (explainAllQueries) {
        explainQueryPlan(query)
      }
    }

    return cursor
  }

  override fun query(query: String, bindArgs: Array<out Any?>): Cursor =
    logQueryIfRequired(query) { delegate.query(query, bindArgs) }

  override fun query(query: SupportSQLiteQuery): Cursor =
    logQueryIfRequired(query.sql) { delegate.query(query) }

  override fun query(query: SupportSQLiteQuery, cancellationSignal: CancellationSignal?): Cursor =
    logQueryIfRequired(query.sql) { delegate.query(query, cancellationSignal) }

  override fun query(query: String): Cursor = logQueryIfRequired(query) { delegate.query(query) }

  private fun logSlowQuery(sql: String, duration: Long) {
    Log.w("TimingDatabase", "Slow query (${duration}ms): $sql")
  }

  private fun explainQueryPlan(sql: String) {
    val explainCursor = delegate.query("EXPLAIN QUERY PLAN $sql")
    explainCursor.use {
      while (it.moveToNext()) {
        Log.w(
          "TimingDatabase",

          // Technically according to the docs for sqlite, we shouldn't rely on the structure
          // of the data returned from EXPLAIN QUERY PLAN. However in practice, this is only
          // used in dev, and the structure doesn't change. If it does, we just change this code.
          "Plan: ${it.getString(3)}",
        )
      }
    }
  }
}

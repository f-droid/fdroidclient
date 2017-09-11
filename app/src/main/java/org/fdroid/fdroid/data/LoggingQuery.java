package org.fdroid.fdroid.data;

import android.database.Cursor;
import android.database.CursorWrapper;
import android.database.sqlite.SQLiteDatabase;

import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.Utils;

/**
 * Helper class to log slow queries to logcat when in debug mode. When not in debug mode, it
 * runs the queries without any logging.
 *
 * Here is an example of what would be output to logcat for a query that takes too long (except the
 * query would not be formatted as nicely):
 *
 *   Query [155ms]:
 *     SELECT fdroid_app.rowid as _id, ...
 *     FROM fdroid_app
 *       LEFT JOIN fdroid_apk ON (fdroid_apk.appId = fdroid_app.rowid)
 *       LEFT JOIN fdroid_repo ON (fdroid_apk.repo = fdroid_repo._id)
 *       LEFT JOIN fdroid_installedApp AS installed ON (installed.appId = fdroid_app.id)
 *       LEFT JOIN fdroid_apk AS suggestedApk ON (fdroid_app.suggestedVercode = suggestedApk.vercode
 *                                                AND fdroid_app.rowid = suggestedApk.appId)
 *     WHERE
 *       fdroid_repo.isSwap = 0 OR fdroid_repo.isSwap IS NULL
 *     GROUP BY fdroid_app.rowid
 *     ORDER BY fdroid_app.name COLLATE LOCALIZED
 *   Explain:
 *     SCAN TABLE fdroid_app
 *     SEARCH TABLE fdroid_apk USING COVERING INDEX sqlite_autoindex_fdroid_apk_1 (appId=?)
 *     SEARCH TABLE fdroid_repo USING INTEGER PRIMARY KEY (rowid=?)
 *     SEARCH TABLE fdroid_installedApp AS installed USING INDEX sqlite_autoindex_fdroid_installedApp_1 (appId=?)
 *     SEARCH TABLE fdroid_apk AS suggestedApk USING INDEX sqlite_autoindex_fdroid_apk_1 (appId=? AND vercode=?)
 *     USE TEMP B-TREE FOR ORDER BY
 */
final class LoggingQuery {

    private static final long SLOW_QUERY_DURATION = 100;
    private static final String TAG = "Slow Query";

    private final SQLiteDatabase db;
    private final String query;
    private final String[] queryArgs;

    private LoggingQuery(SQLiteDatabase db, String query, String[] queryArgs) {
        this.db = db;
        this.query = query;
        this.queryArgs = queryArgs;
    }

    /**
     * When running a debug build, this will log details (including query plans) for any query which
     * takes longer than {@link LoggingQuery#SLOW_QUERY_DURATION}.
     */
    private Cursor rawQuery() {
        if (BuildConfig.DEBUG) {
            long startTime = System.currentTimeMillis();
            Cursor cursor = db.rawQuery(query, queryArgs);
            long queryDuration = System.currentTimeMillis() - startTime;

            if (queryDuration >= SLOW_QUERY_DURATION) {
                logSlowQuery(queryDuration);
            }

            return new LogGetCountCursorWrapper(cursor);
        }
        return db.rawQuery(query, queryArgs);
    }

    /**
     * Sometimes the query will not actually be run when invoking "query()".
     * Under such circumstances, it falls to the {@link android.content.ContentProvider#query}
     * method to manually invoke the {@link Cursor#getCount()} method to force query execution.
     * It does so with a comment saying "Force query execution". When this happens, the call to
     * query() takes 1ms, whereas the call go getCount() is the bit which takes time.
     * As such, we will also track that method duration in order to potentially log slow queries.
     */
    private final class LogGetCountCursorWrapper extends CursorWrapper {
        private LogGetCountCursorWrapper(Cursor cursor) {
            super(cursor);
        }

        @Override
        public int getCount() {
            long startTime = System.currentTimeMillis();
            int count = super.getCount();
            long queryDuration = System.currentTimeMillis() - startTime;
            if (queryDuration >= SLOW_QUERY_DURATION) {
                logSlowQuery(queryDuration);
            }
            return count;
        }
    }

    private void execSQLInternal() {
        if (BuildConfig.DEBUG) {
            long startTime = System.currentTimeMillis();
            long queryDuration = System.currentTimeMillis() - startTime;
            executeSQLInternal();
            if (queryDuration >= SLOW_QUERY_DURATION) {
                logSlowQuery(queryDuration);
            }
        } else {
            executeSQLInternal();
        }
    }

    private void executeSQLInternal() {
        if (queryArgs == null || queryArgs.length == 0) {
            db.execSQL(query);
        } else {
            db.execSQL(query, queryArgs);
        }
    }

    /**
     * Log the query and its duration to the console. In addition, execute an "EXPLAIN QUERY PLAN"
     * for the query in question so that the query can be diagnosed (https://sqlite.org/eqp.html)
     */
    private void logSlowQuery(long queryDuration) {
        StringBuilder sb = new StringBuilder();
        sb.append("Query [")
                .append(queryDuration)
                .append("ms]: ")
                .append(query);

        try {
            StringBuilder sbExplain = new StringBuilder("\nExplain:\n");
            for (String plan : getExplainQueryPlan()) {
                sbExplain.append("  ").append(plan).append("\n");
            }
            sb.append(sbExplain);
        } catch (Exception e) {
            // Ignore exception, we caught this because the SQLite docs say explain query plan can
            // change between versions. We do our best in getExplainQueryPlan() to mitigate this,
            // but it may still break with newer releases. In that case, at least we will still be
            // logging the slow query to logcat which is helpful.
        }

        Utils.debugLog(TAG, sb.toString());
    }

    private String[] getExplainQueryPlan() {
        Cursor cursor = db.rawQuery("EXPLAIN QUERY PLAN " + query, queryArgs);
        String[] plan = new String[cursor.getCount()];
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            // The docs at https://sqlite.org/eqp.html talk about how the output format of
            // EXPLAIN QUERY PLAN can change between SQLite versions. This has been observed
            // between the sqlite versions on Android 2.3.3 and Android 5.0. However, it seems
            // that the last column is always the one with the interesting details that we wish
            // to log. If this fails for some reason, then hey, it is only for debug builds, right?
            if (cursor.getColumnCount() > 0) {
                int index = cursor.getColumnCount() - 1;
                plan[cursor.getPosition()] = cursor.getString(index);
            }
            cursor.moveToNext();
        }
        cursor.close();
        return plan;
    }

    public static Cursor query(SQLiteDatabase db, String query, String[] queryBuilderArgs) {
        return new LoggingQuery(db, query, queryBuilderArgs).rawQuery();
    }

    public static void execSQL(SQLiteDatabase db, String sql, String[] queryArgs) {
        new LoggingQuery(db, sql, queryArgs).execSQLInternal();
    }
}

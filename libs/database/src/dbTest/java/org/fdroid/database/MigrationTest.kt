package org.fdroid.database

import android.database.Cursor
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
internal class MigrationTest {
    private val TEST_DB = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        FDroidDatabaseInt::class.java
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2() {
        // use sqlite to query because daos are tied to the latest db schema
        var db = helper.createDatabase(TEST_DB, 1)
        val v1Cursor: Cursor = db.query(
            "SELECT COUNT(*) FROM sqlite_master WHERE type = ? AND name = ?",
            arrayOf<String>("table", DnsCacheDb.TABLE)
        )
        v1Cursor.moveToFirst()
        val v1Result: String = v1Cursor.getString(v1Cursor.getColumnIndex("COUNT(*)"))
        val v1Count: Int = Integer.valueOf(v1Result).toInt()

        // table should not exist in v1, count should be 0
        assertEquals(0, v1Count)
        v1Cursor.close()

        // reopen the database with v2 schema, migration is automatic
        db = helper.runMigrationsAndValidate(TEST_DB, 2, true)
        val v2Cursor: Cursor = db.query(
            "SELECT COUNT(*) FROM sqlite_master WHERE type = ? AND name = ?",
            arrayOf<String>("table", DnsCacheDb.TABLE)
        )
        v2Cursor.moveToFirst()
        val v2Result: String = v2Cursor.getString(v2Cursor.getColumnIndex("COUNT(*)"))
        val v2Count: Int = Integer.valueOf(v2Result).toInt()

        // table should exist in v2, count should be 1
        assertEquals(1, v2Count)
        v2Cursor.close()
    }
}

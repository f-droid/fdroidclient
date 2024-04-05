package org.fdroid.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import org.fdroid.database.Converters.localizedTextV2toString
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

private const val TEST_DB = "migration-test"

@RunWith(AndroidJUnit4::class)
internal class RepoCertNonNullMigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        instrumentation = getInstrumentation(),
        databaseClass = FDroidDatabaseInt::class.java,
        specs = emptyList(),
        openFactory = FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrateRepos() {
        helper.createDatabase(TEST_DB, 2).use { db ->
            // Database has schema version 2. Insert some data using SQL queries.
            // We can't use DAO classes because they expect the latest schema.
            val repoId1 = db.insert(
                CoreRepository.TABLE,
                SQLiteDatabase.CONFLICT_FAIL,
                ContentValues().apply {
                    put("name", localizedTextV2toString(mapOf("en-US" to "foo")))
                    put("description", localizedTextV2toString(mapOf("en-US" to "bar")))
                    put("address", "https://example.org/repo")
                    put("certificate", "0123")
                    put("timestamp", -1)
                })
            db.insert(
                RepositoryPreferences.TABLE,
                SQLiteDatabase.CONFLICT_FAIL,
                ContentValues().apply {
                    put("repoId", repoId1)
                    put("enabled", true)
                    put("weight", Long.MAX_VALUE)
                })
            val repoId2 = db.insert(
                CoreRepository.TABLE,
                SQLiteDatabase.CONFLICT_FAIL,
                ContentValues().apply {
                    put("name", localizedTextV2toString(mapOf("en-US" to "no cert")))
                    put("description", localizedTextV2toString(mapOf("en-US" to "no cert desc")))
                    put("address", "https://example.com/repo")
                    put("timestamp", -1)
                })
            db.insert(
                RepositoryPreferences.TABLE,
                SQLiteDatabase.CONFLICT_FAIL,
                ContentValues().apply {
                    put("repoId", repoId2)
                    put("enabled", true)
                    put("weight", Long.MAX_VALUE - 2)
                })
        }

        // Re-open the database with version 2, auto-migrations are applied automatically
        helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_2_3).close()

        // now get the Room DB, so we can use our DAOs for verifying the migration
        Room.databaseBuilder(getApplicationContext(), FDroidDatabaseInt::class.java, TEST_DB)
            .addMigrations(MIGRATION_2_3)
            .allowMainThreadQueries()
            .build().use { db ->
                // repo without cert did not get migrated, the other one did
                assertEquals(1, db.getRepositoryDao().getRepositories().size)
                val repo = db.getRepositoryDao().getRepositories()[0]
                assertEquals("https://example.org/repo", repo.address)
            }
    }

}

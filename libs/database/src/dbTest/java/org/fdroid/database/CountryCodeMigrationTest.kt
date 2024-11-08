package org.fdroid.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_DB = "migration-test"

@RunWith(AndroidJUnit4::class)
internal class CountryCodeMigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        FDroidDatabaseInt::class.java,
        listOf(CountryCodeMigration()),
        FrameworkSQLiteOpenHelperFactory(),
    )

    private val repo = InitialRepository(
        name = "F-Droid",
        address = "https://f-droid.org/repo",
        description = "The official F-Droid Free Software repository. " +
            "Everything in this repository is always built from the source code.",
        certificate = "3082035e30820246a00302010202044c49cd00300d06092a864886f70d010105",
        version = 13L,
        enabled = true,
        weight = 1,
    )

    @Test
    fun migrateCountryCode() {
        helper.createDatabase(TEST_DB, 7).use { db ->
            // Database has schema version 7. Insert some data using SQL queries.
            // We can't use DAO classes because they expect the latest schema.
            val repoId = db.insert(
                CoreRepository.TABLE,
                SQLiteDatabase.CONFLICT_FAIL, ContentValues().apply {
                    put("name", Converters.localizedTextV2toString(mapOf("en-US" to repo.name)))
                    put(
                        "description",
                        Converters.localizedTextV2toString(mapOf("en-US" to repo.description))
                    )
                    put("address", repo.address)
                    put("timestamp", 42)
                    put("certificate", repo.certificate)
                })
            db.insert(
                RepositoryPreferences.TABLE,
                SQLiteDatabase.CONFLICT_FAIL, ContentValues().apply {
                    put("repoId", repoId)
                    put("enabled", repo.enabled)
                    put("lastETag", "foo")
                    put("weight", repo.weight)
                })
            db.insert(
                Mirror.TABLE,
                SQLiteDatabase.CONFLICT_FAIL, ContentValues().apply {
                    put("repoId", repoId)
                    put("url", "foo")
                    put("location", "bar")
                })
        }

        // Re-open the database with version 8, auto-migrations are applied automatically
        helper.runMigrationsAndValidate(TEST_DB, 8, true).close()

        // now get the Room DB, so we can use our DAOs for verifying the migration
        Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FDroidDatabaseInt::class.java,
            TEST_DB
        )
            .addMigrations(MIGRATION_2_3, MIGRATION_5_6)
            .allowMainThreadQueries()
            .build().use { db ->
                // check repo got timestamp and etag reset
                val repos = db.getRepositoryDao().getRepositories()
                assertEquals(1, repos.size)
                assertEquals(null, repos[0].lastETag)
                assertEquals(-1, repos[0].timestamp)
                // check mirror
                assertEquals(1, repos[0].mirrors.size)
                val mirror = repos[0].mirrors[0]
                assertEquals(repos[0].repoId, mirror.repoId)
                assertEquals("foo", mirror.url)
                assertEquals("bar", mirror.countryCode)
            }
    }
}

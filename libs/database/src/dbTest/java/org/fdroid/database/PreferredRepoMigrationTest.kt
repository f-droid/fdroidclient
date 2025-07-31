package org.fdroid.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.fdroid.database.Converters.localizedTextV2toString
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random
import kotlin.test.assertEquals

private const val TEST_DB = "migration-test"

@RunWith(AndroidJUnit4::class)
internal class PreferredRepoMigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        databaseClass = FDroidDatabaseInt::class.java,
        specs = emptyList(),
        openFactory = FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrateRepos() {
        helper.createDatabase(TEST_DB, 6).use { db ->
            // Database has schema version 6. Insert some data using SQL queries.
            // We can't use DAO classes because they expect the latest schema.
            val repoId = db.insert(
                CoreRepository.TABLE,
                SQLiteDatabase.CONFLICT_FAIL,
                ContentValues().apply {
                    put("name", localizedTextV2toString(mapOf("en-US" to "foo")))
                    put("description", localizedTextV2toString(mapOf("en-US" to "bar")))
                    put("address", "https://example.org/repo")
                    put("certificate", "0123")
                    put("timestamp", -1)
                },
            )
            db.insert(
                RepositoryPreferences.TABLE,
                SQLiteDatabase.CONFLICT_FAIL,
                ContentValues().apply {
                    put("repoId", repoId)
                    put("enabled", true)
                    put("weight", Long.MAX_VALUE)
                },
            )
            val oeffiMetadata = ContentValues().apply {
                put("packageName", "de.schildbach.oeffi")
                put("repoId", repoId)
                put("localizedName", "Öffi")
                put("localizedSummary", "Der König des Fahrplandschungels!")
                put("added", Random.nextLong())
                put("lastUpdated", Random.nextLong())
                put("isCompatible", true)
            }
            db.insert(AppMetadata.TABLE, SQLiteDatabase.CONFLICT_FAIL, oeffiMetadata)
            val oeffiPrefs = ContentValues().apply {
                put("packageName", "de.schildbach.oeffi")
                put("ignoreVersionCodeUpdate", 0)
                put("preferredRepoId", 42) // repo doesn't exist
            }
            db.insert(AppPrefs.TABLE, SQLiteDatabase.CONFLICT_FAIL, oeffiPrefs)
        }

        // Re-open the database with version 2, auto-migrations are applied automatically
        helper.runMigrationsAndValidate(TEST_DB, 7, true).close()

        // now get the Room DB, so we can use our DAOs for verifying the migration
        Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FDroidDatabaseInt::class.java,
            TEST_DB
        )
            .addMigrations(MIGRATION_2_3, MIGRATION_5_6, MIGRATION_8_9)
            .allowMainThreadQueries()
            .build().use { db ->
                // repo without cert did not get migrated, the other one did
                assertEquals(1, db.getRepositoryDao().getRepositories().size)
                val repo = db.getRepositoryDao().getRepositories()[0]
                assertEquals("https://example.org/repo", repo.address)

                db.getAppPrefsDao().getPreferredRepos(listOf("de.schildbach.oeffi")).let {
                    assertEquals(1, it.size)
                    // now correct PreferredRepo gets returned
                    assertEquals(repo.repoId, it["de.schildbach.oeffi"])
                }
            }
    }

}

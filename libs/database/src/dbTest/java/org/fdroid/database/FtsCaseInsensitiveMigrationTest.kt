package org.fdroid.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.fdroid.database.TestUtils.getOrFail
import org.fdroid.test.TestUtils.getRandomString
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random
import kotlin.test.assertEquals

private const val TEST_DB = "fts-test"

@RunWith(AndroidJUnit4::class)
internal class FtsCaseInsensitiveMigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        databaseClass = FDroidDatabaseInt::class.java,
        specs = emptyList(),
        openFactory = FrameworkSQLiteOpenHelperFactory(),
    )

    @get:Rule
    val instantExec = InstantTaskExecutorRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val repo = ContentValues().apply {
        put("repoId", 1)
        put("name", Converters.localizedTextV2toString(mapOf("de" to "a", "en-US" to "b")))
        put("address", getRandomString())
        put("certificate", "abcdef")
        put("description", Converters.localizedTextV2toString(mapOf("de" to "aa", "en-US" to "bb")))
        put("version", Random.nextLong())
        put("timestamp", Random.nextLong())
    }

    private val repoPrefs = ContentValues().apply {
        put("repoId", 1)
        put("enabled", true)
        put("weight", 1)
    }

    private val oeffiMetadata = ContentValues().apply {
        put("packageName", "de.schildbach.oeffi")
        put("repoId", 1)
        put(
            "name", Converters.localizedTextV2toString(
                mapOf(
                    "de" to "Öffi", "en-US" to "Offi"
                )
            )
        )
        put(
            "description", Converters.localizedTextV2toString(
                mapOf(
                    "de" to "Öffentlicher Nahverkehr", "en-US" to "Public Transport"
                )
            )
        )
        put("license", "GPL-3.0")
        put(
            "summary", Converters.localizedTextV2toString(
                mapOf(
                    "de" to "Der König des Fahrplandschungels!",
                    "en-US" to " King of public transit planning!"
                )
            )
        )
        put("localizedName", "Öffi")
        put("localizedSummary", "Der König des Fahrplandschungels!")
        put("added", Random.nextLong())
        put("lastUpdated", Random.nextLong())
        put("isCompatible", true)
    }

    private val oeffiVersion = ContentValues().apply {
        put("repoId", 1)
        put("packageName", "de.schildbach.oeffi")
        put("versionId", 1)
        put("added", 1000)
        put("isCompatible", true)
        put("file_name", "transportr.apk")
        put("file_sha256", "73475CB40A568E8DA8A045CED110137E159F890AC4DA883B6B17DC651B3A8049")
        put("manifest_versionName", "1.0.0")
        put("manifest_versionCode", 1)
    }

    private val transportrMetadata = ContentValues().apply {
        put("packageName", "de.grobox.liberario")
        put("repoId", 1)
        put(
            "name", Converters.localizedTextV2toString(
                mapOf(
                    "de" to "Transportr", "en-US" to "Transportr"
                )
            )
        )
        put(
            "description", Converters.localizedTextV2toString(
                mapOf(
                    "de" to "Öffentlicher Nahverkehr", "en-US" to "Public Transport"
                )
            )
        )
        put("license", "GPL-3.0")
        put(
            "summary", Converters.localizedTextV2toString(
                mapOf(
                    "de" to "Freier Assistent für den öffentlichen Nahverkehr ohne Werbung",
                    "en-US" to "Free Public Transport Assistant without Ads or Tracking"
                )
            )
        )
        put("localizedName", "Transportr")
        put(
            "localizedSummary",
            "Freier Assistent für den öffentlichen Nahverkehr ohne Werbung und Tracking"
        )
        put("added", Random.nextLong())
        put("lastUpdated", Random.nextLong())
        put("isCompatible", true)
    }

    private val transportrVersion = ContentValues().apply {
        put("repoId", 1)
        put("packageName", "de.grobox.liberario")
        put("versionId", 1)
        put("added", 1000)
        put("isCompatible", true)
        put("file_name", "transportr.apk")
        put("file_sha256", "73475CB40A568E8DA8A045CED110137E159F890AC4DA883B6B17DC651B3A8049")
        put("manifest_versionName", "1.0.0")
        put("manifest_versionCode", 1)
    }

    @Test
    fun testMigration() = runBlocking {
        helper.createDatabase(TEST_DB, 5).use { db ->
            // Database has schema version 5. Insert some data using SQL queries.
            // We can't use DAO classes because they expect the latest schema.
            db.insert(CoreRepository.TABLE, SQLiteDatabase.CONFLICT_FAIL, repo)
            db.insert(RepositoryPreferences.TABLE, SQLiteDatabase.CONFLICT_FAIL, repoPrefs)
            db.insert(AppMetadata.TABLE, SQLiteDatabase.CONFLICT_FAIL, oeffiMetadata)
            db.insert(AppMetadata.TABLE, SQLiteDatabase.CONFLICT_FAIL, transportrMetadata)
            db.insert(Version.TABLE, SQLiteDatabase.CONFLICT_FAIL, oeffiVersion)
            db.insert(Version.TABLE, SQLiteDatabase.CONFLICT_FAIL, transportrVersion)

            // Show that search is case sensitive for diacritics
            assertSearch(db, "Öffi", 1)
            assertSearch(db, "öffi", 0)
            // using no diacritics does match any case
            assertSearch(db, "Offi", 0)
            assertSearch(db, "offi", 0)
            // it's case sensitive so only "öffentlichen" from Transportr is found
            assertSearch(db, "öff*", 1)
            assertSearch(db, "König", 1)

        }

        helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6).close()

        // now get the Room DB, so we can use our DAOs for verifying the migration
        Room.databaseBuilder(context, FDroidDatabaseInt::class.java, TEST_DB)
            .allowMainThreadQueries()
            .addMigrations(MIGRATION_8_9) // was added later
            .build().use { db ->
                // assert that apps are still there
                val metadata = db.getAppDao().getAppMetadata()
                assertEquals(2, metadata.size)
                // default search with no diacritics
                assertGetAppListItems(db, "*Transport*", 2)

                // other tests here were removed, because MIGRATION_8_9 changed this once again
                // and has its own test
            }
    }

    private fun assertSearch(db: SupportSQLiteDatabase, query: String, expected: Int) {
        db.query("SELECT * FROM AppMetadataFts WHERE AppMetadataFts MATCH '$query'")
            .use { cursor -> assertEquals(expected, cursor.count) }
    }

    private fun assertGetAppListItems(db: FDroidDatabaseInt, query: String, expected: Int) {
        db.getAppDao().getAppListItems(query).getOrFail().let {
            assertEquals(expected, it.size)
        }
    }

}

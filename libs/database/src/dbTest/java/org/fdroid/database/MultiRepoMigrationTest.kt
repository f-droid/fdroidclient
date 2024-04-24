package org.fdroid.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase.CONFLICT_FAIL
import androidx.room.Room.databaseBuilder
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.fdroid.database.Converters.localizedTextV2toString
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.fail

private const val TEST_DB = "migration-test"

@RunWith(AndroidJUnit4::class)
internal class MultiRepoMigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        FDroidDatabaseInt::class.java,
        listOf(MultiRepoMigration()),
        FrameworkSQLiteOpenHelperFactory(),
    )

    private val fdroidArchiveRepo = InitialRepository(
        name = "F-Droid Archive",
        address = "https://f-droid.org/archive",
        description = "The archive repository of the F-Droid client. " +
            "This contains older versions of\n" +
            "applications from the main repository.",
        certificate = "3082035e30820246a00302010202044c49cd00300d06092a864886f70d010105",
        version = 13L,
        enabled = false,
        weight = 0, // gets set later
    )
    private val fdroidRepo = InitialRepository(
        name = "F-Droid",
        address = "https://f-droid.org/repo",
        description = "The official F-Droid Free Software repository. " +
            "Everything in this repository is always built from the source code.",
        certificate = "3082035e30820246a00302010202044c49cd00300d06092a864886f70d010105",
        version = 13L,
        enabled = true,
        weight = 0, // gets set later
    )
    private val guardianArchiveRepo = InitialRepository(
        name = "Guardian Project Archive",
        address = "https://guardianproject.info/fdroid/archive",
        description = "The official repository of The Guardian Project apps" +
            " for use with F-Droid client. This\n" +
            "            contains older versions of applications from the main repository.\n",
        certificate = "308205d8308203c0020900a397b4da7ecda034300d06092a864886f70d010105",
        version = 13L,
        enabled = false,
        weight = 0, // gets set later
    )
    private val guardianRepo = InitialRepository(
        name = "Guardian Project",
        address = "https://guardianproject.info/fdroid/repo",
        description = "The official app repository of The Guardian Project. " +
            "Applications in this repository\n" +
            "            are official binaries build by the original application developers " +
            "and signed by the\n" +
            "            same key as the APKs that are released in the Google Play store.",
        certificate = "308205d8308203c0020900a397b4da7ecda034300d06092a864886f70d010105",
        version = 13L,
        enabled = false,
        weight = 0, // gets set later
    )

    @Test
    fun migrateDefaultRepos() {
        val reposToMigrate = listOf(
            fdroidArchiveRepo.copy(weight = 1),
            fdroidRepo.copy(weight = 2),
        )
        runRepoMigration(reposToMigrate) { db ->
            db.getRepositoryDao().getRepositories().sortedByDescending { it.weight }.also { repos ->
                assertEquals(reposToMigrate.size, repos.size)
                assertEquals(reposToMigrate.size, repos.map { it.weight }.toSet().size)
                assertEquals(fdroidRepo.address, repos[0].address)
                assertEquals(1_000_000_000, repos[0].weight)
                assertEquals(fdroidArchiveRepo.address, repos[1].address)
                assertEquals(999_999_999, repos[1].weight)
            }
        }
    }

    @Test
    fun migrateOldDefaultRepos() {
        val reposToMigrate = listOf(
            fdroidArchiveRepo.copy(weight = 1),
            fdroidRepo.copy(weight = 2),
            guardianArchiveRepo.copy(weight = 3),
            guardianRepo.copy(weight = 4),
        )
        runRepoMigration(reposToMigrate) { db ->
            db.getRepositoryDao().getRepositories().sortedByDescending { it.weight }.also { repos ->
                assertEquals(reposToMigrate.size, repos.size)
                assertEquals(reposToMigrate.size, repos.map { it.weight }.toSet().size)
                assertEquals(guardianRepo.address, repos[0].address)
                assertEquals(1_000_000_000, repos[0].weight)
                assertEquals(guardianArchiveRepo.address, repos[1].address)
                assertEquals(999_999_999, repos[1].weight)
                assertEquals(fdroidRepo.address, repos[2].address)
                assertEquals(999_999_998, repos[2].weight)
                assertEquals(fdroidArchiveRepo.address, repos[3].address)
                assertEquals(999_999_997, repos[3].weight)
            }
        }
    }

    @Test
    fun migrateOldDefaultReposPlusRandomOnes() {
        val reposToMigrate = listOf(
            fdroidArchiveRepo.copy(weight = 1),
            fdroidRepo.copy(weight = 2),
            guardianArchiveRepo.copy(weight = 3),
            guardianRepo.copy(weight = 4),
            InitialRepository(
                name = "Foo bar",
                address = "https://example.org/fdroid/repo",
                description = "foo bar repo",
                certificate = "1234567890",
                version = 0L,
                enabled = true,
                weight = 5,
            ),
            InitialRepository(
                name = "Bla Blub",
                address = "https://example.com/fdroid/repo",
                description = "bla blub repo",
                certificate = "0987654321",
                version = 0L,
                enabled = true,
                weight = 6,
            ),
        )
        runRepoMigration(reposToMigrate) { db ->
            db.getRepositoryDao().getRepositories().sortedByDescending { it.weight }.also { repos ->
                assertEquals(reposToMigrate.size, repos.size)
                assertEquals(reposToMigrate.size, repos.map { it.weight }.toSet().size)
                assertEquals("https://example.com/fdroid/repo", repos[0].address)
                assertEquals(1_000_000_000, repos[0].weight)
                assertEquals("https://example.org/fdroid/repo", repos[1].address)
                assertEquals(999_999_998, repos[1].weight) // space for archive above
                assertEquals(guardianRepo.address, repos[2].address)
                assertEquals(999_999_996, repos[2].weight)
                assertEquals(guardianArchiveRepo.address, repos[3].address)
                assertEquals(999_999_995, repos[3].weight)
                assertEquals(fdroidRepo.address, repos[4].address)
                assertEquals(999_999_994, repos[4].weight)
                assertEquals(fdroidArchiveRepo.address, repos[5].address)
                assertEquals(999_999_993, repos[5].weight)
            }
        }
    }

    @Test
    fun migrateArchiveWithoutMainRepo() {
        val reposToMigrate = listOf(
            InitialRepository(
                name = "Foo bar",
                address = "https://example.org/fdroid/repo",
                description = "foo bar repo",
                certificate = "1234567890",
                version = 0L,
                enabled = true,
                weight = 2,
            ),
            fdroidArchiveRepo.copy(weight = 5),
            guardianRepo.copy(weight = 6),
        )
        runRepoMigration(reposToMigrate) { db ->
            db.getRepositoryDao().getRepositories().sortedByDescending { it.weight }.also { repos ->
                assertEquals(reposToMigrate.size, repos.size)
                assertEquals(reposToMigrate.size, repos.map { it.weight }.toSet().size)
                assertEquals(guardianRepo.address, repos[0].address)
                assertEquals(1_000_000_000, repos[0].weight) // space for archive below
                assertEquals("https://example.org/fdroid/repo", repos[1].address)
                assertEquals(999_999_998, repos[1].weight)
                assertEquals(fdroidArchiveRepo.address, repos[2].address) // archive at the end
                assertEquals(999_999_996, repos[2].weight)
            }
        }
    }

    @Test
    fun testPreferredRepoChanges() {
        var repoId: Long
        val packageName = "org.example"
        helper.createDatabase(TEST_DB, 1).use { db ->
            // Database has schema version 1. Insert some data using SQL queries.
            // We can't use DAO classes because they expect the latest schema.
            val repo = fdroidRepo
            repoId = db.insert(CoreRepository.TABLE, CONFLICT_FAIL, ContentValues().apply {
                put("name", localizedTextV2toString(mapOf("en-US" to repo.name)))
                put("description", localizedTextV2toString(mapOf("en-US" to repo.description)))
                put("address", repo.address)
                put("timestamp", -1)
                put("certificate", repo.certificate)
            })
            db.insert(RepositoryPreferences.TABLE, CONFLICT_FAIL, ContentValues().apply {
                put("repoId", repoId)
                put("enabled", repo.enabled)
                put("weight", repo.weight)
            })
            // insert an app with empty app prefs
            db.insert(AppMetadata.TABLE, CONFLICT_FAIL, ContentValues().apply {
                put("repoId", repoId)
                put("packageName", packageName)
                put("added", 23L)
                put("lastUpdated", 42L)
                put("isCompatible", true)
            })
            db.insert(AppPrefs.TABLE, CONFLICT_FAIL, ContentValues().apply {
                put("packageName", packageName)
                put("ignoreVersionCodeUpdate", 0)
            })
        }

        // Re-open the database with version 2, auto-migrations are applied automatically
        helper.runMigrationsAndValidate(TEST_DB, 2, true).close()

        // now get the Room DB, so we can use our DAOs for verifying the migration
        databaseBuilder(getApplicationContext(), FDroidDatabaseInt::class.java, TEST_DB)
            .allowMainThreadQueries()
            .build()
            .use { db ->
                // migrated apps have no preferred repo set
                assertNotNull(db.getAppDao().getApp(repoId, packageName))
                val appPrefs = db.getAppPrefsDao().getAppPrefsOrNull(packageName) ?: fail()
                assertEquals(packageName, appPrefs.packageName)
                assertNull(appPrefs.preferredRepoId)

                // preferred repo inferred from repo priorities
                val preferredRepos = db.getAppPrefsDao().getPreferredRepos(listOf(packageName))
                assertEquals(1, preferredRepos.size)
                assertEquals(repoId, preferredRepos[packageName])
            }
    }

    @Test
    fun repoWithoutCertificate() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            // Database has schema version 1. Insert some data using SQL queries.
            // We can't use DAO classes because they expect the latest schema.
            val repoId = db.insert(CoreRepository.TABLE, CONFLICT_FAIL, ContentValues().apply {
                put("name", localizedTextV2toString(mapOf("en-US" to fdroidRepo.name)))
                put(
                    "description",
                    localizedTextV2toString(mapOf("en-US" to fdroidRepo.description))
                )
                put("address", fdroidRepo.address)
                put("timestamp", -1)
            })
            db.insert(RepositoryPreferences.TABLE, CONFLICT_FAIL, ContentValues().apply {
                put("repoId", repoId)
                put("enabled", fdroidRepo.enabled)
                put("weight", fdroidRepo.weight)
            })
        }

        // Re-open the database with version 2, auto-migrations are applied automatically
        helper.runMigrationsAndValidate(TEST_DB, 2, true).close()

        // now get the Room DB, so we can use our DAOs for verifying the migration
        databaseBuilder(getApplicationContext(), FDroidDatabaseInt::class.java, TEST_DB)
            .allowMainThreadQueries()
            .build().use { db ->
                // repo without cert did not get migrated
                assertEquals(1, db.getRepositoryDao().getRepositories().size)
                val repo = db.getRepositoryDao().getRepositories()[0]
                // cert is still null
                assertNull(repo.certificate)
                // address still the same
                assertEquals(fdroidRepo.address, repo.address)
            }
    }

    private fun runRepoMigration(
        repos: List<InitialRepository>,
        check: (FDroidDatabaseInt) -> Unit,
    ) {
        helper.createDatabase(TEST_DB, 1).use { db ->
            // Database has schema version 1. Insert some data using SQL queries.
            // We can't use DAO classes because they expect the latest schema.
            repos.forEach { repo ->
                val repoId = db.insert(CoreRepository.TABLE, CONFLICT_FAIL, ContentValues().apply {
                    put("name", localizedTextV2toString(mapOf("en-US" to repo.name)))
                    put("description", localizedTextV2toString(mapOf("en-US" to repo.description)))
                    put("address", repo.address)
                    put("timestamp", -1)
                    put("certificate", repo.certificate)
                })
                db.insert(RepositoryPreferences.TABLE, CONFLICT_FAIL, ContentValues().apply {
                    put("repoId", repoId)
                    put("enabled", repo.enabled)
                    put("weight", repo.weight)
                })
            }
        }

        // Re-open the database with version 2, auto-migrations are applied automatically
        helper.runMigrationsAndValidate(TEST_DB, 2, true).close()

        // now get the Room DB, so we can use our DAOs for verifying the migration
        databaseBuilder(getApplicationContext(), FDroidDatabaseInt::class.java, TEST_DB)
            .allowMainThreadQueries()
            .build().use { db ->
                check(db)
            }
    }
}

package org.fdroid.repo

import androidx.core.os.LocaleListCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.fdroid.database.FDroidDatabase
import org.fdroid.database.NewRepository
import org.fdroid.database.Repository
import org.fdroid.database.RepositoryDaoInt
import org.fdroid.download.HttpManager
import org.fdroid.download.TestDownloadFactory
import org.fdroid.index.TempFileProvider
import org.fdroid.repo.AddRepoError.ErrorType.INVALID_FINGERPRINT
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
internal class RepoAdderIntegrationTest {

    @get:Rule
    var folder: TemporaryFolder = TemporaryFolder()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val db = mockk<FDroidDatabase>()
    private val repoDao = mockk<RepositoryDaoInt>()
    private val tempFileProvider = TempFileProvider { folder.newFile() }
    private val httpManager = HttpManager("test")
    private val downloaderFactory = TestDownloadFactory(httpManager)

    private val repoAdder: RepoAdder

    init {
        every { db.getRepositoryDao() } returns repoDao
        repoAdder = RepoAdder(context, db, tempFileProvider, downloaderFactory, httpManager)
    }

    @Before
    fun optIn() {
        assumeTrue(false) // don't run integration tests with real repos all the time
    }

    @Test
    fun testFedilabV1() = runTest {
        // repo not in DB
        every { repoDao.getRepository(any<String>()) } returns null

        repoAdder.fetchRepository(
            url = "https://fdroid.fedilab.app/repo/" +
                "?fingerprint=11F0A69910A4280E2CD3CCC3146337D006BE539B18E1A9FEACE15FF757A94FEB",
            username = null,
            password = null,
            proxy = null
        )
        repoAdder.addRepoState.test {
            assertEquals(None, awaitItem())
            val firstFetching = awaitItem()
            assertTrue(firstFetching is Fetching)
            assertNull(firstFetching.repo)
            assertTrue(firstFetching.apps.isEmpty())

            val secondFetching = awaitItem()
            assertTrue(secondFetching is Fetching, "$secondFetching")
            val repo = secondFetching.repo
            assertNotNull(repo)
            assertEquals("https://fdroid.fedilab.app/repo", repo.address)
            println(repo.getName(LocaleListCompat.getDefault()) ?: "null")
            println(repo.certificate)

            assertEquals(1, (awaitItem() as Fetching).apps.size)
            assertEquals(2, (awaitItem() as Fetching).apps.size)
            assertEquals(3, (awaitItem() as Fetching).apps.size)
            assertTrue(awaitItem() is Fetching)
        }

        val state = repoAdder.addRepoState.value
        assertTrue(state is Fetching, state.toString())
        assertTrue(state.apps.isNotEmpty())
        state.apps.forEach { app ->
            println("  ${app.packageName} ${app.summary}")
        }

        val newRepo: Repository = mockk()
        every { repoDao.insert(any<NewRepository>()) } returns 42L
        every { repoDao.getRepository(42L) } returns newRepo

        repoAdder.addFetchedRepository()
        repoAdder.addRepoState.test {
            val addedState = awaitItem()
            assertTrue(addedState is Added, addedState.toString())
            assertEquals(newRepo, addedState.repo)
        }
    }

    @Test
    fun testIzzy() = runBlocking {
        // repo not in DB
        every { repoDao.getRepository(any<String>()) } returns null

        repoAdder.fetchRepositoryInt(
            url = "https://apt.izzysoft.de/fdroid/repo" +
                "?fingerprint=3BF0D6ABFEAE2F401707B6D966BE743BF0EEE49C2561B9BA39073711F628937A"
        )
        val state = repoAdder.addRepoState.value
        assertTrue(state is Fetching, state.toString())
        assertTrue(state.apps.isNotEmpty())

        println(state.repo?.getName(LocaleListCompat.getDefault()) ?: "null")
        println(state.repo?.certificate)
        state.apps.forEach { app ->
            println("  ${app.packageName} ${app.summary}")
        }
    }

    @Test
    fun testIzzyWrongFingerprint() = runBlocking {
        repoAdder.fetchRepositoryInt("https://apt.izzysoft.de/fdroid/repo?fingerprint=fooBar")
        val state = repoAdder.addRepoState.value
        assertTrue(state is AddRepoError, state.toString())
        assertEquals(state.errorType, INVALID_FINGERPRINT, state.errorType.name)
    }

}

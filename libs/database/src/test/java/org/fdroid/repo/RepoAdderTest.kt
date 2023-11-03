package org.fdroid.repo

import android.content.Context
import android.content.res.AssetManager
import android.net.Uri
import android.os.UserManager
import android.os.UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES
import androidx.core.os.LocaleListCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.turbine.test
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.fdroid.LocaleChooser.getBestLocale
import org.fdroid.database.FDroidDatabase
import org.fdroid.database.Mirror
import org.fdroid.database.NewRepository
import org.fdroid.database.Repository
import org.fdroid.database.RepositoryDaoInt
import org.fdroid.database.RepositoryPreferences
import org.fdroid.database.toCoreRepository
import org.fdroid.download.Downloader
import org.fdroid.download.DownloaderFactory
import org.fdroid.download.HttpManager
import org.fdroid.download.NotFoundException
import org.fdroid.download.getDigestInputStream
import org.fdroid.fdroid.DigestInputStream
import org.fdroid.index.IndexFormatVersion
import org.fdroid.index.IndexParser.json
import org.fdroid.index.SigningException
import org.fdroid.index.TempFileProvider
import org.fdroid.index.v2.IndexV2
import org.fdroid.index.v2.RepoV2
import org.fdroid.repo.AddRepoError.ErrorType.INVALID_FINGERPRINT
import org.fdroid.repo.AddRepoError.ErrorType.INVALID_INDEX
import org.fdroid.repo.AddRepoError.ErrorType.IO_ERROR
import org.fdroid.repo.AddRepoError.ErrorType.UNKNOWN_SOURCES_DISALLOWED
import org.fdroid.test.TestDataMinV2
import org.fdroid.test.TestUtils.decodeHex
import org.fdroid.test.TestUtils.getRandomString
import org.fdroid.test.VerifierConstants
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.Callable
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

@RunWith(AndroidJUnit4::class)
internal class RepoAdderTest {

    @get:Rule
    var folder: TemporaryFolder = TemporaryFolder()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val db = mockk<FDroidDatabase>()
    private val repoDao = mockk<RepositoryDaoInt>()
    private val tempFileProvider = mockk<TempFileProvider>()
    private val httpManager = mockk<HttpManager>()
    private val downloaderFactory = mockk<DownloaderFactory>()
    private val downloader = mockk<Downloader>()
    private val digest = mockk<MessageDigest>()

    private val repoAdder: RepoAdder
    private val assets: AssetManager = context.resources.assets
    private val localeList = LocaleListCompat.getDefault()

    init {
        every { db.getRepositoryDao() } returns repoDao
        every { digest.update(any(), any(), any()) } just Runs
        every { digest.update(any<Byte>()) } just Runs

        mockkStatic("org.fdroid.download.HttpManagerKt")

        repoAdder = RepoAdder(context, db, tempFileProvider, downloaderFactory, httpManager)
    }

    @Test
    fun testDisallowInstallUnknownSources() = runTest {
        val context = mockk<Context>()
        val userManager = mockk<UserManager>()
        val repoAdder = RepoAdder(context, db, tempFileProvider, downloaderFactory, httpManager)

        every { context.getSystemService(UserManager::class.java) } returns userManager
        every {
            userManager.hasUserRestriction(DISALLOW_INSTALL_UNKNOWN_SOURCES)
        } returns true

        repoAdder.fetchRepositoryInt("https://example.org/repo/")
        repoAdder.addRepoState.test {
            val state1 = awaitItem()
            assertTrue(state1 is AddRepoError)
            assertEquals(UNKNOWN_SOURCES_DISALLOWED, state1.errorType)
        }
    }

    @Test
    fun testInvalidUri() = runTest {
        repoAdder.fetchRepositoryInt("irc://example.org/repo/") // invalid scheme

        repoAdder.addRepoState.test {
            val state1 = awaitItem()
            assertIs<AddRepoError>(state1)
            assertEquals(INVALID_INDEX, state1.errorType)
        }

        repoAdder.abortAddingRepo()
        repoAdder.fetchRepositoryInt("https://%-") // invalid hostname

        repoAdder.addRepoState.test {
            val state1 = awaitItem()
            assertIs<AddRepoError>(state1)
            assertEquals(INVALID_INDEX, state1.errorType)
        }
    }

    @Test
    fun testAddingMinRepo() = runTest {
        val url = "https://example.org/repo/"
        val urlTrimmed = url.trimEnd('/')
        val repoName = TestDataMinV2.repo.name.getBestLocale(localeList)

        expectDownloadOfMinRepo(url)

        // repo not in DB
        every { repoDao.getRepository(any<String>()) } returns null

        expectMinRepoPreview(repoName, FetchResult.IsNewRepository(urlTrimmed)) {
            repoAdder.fetchRepository(url = url, proxy = null)
        }

        val newRepo: Repository = mockk()
        val txnSlot = slot<Callable<Repository>>()
        every { db.runInTransaction(capture(txnSlot)) } answers {
            assertTrue(txnSlot.isCaptured)
            txnSlot.captured.call()
        }
        every {
            repoDao.insert(match<NewRepository> {
                // Note that we are not using the url the user used to add the repo,
                // but what the repo tells us to use
                it.address == TestDataMinV2.repo.address &&
                    it.formatVersion == IndexFormatVersion.TWO &&
                    it.name.getBestLocale(localeList) == repoName
            })
        } returns 42L
        every { repoDao.getRepository(42L) } returns newRepo
        every { repoDao.updateUserMirrors(42L, listOf(urlTrimmed)) } just Runs

        repoAdder.addRepoState.test {
            assertIs<Fetching>(awaitItem()) // still Fetching from last call

            repoAdder.addFetchedRepository()

            assertIs<Adding>(awaitItem()) // now moved to Adding

            val addedState = awaitItem()
            assertIs<Added>(addedState)
            assertEquals(newRepo, addedState.repo)
        }

        verify {
            repoDao.updateUserMirrors(42L, listOf(urlTrimmed))
        }
    }

    @Test
    fun testAddingMirrorForMinRepo() = runTest {
        val url = "https://example.com/repo/"
        val repoName = TestDataMinV2.repo.name.getBestLocale(localeList)

        expectDownloadOfMinRepo(url)

        // repo is already in the DB
        val existingRepo = Repository(
            repository = TestDataMinV2.repo.toCoreRepository(
                repoId = 42L,
                version = 1337L,
                formatVersion = IndexFormatVersion.TWO,
                certificate = "cert",
            ),
            mirrors = emptyList(),
            antiFeatures = emptyList(),
            categories = emptyList(),
            releaseChannels = emptyList(),
            preferences = RepositoryPreferences(42L, 23),
        )
        every { repoDao.getRepository(any<String>()) } returns existingRepo

        expectMinRepoPreview(repoName, FetchResult.IsNewMirror(42L, url.trimEnd('/'))) {
            repoAdder.fetchRepository(url = url, proxy = null)
        }

        val transactionSlot = slot<Callable<Repository>>()
        every {
            db.runInTransaction(capture(transactionSlot))
        } answers { transactionSlot.captured.call() }
        every { repoDao.getRepository(42L) } returns existingRepo
        every { repoDao.updateUserMirrors(42L, listOf(url.trimEnd('/'))) } just Runs

        repoAdder.addRepoState.test {
            assertIs<Fetching>(awaitItem()) // still Fetching from last call

            repoAdder.addFetchedRepository()

            assertIs<Adding>(awaitItem()) // now moved to Adding

            val addedState = awaitItem()
            assertTrue(addedState is Added, addedState.toString())
            assertEquals(existingRepo, addedState.repo)
        }

        verify(exactly = 1) {
            repoDao.updateUserMirrors(42L, listOf(url.trimEnd('/')))
        }
    }

    @Test
    fun testRepoAlreadyExists() = runTest {
        val url = "https://min-v1.org/repo/"

        // repo is already in the DB
        val existingRepo = Repository(
            repository = TestDataMinV2.repo.toCoreRepository(
                repoId = REPO_ID,
                version = 1337L,
                formatVersion = IndexFormatVersion.TWO,
                certificate = "cert",
            ).copy(address = url), // change address, because TestDataMinV2 misses /repo
            mirrors = emptyList(),
            antiFeatures = emptyList(),
            categories = emptyList(),
            releaseChannels = emptyList(),
            preferences = RepositoryPreferences(REPO_ID, 23),
        )
        testRepoAlreadyExists(url, existingRepo)
    }

    @Test
    fun testRepoAlreadyExistsWithMirror() = runTest {
        val url = "https://example.org/repo/"

        // repo is already in the DB
        val existingRepo = Repository(
            repository = TestDataMinV2.repo.toCoreRepository(
                repoId = REPO_ID,
                version = 1337L,
                formatVersion = IndexFormatVersion.TWO,
                certificate = "cert",
            ),
            mirrors = listOf(Mirror(REPO_ID, url), Mirror(REPO_ID, "http://example.org")),
            antiFeatures = emptyList(),
            categories = emptyList(),
            releaseChannels = emptyList(),
            preferences = RepositoryPreferences(REPO_ID, 23),
        )
        testRepoAlreadyExists(url, existingRepo)
    }

    @Test
    fun testRepoAlreadyExistsWithFingerprint() = runTest {
        val url = "https://example.org/repo?fingerprint=${VerifierConstants.FINGERPRINT}"

        // repo is already in the DB
        val existingRepo = Repository(
            repository = TestDataMinV2.repo.toCoreRepository(
                repoId = REPO_ID,
                version = 1337L,
                formatVersion = IndexFormatVersion.TWO,
                certificate = VerifierConstants.CERTIFICATE,
            ),
            mirrors = listOf(Mirror(REPO_ID, "https://example.org/repo/")),
            antiFeatures = emptyList(),
            categories = emptyList(),
            releaseChannels = emptyList(),
            preferences = RepositoryPreferences(REPO_ID, 23),
        )
        testRepoAlreadyExists(url, existingRepo, "https://example.org/repo")
    }

    @Test
    fun testRepoAlreadyExistsWithFingerprintTrailingSlash() = runTest {
        val url = "https://example.org/repo/?fingerprint=${VerifierConstants.FINGERPRINT}"

        // repo is already in the DB
        val existingRepo = Repository(
            repository = TestDataMinV2.repo.toCoreRepository(
                repoId = REPO_ID,
                version = 1337L,
                formatVersion = IndexFormatVersion.TWO,
                certificate = VerifierConstants.CERTIFICATE,
            ),
            mirrors = listOf(
                Mirror(REPO_ID, "https://example.org/repo"),
                Mirror(REPO_ID, "http://example.org"),
            ),
            antiFeatures = emptyList(),
            categories = emptyList(),
            releaseChannels = emptyList(),
            preferences = RepositoryPreferences(REPO_ID, 23),
        )
        testRepoAlreadyExists(url, existingRepo, "https://example.org/repo")
    }

    @Test
    fun testRepoAlreadyExistsUserMirror() = runTest {
        val url = "https://example.net/repo/"

        // repo is already in the DB
        val existingRepo = Repository(
            repository = TestDataMinV2.repo.toCoreRepository(
                repoId = REPO_ID,
                version = 1337L,
                formatVersion = IndexFormatVersion.TWO,
                certificate = "cert",
            ),
            mirrors = emptyList(),
            antiFeatures = emptyList(),
            categories = emptyList(),
            releaseChannels = emptyList(),
            preferences = RepositoryPreferences(
                repoId = REPO_ID,
                weight = 23,
                userMirrors = listOf(url, "http://example.org"),
            ),
        )
        testRepoAlreadyExists(url, existingRepo)
    }

    private suspend fun testRepoAlreadyExists(
        url: String,
        existingRepo: Repository,
        downloadUrl: String = url,
    ) {
        val repoName = TestDataMinV2.repo.name.getBestLocale(localeList)

        expectDownloadOfMinRepo(downloadUrl)

        // repo is already in the DB
        every { repoDao.getRepository(any<String>()) } returns existingRepo

        expectMinRepoPreview(repoName, FetchResult.IsExistingRepository, canAdd = false) {
            repoAdder.fetchRepository(url = url, proxy = null)
        }
        assertFailsWith<IllegalStateException> {
            repoAdder.addFetchedRepository()
        }
    }

    @Test
    fun testDownloadEntryThrowsIoException() = runTest {
        val url = "https://example.org/repo"
        val jarFile = folder.newFile()

        every { tempFileProvider.createTempFile() } returns jarFile
        every {
            downloaderFactory.create(
                repo = match { it.address == url && it.formatVersion == IndexFormatVersion.TWO },
                uri = Uri.parse("$url/entry.jar"),
                indexFile = any(),
                destFile = jarFile,
            )
        } returns downloader
        every { downloader.download() } throws IOException()

        repoAdder.addRepoState.test {
            assertIs<None>(awaitItem())

            repoAdder.fetchRepository(url = url, proxy = null)

            val state1 = awaitItem()
            assertIs<Fetching>(state1)
            assertNull(state1.repo)
            assertTrue(state1.apps.isEmpty())
            assertFalse(state1.canAdd)

            val state2 = awaitItem()
            assertTrue(state2 is AddRepoError, "$state2")
            assertEquals(IO_ERROR, state2.errorType)
        }
    }

    @Test
    fun testParsingThrowsSerializationException() = runTest {
        val url = "https://example.org/repo"
        val urlTrimmed = url.trimEnd('/')
        val jarFile = folder.newFile()
        val index = "{ invalid JSON foo bar,".toByteArray()
        val indexStream = DigestInputStream(ByteArrayInputStream(index), digest)

        every { tempFileProvider.createTempFile() } returns jarFile
        every {
            downloaderFactory.create(
                repo = match {
                    it.address == urlTrimmed && it.formatVersion == IndexFormatVersion.TWO
                },
                uri = Uri.parse("$urlTrimmed/entry.jar"),
                indexFile = any(),
                destFile = jarFile,
            )
        } returns downloader
        every { downloader.download() } answers {
            jarFile.outputStream().use { outputStream ->
                assets.open("diff-empty-min/entry.jar").use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
        coEvery {
            httpManager.getDigestInputStream(match {
                it.indexFile.name == "../index-min-v2.json" &&
                    it.mirrors.size == 1 && it.mirrors[0].baseUrl == urlTrimmed
            })
        } returns indexStream
        every {
            digest.digest() // sha256 from entry.json
        } returns "851ecda085ed53adab25f761a9dbf4c09d59e5bff9c9d5530814d56445ae30f2".decodeHex()

        repoAdder.addRepoState.test {
            assertIs<None>(awaitItem())

            repoAdder.fetchRepository(url = url, proxy = null)

            val state1 = awaitItem()
            assertIs<Fetching>(state1)
            assertNull(state1.repo)
            assertTrue(state1.apps.isEmpty())
            assertFalse(state1.canAdd)

            val state2 = awaitItem()
            assertTrue(state2 is AddRepoError, "$state2")
            assertEquals(INVALID_INDEX, state2.errorType)
        }
    }

    @Test
    fun testWrongFingerprint() = runTest {
        val url = "https://example.org/repo/"
        val jarFile = folder.newFile()

        every { tempFileProvider.createTempFile() } returns jarFile
        every {
            downloaderFactory.create(
                repo = match {
                    it.address == url.trimEnd('/') && it.formatVersion == IndexFormatVersion.TWO
                },
                uri = Uri.parse(url + "entry.jar"),
                indexFile = any(),
                destFile = jarFile,
            )
        } returns downloader
        // not actually thrown by the downloader, but mocking verifier is harder
        every { downloader.download() } throws SigningException("boom!")

        repoAdder.addRepoState.test {
            assertIs<None>(awaitItem())

            repoAdder.fetchRepository(url = url, proxy = null)

            val state1 = awaitItem()
            assertIs<Fetching>(state1)
            assertNull(state1.repo)
            assertTrue(state1.apps.isEmpty())
            assertFalse(state1.canAdd)

            val state2 = awaitItem()
            assertTrue(state2 is AddRepoError, "$state2")
            assertEquals(INVALID_FINGERPRINT, state2.errorType)
        }
    }

    @Test
    fun testWrongKnownFingerprint() = runTest {
        val url = "https://example.org/repo"
        testMinRepoPreview(url) { state2 ->
            assertTrue(state2 is AddRepoError, "$state2")
            assertEquals(INVALID_FINGERPRINT, state2.errorType)
            val e = assertIs<SigningException>(state2.exception)
            assertTrue(e.message!!.contains("Known fingerprint different"))
        }
    }

    @Test
    fun testWrongKnownFingerprintWithGivenFingerprint() = runTest {
        val url = "https://example.org/repo?fingerprint=${VerifierConstants.FINGERPRINT}"
        testMinRepoPreview("https://example.org/repo", url) { state2 ->
            assertTrue(state2 is AddRepoError, "$state2")
            assertEquals(INVALID_FINGERPRINT, state2.errorType)
            val e = assertIs<SigningException>(state2.exception)
            assertTrue(e.message!!.contains("Known fingerprint different"))
        }
    }

    private suspend fun testMinRepoPreview(
        repoAddress: String,
        url: String = repoAddress,
        onSecondState: (AddRepoState) -> Unit,
    ) {
        val jarFile = folder.newFile()
        val repoV2 = RepoV2(
            address = "https://briarproject.org/fdroid/repo",
            timestamp = 42L,
        )
        val indexV2 = IndexV2(repo = repoV2)
        val index = json.encodeToString(IndexV2.serializer(), indexV2).toByteArray()
        val indexStream = DigestInputStream(ByteArrayInputStream(index), digest)

        every { tempFileProvider.createTempFile() } returns jarFile
        every {
            downloaderFactory.create(
                repo = match {
                    it.address == repoAddress && it.formatVersion == IndexFormatVersion.TWO
                },
                uri = Uri.parse("$repoAddress/entry.jar"),
                indexFile = any(),
                destFile = jarFile,
            )
        } returns downloader
        every { downloader.download() } answers {
            jarFile.outputStream().use { outputStream ->
                assets.open("diff-empty-min/entry.jar").use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
        coEvery {
            httpManager.getDigestInputStream(match {
                it.indexFile.name == "../index-min-v2.json" &&
                    it.mirrors.size == 1 && it.mirrors[0].baseUrl == repoAddress
            })
        } returns indexStream
        every {
            digest.digest() // sha256 from entry.json
        } returns "851ecda085ed53adab25f761a9dbf4c09d59e5bff9c9d5530814d56445ae30f2".decodeHex()

        repoAdder.addRepoState.test {
            assertIs<None>(awaitItem())

            repoAdder.fetchRepository(url = url, proxy = null)

            val state1 = awaitItem()
            assertIs<Fetching>(state1)
            assertNull(state1.repo)
            assertTrue(state1.apps.isEmpty())
            assertFalse(state1.canAdd)

            val state2 = awaitItem()
            onSecondState(state2)
        }
    }

    @Test
    fun testKnownFingerprintIsAccepted() = runTest {
        val repoAddress = "https://guardianproject.info/fdroid/repo"
        val fingerprint = knownRepos[repoAddress]
        val url = "https://example.org/repo?fingerprint=$fingerprint"

        val jarFile = folder.newFile()
        val repoV2 = RepoV2(
            address = repoAddress,
            timestamp = 42L,
        )
        val indexV2 = IndexV2(repo = repoV2)
        val index = json.encodeToString(IndexV2.serializer(), indexV2).toByteArray()
        val indexStream = DigestInputStream(ByteArrayInputStream(index), digest)

        every { tempFileProvider.createTempFile() } returns jarFile
        every {
            downloaderFactory.create(
                repo = match {
                    it.address == "https://example.org/repo" &&
                        it.formatVersion == IndexFormatVersion.TWO
                },
                uri = Uri.parse("https://example.org/repo/entry.jar"),
                indexFile = any(),
                destFile = jarFile,
            )
        } returns downloader
        every { downloader.download() } answers {
            jarFile.outputStream().use { outputStream ->
                assets.open("guardianproject_entry.jar").use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
        coEvery {
            httpManager.getDigestInputStream(match {
                it.indexFile.name == "/index-v2.json" &&
                    it.mirrors.size == 1 && it.mirrors[0].baseUrl == "https://example.org/repo"
            })
        } returns indexStream
        every {
            digest.digest() // sha256 from entry.json
        } returns "cd925cdc31c88e8509bd64e62f7680d8dbffe2643990f62404acfda71e538906".decodeHex()

        // repo not in DB
        every { repoDao.getRepository(any<String>()) } returns null

        repoAdder.addRepoState.test {
            assertIs<None>(awaitItem())

            repoAdder.fetchRepository(url = url, proxy = null)

            val state1 = awaitItem()
            assertIs<Fetching>(state1)
            assertNull(state1.repo)
            assertTrue(state1.apps.isEmpty())
            assertFalse(state1.canAdd)

            val state2 = awaitItem()
            assertIs<Fetching>(state2)
            assertEquals(repoAddress, state2.repo?.address)
            assertTrue(state2.canAdd)
            assertFalse(state2.done)

            val state3 = awaitItem()
            assertIs<Fetching>(state3)
            assertTrue(state3.canAdd)
            assertTrue(state3.done)
        }
    }

    @Test
    fun testFallbackToV1() = runTest {
        val url = "https://example.org/repo/"
        val urlTrimmed = "https://example.org/repo"
        val jarFile = folder.newFile()

        every { tempFileProvider.createTempFile() } returns jarFile
        every {
            downloaderFactory.create(
                repo = match {
                    it.address == urlTrimmed && it.formatVersion == IndexFormatVersion.TWO
                },
                uri = Uri.parse("$urlTrimmed/entry.jar"),
                indexFile = any(),
                destFile = jarFile,
            )
        } returns downloader
        every { downloader.download() } throws NotFoundException()
        val downloaderV1 = mockk<Downloader>()
        every {
            downloaderFactory.create(
                repo = match {
                    it.address == urlTrimmed && it.formatVersion == IndexFormatVersion.ONE
                },
                uri = Uri.parse("$urlTrimmed/index-v1.jar"),
                indexFile = any(),
                destFile = jarFile,
            )
        } returns downloaderV1
        every { downloaderV1.download() } answers {
            jarFile.outputStream().use { outputStream ->
                assets.open("testy.at.or.at_index-v1.jar").use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
        every { repoDao.getRepository(any<String>()) } returns null

        repoAdder.addRepoState.test {
            assertIs<None>(awaitItem())

            repoAdder.fetchRepository(url = url, proxy = null)

            val state1 = awaitItem()
            assertIs<Fetching>(state1)
            assertNull(state1.repo)
            assertTrue(state1.apps.isEmpty())
            assertFalse(state1.canAdd)

            for (i in 0..64) assertIs<Fetching>(awaitItem())
        }
        val addRepoState = repoAdder.addRepoState.value
        assertIs<Fetching>(addRepoState)
        assertTrue(addRepoState.canAdd)
        assertEquals(63, addRepoState.apps.size)
    }

    @Test
    fun testDownloadV1ThrowsNotFoundException() = runTest {
        val url = "https://example.org/repo"
        val jarFile = folder.newFile()

        every { tempFileProvider.createTempFile() } returns jarFile
        every {
            downloaderFactory.create(
                repo = match { it.address == url && it.formatVersion == IndexFormatVersion.TWO },
                uri = Uri.parse("$url/entry.jar"),
                indexFile = any(),
                destFile = jarFile,
            )
        } returns downloader
        every { downloader.download() } throws NotFoundException()

        every {
            downloaderFactory.create(
                repo = match { it.address == url && it.formatVersion == IndexFormatVersion.ONE },
                uri = Uri.parse("$url/index-v1.jar"),
                indexFile = any(),
                destFile = jarFile,
            )
        } returns downloader
        every { downloader.download() } throws NotFoundException()

        repoAdder.addRepoState.test {
            assertIs<None>(awaitItem())

            repoAdder.fetchRepository(url = url, proxy = null)

            val state1 = awaitItem()
            assertIs<Fetching>(state1)
            assertNull(state1.repo)
            assertTrue(state1.apps.isEmpty())
            assertFalse(state1.canAdd)

            val state2 = awaitItem()
            assertTrue(state2 is AddRepoError, "$state2")
            assertEquals(INVALID_INDEX, state2.errorType)
        }
    }

    @Test
    fun testAddingMinRepoWithBasicAuth() = runTest {
        val username = getRandomString()
        val password = getRandomString()
        val url = "https://$username:$password@example.org/repo/"
        val repoName = TestDataMinV2.repo.name.getBestLocale(localeList)

        val urlTrimmed = "https://example.org/repo"
        val jarFile = folder.newFile()
        val indexFile = assets.open("index-min-v2.json")
        val index = indexFile.use { it.readBytes() }
        val indexStream = DigestInputStream(ByteArrayInputStream(index), digest)

        every { tempFileProvider.createTempFile() } returns jarFile
        every {
            downloaderFactory.create(
                repo = match {
                    it.address == urlTrimmed && it.formatVersion == IndexFormatVersion.TWO
                },
                uri = Uri.parse("$urlTrimmed/entry.jar"),
                indexFile = any(),
                destFile = jarFile,
            )
        } returns downloader
        every { downloader.download() } answers {
            jarFile.outputStream().use { outputStream ->
                assets.open("diff-empty-min/entry.jar").use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
        coEvery {
            httpManager.getDigestInputStream(match {
                it.indexFile.name == "../index-min-v2.json" &&
                    it.mirrors.size == 1 && it.mirrors[0].baseUrl == urlTrimmed
            })
        } returns indexStream
        every {
            digest.digest() // sha256 from entry.json
        } returns "851ecda085ed53adab25f761a9dbf4c09d59e5bff9c9d5530814d56445ae30f2".decodeHex()

        // repo not in DB
        every { repoDao.getRepository(any<String>()) } returns null

        expectMinRepoPreview(repoName, FetchResult.IsNewRepository(urlTrimmed)) {
            repoAdder.fetchRepository(url = url, proxy = null)
        }

        val newRepo: Repository = mockk()
        val txnSlot = slot<Callable<Repository>>()
        every { db.runInTransaction(capture(txnSlot)) } answers {
            assertTrue(txnSlot.isCaptured)
            txnSlot.captured.call()
        }
        every {
            repoDao.insert(match<NewRepository> {
                // Note that we are not using the url the user used to add the repo,
                // but what the repo tells us to use
                it.address == TestDataMinV2.repo.address &&
                    it.formatVersion == IndexFormatVersion.TWO &&
                    it.name.getBestLocale(localeList) == repoName &&
                    it.username == username && it.password == password // this is the important bit
            })
        } returns 42L
        every { repoDao.updateUserMirrors(42L, listOf(urlTrimmed)) } just Runs
        every { repoDao.getRepository(42L) } returns newRepo

        repoAdder.addRepoState.test {
            assertIs<Fetching>(awaitItem()) // still Fetching from last call

            repoAdder.addFetchedRepository()

            assertIs<Adding>(awaitItem()) // now moved to Adding

            val addedState = awaitItem()
            assertIs<Added>(addedState)
            assertEquals(newRepo, addedState.repo)
        }
    }

    private fun expectDownloadOfMinRepo(url: String) {
        val urlTrimmed = url.trimEnd('/')
        val jarFile = folder.newFile()
        val indexFile = assets.open("index-min-v2.json")
        val index = indexFile.use { it.readBytes() }
        val indexStream = DigestInputStream(ByteArrayInputStream(index), digest)

        every { tempFileProvider.createTempFile() } returns jarFile
        every {
            downloaderFactory.create(
                repo = match {
                    it.address == urlTrimmed && it.formatVersion == IndexFormatVersion.TWO
                },
                uri = Uri.parse("$urlTrimmed/entry.jar"),
                indexFile = any(),
                destFile = jarFile,
            )
        } returns downloader
        every { downloader.download() } answers {
            jarFile.outputStream().use { outputStream ->
                assets.open("diff-empty-min/entry.jar").use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
        coEvery {
            httpManager.getDigestInputStream(match {
                it.indexFile.name == "../index-min-v2.json" &&
                    it.mirrors.size == 1 && it.mirrors[0].baseUrl == urlTrimmed
            })
        } returns indexStream
        every {
            digest.digest() // sha256 from entry.json
        } returns "851ecda085ed53adab25f761a9dbf4c09d59e5bff9c9d5530814d56445ae30f2".decodeHex()
    }

    private suspend fun expectMinRepoPreview(
        repoName: String?,
        fetchResult: FetchResult,
        canAdd: Boolean = true,
        block: suspend () -> Unit = {},
    ) {
        repoAdder.addRepoState.test {
            assertIs<None>(awaitItem())

            launch(Dispatchers.IO) {
                // FIXME executing this block may emit items too fast, so we might miss one
                //  causing flaky tests. A short delay may fix it, let's see.
                delay(250)
                block()
            }

            val state1 = awaitItem()
            assertIs<Fetching>(state1)
            assertNull(state1.repo)
            assertEquals(emptyList(), state1.apps)
            assertFalse(state1.canAdd)
            assertFalse(state1.done)

            val state2 = awaitItem()
            assertIs<Fetching>(state2)
            val repo = state2.repo ?: fail()
            assertEquals(TestDataMinV2.repo.address, repo.address)
            assertEquals(repoName, repo.getName(localeList))
            val result = state2.fetchResult ?: fail()
            assertEquals(fetchResult, result)
            assertTrue(state2.apps.isEmpty())
            assertEquals(canAdd, state2.canAdd)
            assertFalse(state2.done)

            val state3 = awaitItem()
            assertIs<Fetching>(state3)
            assertEquals(TestDataMinV2.packages.size, state3.apps.size)
            assertEquals(TestDataMinV2.packageName, state3.apps[0].packageName)
            assertEquals(canAdd, state3.canAdd)
            assertFalse(state3.done)

            val state4 = awaitItem()
            assertIs<Fetching>(state4)
            assertEquals(canAdd, state4.canAdd)
            assertTrue(state4.done)
        }
    }
}

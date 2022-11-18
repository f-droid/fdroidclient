package org.fdroid.index.v1

import android.Manifest
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.fdroid.CompatibilityChecker
import org.fdroid.database.DbTest
import org.fdroid.database.Repository
import org.fdroid.database.TestUtils.getOrAwaitValue
import org.fdroid.database.TestUtils.getOrFail
import org.fdroid.download.Downloader
import org.fdroid.download.DownloaderFactory
import org.fdroid.index.IndexUpdateResult
import org.fdroid.index.SigningException
import org.fdroid.index.TempFileProvider
import org.fdroid.index.v2.ANTI_FEATURE_KNOWN_VULNERABILITY
import org.fdroid.index.v2.FileV2
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

@RunWith(AndroidJUnit4::class)
internal class IndexV1UpdaterTest : DbTest() {

    @get:Rule
    var tmpFolder: TemporaryFolder = TemporaryFolder()

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val tempFileProvider: TempFileProvider = mockk()
    private val downloaderFactory: DownloaderFactory = mockk()
    private val downloader: Downloader = mockk()
    private val compatibilityChecker: CompatibilityChecker = CompatibilityChecker { true }
    private lateinit var indexUpdater: IndexV1Updater

    @Before
    override fun createDb() {
        super.createDb()
        indexUpdater = IndexV1Updater(
            database = db,
            tempFileProvider = tempFileProvider,
            downloaderFactory = downloaderFactory,
            compatibilityChecker = compatibilityChecker,
        )
    }

    @Test
    fun testIndexV1Processing() {
        val repoId = repoDao.insertEmptyRepo(TESTY_CANONICAL_URL)
        val repo = repoDao.getRepository(repoId) ?: fail()
        downloadIndex(repo, TESTY_JAR)
        val result = indexUpdater.updateNewRepo(repo, TESTY_FINGERPRINT).noError()
        assertIs<IndexUpdateResult.Processed>(result)

        // repo got updated
        val updatedRepo = repoDao.getRepository(repoId) ?: fail()
        assertEquals(TESTY_CERT, updatedRepo.certificate)
        assertEquals(TESTY_FINGERPRINT, updatedRepo.fingerprint)

        // some assertions ported from old IndexV1UpdaterTest
        assertEquals(1, repoDao.getRepositories().size)
        assertEquals(63, appDao.countApps())
        listOf("fake.app.one", "org.adaway", "This_does_not_exist").forEach { packageName ->
            assertNull(appDao.getApp(packageName).getOrAwaitValue())
        }
        appDao.getAppMetadata().forEach { app ->
            val numVersions = versionDao.getVersions(listOf(app.packageName)).size
            assertTrue(numVersions > 0)
        }
        assertEquals(1497639511824, updatedRepo.timestamp)
        assertEquals(TESTY_CANONICAL_URL, updatedRepo.address)
        assertEquals("non-public test repo", updatedRepo.repository.name.values.first())
        assertEquals(18, updatedRepo.version)
        assertEquals("/icons/fdroid-icon.png", updatedRepo.repository.icon?.values?.first()?.name)
        val description = "This is a repository of apps to be used with F-Droid. " +
            "Applications in this repository are either official binaries built " +
            "by the original application developers, or are binaries built " +
            "from source by the admin of f-droid.org using the tools on " +
            "https://gitlab.com/u/fdroid. "
        assertEquals(description, updatedRepo.repository.description.values.first())
        assertEquals(
            setOf(TESTY_CANONICAL_URL, "http://frkcchxlcvnb4m5a.onion/fdroid/repo"),
            updatedRepo.mirrors.map { it.url }.toSet(),
        )

        // Make sure the per-apk anti features which are new in index v1 get added correctly.
        val wazeVersion = versionDao.getVersions(listOf("com.waze")).find {
            it.manifest.versionCode == 1019841L
        }
        assertNotNull(wazeVersion)
        assertEquals(setOf(ANTI_FEATURE_KNOWN_VULNERABILITY), wazeVersion.antiFeatures?.keys)

        val protoVersion = versionDao.getAppVersions("io.proto.player").getOrFail().find {
            it.version.versionCode == 1110L
        }
        assertNotNull(protoVersion)
        assertEquals("/io.proto.player-1.apk", protoVersion.version.file.name)
        val perms = protoVersion.usesPermission.map { it.name }
        assertTrue(perms.contains(Manifest.permission.READ_EXTERNAL_STORAGE))
        assertTrue(perms.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE))
        assertFalse(perms.contains(Manifest.permission.READ_CALENDAR))
        val icon = appDao.getApp("com.autonavi.minimap").getOrFail()?.icon?.values?.first()?.name
        assertEquals("/com.autonavi.minimap/en-US/icon.png", icon)

        // update again and get unchanged
        downloadIndex(updatedRepo, TESTY_JAR)
        val result2 = indexUpdater.update(updatedRepo).noError()
        assertIs<IndexUpdateResult.Unchanged>(result2)
    }

    @Test
    fun testIndexV1WithWrongCert() {
        val repoId = repoDao.insertEmptyRepo(TESTY_CANONICAL_URL)
        val repo = repoDao.getRepository(repoId) ?: fail()
        downloadIndex(repo, TESTY_JAR)
        val result = indexUpdater.updateNewRepo(repo, "not the right fingerprint")
        assertIs<IndexUpdateResult.Error>(result)
        assertIs<SigningException>(result.e)

        // check that the DB transaction was rolled back and the DB wasn't changed
        assertEquals(repo, repoDao.getRepository(repoId) ?: fail())
        assertEquals(0, appDao.countApps())
        assertEquals(0, versionDao.countAppVersions())
    }

    @Test
    fun testIndexV1WithOldTimestamp() {
        val repoId = repoDao.insertEmptyRepo(TESTY_CANONICAL_URL)
        val repo = repoDao.getRepository(repoId) ?: fail()
        val futureRepo =
            repo.copy(repository = repo.repository.copy(timestamp = System.currentTimeMillis()))
        downloadIndex(futureRepo, TESTY_JAR)
        val result = indexUpdater.updateNewRepo(futureRepo, TESTY_FINGERPRINT)
        assertIs<IndexUpdateResult.Error>(result)
        assertIs<OldIndexException>(result.e)
        assertFalse((result.e as OldIndexException).isSameTimestamp)
    }

    @Test
    fun testIndexV1WithCorruptAppPackageName() {
        val result = testBadTestyJar("testy.at.or.at_corrupt_app_package_name_index-v1.jar")
        assertIs<IndexUpdateResult.Error>(result)
    }

    @Test
    fun testIndexV1WithCorruptPackageName() {
        val result = testBadTestyJar("testy.at.or.at_corrupt_package_name_index-v1.jar")
        assertIs<IndexUpdateResult.Error>(result)
    }

    @Test
    fun testIndexV1WithBadTestyJarNoManifest() {
        val result = testBadTestyJar("testy.at.or.at_no-MANIFEST.MF_index-v1.jar")
        assertIs<IndexUpdateResult.Error>(result)
        assertIs<SigningException>(result.e)
    }

    @Test
    fun testIndexV1WithBadTestyJarNoSigningCert() {
        val result = testBadTestyJar("testy.at.or.at_no-.RSA_index-v1.jar")
        assertIs<IndexUpdateResult.Error>(result)
    }

    @Test
    fun testIndexV1WithBadTestyJarNoSignature() {
        val result = testBadTestyJar("testy.at.or.at_no-.SF_index-v1.jar")
        assertIs<IndexUpdateResult.Error>(result)
    }

    @Test
    fun testIndexV1WithBadTestyJarNoSignatureFiles() {
        val result = testBadTestyJar("testy.at.or.at_no-signature_index-v1.jar")
        assertIs<IndexUpdateResult.Error>(result)
        assertIs<SigningException>(result.e)
    }

    @Suppress("DEPRECATION")
    private fun downloadIndex(repo: Repository, jar: String) {
        val uri = Uri.parse("${repo.address}/$SIGNED_FILE_NAME")
        val indexFile = FileV2.fromPath("/$SIGNED_FILE_NAME")

        val jarFile = tmpFolder.newFile()
        assets.open(jar).use { inputStream ->
            jarFile.outputStream().use { inputStream.copyTo(it) }
        }
        every { tempFileProvider.createTempFile() } returns jarFile
        every {
            downloaderFactory.createWithTryFirstMirror(repo, uri, indexFile, jarFile)
        } returns downloader
        every { downloader.cacheTag = null } just Runs
        every { downloader.download() } just Runs
        every { downloader.hasChanged() } returns true
        every { downloader.cacheTag } returns null
    }

    private fun testBadTestyJar(jar: String): IndexUpdateResult {
        val repoId = repoDao.insertEmptyRepo("http://example.org")
        val repo = repoDao.getRepository(repoId) ?: fail()
        downloadIndex(repo, jar)
        return indexUpdater.updateNewRepo(repo, null)
    }

    /**
     * Easier for debugging, if we throw the index error.
     */
    private fun IndexUpdateResult.noError(): IndexUpdateResult {
        if (this is IndexUpdateResult.Error) throw e
        return this
    }

}

private const val TESTY_CANONICAL_URL = "http://testy.at.or.at/fdroid/repo"
private const val TESTY_JAR = "testy.at.or.at_index-v1.jar"
private const val TESTY_FINGERPRINT =
    "818e469465f96b704e27be2fee4c63ab9f83ddf30e7a34c7371a4728d83b0bc1"
private const val TESTY_CERT = "308204e1308202c9a0030201020204483450fa300d06092a864886f70d01010b" +
    "050030213110300e060355040b1307462d44726f6964310d300b060355040313" +
    "04736f7661301e170d3136303832333133333131365a170d3434303130393133" +
    "333131365a30213110300e060355040b1307462d44726f6964310d300b060355" +
    "04031304736f766130820222300d06092a864886f70d01010105000382020f00" +
    "3082020a0282020100dfdcd120f3ab224999dddf4ea33ea588d295e4d7130bef" +
    "48c143e9d76e5c0e0e9e5d45e64208e35feebc79a83f08939dd6a343b7d1e217" +
    "9930a105a1249ccd36d88ff3feffc6e4dc53dae0163a7876dd45ecc1ddb0adf5" +
    "099aa56c1a84b52affcd45d0711ffa4de864f35ac0333ebe61ea8673eeda35a8" +
    "8f6af678cc4d0f80b089338ac8f2a8279a64195c611d19445cab3fd1a020afed" +
    "9bd739bb95142fb2c00a8f847db5ef3325c814f8eb741bacf86ed3907bfe6e45" +
    "64d2de5895df0c263824e0b75407589bae2d3a4666c13b92102d8781a8ee9bb4" +
    "a5a1a78c4a9c21efdaf5584da42e84418b28f5a81d0456a3dc5b420991801e6b" +
    "21e38c99bbe018a5b2d690894a114bc860d35601416aa4dc52216aff8a288d47" +
    "75cddf8b72d45fd2f87303a8e9c0d67e442530be28eaf139894337266e0b33d5" +
    "7f949256ab32083bcc545bc18a83c9ab8247c12aea037e2b68dee31c734cb1f0" +
    "4f241d3b94caa3a2b258ffaf8e6eae9fbbe029a934dc0a0859c5f12033481269" +
    "3a1c09352340a39f2a678dbc1afa2a978bfee43afefcb7e224a58af2f3d647e5" +
    "745db59061236b8af6fcfd93b3602f9e456978534f3a7851e800071bf56da804" +
    "01c81d91c45f82568373af0576b1cc5eef9b85654124b6319770be3cdba3fbeb" +
    "e3715e8918fb6c8966624f3d0e815effac3d2ee06dd34ab9c693218b2c7c06ba" +
    "99d6b74d4f17b8c3cb0203010001a321301f301d0603551d0e04160414d62bee" +
    "9f3798509546acc62eb1de14b08b954d4f300d06092a864886f70d01010b0500" +
    "0382020100743f7c5692085895f9d1fffad390fb4202c15f123ed094df259185" +
    "960fd6dadf66cb19851070f180297bba4e6996a4434616573b375cfee94fee73" +
    "a4505a7ec29136b7e6c22e6436290e3686fe4379d4e3140ec6a08e70cfd3ed5b" +
    "634a5eb5136efaaabf5f38e0432d3d79568a556970b8cfba2972f5d23a3856d8" +
    "a981b9e9bbbbb88f35e708bde9cbc5f681cbd974085b9da28911296fe2579fa6" +
    "4bbe9fa0b93475a7a8db051080b0c5fade0d1c018e7858cd4cbe95145b0620e2" +
    "f632cbe0f8af9cbf22e2fdaa72245ae31b0877b07181cc69dd2df74454251d8d" +
    "e58d25e76354abe7eb690f22e59b08795a8f2c98c578e0599503d90859276340" +
    "72c82c9f82abd50fd12b8fd1a9d1954eb5cc0b4cfb5796b5aaec0356643b4a65" +
    "a368442d92ef94edd3ac6a2b7fe3571b8cf9f462729228aab023ef9183f73792" +
    "f5379633ccac51079177d604c6bc1873ada6f07d8da6d68c897e88a5fa5d63fd" +
    "b8df820f46090e0716e7562dd3c140ba279a65b996f60addb0abe29d4bf2f5ab" +
    "e89480771d492307b926d91f02f341b2148502903c43d40f3c6c86a811d06071" +
    "1f0698b384acdcc0add44eb54e42962d3d041accc715afd49407715adc09350c" +
    "b55e8d9281a3b0b6b5fcd91726eede9b7c8b13afdebb2c2b377629595f1096ba" +
    "62fb14946dbac5f3c5f0b4e5b712e7acc7dcf6c46cdc5e6d6dfdeee55a0c92c2" +
    "d70f080ac6"

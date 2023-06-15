package org.fdroid.database

import android.content.Context
import android.content.res.AssetManager
import android.os.Build
import androidx.core.os.LocaleListCompat
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import io.mockk.every
import io.mockk.mockkObject
import kotlinx.coroutines.Dispatchers
import org.fdroid.database.TestUtils.assertRepoEquals
import org.fdroid.database.TestUtils.toMetadataV2
import org.fdroid.database.TestUtils.toPackageVersionV2
import org.fdroid.index.v1.IndexV1StreamProcessor
import org.fdroid.index.v2.IndexV2
import org.fdroid.index.v2.IndexV2FullStreamProcessor
import org.fdroid.test.TestUtils.sort
import org.fdroid.test.TestUtils.sorted
import org.fdroid.test.VerifierConstants.CERTIFICATE
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import java.io.IOException
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.fail

internal abstract class DbTest {

    internal lateinit var repoDao: RepositoryDaoInt
    internal lateinit var appDao: AppDaoInt
    internal lateinit var appPrefsDao: AppPrefsDaoInt
    internal lateinit var versionDao: VersionDaoInt
    internal lateinit var db: FDroidDatabaseInt
    private val testCoroutineDispatcher = Dispatchers.Unconfined

    private val context: Context = getApplicationContext()
    protected val assets: AssetManager = context.resources.assets
    protected val locales = LocaleListCompat.create(Locale.US)

    @Before
    open fun createDb() {
        db = Room.inMemoryDatabaseBuilder(context, FDroidDatabaseInt::class.java)
            .allowMainThreadQueries()
            .build()
        repoDao = db.getRepositoryDao()
        appDao = db.getAppDao()
        appPrefsDao = db.getAppPrefsDao()
        versionDao = db.getVersionDao()

        // pre-Android P limitations for instrumentation tests (unit tests w/ robolectric are fine):
        // https://mockk.io/ANDROID#supported-features
        // See also: https://github.com/mockk/mockk/issues/182
        assumeTrue(Build.MODEL == "robolectric" || Build.VERSION.SDK_INT >= 28)

        mockkObject(FDroidDatabaseHolder)
        every { FDroidDatabaseHolder.dispatcher } returns testCoroutineDispatcher
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    protected fun streamIndexV1IntoDb(
        indexAssetPath: String,
        address: String = "https://f-droid.org/repo",
        certificate: String = CERTIFICATE,
        lastTimestamp: Long = -1,
    ): Long {
        val repoId = db.getRepositoryDao().insertEmptyRepo(address)
        val streamReceiver = DbV1StreamReceiver(db, repoId) { true }
        val indexProcessor = IndexV1StreamProcessor(streamReceiver, certificate, lastTimestamp)
        db.runInTransaction {
            assets.open(indexAssetPath).use { indexStream ->
                indexProcessor.process(indexStream)
            }
        }
        return repoId
    }

    protected fun streamIndexV2IntoDb(
        indexAssetPath: String,
        address: String = "https://f-droid.org/repo",
        version: Long = 42L,
        certificate: String = CERTIFICATE,
    ): Long {
        val repoId = db.getRepositoryDao().insertEmptyRepo(address)
        val streamReceiver = DbV2StreamReceiver(db, repoId) { true }
        val indexProcessor = IndexV2FullStreamProcessor(streamReceiver, certificate)
        db.runInTransaction {
            assets.open(indexAssetPath).use { indexStream ->
                indexProcessor.process(version, indexStream) {}
            }
        }
        return repoId
    }

    /**
     * Asserts that data associated with the given [repoId] is equal to the given [index].
     */
    protected fun assertDbEquals(repoId: Long, index: IndexV2) {
        val repo = repoDao.getRepository(repoId) ?: fail()
        val sortedIndex = index.sorted()
        assertRepoEquals(sortedIndex.repo, repo)
        assertEquals(sortedIndex.packages.size, appDao.countApps(), "number of packages")
        sortedIndex.packages.forEach { (packageName, packageV2) ->
            assertEquals(
                packageV2.metadata,
                appDao.getApp(repoId, packageName)?.toMetadataV2()?.sort()
            )
            val versions = versionDao.getAppVersions(repoId, packageName).map {
                it.toPackageVersionV2()
            }.associateBy { it.file.sha256 }
            assertEquals(packageV2.versions.size, versions.size, "number of versions")
            packageV2.versions.forEach { (versionId, packageVersionV2) ->
                val version = versions[versionId] ?: fail()
                assertEquals(packageVersionV2, version)
            }
        }
    }

}

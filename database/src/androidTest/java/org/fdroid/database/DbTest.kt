package org.fdroid.database

import android.content.Context
import android.content.res.AssetManager
import androidx.core.os.LocaleListCompat
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.setMain
import org.fdroid.database.test.TestUtils.assertRepoEquals
import org.fdroid.database.test.TestUtils.toMetadataV2
import org.fdroid.database.test.TestUtils.toPackageVersionV2
import org.fdroid.index.v2.IndexV2
import org.fdroid.test.TestUtils.sort
import org.fdroid.test.TestUtils.sorted
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import java.io.IOException
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.fail

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
internal abstract class DbTest {

    internal lateinit var repoDao: RepositoryDaoInt
    internal lateinit var appDao: AppDaoInt
    internal lateinit var versionDao: VersionDaoInt
    internal lateinit var db: FDroidDatabaseInt
    private val testCoroutineDispatcher = Dispatchers.Unconfined

    protected val context: Context = ApplicationProvider.getApplicationContext()
    protected val assets: AssetManager = context.resources.assets
    protected val locales = LocaleListCompat.create(Locale.US)

    @Before
    open fun createDb() {
        db = Room.inMemoryDatabaseBuilder(context, FDroidDatabaseInt::class.java).build()
        repoDao = db.getRepositoryDao()
        appDao = db.getAppDao()
        versionDao = db.getVersionDao()

        Dispatchers.setMain(testCoroutineDispatcher)

        mockkObject(FDroidDatabaseHolder)
        every { FDroidDatabaseHolder.dispatcher } returns testCoroutineDispatcher
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
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

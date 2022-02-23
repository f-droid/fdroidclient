package org.fdroid.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.fdroid.index.v2.RepoV2
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
abstract class DbTest {

    internal lateinit var repoDao: RepositoryDaoInt
    private lateinit var db: FDroidDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FDroidDatabase::class.java).build()
        repoDao = db.getRepositoryDaoInt()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    protected fun assertRepoEquals(repoV2: RepoV2, repo: Repository) {
        val repoId = repo.repository.repoId
        // mirrors
        val expectedMirrors = repoV2.mirrors.map { it.toMirror(repoId) }.toSet()
        Assert.assertEquals(expectedMirrors, repo.mirrors.toSet())
        // anti-features
        val expectedAntiFeatures = repoV2.antiFeatures.toRepoAntiFeatures(repoId).toSet()
        Assert.assertEquals(expectedAntiFeatures, repo.antiFeatures.toSet())
        // categories
        val expectedCategories = repoV2.categories.toRepoCategories(repoId).toSet()
        Assert.assertEquals(expectedCategories, repo.categories.toSet())
        // release channels
        val expectedReleaseChannels = repoV2.releaseChannels.toRepoReleaseChannel(repoId).toSet()
        Assert.assertEquals(expectedReleaseChannels, repo.releaseChannels.toSet())
        // core repo
        val coreRepo = repoV2.toCoreRepository().copy(repoId = repoId)
        Assert.assertEquals(coreRepo, repo.repository)
    }

}

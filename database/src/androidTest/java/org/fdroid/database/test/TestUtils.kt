package org.fdroid.database.test

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import org.fdroid.database.Repository
import org.fdroid.database.toCoreRepository
import org.fdroid.database.toMirror
import org.fdroid.database.toRepoAntiFeatures
import org.fdroid.database.toRepoCategories
import org.fdroid.database.toRepoReleaseChannel
import org.fdroid.index.v2.RepoV2
import org.junit.Assert
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

object TestUtils {

    fun assertRepoEquals(repoV2: RepoV2, repo: Repository) {
        val repoId = repo.repoId
        // mirrors
        val expectedMirrors = repoV2.mirrors.map { it.toMirror(repoId) }.toSet()
        Assert.assertEquals(expectedMirrors, repo.mirrors.toSet())
        // anti-features
        val expectedAntiFeatures = repoV2.antiFeatures.toRepoAntiFeatures(repoId).toSet()
        assertEquals(expectedAntiFeatures, repo.antiFeatures.toSet())
        // categories
        val expectedCategories = repoV2.categories.toRepoCategories(repoId).toSet()
        assertEquals(expectedCategories, repo.categories.toSet())
        // release channels
        val expectedReleaseChannels = repoV2.releaseChannels.toRepoReleaseChannel(repoId).toSet()
        assertEquals(expectedReleaseChannels, repo.releaseChannels.toSet())
        // core repo
        val coreRepo = repoV2.toCoreRepository(version = repo.repository.version!!)
            .copy(repoId = repoId)
        assertEquals(coreRepo, repo.repository)
    }

    fun <T> LiveData<T>.getOrAwaitValue(): T? {
        val data = arrayOfNulls<Any>(1)
        val latch = CountDownLatch(1)
        val observer: Observer<T> = object : Observer<T> {
            override fun onChanged(o: T?) {
                data[0] = o
                latch.countDown()
                removeObserver(this)
            }
        }
        observeForever(observer)
        latch.await(2, TimeUnit.SECONDS)
        @Suppress("UNCHECKED_CAST")
        return data[0] as T?
    }

}

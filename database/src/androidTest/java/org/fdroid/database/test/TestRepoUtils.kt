package org.fdroid.database.test

import org.fdroid.database.Repository
import org.fdroid.database.test.TestUtils.orNull
import org.fdroid.database.toCoreRepository
import org.fdroid.database.toMirror
import org.fdroid.database.toRepoAntiFeatures
import org.fdroid.database.toRepoCategories
import org.fdroid.database.toRepoReleaseChannel
import org.fdroid.index.v2.AntiFeatureV2
import org.fdroid.index.v2.CategoryV2
import org.fdroid.index.v2.FileV2
import org.fdroid.index.v2.LocalizedTextV2
import org.fdroid.index.v2.MirrorV2
import org.fdroid.index.v2.ReleaseChannelV2
import org.fdroid.index.v2.RepoV2
import org.junit.Assert
import kotlin.random.Random

object TestRepoUtils {

    fun getRandomMirror() = MirrorV2(
        url = TestUtils.getRandomString(),
        location = TestUtils.getRandomString().orNull()
    )

    fun getRandomLocalizedTextV2(size: Int = Random.nextInt(0, 23)): LocalizedTextV2 = buildMap {
        repeat(size) {
            put(TestUtils.getRandomString(4), TestUtils.getRandomString())
        }
    }

    fun getRandomFileV2(sha256Nullable: Boolean = true) = FileV2(
        name = TestUtils.getRandomString(),
        sha256 = TestUtils.getRandomString(64).also { if (sha256Nullable) orNull() },
        size = Random.nextLong(-1, Long.MAX_VALUE)
    )

    fun getRandomLocalizedFileV2() = TestUtils.getRandomMap(Random.nextInt(1, 8)) {
        TestUtils.getRandomString(4) to getRandomFileV2()
    }

    fun getRandomRepo() = RepoV2(
        name = TestUtils.getRandomString(),
        icon = getRandomFileV2(),
        address = TestUtils.getRandomString(),
        description = getRandomLocalizedTextV2(),
        mirrors = TestUtils.getRandomList { getRandomMirror() },
        timestamp = System.currentTimeMillis(),
        antiFeatures = TestUtils.getRandomMap {
            TestUtils.getRandomString() to AntiFeatureV2(getRandomFileV2(), getRandomLocalizedTextV2())
        },
        categories = TestUtils.getRandomMap {
            TestUtils.getRandomString() to CategoryV2(getRandomFileV2(), getRandomLocalizedTextV2())
        },
        releaseChannels = TestUtils.getRandomMap {
            TestUtils.getRandomString() to ReleaseChannelV2(getRandomLocalizedTextV2())
        },
    )

    internal fun assertRepoEquals(repoV2: RepoV2, repo: Repository) {
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

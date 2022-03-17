package org.fdroid.database.test

import org.fdroid.database.Repository
import org.fdroid.database.test.TestUtils.getRandomList
import org.fdroid.database.test.TestUtils.getRandomString
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
import org.junit.Assert.assertEquals
import kotlin.random.Random

object TestRepoUtils {

    fun getRandomMirror() = MirrorV2(
        url = getRandomString(),
        location = getRandomString().orNull()
    )

    fun getRandomLocalizedTextV2(size: Int = Random.nextInt(0, 23)): LocalizedTextV2 = buildMap {
        repeat(size) {
            put(getRandomString(4), getRandomString())
        }
    }

    fun getRandomFileV2(sha256Nullable: Boolean = true) = FileV2(
        name = getRandomString(),
        sha256 = getRandomString(64).also { if (sha256Nullable) orNull() },
        size = Random.nextLong(-1, Long.MAX_VALUE)
    )

    fun getRandomLocalizedFileV2() = TestUtils.getRandomMap(Random.nextInt(1, 8)) {
        getRandomString(4) to getRandomFileV2()
    }

    fun getRandomRepo() = RepoV2(
        name = getRandomString(),
        icon = getRandomFileV2(),
        address = getRandomString(),
        description = getRandomLocalizedTextV2(),
        mirrors = getRandomList { getRandomMirror() },
        timestamp = System.currentTimeMillis(),
        antiFeatures = TestUtils.getRandomMap {
            getRandomString() to AntiFeatureV2(
                icon = getRandomFileV2(),
                name = getRandomLocalizedTextV2(),
                description = getRandomLocalizedTextV2(),
            )
        },
        categories = TestUtils.getRandomMap {
            getRandomString() to CategoryV2(
                icon = getRandomFileV2(),
                name = getRandomLocalizedTextV2(),
                description = getRandomLocalizedTextV2(),
            )
        },
        releaseChannels = TestUtils.getRandomMap {
            getRandomString() to ReleaseChannelV2(
                name = getRandomLocalizedTextV2(),
                description = getRandomLocalizedTextV2(),
            )
        },
    )

    internal fun assertRepoEquals(repoV2: RepoV2, repo: Repository) {
        val repoId = repo.repoId
        // mirrors
        val expectedMirrors = repoV2.mirrors.map { it.toMirror(repoId) }.toSet()
        assertEquals(expectedMirrors, repo.mirrors.toSet())
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
        val coreRepo = repoV2.toCoreRepository().copy(repoId = repoId)
        assertEquals(coreRepo, repo.repository)
    }

}

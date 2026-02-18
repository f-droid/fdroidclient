package org.fdroid.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.fdroid.database.TestUtils.assertRepoEquals
import org.fdroid.index.v2.AntiFeatureV2
import org.fdroid.index.v2.CategoryV2
import org.fdroid.index.v2.MirrorV2
import org.fdroid.index.v2.ReleaseChannelV2
import org.fdroid.index.v2.RepoV2
import org.fdroid.test.DiffUtils.applyDiff
import org.fdroid.test.DiffUtils.randomDiff
import org.fdroid.test.TestRepoUtils.getRandomLocalizedFileV2
import org.fdroid.test.TestRepoUtils.getRandomLocalizedTextV2
import org.fdroid.test.TestRepoUtils.getRandomMirror
import org.fdroid.test.TestRepoUtils.getRandomRepo
import org.fdroid.test.TestUtils.getRandomList
import org.fdroid.test.TestUtils.getRandomMap
import org.fdroid.test.TestUtils.getRandomString
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests that repository diffs get applied to the database correctly.
 */
@RunWith(AndroidJUnit4::class)
internal class RepositoryDiffTest : DbTest() {

    private val j = Json

    @Test
    fun timestampDiff() {
        val repo = getRandomRepo()
        val updateTimestamp = repo.timestamp + 1
        val json = """
            {
              "timestamp": $updateTimestamp
            }""".trimIndent()
        testDiff(repo, json) { repos ->
            assertEquals(updateTimestamp, repos[0].timestamp)
            assertRepoEquals(repo.copy(timestamp = updateTimestamp), repos[0])
        }
    }

    @Test
    fun timestampDiffTwoReposInDb() {
        // insert repo
        val repo = getRandomRepo()
        repoDao.insertOrReplace(repo)

        // insert another repo before updating
        repoDao.insertOrReplace(getRandomRepo())

        // check that the repo got added and retrieved as expected
        var repos = repoDao.getRepositories().sortedBy { it.repoId }
        assertEquals(2, repos.size)
        val repoId = repos[0].repoId

        val updateTimestamp = Random.nextLong()
        val json = """
            {
              "timestamp": $updateTimestamp,
              "unknown": "field"
            }""".trimIndent()

        // decode diff from JSON and update DB with it
        val diff = j.parseToJsonElement(json).jsonObject // Json.decodeFromString<RepoDiffV2>(json)
        repoDao.updateRepository(repoId, 42, diff)

        // fetch repos again and check that the result is as expected
        repos = repoDao.getRepositories().sortedBy { it.repoId }
        assertEquals(2, repos.size)
        assertEquals(repoId, repos[0].repoId)
        assertEquals(updateTimestamp, repos[0].timestamp)
        assertRepoEquals(repo.copy(timestamp = updateTimestamp), repos[0])
    }

    @Test
    fun mirrorDiff() {
        val repo = getRandomRepo()
        val updateMirrors = repo.mirrors.toMutableList().apply {
            removeLastOrNull()
            add(getRandomMirror())
            add(getRandomMirror())
        }
        val json = """
            {
              "mirrors": ${Json.encodeToString(updateMirrors)}
            }""".trimIndent()
        testDiff(repo, json) { repos ->
            val expectedMirrors = updateMirrors.map { mirror ->
                mirror.toMirror(repos[0].repoId)
            }.toSet()
            assertEquals(expectedMirrors, repos[0].mirrors.toSet())
            assertRepoEquals(repo.copy(mirrors = updateMirrors), repos[0])
        }
    }

    @Test
    fun mirrorUnknownKeyDiff() {
        val repo = getRandomRepo()
        val json = """
            {
              "mirrors": [
                { "url": "foo", "countryCode": "bar", "unknown": "doesntexist" }
              ]
            }""".trimIndent()
        testDiff(repo, json) { repos ->
            val expectedMirrors = setOf(Mirror(repos[0].repoId, "foo", "bar"))
            assertEquals(expectedMirrors, repos[0].mirrors.toSet())
            assertRepoEquals(repo.copy(mirrors = listOf(MirrorV2("foo", "bar"))), repos[0])
        }
    }

    @Test
    fun descriptionDiff() {
        val repo = getRandomRepo().copy(description = mapOf("de" to "foo", "en" to "bar"))
        val updateText = if (Random.nextBoolean()) mapOf("de" to null, "en" to "foo") else null
        val json = """
            {
              "description": ${Json.encodeToString(updateText)}
            }""".trimIndent()
        val expectedText = if (updateText == null) emptyMap() else mapOf("en" to "foo")
        testDiff(repo, json) { repos ->
            assertEquals(expectedText, repos[0].repository.description)
            assertRepoEquals(repo.copy(description = expectedText), repos[0])
        }
    }

    @Test
    fun antiFeaturesDiff() {
        val repo = getRandomRepo().copy(antiFeatures = getRandomMap {
            getRandomString() to AntiFeatureV2(
                icon = getRandomLocalizedFileV2(),
                name = getRandomLocalizedTextV2(),
                description = getRandomLocalizedTextV2(),
            )
        })
        val antiFeatures = repo.antiFeatures.randomDiff {
            AntiFeatureV2(
                icon = getRandomLocalizedFileV2(),
                name = getRandomLocalizedTextV2(),
                description = getRandomLocalizedTextV2(),
            )
        }
        val json = """
            {
              "antiFeatures": ${Json.encodeToString(antiFeatures)}
            }""".trimIndent()
        testDiff(repo, json) { repos ->
            val expectedFeatures = repo.antiFeatures.applyDiff(antiFeatures)
            val expectedRepoAntiFeatures =
                expectedFeatures.toRepoAntiFeatures(repos[0].repoId)
            assertEquals(expectedRepoAntiFeatures.toSet(), repos[0].antiFeatures.toSet())
            assertRepoEquals(repo.copy(antiFeatures = expectedFeatures), repos[0])
        }
    }

    @Test
    fun antiFeatureKeyChangeDiff() {
        val antiFeatureKey = getRandomString()
        val antiFeature = AntiFeatureV2(
            icon = getRandomLocalizedFileV2(),
            name = getRandomLocalizedTextV2(),
            description = getRandomLocalizedTextV2(),
        )
        val antiFeatures = mapOf(antiFeatureKey to antiFeature)
        val repo = getRandomRepo().copy(antiFeatures = antiFeatures)

        @Suppress("UNCHECKED_CAST")
        val newAntiFeatures = mapOf(
            antiFeatureKey to antiFeature.copy(
                icon = emptyMap(),
                name = getRandomLocalizedTextV2(),
                description = getRandomLocalizedTextV2(),
            )
        )
        val json = """
            {
              "antiFeatures": {
                  "$antiFeatureKey": ${Json.encodeToString(newAntiFeatures)}
              }
            }""".trimIndent()
        testDiff(repo, json) { repos ->
            val expectedFeatures = repo.antiFeatures.applyDiff(antiFeatures)
            val expectedRepoAntiFeatures =
                expectedFeatures.toRepoAntiFeatures(repos[0].repoId)
            assertEquals(expectedRepoAntiFeatures.toSet(), repos[0].antiFeatures.toSet())
            assertRepoEquals(repo.copy(antiFeatures = expectedFeatures), repos[0])
        }
    }

    @Test
    fun antiFeaturesRemovedOptionalsDiff() {
        val antiFeatureKey = getRandomString()
        val antiFeature = AntiFeatureV2(
            icon = getRandomLocalizedFileV2(),
            name = getRandomLocalizedTextV2(),
            description = getRandomLocalizedTextV2(),
        )
        val antiFeatures = mapOf(antiFeatureKey to antiFeature)
        val repo = getRandomRepo().copy(antiFeatures = antiFeatures)
        val json = """
            {
              "antiFeatures": {
                  "$antiFeatureKey": {
                      "icon": null,
                      "description": null
                  }
              }
            }""".trimIndent()
        testDiff(repo, json) { repos ->
            // icon and description maps were emptied (not nulled as they aren't nullable)
            assertEquals(emptyMap(), repos[0].getAntiFeatures()[antiFeatureKey]!!.icon)
            assertEquals(emptyMap(), repos[0].getAntiFeatures()[antiFeatureKey]!!.description)
        }
    }

    @Test
    fun categoriesDiff() {
        val repo = getRandomRepo().copy(categories = getRandomMap {
            getRandomString() to CategoryV2(
                icon = getRandomLocalizedFileV2(),
                name = getRandomLocalizedTextV2(),
                description = getRandomLocalizedTextV2(),
            )
        })
        val categories = repo.categories.randomDiff {
            CategoryV2(
                icon = getRandomLocalizedFileV2(),
                name = getRandomLocalizedTextV2(),
                description = getRandomLocalizedTextV2(),
            )
        }
        val json = """
            {
              "categories": ${Json.encodeToString(categories)}
            }""".trimIndent()
        testDiff(repo, json) { repos ->
            val expectedCategories = repo.categories.applyDiff(categories)
            val expectedRepoCategories =
                expectedCategories.toRepoCategories(repos[0].repoId)
            assertEquals(expectedRepoCategories.toSet(), repos[0].categories.toSet())
            assertRepoEquals(repo.copy(categories = expectedCategories), repos[0])
        }
    }

    @Test
    fun categoriesRemovedOptionalsDiff() {
        val categoryId = getRandomString()
        val repo = getRandomRepo().copy(categories = getRandomMap {
            categoryId to CategoryV2(
                icon = getRandomLocalizedFileV2(),
                name = getRandomLocalizedTextV2(),
                description = getRandomLocalizedTextV2(),
            )
        })
        val json = """
            {
              "categories": {
                  "$categoryId": {
                      "icon": null,
                      "description": null
                  }
              }
            }""".trimIndent()
        testDiff(repo, json) { repos ->
            // icon and description maps were emptied (not nulled as they aren't nullable)
            assertEquals(emptyMap(), repos[0].getCategories()[categoryId]!!.icon)
            assertEquals(emptyMap(), repos[0].getCategories()[categoryId]!!.description)
        }
    }

    @Test
    fun releaseChannelsDiff() {
        val repo = getRandomRepo().copy(releaseChannels = getRandomMap {
            getRandomString() to ReleaseChannelV2(
                name = getRandomLocalizedTextV2(),
                description = getRandomLocalizedTextV2(),
            )
        })
        val releaseChannels = repo.releaseChannels.randomDiff {
            ReleaseChannelV2(getRandomLocalizedTextV2(), getRandomLocalizedTextV2())
        }
        val json = """
            {
              "releaseChannels": ${Json.encodeToString(releaseChannels)}
            }""".trimIndent()
        testDiff(repo, json) { repos ->
            val expectedFeatures = repo.releaseChannels.applyDiff(releaseChannels)
            val expectedRepoReleaseChannels =
                expectedFeatures.toRepoReleaseChannel(repos[0].repoId)
            assertEquals(expectedRepoReleaseChannels.toSet(), repos[0].releaseChannels.toSet())
            assertRepoEquals(repo.copy(releaseChannels = expectedFeatures), repos[0])
        }
    }

    @Test
    fun releaseChannelsRemovedOptionalsDiff() {
        val releaseChannelKey = getRandomString()
        val repo = getRandomRepo().copy(releaseChannels = getRandomMap {
            releaseChannelKey to ReleaseChannelV2(
                name = getRandomLocalizedTextV2(),
                description = getRandomLocalizedTextV2(),
            )
        })
        val json = """
            {
              "releaseChannels": {
                  "$releaseChannelKey": {
                      "description": null,
                      "icon": null
                  }
              }
            }""".trimIndent()
        testDiff(repo, json) { repos ->
            // icon and description maps were emptied (not nulled as they aren't nullable)
            assertEquals(emptyMap(), repos[0].getReleaseChannels()[releaseChannelKey]!!.icon)
            assertEquals(emptyMap(), repos[0].getReleaseChannels()[releaseChannelKey]!!.description)
        }
    }

    @Test
    fun removeAllOptionalsDiff() {
        val repo = getRandomRepo().copy(
            // ensure optional values exist
            name = getRandomLocalizedTextV2(2),
            webBaseUrl = getRandomString(),
            mirrors = getRandomList(2) { getRandomMirror() },
            antiFeatures = getRandomMap(2) {
                getRandomString() to AntiFeatureV2(
                    icon = getRandomLocalizedFileV2(),
                    name = getRandomLocalizedTextV2(),
                    description = getRandomLocalizedTextV2(),
                )
            },
            categories = getRandomMap(2) {
                getRandomString() to CategoryV2(
                    icon = getRandomLocalizedFileV2(),
                    name = getRandomLocalizedTextV2(),
                    description = getRandomLocalizedTextV2(),
                )
            },
            releaseChannels = getRandomMap(2) {
                getRandomString() to ReleaseChannelV2(
                    name = getRandomLocalizedTextV2(),
                    description = getRandomLocalizedTextV2(),
                )
            },
        )
        assertTrue(repo.name.isNotEmpty())
        assertTrue(repo.icon.isNotEmpty())
        assertTrue(!repo.webBaseUrl.isNullOrEmpty())
        assertTrue(repo.mirrors.isNotEmpty())
        assertTrue(repo.antiFeatures.isNotEmpty())
        assertTrue(repo.categories.isNotEmpty())
        assertTrue(repo.releaseChannels.isNotEmpty())
        val json = """
            {
              "name": null,
              "icon": null,
              "webBaseUrl": null,
              "mirrors": null,
              "antiFeatures": null,
              "categories": null,
              "releaseChannels": null
            }""".trimIndent()
        testDiff(repo, json) { repos ->
            assertEquals(emptyMap(), repos[0].repository.name)
            assertEquals(null, repos[0].repository.icon)
            assertEquals(null, repos[0].webBaseUrl)
            assertEquals(emptyList(), repos[0].mirrors)
            assertEquals(emptyMap(), repos[0].getAntiFeatures())
            assertEquals(emptyMap(), repos[0].getCategories())
            assertEquals(emptyMap(), repos[0].getReleaseChannels())
        }
    }

    private fun testDiff(repo: RepoV2, json: String, repoChecker: (List<Repository>) -> Unit) {
        // insert repo
        repoDao.insertOrReplace(repo)

        // check that the repo got added and retrieved as expected
        var repos = repoDao.getRepositories()
        assertEquals(1, repos.size)
        val repoId = repos[0].repoId

        // decode diff from JSON and update DB with it
        val diff = j.parseToJsonElement(json).jsonObject
        repoDao.updateRepository(repoId, 42, diff)

        // fetch repos again and check that the result is as expected
        repos = repoDao.getRepositories().sortedBy { it.repoId }
        assertEquals(1, repos.size)
        assertEquals(repoId, repos[0].repoId)
        repoChecker(repos)
    }

}

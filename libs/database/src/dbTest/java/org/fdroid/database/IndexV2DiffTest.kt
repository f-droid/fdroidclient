package org.fdroid.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.serialization.SerializationException
import org.fdroid.index.IndexParser
import org.fdroid.index.parseV2
import org.fdroid.index.v2.IndexV2
import org.fdroid.index.v2.IndexV2DiffStreamProcessor
import org.fdroid.test.TestDataMaxV2
import org.fdroid.test.TestDataMidV2
import org.fdroid.test.TestDataMinV2
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlin.test.assertFailsWith

@RunWith(AndroidJUnit4::class)
internal class IndexV2DiffTest : DbTest() {

    @Test
    @Ignore("use for testing specific index on demand")
    fun testBrokenIndexDiff() {
        val endPath = "tmp/index-end.json"
        val endIndex = IndexParser.parseV2(assets.open(endPath))
        testDiff(
            startPath = "tmp/index-start.json",
            diffPath = "tmp/diff.json",
            endIndex = endIndex,
        )
    }

    @Test
    fun testEmptyToMin() = testDiff(
        startPath = "index-empty-v2.json",
        diffPath = "diff-empty-min/23.json",
        endIndex = TestDataMinV2.index,
    )

    @Test
    fun testEmptyToMid() = testDiff(
        startPath = "index-empty-v2.json",
        diffPath = "diff-empty-mid/23.json",
        endIndex = TestDataMidV2.index,
    )

    @Test
    fun testEmptyToMax() = testDiff(
        startPath = "index-empty-v2.json",
        diffPath = "diff-empty-max/23.json",
        endIndex = TestDataMaxV2.index,
    )

    @Test
    fun testMinToMid() = testDiff(
        startPath = "index-min-v2.json",
        diffPath = "diff-empty-mid/42.json",
        endIndex = TestDataMidV2.index,
    )

    @Test
    fun testMinToMax() = testDiff(
        startPath = "index-min-v2.json",
        diffPath = "diff-empty-max/42.json",
        endIndex = TestDataMaxV2.index,
    )

    @Test
    fun testMidToMax() = testDiff(
        startPath = "index-mid-v2.json",
        diffPath = "diff-empty-max/1337.json",
        endIndex = TestDataMaxV2.index,
    )

    @Test
    fun testMinRemoveApp() {
        val diffJson = """{
          "packages": {
            "org.fdroid.min1": null
          }
        }""".trimIndent()
        testJsonDiff(
            startPath = "index-min-v2.json",
            diff = diffJson,
            endIndex = TestDataMinV2.index.copy(packages = emptyMap()),
        )
    }

    @Test
    fun testMinNoMetadataRemoveVersion() {
        val diffJson = """{
          "packages": {
            "org.fdroid.min1": {
              "metadata": {
                "added": 0
              },
              "versions": {
                "824a109b2352138c3699760e1683385d0ed50ce526fc7982f8d65757743374bf": null
              }
            }
          }
        }""".trimIndent()
        testJsonDiff(
            startPath = "index-min-v2.json",
            diff = diffJson,
            endIndex = TestDataMinV2.index.copy(
                packages = TestDataMinV2.index.packages.mapValues {
                    it.value.copy(versions = emptyMap())
                }
            ),
        )
    }

    @Test
    fun testMinNoVersionsUnknownKey() {
        val diffJson = """{
          "packages": {
            "org.fdroid.min1": {
              "metadata": {
                "added": 42
              },
              "unknownKey": "should get ignored" 
            }
          }
        }""".trimIndent()
        testJsonDiff(
            startPath = "index-min-v2.json",
            diff = diffJson,
            endIndex = TestDataMinV2.index.copy(
                packages = TestDataMinV2.index.packages.mapValues {
                    it.value.copy(metadata = it.value.metadata.copy(added = 42))
                }
            ),
        )
    }

    @Test
    fun testMinRemoveMetadata() {
        val diffJson = """{
          "packages": {
            "org.fdroid.min1": {
              "metadata": null
            }
          },
          "unknownKey": "should get ignored" 
        }""".trimIndent()
        testJsonDiff(
            startPath = "index-min-v2.json",
            diff = diffJson,
            endIndex = TestDataMinV2.index.copy(
                packages = emptyMap()
            ),
        )
    }

    @Test
    fun testMinRemoveVersions() {
        val diffJson = """{
          "packages": {
            "org.fdroid.min1": {
              "versions": null
            }
          }
        }""".trimIndent()
        testJsonDiff(
            startPath = "index-min-v2.json",
            diff = diffJson,
            endIndex = TestDataMinV2.index.copy(
                packages = TestDataMinV2.index.packages.mapValues {
                    it.value.copy(versions = emptyMap())
                }
            ),
        )
    }

    @Test
    fun testMinNoMetadataNoVersion() {
        val diffJson = """{
          "packages": {
            "org.fdroid.min1": {
            }
          }
        }""".trimIndent()
        testJsonDiff(
            startPath = "index-min-v2.json",
            diff = diffJson,
            endIndex = TestDataMinV2.index,
        )
    }

    @Test
    fun testAppDenyKeyList() {
        val diffRepoIdJson = """{
          "packages": {
            "org.fdroid.min1": {
              "metadata": {
                "repoId": 1
              }
            }
          }
        }""".trimIndent()
        assertFailsWith<SerializationException> {
            testJsonDiff(
                startPath = "index-min-v2.json",
                diff = diffRepoIdJson,
                endIndex = TestDataMinV2.index,
            )
        }
        val diffPackageNameJson = """{
          "packages": {
            "org.fdroid.min1": {
              "metadata": {
                "packageName": "foo"
              }
            }
          }
        }""".trimIndent()
        assertFailsWith<SerializationException> {
            testJsonDiff(
                startPath = "index-min-v2.json",
                diff = diffPackageNameJson,
                endIndex = TestDataMinV2.index,
            )
        }
    }

    @Test
    fun testVersionsDenyKeyList() {
        assertFailsWith<SerializationException> {
            testJsonDiff(
                startPath = "index-min-v2.json",
                diff = getMinVersionJson(""""packageName": "foo""""),
                endIndex = TestDataMinV2.index,
            )
        }
        assertFailsWith<SerializationException> {
            testJsonDiff(
                startPath = "index-min-v2.json",
                diff = getMinVersionJson(""""repoId": 1"""),
                endIndex = TestDataMinV2.index,
            )
        }
        assertFailsWith<SerializationException> {
            testJsonDiff(
                startPath = "index-min-v2.json",
                diff = getMinVersionJson(""""versionId": "bar""""),
                endIndex = TestDataMinV2.index,
            )
        }
    }

    private fun getMinVersionJson(insert: String) = """{
      "packages": {
        "org.fdroid.min1": {
          "versions": {
            "824a109b2352138c3699760e1683385d0ed50ce526fc7982f8d65757743374bf": {
              $insert
            }
        }
      }
    }""".trimIndent()

    @Test
    fun testMidRemoveScreenshots() {
        val diffRepoIdJson = """{
          "packages": {
            "org.fdroid.fdroid": {
              "metadata": {
                "screenshots": null
              }
            }
          }
        }""".trimIndent()
        val fdroidPackage = TestDataMidV2.packages["org.fdroid.fdroid"]!!.copy(
            metadata = TestDataMidV2.packages["org.fdroid.fdroid"]!!.metadata.copy(
                screenshots = null,
            )
        )
        testJsonDiff(
            startPath = "index-mid-v2.json",
            diff = diffRepoIdJson,
            endIndex = TestDataMidV2.index.copy(
                packages = mapOf(
                    TestDataMidV2.packageName1 to TestDataMidV2.app1,
                    TestDataMidV2.packageName2 to fdroidPackage,
                )
            ),
        )
    }

    private fun testJsonDiff(startPath: String, diff: String, endIndex: IndexV2) {
        testDiff(startPath, ByteArrayInputStream(diff.toByteArray()), endIndex)
    }

    private fun testDiff(startPath: String, diffPath: String, endIndex: IndexV2) {
        testDiff(startPath, assets.open(diffPath), endIndex)
    }

    private fun testDiff(startPath: String, diffStream: InputStream, endIndex: IndexV2) {
        // stream start index into the DB
        val repoId = streamIndexV2IntoDb(startPath)

        // apply diff stream to the DB
        val streamReceiver = DbV2DiffStreamReceiver(db, repoId) { true }
        val streamProcessor = IndexV2DiffStreamProcessor(streamReceiver)
        db.runInTransaction {
            streamProcessor.process(42, diffStream) {}
        }
        // assert that changed DB data is equal to given endIndex
        assertDbEquals(repoId, endIndex)
    }

}

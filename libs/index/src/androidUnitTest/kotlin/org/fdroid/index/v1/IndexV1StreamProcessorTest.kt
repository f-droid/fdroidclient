package org.fdroid.index.v1

import kotlinx.serialization.SerializationException
import org.fdroid.index.assetPath
import org.fdroid.index.v2.AntiFeatureV2
import org.fdroid.index.v2.CategoryV2
import org.fdroid.index.v2.IndexV2
import org.fdroid.index.v2.MetadataV2
import org.fdroid.index.v2.PackageV2
import org.fdroid.index.v2.PackageVersionV2
import org.fdroid.index.v2.ReleaseChannelV2
import org.fdroid.index.v2.RepoV2
import org.fdroid.test.TestDataEmptyV2
import org.fdroid.test.TestDataMaxV2
import org.fdroid.test.TestDataMidV2
import org.fdroid.test.TestDataMinV2
import org.fdroid.test.v1compat
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.fail

internal class IndexV1StreamProcessorTest {

    @Test
    fun testEmpty() {
        testStreamProcessing("$assetPath/index-empty-v1.json", TestDataEmptyV2.index.v1compat())
    }

    @Test(expected = OldIndexException::class)
    fun testEmptyEqualTimestamp() {
        testStreamProcessing("$assetPath/index-empty-v1.json",
            TestDataEmptyV2.index.v1compat(), TestDataEmptyV2.index.repo.timestamp)
    }

    @Test(expected = OldIndexException::class)
    fun testEmptyHigherTimestamp() {
        testStreamProcessing("$assetPath/index-empty-v1.json",
            TestDataEmptyV2.index.v1compat(), TestDataEmptyV2.index.repo.timestamp + 1)
    }

    @Test
    fun testMin() {
        testStreamProcessing("$assetPath/index-min-v1.json", TestDataMinV2.index.v1compat())
    }

    @Test
    fun testMid() {
        testStreamProcessing("$assetPath/index-mid-v1.json", TestDataMidV2.indexCompat)
    }

    @Test
    fun testMax() {
        testStreamProcessing("$assetPath/index-max-v1.json", TestDataMaxV2.indexCompat)
    }

    @Test
    fun testMalformedIndex() {
        // empty dict
        assertFailsWith<IllegalArgumentException> {
            testStreamError("{ }")
        }.also { assertContains(it.message!!, "Failed requirement") }

        // garbage input
        assertFailsWith<SerializationException> {
            testStreamError("{ 23^^%*dfDFG568 }")
        }

        // empty repo dict
        assertFailsWith<SerializationException> {
            testStreamError("""{
                "repo": {}
            }""".trimIndent())
        }.also { assertContains(it.message!!, "timestamp") }

        // timestamp not a number
        assertFailsWith<SerializationException> {
            testStreamError("""{
                "repo": { "timestamp": "string" }
            }""".trimIndent())
        }.also { assertContains(it.message!!, "numeric literal") }

        // remember valid repo for further tests
        val validRepo = """
            "repo": {
                    "timestamp": 42,
                    "version": 23,
                    "name": "foo",
                    "icon": "bar",
                    "address": "https://example.com",
                    "description": "desc"
                }
        """.trimIndent()

        // apps is dict
        assertFailsWith<SerializationException> {
            testStreamError("""{
                $validRepo,
                "requests": {"install": [], "uninstall": []},
                "apps": {}
            }""".trimIndent())
        }.also { assertContains(it.message!!, "apps") }

        // packages is list
        assertFailsWith<SerializationException> {
            testStreamError("""{
                $validRepo,
                "requests": {"install": [], "uninstall": []},
                "apps": [],
                "packages": []
            }""".trimIndent())
        }.also { assertContains(it.message!!, "packages") }

    }

    private fun testStreamProcessing(
        filePath: String,
        indexV2: IndexV2,
        lastTimestamp: Long = indexV2.repo.timestamp - 1,
    ) {
        val file = File(filePath)
        val testStreamReceiver = TestStreamReceiver()
        val streamProcessor = IndexV1StreamProcessor(testStreamReceiver, null, lastTimestamp)
        FileInputStream(file).use { streamProcessor.process(it) }
        assertEquals(indexV2.repo, testStreamReceiver.repo)
        assertEquals(indexV2.packages, testStreamReceiver.packages)
    }

    private fun testStreamError(index: String) {
        val testStreamReceiver = TestStreamReceiver()
        val streamProcessor = IndexV1StreamProcessor(testStreamReceiver, null, -1)
        ByteArrayInputStream(index.encodeToByteArray()).use { streamProcessor.process(it) }
        assertNull(testStreamReceiver.repo)
        assertEquals(0, testStreamReceiver.packages.size)
    }

    @Suppress("DEPRECATION")
    private class TestStreamReceiver : IndexV1StreamReceiver {
        var repo: RepoV2? = null
        val packages = HashMap<String, PackageV2>()

        override fun receive(repo: RepoV2, version: Long, certificate: String?) {
            this.repo = repo
        }

        override fun receive(packageName: String, m: MetadataV2) {
            packages[packageName] = PackageV2(
                metadata = m,
                versions = emptyMap(),
            )
        }

        override fun receive(packageName: String, v: Map<String, PackageVersionV2>) {
            packages[packageName] = packages[packageName]!!.copy(versions = v)
        }

        override fun updateRepo(
            antiFeatures: Map<String, AntiFeatureV2>,
            categories: Map<String, CategoryV2>,
            releaseChannels: Map<String, ReleaseChannelV2>,
        ) {
            repo = repo?.copy(
                antiFeatures = antiFeatures,
                categories = categories,
                releaseChannels = releaseChannels,
            ) ?: fail()
        }

        override fun updateAppMetadata(packageName: String, preferredSigner: String?) {
            val currentPackage = packages[packageName] ?: fail()
            packages[packageName] = currentPackage.copy(
                metadata = currentPackage.metadata.copy(preferredSigner = preferredSigner),
            )
        }
    }

}

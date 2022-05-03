package org.fdroid.index.v1

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
import java.io.File
import java.io.FileInputStream
import kotlin.test.assertEquals
import kotlin.test.fail

internal class IndexV1StreamProcessorTest {

    @Test
    fun testEmpty() {
        testStreamProcessing("src/sharedTest/resources/index-empty-v1.json",
            TestDataEmptyV2.index.v1compat())
    }

    @Test
    fun testMin() {
        testStreamProcessing("src/sharedTest/resources/index-min-v1.json",
            TestDataMinV2.index.v1compat())
    }

    @Test
    fun testMid() {
        testStreamProcessing("src/sharedTest/resources/index-mid-v1.json",
            TestDataMidV2.indexCompat)
    }

    @Test
    fun testMax() {
        testStreamProcessing("src/sharedTest/resources/index-max-v1.json",
            TestDataMaxV2.indexCompat)
    }

    private fun testStreamProcessing(filePath: String, indexV2: IndexV2) {
        val file = File(filePath)
        val testStreamReceiver = TestStreamReceiver()
        val streamProcessor = IndexV1StreamProcessor(testStreamReceiver, null)
        FileInputStream(file).use { streamProcessor.process(it) }
        assertEquals(indexV2.repo, testStreamReceiver.repo)
        assertEquals(indexV2.packages, testStreamReceiver.packages)
    }

    @Suppress("DEPRECATION")
    private class TestStreamReceiver : IndexV1StreamReceiver {
        var repo: RepoV2? = null
        val packages = HashMap<String, PackageV2>()

        override fun receive(repo: RepoV2, version: Long, certificate: String?) {
            this.repo = repo
        }

        override fun receive(packageId: String, m: MetadataV2) {
            packages[packageId] = PackageV2(
                metadata = m,
                versions = emptyMap(),
            )
        }

        override fun receive(packageId: String, v: Map<String, PackageVersionV2>) {
            packages[packageId] = packages[packageId]!!.copy(versions = v)
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

        override fun updateAppMetadata(packageId: String, preferredSigner: String?) {
            val currentPackage = packages[packageId] ?: fail()
            packages[packageId] = currentPackage.copy(
                metadata = currentPackage.metadata.copy(preferredSigner = preferredSigner),
            )
        }
    }

}

package org.fdroid.index

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.fdroid.index.v1.IndexV1StreamReceiver
import org.fdroid.index.v2.AntiFeatureV2
import org.fdroid.index.v2.CategoryV2
import org.fdroid.index.v2.IndexV2
import org.fdroid.index.v2.MetadataV2
import org.fdroid.index.v2.PackageV2
import org.fdroid.index.v2.PackageVersionV2
import org.fdroid.index.v2.ReleaseChannelV2
import org.fdroid.index.v2.RepoV2
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import kotlin.test.assertEquals
import kotlin.test.fail

@OptIn(ExperimentalSerializationApi::class)
internal class IndexV1StreamProcessorTest {

    private val json = Json

    /**
     * Tests that indexV1 parsed with a stream receiver matches the indexV2 parsed normally.
     */
    @Test
    fun testFDroidStreamProcessing() {
        val file1 = File("src/commonTest/resources/index-v1.json")
        val file2 = File("src/commonTest/resources/index-v2.json")
        val indexParsed: IndexV2 = FileInputStream(file2).use { json.decodeFromStream(it) }

        val testStreamReceiver = TestStreamReceiver()
        val streamProcessor = IndexV1StreamProcessor(testStreamReceiver, json = json)
        FileInputStream(file1).use { streamProcessor.process(1, it) }

        assertEquals(indexParsed.repo, testStreamReceiver.repo)
        assertEquals(indexParsed.packages.size, testStreamReceiver.packages.size)
        indexParsed.packages.entries.forEach { (packageName, packageV2) ->
            assertEquals(packageV2, testStreamReceiver.packages[packageName])
        }
    }

    @Test
    fun testFDroidArchiveStreamProcessing() {
        testStreamProcessing("src/commonTest/resources/fdroid-archive/index-v1.json")
    }

    @Test
    fun testGuardianStreamProcessing() {
        testStreamProcessing("src/commonTest/resources/guardian/index-v1.json")
    }

    @Test
    fun testIzzyStreamProcessing() {
        testStreamProcessing("src/commonTest/resources/izzy/index-v1.json")
    }

    @Test
    fun testWindStreamProcessing() {
        testStreamProcessing("src/commonTest/resources/wind/index-v1.json")
    }

    private fun testStreamProcessing(filePath1: String) {
        val file1 = File(filePath1)
        val testStreamReceiver = TestStreamReceiver()
        val streamProcessor = IndexV1StreamProcessor(testStreamReceiver, json = json)
        FileInputStream(file1).use { streamProcessor.process(1, it) }
    }

    private class TestStreamReceiver : IndexV1StreamReceiver {
        var repo: RepoV2? = null
        val packages = HashMap<String, PackageV2>()

        override fun receive(repoId: Long, repo: RepoV2) {
            this.repo = repo
        }

        override fun receive(repoId: Long, packageId: String, m: MetadataV2) {
            packages[packageId] = PackageV2(
                metadata = m,
                versions = emptyMap(),
            )
        }

        override fun receive(repoId: Long, packageId: String, v: Map<String, PackageVersionV2>) {
            packages[packageId] = packages[packageId]!!.copy(versions = v)
        }

        override fun updateRepo(
            repoId: Long,
            antiFeatures: Map<String, AntiFeatureV2>,
            categories: Map<String, CategoryV2>,
            releaseChannels: Map<String, ReleaseChannelV2>,
        ) {
            repo = repo!!.copy(
                antiFeatures = antiFeatures,
                categories = categories,
                releaseChannels = releaseChannels,
            )
        }

        override fun updateAppMetadata(repoId: Long, packageId: String, preferredSigner: String?) {
            val currentPackage = packages[packageId] ?: fail()
            packages[packageId] = currentPackage.copy(
                metadata = currentPackage.metadata.copy(preferredSigner = preferredSigner),
            )
        }
    }

}

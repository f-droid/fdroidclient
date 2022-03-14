package org.fdroid.index

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.fdroid.index.v2.IndexStreamReceiver
import org.fdroid.index.v2.IndexV2
import org.fdroid.index.v2.PackageV2
import org.fdroid.index.v2.RepoV2
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val INDEX_V2 = "src/commonTest/resources/index-v2.json"

@OptIn(ExperimentalSerializationApi::class)
internal class IndexStreamProcessorTest {

    @get:Rule
    var folder: TemporaryFolder = TemporaryFolder()

    private val json = Json

    /**
     * Tests that index parsed with a stream receiver matches the index parsed normally.
     */
    @Test
    fun testStreamProcessing() {
        val file = File(INDEX_V2)
        val indexParsed: IndexV2 = FileInputStream(file).use { json.decodeFromStream(it) }

        val testStreamReceiver = TestStreamReceiver()
        val streamProcessor = IndexStreamProcessor(testStreamReceiver, json)
        FileInputStream(file).use { streamProcessor.process(1, it) }

        assertEquals(indexParsed.repo, testStreamReceiver.repo)
        assertEquals(indexParsed.packages.size, testStreamReceiver.packages.size)
        indexParsed.packages.entries.forEach { (packageName, packageV2) ->
            assertEquals(packageV2, testStreamReceiver.packages[packageName])
        }
    }

    /**
     * Tests that that [IndexStreamProcessor] can handle a re-ordered index,
     * i.e. repo only after packages.
     */
    @Test
    fun testReorderedStreamProcessing() {
        val file = File(INDEX_V2)
        val indexParsed: IndexV2 = FileInputStream(file).use { json.decodeFromStream(it) }
        val newFile = folder.newFile()
        // write out parsed index in reverse order (repo after packages)
        FileOutputStream(newFile).use { outputStream ->
            outputStream.write("{ \"packages\": ".encodeToByteArray())
            json.encodeToStream(indexParsed.packages, outputStream)
            outputStream.write(", \"repo\": ".encodeToByteArray())
            json.encodeToStream(indexParsed.repo, outputStream)
            outputStream.write("}".encodeToByteArray())
        }
        val testStreamReceiver = ReorderedTestStreamReceiver()
        val streamProcessor = IndexStreamProcessor(testStreamReceiver, json)
        FileInputStream(newFile).use { streamProcessor.process(1, it) }

        assertTrue(testStreamReceiver.repoReceived)
        assertEquals(indexParsed.repo, testStreamReceiver.repo)
        assertEquals(indexParsed.packages.size, testStreamReceiver.packages.size)
        indexParsed.packages.entries.forEach { (packageName, packageV2) ->
            assertEquals(packageV2, testStreamReceiver.packages[packageName])
        }
    }

    private open class TestStreamReceiver : IndexStreamReceiver {
        var repo: RepoV2? = null
        val packages = HashMap<String, PackageV2>()

        override fun receive(repoId: Long, repo: RepoV2) {
            this.repo = repo
        }

        override fun receive(repoId: Long, packageId: String, p: PackageV2) {
            packages[packageId] = p
        }
    }

    private class ReorderedTestStreamReceiver : TestStreamReceiver() {
        var repoReceived: Boolean = false

        override fun receive(repoId: Long, repo: RepoV2) {
            super.receive(repoId, repo)
            assertFalse(repoReceived)
            repoReceived = true
        }

        override fun receive(repoId: Long, packageId: String, p: PackageV2) {
            super.receive(repoId, packageId, p)
            assertFalse(repoReceived)
        }
    }

}

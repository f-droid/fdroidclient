package org.fdroid.index.v2

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

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
        assumeTrue(file.isFile)
        val indexParsed: IndexV2 = FileInputStream(file).use { json.decodeFromStream(it) }

        val testStreamReceiver = TestStreamReceiver()
        val certificate = "foo bar"
        val streamProcessor = IndexStreamProcessor(testStreamReceiver, certificate, json)
        FileInputStream(file).use { streamProcessor.process(1, 42, it) }

        assertEquals(indexParsed.repo, testStreamReceiver.repo)
        assertTrue(testStreamReceiver.calledOnStreamEnded)
        assertEquals(certificate, testStreamReceiver.certificate)
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
        assumeTrue(file.isFile)
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
        val certificate = "foo bar"
        val streamProcessor = IndexStreamProcessor(testStreamReceiver, certificate, json)
        FileInputStream(newFile).use { streamProcessor.process(1, 42, it) }

        assertTrue(testStreamReceiver.repoReceived)
        assertTrue(testStreamReceiver.calledOnStreamEnded)
        assertEquals(indexParsed.repo, testStreamReceiver.repo)
        assertEquals(certificate, testStreamReceiver.certificate)
        assertEquals(indexParsed.packages.size, testStreamReceiver.packages.size)
        indexParsed.packages.entries.forEach { (packageName, packageV2) ->
            assertEquals(packageV2, testStreamReceiver.packages[packageName])
        }
    }

    private open class TestStreamReceiver : IndexStreamReceiver {
        var repo: RepoV2? = null
        var certificate: String? = null
        val packages = HashMap<String, PackageV2>()
        var calledOnStreamEnded: Boolean = false

        override fun receive(repoId: Long, repo: RepoV2, version: Int, certificate: String?) {
            this.repo = repo
            this.certificate = certificate
        }

        override fun receive(repoId: Long, packageId: String, p: PackageV2) {
            packages[packageId] = p
        }

        override fun onStreamEnded(repoId: Long) {
            if (calledOnStreamEnded) fail()
            calledOnStreamEnded = true
        }
    }

    private class ReorderedTestStreamReceiver : TestStreamReceiver() {
        var repoReceived: Boolean = false

        override fun receive(repoId: Long, repo: RepoV2, version: Int, certificate: String?) {
            super.receive(repoId, repo, version, certificate)
            assertFalse(repoReceived)
            repoReceived = true
        }

        override fun receive(repoId: Long, packageId: String, p: PackageV2) {
            super.receive(repoId, packageId, p)
            assertFalse(repoReceived)
        }
    }

}

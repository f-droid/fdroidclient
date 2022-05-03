package org.fdroid.index.v2

import kotlinx.serialization.ExperimentalSerializationApi
import org.fdroid.test.TestDataEmptyV2
import org.fdroid.test.TestDataMaxV2
import org.fdroid.test.TestDataMidV2
import org.fdroid.test.TestDataMinV2
import org.fdroid.test.TestUtils.getRandomString
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileInputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

@OptIn(ExperimentalSerializationApi::class)
internal class IndexV2FullStreamProcessorTest {

    @get:Rule
    var folder: TemporaryFolder = TemporaryFolder()

    @Test
    fun testEmpty() {
        testStreamProcessing("src/sharedTest/resources/index-empty-v2.json",
            TestDataEmptyV2.index, 0)
    }

    @Test
    fun testMin() {
        testStreamProcessing("src/sharedTest/resources/index-min-v2.json", TestDataMinV2.index, 1)
    }

    @Test
    fun testMinReordered() {
        testStreamProcessing("src/sharedTest/resources/index-min-reordered-v2.json",
            TestDataMinV2.index, 1)
    }

    @Test
    fun testMid() {
        testStreamProcessing("src/sharedTest/resources/index-mid-v2.json", TestDataMidV2.index, 2)
    }

    @Test
    fun testMax() {
        testStreamProcessing("src/sharedTest/resources/index-max-v2.json", TestDataMaxV2.index, 3)
    }

    /**
     * Tests that index parsed with a stream receiver is equal to the expected test data.
     */
    fun testStreamProcessing(filePath: String, index: IndexV2, expectedNumApps: Int) {
        val file = File(filePath)
        val testStreamReceiver = TestStreamReceiver()
        val certificate = getRandomString()
        val streamProcessor = IndexV2FullStreamProcessor(testStreamReceiver, certificate)
        var totalApps = 0
        FileInputStream(file).use {
            streamProcessor.process(42, it) { numAppsProcessed ->
                totalApps = numAppsProcessed
            }
        }

        assertTrue(testStreamReceiver.calledOnStreamEnded)
        assertEquals(index.repo, testStreamReceiver.repo)
        assertEquals(certificate, testStreamReceiver.certificate)
        assertEquals(index.packages, testStreamReceiver.packages)
        assertEquals(expectedNumApps, totalApps)
    }

    private open class TestStreamReceiver : IndexV2StreamReceiver {
        var repo: RepoV2? = null
        var certificate: String? = null
        val packages = HashMap<String, PackageV2>()
        var calledOnStreamEnded: Boolean = false

        override fun receive(repo: RepoV2, version: Long, certificate: String) {
            this.repo = repo
            this.certificate = certificate
        }

        override fun receive(packageId: String, p: PackageV2) {
            packages[packageId] = p
        }

        override fun onStreamEnded() {
            if (calledOnStreamEnded) fail()
            calledOnStreamEnded = true
        }
    }

}

package org.fdroid.index.v2

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.fdroid.index.IndexParser
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

internal class IndexV2DiffStreamProcessorTest {

    @Test
    fun testEmptyToMin() = testDiff("src/sharedTest/resources/diff-empty-min/23.json", 1)

    @Test
    fun testEmptyToMid() = testDiff("src/sharedTest/resources/diff-empty-mid/23.json", 2)

    @Test
    fun testEmptyToMax() = testDiff("src/sharedTest/resources/diff-empty-max/23.json", 3)

    @Test
    fun testMinToMid() = testDiff("src/sharedTest/resources/diff-empty-mid/42.json", 2)

    @Test
    fun testMinToMax() = testDiff("src/sharedTest/resources/diff-empty-max/42.json", 3)

    @Test
    fun testMidToMax() = testDiff("src/sharedTest/resources/diff-empty-max/1337.json", 2)

    @Test
    fun testRemovePackage() {
        val diffJson = """
            {
                "repo": { "timestamp": 42 },
                "packages": { "foo": null }
            }
        """.trimIndent()

        val streamReceiver = TestDiffReceiver()
        val streamProcessor = IndexV2DiffStreamProcessor(streamReceiver)
        streamProcessor.process(42, ByteArrayInputStream(diffJson.toByteArray())) {
            assertEquals(1, it)
        }

        val diff = IndexParser.json.parseToJsonElement(diffJson).jsonObject
        assertTrue(streamReceiver.endCalled)
        assertEquals(diff, streamReceiver.index)
    }

    private fun testDiff(diffPath: String, expectedNumApps: Int) {
        val diffFile = File(diffPath)

        val streamReceiver = TestDiffReceiver()
        val streamProcessor = IndexV2DiffStreamProcessor(streamReceiver)
        var totalApps = 0
        streamProcessor.process(42, FileInputStream(diffFile)) { numAppsProcessed ->
            totalApps = numAppsProcessed
        }

        val diff = IndexParser.json.parseToJsonElement(diffFile.readText()).jsonObject
        assertTrue(streamReceiver.endCalled)
        assertEquals(diff, streamReceiver.index)
        assertEquals(expectedNumApps, totalApps)
    }

    private class TestDiffReceiver : IndexV2DiffStreamReceiver {
        private val packages = HashMap<String, JsonElement>()
        private val indexMap = HashMap<String, JsonElement>().apply {
            put("packages", JsonObject(packages))
        }
        val index = JsonObject(indexMap)
        var endCalled = false

        override fun receiveRepoDiff(version: Long, repoJsonObject: JsonObject) {
            indexMap["repo"] = repoJsonObject
        }

        override fun receivePackageMetadataDiff(packageId: String, packageJsonObject: JsonObject?) {
            if (packageJsonObject == null) {
                packages[packageId] = JsonNull
            } else {
                val packageV2 = HashMap<String, JsonElement>(2)
                packageV2["metadata"] = packageJsonObject
                packages[packageId] = JsonObject(packageV2)
            }
        }

        override fun receiveVersionsDiff(
            packageId: String,
            versionsDiffMap: Map<String, JsonObject?>?,
        ) {
            val packageV2 = HashMap<String, JsonElement>(packages[packageId]?.jsonObject ?: fail())
            val versions = versionsDiffMap?.mapValues { it.value ?: JsonNull }
            packageV2["versions"] = versions?.let { JsonObject(it) } ?: JsonNull
            packages[packageId] = JsonObject(packageV2)
        }

        override fun onStreamEnded() {
            endCalled = true
        }
    }

}

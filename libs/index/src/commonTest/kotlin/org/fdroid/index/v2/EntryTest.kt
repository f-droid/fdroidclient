package org.fdroid.index.v2

import com.goncalossilva.resources.Resource
import kotlinx.serialization.SerializationException
import org.fdroid.index.IndexParser
import org.fdroid.index.assetPath
import org.fdroid.test.TestDataEntry
import org.junit.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class EntryTest {

    @Test
    fun testEmpty() {
        testEntryEquality("$assetPath/entry-empty-v2.json", TestDataEntry.empty)
    }

    @Test
    fun testEmptyToMin() {
        testEntryEquality("$assetPath/diff-empty-min/$DATA_FILE_NAME", TestDataEntry.emptyToMin)
    }

    @Test
    fun testEmptyToMid() {
        testEntryEquality("$assetPath/diff-empty-mid/$DATA_FILE_NAME", TestDataEntry.emptyToMid)
    }

    @Test
    fun testEmptyToMax() {
        testEntryEquality("$assetPath/diff-empty-max/$DATA_FILE_NAME", TestDataEntry.emptyToMax)
    }

    @Test
    fun testMalformedEntry() {
        // empty dict
        assertFailsWith<SerializationException> {
            IndexParser.parseEntry("{ }")
        }.also { assertContains(it.message!!, "missing") }

        // garbage input
        assertFailsWith<SerializationException> {
            IndexParser.parseEntry("{ 23^^%*dfDFG568 }")
        }

        // timestamp is list
        assertFailsWith<SerializationException> {
            IndexParser.parseEntry("""{
                "timestamp": [1, 2]
            }""".trimIndent())
        }.also { assertContains(it.message!!, "timestamp") }

        // version is dict
        assertFailsWith<SerializationException> {
            IndexParser.parseEntry("""{
                "timestamp": 23,
                "version": {}
            }""".trimIndent())
        }.also { assertContains(it.message!!, "version") }

        // index is number
        assertFailsWith<SerializationException> {
            IndexParser.parseEntry("""{
                "timestamp": 23,
                "version": 43,
                "index": 1337
            }""".trimIndent())
        }.also { assertContains(it.message!!, "object") }

        // index is missing numPackages
        assertFailsWith<SerializationException> {
            IndexParser.parseEntry("""{
                "timestamp": 23,
                "version": 43,
                "index": {
                    "name": "sdfsdf",
                    "sha256": "adfsdf",
                    "size": 0
                }
            }""".trimIndent())
        }.also { assertContains(it.message!!, "numPackages") }

        // diffs is a list
        assertFailsWith<SerializationException> {
            IndexParser.parseEntry("""{
                "timestamp": 23,
                "version": 43,
                "index": {
                    "name": "sdfsdf",
                    "sha256": "adfsdf",
                    "size": 0,
                    "numPackages": 0
                },
                "diffs": []
            }""".trimIndent())
        }.also { assertContains(it.message!!, "diffs") }
    }

    private fun testEntryEquality(path: String, expectedEntry: Entry) {
        val entryRes = Resource(path)
        val entryStr = entryRes.readText()
        val entry = IndexParser.parseEntry(entryStr)

        assertEquals(expectedEntry, entry)
    }

}

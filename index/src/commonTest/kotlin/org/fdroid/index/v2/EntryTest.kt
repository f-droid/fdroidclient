package org.fdroid.index.v2

import com.goncalossilva.resources.Resource
import kotlinx.serialization.SerializationException
import org.fdroid.index.IndexParser
import org.fdroid.test.TestDataEntryV2
import org.junit.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class EntryTest {

    @Test
    fun testEmpty() {
        testEntryEquality("src/sharedTest/resources/entry-empty-v2.json",
            TestDataEntryV2.empty)
    }

    @Test
    fun testEmptyToMin() {
        testEntryEquality("src/sharedTest/resources/diff-empty-min/entry.json",
            TestDataEntryV2.emptyToMin)
    }

    @Test
    fun testEmptyToMid() {
        testEntryEquality("src/sharedTest/resources/diff-empty-mid/entry.json",
            TestDataEntryV2.emptyToMid)
    }

    @Test
    fun testEmptyToMax() {
        testEntryEquality("src/sharedTest/resources/diff-empty-max/entry.json",
            TestDataEntryV2.emptyToMax)
    }

    @Test
    fun testMalformedEntry() {
        // empty dict
        assertFailsWith<SerializationException> {
            IndexParser.parseEntryV2("{ }")
        }.also { assertContains(it.message!!, "missing") }

        // garbage input
        assertFailsWith<SerializationException> {
            IndexParser.parseEntryV2("{ 23^^%*dfDFG568 }")
        }

        // timestamp is list
        assertFailsWith<SerializationException> {
            IndexParser.parseEntryV2("""{
                "timestamp": [1, 2]
            }""".trimIndent())
        }.also { assertContains(it.message!!, "timestamp") }

        // version is dict
        assertFailsWith<SerializationException> {
            IndexParser.parseEntryV2("""{
                "timestamp": 23,
                "version": {}
            }""".trimIndent())
        }.also { assertContains(it.message!!, "version") }

        // index is number
        assertFailsWith<SerializationException> {
            IndexParser.parseEntryV2("""{
                "timestamp": 23,
                "version": 43,
                "index": 1337
            }""".trimIndent())
        }.also { assertContains(it.message!!, "object") }

        // index is missing numPackages
        assertFailsWith<SerializationException> {
            IndexParser.parseEntryV2("""{
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
            IndexParser.parseEntryV2("""{
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

    private fun testEntryEquality(path: String, expectedEntry: EntryV2) {
        val entryV2Res = Resource(path)
        val entryV2Str = entryV2Res.readText()
        val entryV2 = IndexParser.parseEntryV2(entryV2Str)

        assertEquals(expectedEntry, entryV2)
    }

}

package org.fdroid.index.v2

import com.goncalossilva.resources.Resource
import org.fdroid.index.IndexParser
import org.fdroid.test.TestDataEntryV2
import org.junit.Test
import kotlin.test.assertEquals

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

    private fun testEntryEquality(path: String, expectedEntry: EntryV2) {
        val entryV2Res = Resource(path)
        val entryV2Str = entryV2Res.readText()
        val entryV2 = IndexParser.parseEntryV2(entryV2Str)

        assertEquals(expectedEntry, entryV2)
    }

}

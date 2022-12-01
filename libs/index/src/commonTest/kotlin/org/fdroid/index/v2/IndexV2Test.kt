package org.fdroid.index.v2

import com.goncalossilva.resources.Resource
import kotlinx.serialization.SerializationException
import org.fdroid.index.IndexParser.parseV2
import org.fdroid.index.assetPath
import org.fdroid.test.TestDataEmptyV2
import org.fdroid.test.TestDataMaxV2
import org.fdroid.test.TestDataMidV2
import org.fdroid.test.TestDataMinV2
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class IndexV2Test {

    @Test
    fun testEmpty() {
        testIndexEquality("$assetPath/index-empty-v2.json", TestDataEmptyV2.index)
    }

    @Test
    fun testMin() {
        testIndexEquality("$assetPath/index-min-v2.json", TestDataMinV2.index)
    }

    @Test
    fun testMinReordered() {
        testIndexEquality("$assetPath/index-min-reordered-v2.json", TestDataMinV2.index)
    }

    @Test
    fun testMid() {
        testIndexEquality("$assetPath/index-mid-v2.json", TestDataMidV2.index)
    }

    @Test
    fun testMax() {
        testIndexEquality("$assetPath/index-max-v2.json", TestDataMaxV2.index)
    }

    @Test
    fun testMalformedIndex() {
        // empty dict
        assertFailsWith<SerializationException> {
            parseV2("{ }")
        }.also { assertContains(it.message!!, "missing") }

        // garbage input
        assertFailsWith<SerializationException> {
            parseV2("{ 23^^%*dfDFG568 }")
        }

        // repo is a number
        assertFailsWith<SerializationException> {
            parseV2("""{
                "repo": 1
            }""".trimIndent())
        }.also { assertContains(it.message!!, "repo") }

        // repo is empty
        assertFailsWith<SerializationException> {
            parseV2("""{
                "repo": { }
            }""".trimIndent())
        }.also { assertContains(it.message!!, "timestamp") }

        // repo misses address
        assertFailsWith<SerializationException> {
            parseV2("""{
                "repo": {
                    "timestamp": 23
                }
            }""".trimIndent())
        }.also { assertContains(it.message!!, "address") }

        // packages is list
        assertFailsWith<SerializationException> {
            parseV2("""{
                "repo": {
                    "timestamp": 23,
                    "address": "http://example.com"
                },
                "packages": []
            }""".trimIndent())
        }.also { assertContains(it.message!!, "packages") }
    }

    private fun testIndexEquality(file: String, expectedIndex: IndexV2) {
        val indexV2Res = Resource(file)
        val indexV2Str = indexV2Res.readText()
        val indexV2 = parseV2(indexV2Str)

        assertEquals(expectedIndex, indexV2)
    }

}

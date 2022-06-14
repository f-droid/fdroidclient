package org.fdroid.index.v1

import com.goncalossilva.resources.Resource
import kotlinx.serialization.SerializationException
import org.fdroid.index.IndexParser.parseV1
import org.fdroid.index.assetPath
import org.fdroid.test.TestDataEmptyV1
import org.fdroid.test.TestDataMaxV1
import org.fdroid.test.TestDataMidV1
import org.fdroid.test.TestDataMinV1
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class IndexV1Test {

    @Test
    fun testIndexEmptyV1() {
        val indexRes = Resource("$assetPath/index-empty-v1.json")
        val indexStr = indexRes.readText()
        val index = parseV1(indexStr)
        assertEquals(TestDataEmptyV1.index, index)
    }

    @Test
    fun testIndexMinV1() {
        val indexRes = Resource("$assetPath/index-min-v1.json")
        val indexStr = indexRes.readText()
        val index = parseV1(indexStr)
        assertEquals(TestDataMinV1.index, index)
    }

    @Test
    fun testIndexMidV1() {
        val indexRes = Resource("$assetPath/index-mid-v1.json")
        val indexStr = indexRes.readText()
        val index = parseV1(indexStr)
        assertEquals(TestDataMidV1.index, index)
    }

    @Test
    fun testIndexMaxV1() {
        val indexRes = Resource("$assetPath/index-max-v1.json")
        val indexStr = indexRes.readText()
        val index = parseV1(indexStr)
        assertEquals(TestDataMaxV1.index, index)
    }

    @Test
    fun testMalformedV1() {
        // empty json dict
        assertFailsWith<SerializationException> {
            parseV1("{}")
        }.also { assertContains(it.message!!, "repo") }

        // garbage input
        assertFailsWith<SerializationException> {
            parseV1("efoj324#FD@(DJ#@DLKWf")
        }

        // empty repo dict
        assertFailsWith<SerializationException> {
            parseV1("""{
                "repo": {}
            }""".trimIndent()
            )
        }.also { assertContains(it.message!!, "timestamp") }

        // timestamp not a number
        assertFailsWith<SerializationException> {
            parseV1("""{
                "repo": { "timestamp": "string" }
            }""".trimIndent()
            )
        }.also { assertContains(it.message!!, "numeric literal") }

        // remember valid repo for further tests
        val validRepo = """
            "repo": {
                    "timestamp": 42,
                    "version": 23,
                    "name": "foo",
                    "icon": "bar",
                    "address": "https://example.com",
                    "description": "desc"
                }
        """.trimIndent()

        // apps is dict
        assertFailsWith<SerializationException> {
            parseV1("""{
                $validRepo,
                "apps": {}
            }""".trimIndent()
            )
        }.also { assertContains(it.message!!, "apps") }

        // packages is list
        assertFailsWith<SerializationException> {
            parseV1("""{
                $validRepo,
                "packages": []
            }""".trimIndent()
            )
        }.also { assertContains(it.message!!, "packages") }
    }

    @Test
    fun testGuardianProjectV1() {
        val indexRes = Resource("$assetPath/guardianproject_index-v1.json")
        val indexStr = indexRes.readText()
        parseV1(indexStr)
    }

    @Test
    fun testLocalizedV1() {
        val indexRes = Resource("$assetPath/localized.json")
        val indexStr = indexRes.readText()
        parseV1(indexStr)
    }

}

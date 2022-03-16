package org.fdroid.index.v2

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ReflectionDifferTest {

    @Test
    fun testLocalizedNullableTextV2Diff() {
        val old: LocalizedTextV2 = buildMap {
            put("de", "football")
            put("en", "bar")
            put("de-DE", "foo bar")
        }
        val json = """
            {
              "de": "foo",
              "en": null,
              "en-US": "bar"
            }
        """.trimIndent()
        val diff = Json.parseToJsonElement(json).jsonObject
        val result = ReflectionDiffer.applyDiff(old, diff)

        assertEquals(3, result.size)
        assertEquals("foo", result["de"])
        assertEquals("bar", result["en-US"])
        assertEquals("foo bar", result["de-DE"])
    }

    @Test
    fun testFileDiffV2() {
        val old = FileV2(
            name = "foo",
            sha256 = "bar",
            size = Random.nextLong()
        )
        val json = """
            {
              "name": "foo bar",
              "size": null
            }
        """.trimIndent()
        val diff = Json.parseToJsonElement(json).jsonObject
        val result = ReflectionDiffer.applyDiff(old, diff)

        assertEquals("foo bar", result.name)
        assertEquals("bar", result.sha256)
        assertEquals(null, result.size)
    }
}

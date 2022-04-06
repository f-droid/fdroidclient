package org.fdroid.database

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.fdroid.index.v2.ReflectionDiffer.applyDiff
import org.fdroid.test.TestRepoUtils.getRandomFileV2
import org.fdroid.test.TestRepoUtils.getRandomRepo
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class ReflectionTest {

    @Test
    fun testRepository() {
        val repo = getRandomRepo().toCoreRepository()
        val icon = getRandomFileV2()
        val description = if (Random.nextBoolean()) mapOf("de" to null, "en" to "foo") else null
        val json = """
            {
              "name": "test",
              "timestamp": ${Long.MAX_VALUE},
              "icon": ${Json.encodeToString(icon)},
              "description": ${Json.encodeToString(description)}
            }
        """.trimIndent()
        val diff = Json.parseToJsonElement(json).jsonObject
        val diffed = applyDiff(repo, diff)
        println(diffed)
        assertEquals(Long.MAX_VALUE, diffed.timestamp)
    }

}

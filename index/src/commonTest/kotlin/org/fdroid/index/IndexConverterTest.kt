package org.fdroid.index

import com.goncalossilva.resources.Resource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.fdroid.index.v1.IndexV1
import org.fdroid.index.v2.IndexV2
import kotlin.test.Test
import kotlin.test.assertEquals

class IndexConverterTest {

    @Test
    fun testToIndexV2() {
        val indexV1Str = Resource("src/commonTest/resources/index-v1.json").readText()
        val indexV1 = Json.decodeFromString<IndexV1>(indexV1Str)

        val v2 = IndexConverter().toIndexV2(indexV1)

        val indexV2Str = Resource("src/commonTest/resources/index-v2.json").readText()
        val indexV2 = Json.decodeFromString<IndexV2>(indexV2Str)

        assertEquals(indexV2.repo, v2.repo)
        assertEquals(indexV2.packages.size, v2.packages.size)
        indexV2.packages.keys.forEach { packageName ->
            assertEquals(indexV2.packages[packageName], v2.packages[packageName])
        }
    }

}

package org.fdroid.index

import com.goncalossilva.resources.Resource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.fdroid.index.v1.IndexV1
import org.fdroid.index.v2.IndexV2
import org.junit.Assume.assumeTrue
import kotlin.test.Test
import kotlin.test.assertEquals

internal class IndexConverterTest {

    @Test
    fun testToIndexV2() {
        val res1 = Resource("src/commonTest/resources/index-v1.json")
        assumeTrue(res1.exists())
        val indexV1Str = res1.readText()
        val indexV1 = Json.decodeFromString<IndexV1>(indexV1Str)

        val v2 = IndexConverter().toIndexV2(indexV1)

        val res2 = Resource("src/commonTest/resources/index-v2.json")
        assumeTrue(res2.exists())
        val indexV2Str = res2.readText()
        val indexV2 = Json.decodeFromString<IndexV2>(indexV2Str)

        assertEquals(
            indexV2.repo,
            v2.repo.copy(
                antiFeatures = emptyMap(), categories = emptyMap(), releaseChannels = emptyMap()
            )
        ) // TODO remove copies when test data is fixed
        assertEquals(indexV2.packages.size, v2.packages.size)
        indexV2.packages.keys.forEach { packageName ->
            assertEquals(indexV2.packages[packageName], v2.packages[packageName])
        }
    }

}

package org.fdroid.index

import com.goncalossilva.resources.Resource
import org.fdroid.index.IndexParser.parseV1
import org.fdroid.index.v2.IndexV2
import org.fdroid.test.TestDataEmptyV2
import org.fdroid.test.TestDataMaxV2
import org.fdroid.test.TestDataMidV2
import org.fdroid.test.TestDataMinV2
import org.fdroid.test.TestUtils.sorted
import org.fdroid.test.v1compat
import kotlin.test.Test
import kotlin.test.assertEquals

internal const val assetPath = "../sharedTest/src/main/assets"

internal class IndexConverterTest {

    @Test
    fun testEmpty() {
        testConversation("$assetPath/index-empty-v1.json", TestDataEmptyV2.index.v1compat())
    }

    @Test
    fun testMin() {
        testConversation("$assetPath/index-min-v1.json", TestDataMinV2.index.v1compat())
    }

    @Test
    fun testMid() {
        testConversation("$assetPath/index-mid-v1.json", TestDataMidV2.indexCompat)
    }

    @Test
    fun testMax() {
        testConversation("$assetPath/index-max-v1.json", TestDataMaxV2.indexCompat)
    }

    private fun testConversation(file: String, expectedIndex: IndexV2) {
        val indexV1Res = Resource(file)
        val indexV1Str = indexV1Res.readText()
        val indexV1 = parseV1(indexV1Str)

        val v2 = IndexConverter().toIndexV2(indexV1)

        assertEquals(expectedIndex.sorted(), v2.sorted())
    }

}

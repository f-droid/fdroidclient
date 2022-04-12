package org.fdroid.index.v1

import com.goncalossilva.resources.Resource
import org.fdroid.index.IndexParser.parseV1
import org.fdroid.test.TestDataEmptyV1
import org.fdroid.test.TestDataMaxV1
import org.fdroid.test.TestDataMidV1
import org.fdroid.test.TestDataMinV1
import kotlin.test.Test
import kotlin.test.assertEquals

internal class IndexV1Test {

    @Test
    fun testIndexEmptyV1() {
        val indexRes = Resource("src/sharedTest/resources/index-empty-v1.json")
        val indexStr = indexRes.readText()
        val index = parseV1(indexStr)
        assertEquals(TestDataEmptyV1.index, index)
    }

    @Test
    fun testIndexMinV1() {
        val indexRes = Resource("src/sharedTest/resources/index-min-v1.json")
        val indexStr = indexRes.readText()
        val index = parseV1(indexStr)
        assertEquals(TestDataMinV1.index, index)
    }

    @Test
    fun testIndexMidV1() {
        val indexRes = Resource("src/sharedTest/resources/index-mid-v1.json")
        val indexStr = indexRes.readText()
        val index = parseV1(indexStr)
        assertEquals(TestDataMidV1.index, index)
    }

    @Test
    fun testIndexMaxV1() {
        val indexRes = Resource("src/sharedTest/resources/index-max-v1.json")
        val indexStr = indexRes.readText()
        val index = parseV1(indexStr)
        assertEquals(TestDataMaxV1.index, index)
    }

}

package org.fdroid.index.v2

import com.goncalossilva.resources.Resource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assume.assumeTrue
import kotlin.test.Test

internal class IndexV2Test {

    @Test
    fun testIndexV2() {
        val entryRes = Resource("src/commonTest/resources/entry.json")
        assumeTrue(entryRes.exists())
        val entryStr = entryRes.readText()
        val entry = Json.decodeFromString<EntryV2>(entryStr)

        val indexRes = Resource("src/commonTest/resources/index-v2.json")
        assumeTrue(indexRes.exists())
        val indexStr = indexRes.readText()
        val index = Json.decodeFromString<IndexV2>(indexStr)
    }

    @Test
    fun testDiffV2() {
//        val diff1Res = Resource("src/commonTest/resources/tmp.json")
//        assumeTrue(diff1Res.exists())
//        val diff1Str = diff1Res.readText()
//        val diff1 = Json.decodeFromString<DiffV2>(diff1Str)
//        println(diff1)
    }

}

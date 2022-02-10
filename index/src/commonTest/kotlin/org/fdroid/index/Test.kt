package org.fdroid.index

import com.goncalossilva.resources.Resource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.fdroid.index.v1.IndexV1
import org.fdroid.index.v2.EntryV2
import org.fdroid.index.v2.IndexV2
import kotlin.test.Test

class Test {

    @Test
    fun testIndexV1() {
        val indexStr = Resource("src/commonTest/resources/index-v1.json").readText()
        val index = Json.decodeFromString<IndexV1>(indexStr)

        val indexArchiveStr =
            Resource("src/commonTest/resources/fdroid-archive/index-v1.json").readText()
        Json.decodeFromString<IndexV1>(indexArchiveStr)

        val indexGuardianStr =
            Resource("src/commonTest/resources/guardian/index-v1.json").readText()
        Json.decodeFromString<IndexV1>(indexGuardianStr)

        val indexIzzyStr = Resource("src/commonTest/resources/izzy/index-v1.json").readText()
        Json.decodeFromString<IndexV1>(indexIzzyStr)

        val indexWindStr = Resource("src/commonTest/resources/wind/index-v1.json").readText()
        Json.decodeFromString<IndexV1>(indexWindStr)
    }

    @Test
    fun testIndexV2() {
        val entryStr = Resource("src/commonTest/resources/entry.json").readText()
        val entry = Json.decodeFromString<EntryV2>(entryStr)

        val indexStr = Resource("src/commonTest/resources/index-v2.json").readText()
        val index = Json.decodeFromString<IndexV2>(indexStr)
    }

    @Test
    fun testDiffV2() {
//        val diff1Str = Resource("src/commonTest/resources/tmp.json").readText()
//        val diff1 = Json.decodeFromString<DiffV2>(diff1Str)
//        println(diff1)
    }

}

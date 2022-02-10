package org.fdroid.index.v1

import com.goncalossilva.resources.Resource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assume.assumeTrue
import kotlin.test.Test

internal class IndexV1Test {

    @Test
    fun testIndexV1() {
        val indexRes = Resource("src/commonTest/resources/index-v1.json")
        assumeTrue(indexRes.exists())
        val indexStr = indexRes.readText()
        val index = Json.decodeFromString<IndexV1>(indexStr)

        val indexArchiveRes = Resource("src/commonTest/resources/fdroid-archive/index-v1.json")
        assumeTrue(indexArchiveRes.exists())
        val indexArchiveStr = indexArchiveRes.readText()
        assumeTrue(indexRes.exists())
        Json.decodeFromString<IndexV1>(indexArchiveStr)

        val indexGuardianRes = Resource("src/commonTest/resources/guardian/index-v1.json")
        assumeTrue(indexGuardianRes.exists())
        val indexGuardianStr = indexGuardianRes.readText()
        Json.decodeFromString<IndexV1>(indexGuardianStr)

        val indexIzzyRes = Resource("src/commonTest/resources/izzy/index-v1.json")
        assumeTrue(indexIzzyRes.exists())
        val indexIzzyStr = indexIzzyRes.readText()
        Json.decodeFromString<IndexV1>(indexIzzyStr)

        val indexWindRes = Resource("src/commonTest/resources/wind/index-v1.json")
        assumeTrue(indexWindRes.exists())
        val indexWindStr = indexWindRes.readText()
        Json.decodeFromString<IndexV1>(indexWindStr)
    }

}

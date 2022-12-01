package org.fdroid.database

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.fdroid.test.TestAppUtils.getRandomMetadataV2
import org.fdroid.test.TestRepoUtils.getRandomFileV2
import org.fdroid.test.TestUtils.getRandomString
import org.fdroid.test.TestUtils.sort
import org.junit.Rule

internal abstract class AppTest : DbTest() {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    protected val packageName = getRandomString()
    protected val packageName1 = getRandomString()
    protected val packageName2 = getRandomString()
    protected val packageName3 = getRandomString()
    protected val name1 = mapOf("en-US" to "1")
    protected val name2 = mapOf("en-US" to "2")
    protected val name3 = mapOf("en-US" to "3")
    // it is important for testing that the icons are sharing at least one locale
    protected val icons1 = mapOf("en-US" to getRandomFileV2(), "bar" to getRandomFileV2())
    protected val icons2 = mapOf("en-US" to getRandomFileV2(), "42" to getRandomFileV2())
    protected val app1 = getRandomMetadataV2().copy(
        name = name1,
        icon = icons1,
        summary = null,
        lastUpdated = 10,
        categories = listOf("A", "B")
    ).sort()
    protected val app2 = getRandomMetadataV2().copy(
        name = name2,
        icon = icons2,
        summary = name2,
        lastUpdated = 20,
        categories = listOf("A")
    ).sort()
    protected val app3 = getRandomMetadataV2().copy(
        name = name3,
        icon = null,
        summary = name3,
        lastUpdated = 30,
        categories = listOf("A", "B")
    ).sort()

}

package org.fdroid.database

import org.fdroid.test.TestUtils.getRandomList
import org.fdroid.test.TestUtils.getRandomString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class ConvertersTest {

    @Test
    fun testListConversion() {
        val list = getRandomList { getRandomString() }

        val str = Converters.listStringToString(list)
        val convertedList = Converters.fromStringToListString(str)
        assertEquals(list, convertedList)
    }

    @Test
    fun testEmptyListConversion() {
        val list = emptyList<String>()

        val str = Converters.listStringToString(list)
        assertNull(str)
        assertNull(Converters.listStringToString(null))
        val convertedList = Converters.fromStringToListString(str)
        assertEquals(list, convertedList)
    }

}

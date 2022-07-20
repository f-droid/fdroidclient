package org.fdroid.database

import org.fdroid.test.TestUtils.getRandomString
import org.junit.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class AppPrefsTest {

    @Test
    fun testDefaults() {
        val prefs = AppPrefs(getRandomString())
        assertFalse(prefs.ignoreAllUpdates)
        for (i in 1..1337L) assertFalse(prefs.shouldIgnoreUpdate(i))
        assertEquals(emptyList(), prefs.releaseChannels)
    }

    @Test
    fun testIgnoreVersionCodeUpdate() {
        val ignoredCode = Random.nextLong(1, Long.MAX_VALUE - 1)
        val prefs = AppPrefs(getRandomString(), ignoredCode)
        assertFalse(prefs.ignoreAllUpdates)
        assertTrue(prefs.shouldIgnoreUpdate(ignoredCode - 1))
        assertTrue(prefs.shouldIgnoreUpdate(ignoredCode))
        assertFalse(prefs.shouldIgnoreUpdate(ignoredCode + 1))

        // after toggling, it is not ignored anymore
        assertFalse(prefs.toggleIgnoreVersionCodeUpdate(ignoredCode)
            .shouldIgnoreUpdate(ignoredCode))
    }

    @Test
    fun testIgnoreAllUpdates() {
        val prefs = AppPrefs(getRandomString()).toggleIgnoreAllUpdates()
        assertTrue(prefs.ignoreAllUpdates)
        assertTrue(prefs.shouldIgnoreUpdate(Random.nextLong()))
        assertTrue(prefs.shouldIgnoreUpdate(Random.nextLong()))
        assertTrue(prefs.shouldIgnoreUpdate(Random.nextLong()))

        // after toggling, all are not ignored anymore
        val toggled = prefs.toggleIgnoreAllUpdates()
        assertFalse(toggled.ignoreAllUpdates)
        assertFalse(toggled.shouldIgnoreUpdate(Random.nextLong(1, Long.MAX_VALUE - 1)))
        assertFalse(toggled.shouldIgnoreUpdate(Random.nextLong(1, Long.MAX_VALUE - 1)))
        assertFalse(toggled.shouldIgnoreUpdate(Random.nextLong(1, Long.MAX_VALUE - 1)))
    }

    @Test
    fun testReleaseChannels() {
        // no release channels initially
        val prefs = AppPrefs(getRandomString())
        assertEquals(emptyList(), prefs.releaseChannels)

        // A gets toggled and is then in channels
        val a = prefs.toggleReleaseChannel("A")
        assertEquals(listOf("A"), a.releaseChannels)

        // toggling it off returns empty list again
        assertEquals(emptyList(), a.toggleReleaseChannel("A").releaseChannels)

        // toggling A and B returns both
        val ab = prefs.toggleReleaseChannel("A").toggleReleaseChannel("B")
        assertEquals(setOf("A", "B"), ab.releaseChannels.toSet())

        // toggling both off returns empty list again
        assertEquals(emptyList(),
            ab.toggleReleaseChannel("A").toggleReleaseChannel("B").releaseChannels)
    }

}

package org.fdroid.repo

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
internal class RepoUriGetterTest {
    @Test
    fun testTrailingSlash() {
        val uri = RepoUriGetter.getUri("http://example.org/fdroid/repo/")
        assertEquals("http://example.org/fdroid/repo", uri.uri.toString())
        assertNull(uri.fingerprint)
    }

    @Test
    fun testWithoutTrailingSlash() {
        val uri = RepoUriGetter.getUri("http://example.org/fdroid/repo")
        assertEquals("http://example.org/fdroid/repo", uri.uri.toString())
        assertNull(uri.fingerprint)
    }

    @Test
    fun testFingerprint() {
        val uri = RepoUriGetter.getUri("https://example.org/repo?fingerprint=foobar&test=42")
        assertEquals("https://example.org/repo", uri.uri.toString())
        assertEquals("foobar", uri.fingerprint)
    }

    @Test
    fun testHash() {
        val uri = RepoUriGetter.getUri("https://example.org/repo?fingerprint=foobar&test=42#hash")
        assertEquals("https://example.org/repo", uri.uri.toString())
        assertEquals("foobar", uri.fingerprint)
    }

    @Test
    fun testAddFdroidSlashRepo() {
        val uri = RepoUriGetter.getUri("https://example.org")
        assertEquals("https://example.org/fdroid/repo", uri.uri.toString())
        assertNull(uri.fingerprint)
    }

    @Test
    fun testLeaveSingleRepo() {
        val uri = RepoUriGetter.getUri("https://example.org/repo")
        assertEquals("https://example.org/repo", uri.uri.toString())
        assertNull(uri.fingerprint)
    }

    @Test
    fun testAddsMissingRepo() {
        val uri = RepoUriGetter.getUri("https://example.org/fdroid/")
        assertEquals("https://example.org/fdroid/repo", uri.uri.toString())
        assertNull(uri.fingerprint)
    }
}

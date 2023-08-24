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

    @Test
    fun testFDroidLink() {
        val uri1 =
            RepoUriGetter.getUri("https://fdroid.link/index.html#repo=https://f-droid.org/repo?" +
                "fingerprint=43238d512c1e5eb2d6569f4a3afbf5523418b82e0a3ed1552770abb9a9c9ccab")
        assertEquals("https://f-droid.org/repo", uri1.uri.toString())
        assertEquals(
            "43238d512c1e5eb2d6569f4a3afbf5523418b82e0a3ed1552770abb9a9c9ccab",
            uri1.fingerprint
        )

        val uri2 = RepoUriGetter.getUri("https://fdroid.link#repo=https://f-droid.org/repo")
        assertEquals("https://f-droid.org/repo", uri2.uri.toString())
        assertNull(uri2.fingerprint)

        val uri3 = RepoUriGetter.getUri("https://fdroid.link/#repo=http://f-droid.org/repo")
        assertEquals("http://f-droid.org/repo", uri3.uri.toString())
        assertNull(uri3.fingerprint)
    }
}

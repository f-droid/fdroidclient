package org.fdroid.repo

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
    fun testFingerprintWithTrailingWhitespace() {
        val uri1 = RepoUriGetter.getUri("https://example.org/repo?fingerprint=foobar ")
        assertEquals("https://example.org/repo", uri1.uri.toString())
        assertEquals("foobar", uri1.fingerprint)

        val uri2 = RepoUriGetter.getUri("https://example.org/repo?fingerprint=foobar     ")
        assertEquals("https://example.org/repo", uri2.uri.toString())
        assertEquals("foobar", uri2.fingerprint)
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
        val uri1 = RepoUriGetter.getUri(
            "https://fdroid.link/index.html#https://f-droid.org/repo?" +
                "fingerprint=43238d512c1e5eb2d6569f4a3afbf5523418b82e0a3ed1552770abb9a9c9ccab"
        )
        assertEquals("https://f-droid.org/repo", uri1.uri.toString())
        assertEquals(
            "43238d512c1e5eb2d6569f4a3afbf5523418b82e0a3ed1552770abb9a9c9ccab",
            uri1.fingerprint
        )

        val uri2 = RepoUriGetter.getUri("https://fdroid.link#https://f-droid.org/repo")
        assertEquals("https://f-droid.org/repo", uri2.uri.toString())
        assertNull(uri2.fingerprint)

        val uri3 = RepoUriGetter.getUri("https://fdroid.link/#http://f-droid.org/repo")
        assertEquals("http://f-droid.org/repo", uri3.uri.toString())
        assertNull(uri3.fingerprint)
    }

    @Test
    fun testAddScheme() {
        val uri1 = RepoUriGetter.getUri("example.com/repo")
        assertEquals("https://example.com/repo", uri1.uri.toString())
        assertNull(uri1.fingerprint)

        val uri2 = RepoUriGetter.getUri(
            "example.com/repo?" +
                "fingerprint=43238d512c1e5eb2d6569f4a3afbf5523418b82e0a3ed1552770abb9a9c9ccab"
        )
        assertEquals("https://example.com/repo", uri2.uri.toString())
        assertEquals(
            "43238d512c1e5eb2d6569f4a3afbf5523418b82e0a3ed1552770abb9a9c9ccab",
            uri2.fingerprint
        )
    }

    @Test
    fun testFDroidRepoUriScheme() {
        val uri1 = RepoUriGetter.getUri(
            "fdroidrepos://grobox.de/fdroid/repo?fingerprint=" +
                "28e14fb3b280bce8ff1e0f8e82726ff46923662cecff2a0689108ce19e8b347c"
        )
        assertEquals("https://grobox.de/fdroid/repo", uri1.uri.toString())
        assertEquals(
            "28e14fb3b280bce8ff1e0f8e82726ff46923662cecff2a0689108ce19e8b347c",
            uri1.fingerprint,
        )

        val uri2 = RepoUriGetter.getUri("fdroidrepo://grobox.de/fdroid/repo")
        assertEquals("http://grobox.de/fdroid/repo", uri2.uri.toString())
        assertNull(uri2.fingerprint)

        val uri3 = RepoUriGetter.getUri(
            "FDROIDREPOS://grobox.de/fdroid/repo?fingerprint=" +
                "28e14fb3b280bce8ff1e0f8e82726ff46923662cecff2a0689108ce19e8b347c"
        )
        assertEquals("https://grobox.de/fdroid/repo", uri3.uri.toString())
        assertEquals(
            "28e14fb3b280bce8ff1e0f8e82726ff46923662cecff2a0689108ce19e8b347c",
            uri3.fingerprint,
        )

        val uri4 = RepoUriGetter.getUri("fdroidREPO://grobox.de/fdroid/repo")
        assertEquals("http://grobox.de/fdroid/repo", uri4.uri.toString())
        assertNull(uri4.fingerprint)
    }

    @Test
    fun testUsernamePassword() {
        val uri1 = RepoUriGetter.getUri(
            "https://username:password@example.org/repo?fingerprint=foobar&test=42"
        )
        assertEquals("https://example.org/repo", uri1.uri.toString())
        assertEquals("foobar", uri1.fingerprint)
        assertEquals("username", uri1.username)
        assertEquals("password", uri1.password)

        // no password
        val uri2 =
            RepoUriGetter.getUri("https://username@example.org/repo?fingerprint=foobar&test=42")
        assertEquals("https://example.org/repo", uri2.uri.toString())
        assertEquals("foobar", uri2.fingerprint)
        assertEquals("username", uri2.username)
        assertNull(uri2.password)

        // empty host
        val uri3 = RepoUriGetter.getUri("https://foo:bar@/repo?fingerprint=foobar&test=42")
        assertEquals("https:///repo", uri3.uri.toString())
        assertEquals("foobar", uri3.fingerprint)
        assertEquals("foo", uri3.username)
        assertEquals("bar", uri3.password)

        // empty everything doesn't crash
        RepoUriGetter.getUri(":@/")
        RepoUriGetter.getUri(":@")
        RepoUriGetter.getUri("@")
        RepoUriGetter.getUri("")
    }

    @Test
    fun testNonHierarchicalUri() {
        RepoUriGetter.getUri("mailto:nobody@google.com") // should not crash
    }

    @Test
    fun testSwapUri() {
        val uri =
            RepoUriGetter.getUri(
                "http://192.168.3.159:8888/fdroid/repo?FINGERPRINT=" +
                    "BA29D02E303B2604D00C91189600E868B26FA0B248DC39D75C5C0F4349CA5FA9" +
                    "&SWAP=1&BSSID=44:FE:3B:7F:7F:EE"
            )
        assertEquals("http://192.168.3.159:8888/fdroid/repo", uri.uri.toString())
        assertEquals(
            "ba29d02e303b2604d00c91189600e868b26fa0b248dc39d75c5c0f4349ca5fa9",
            uri.fingerprint,
        )
    }

    @Test
    fun testIsSwapUri() {
        val uri1 = Uri.parse(
            "http://192.168.3.159:8888/fdroid/repo?FINGERPRINT=" +
                "BA29D02E303B2604D00C91189600E868B26FA0B248DC39D75C5C0F4349CA5FA9" +
                "&SWAP=1&BSSID=44:FE:3B:7F:7F:EE"
        )
        assertTrue(RepoUriGetter.isSwapUri(uri1))

        val uri2 = Uri.parse(
            "http://192.168.3.159:8888/fdroid/repo?" +
                "swap=1&BSSID=44:FE:3B:7F:7F:EE"
        )
        assertTrue(RepoUriGetter.isSwapUri(uri2))

        val uri3 = Uri.parse("http://192.168.3.159:8888/fdroid/repo?BSSID=44:FE:3B:7F:7F:EE")
        assertFalse(RepoUriGetter.isSwapUri(uri3))

        val uri4 = Uri.parse("mailto:nobody@google.com")
        assertFalse(RepoUriGetter.isSwapUri(uri4))
    }
}

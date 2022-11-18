package org.fdroid.index

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.fdroid.database.Repository
import org.fdroid.index.v2.SIGNED_FILE_NAME
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
internal class IndexUpdaterTest {

    @Test
    fun testDefaultUriBuilder() {
        val repo = Repository(
            repoId = 42L,
            address = "http://example.org/",
            timestamp = 1337L,
            formatVersion = IndexFormatVersion.TWO,
            certificate = null,
            version = 2001,
            weight = 0,
            lastUpdated = 23L,
        )
        val uri = defaultRepoUriBuilder.getUri(repo, SIGNED_FILE_NAME)
        assertEquals("http://example.org/$SIGNED_FILE_NAME", uri.toString())
    }

}

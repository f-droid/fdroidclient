package org.fdroid.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.SerializationException
import org.fdroid.CompatibilityChecker
import org.fdroid.index.v2.FileV2
import org.fdroid.index.v2.RepoV2
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertFailsWith

@RunWith(AndroidJUnit4::class)
internal class DbV2StreamReceiverTest {

    private val db: FDroidDatabaseInt = mockk()
    private val compatChecker: CompatibilityChecker = mockk()
    private val dbV2StreamReceiver = DbV2StreamReceiver(db, 42L, compatChecker)

    @Test
    fun testFileV2Verified() {
        // proper icon file passes
        val repoV2 = RepoV2(
            icon = mapOf("en" to FileV2(name = "/foo", sha256 = "bar", size = 23L)),
            address = "http://example.org",
            timestamp = 42L,
        )
        every { db.getRepositoryDao() } returns mockk(relaxed = true)
        dbV2StreamReceiver.receive(repoV2, 42L, "cert")

        // icon file without leading / does not pass
        val repoV2NoSlash =
            repoV2.copy(icon = mapOf("en" to FileV2(name = "foo", sha256 = "bar", size = 23L)))
        assertFailsWith<SerializationException> {
            dbV2StreamReceiver.receive(repoV2NoSlash, 42L, "cert")
        }

        // icon file without sha256 hash fails
        val repoNoSha256 = repoV2.copy(icon = mapOf("en" to FileV2(name = "/foo", size = 23L)))
        assertFailsWith<SerializationException> {
            dbV2StreamReceiver.receive(repoNoSha256, 42L, "cert")
        }

        // icon file without size fails
        val repoNoSize = repoV2.copy(icon = mapOf("en" to FileV2(name = "/foo", sha256 = "bar")))
        assertFailsWith<SerializationException> {
            dbV2StreamReceiver.receive(repoNoSize, 42L, "cert")
        }
    }
}

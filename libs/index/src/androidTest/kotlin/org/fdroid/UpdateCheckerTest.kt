package org.fdroid

import org.fdroid.index.RELEASE_CHANNEL_BETA
import org.fdroid.index.v2.PackageManifest
import org.fdroid.index.v2.PackageVersion
import org.fdroid.index.v2.SignerV2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class UpdateCheckerTest {

    private val updateChecker = UpdateChecker { true }
    private val signer = "9f9261f0b911c60f8db722f5d430a9e9d557a3f8078ce43e1c07522ef41efedb"
    private val signerV2 = SignerV2(listOf(signer))
    private val betaChannels = listOf(RELEASE_CHANNEL_BETA)
    private val version1 = Version(1)
    private val version2 = Version(2)
    private val version3 = Version(3)
    private val versions = listOf(version3, version2, version1)

    @Test
    fun highestVersionCode() {
        assertEquals(version3, updateChecker.getUpdate(versions))
        assertEquals(version3, updateChecker.getUpdate(versions, installedVersionCode = 2))
        assertEquals(version3, updateChecker.getUpdate(versions, installedVersionCode = 1))
        assertEquals(version3, updateChecker.getUpdate(versions, installedVersionCode = 0))
    }

    @Test
    fun noUpdateIfSameOrHigherVersionInstalled() {
        assertNull(updateChecker.getUpdate(versions, installedVersionCode = 3))
        assertNull(updateChecker.getUpdate(versions, installedVersionCode = 4))
    }

    @Test
    fun testIncompatibleVersionNotConsidered() {
        var versionNum = 0
        val updateChecker = UpdateChecker {
            versionNum++
            versionNum != 1 // highest version is incompatible
        }
        val v = updateChecker.getUpdate(versions)
        assertEquals(version2, v)
    }

    @Test
    fun ignoredVersionNotConsidered() {
        val not4 = { AppPreferences(ignoreVersionCodeUpdate = 4) }
        val not3 = { AppPreferences(ignoreVersionCodeUpdate = 3) }
        val not2 = { AppPreferences(ignoreVersionCodeUpdate = 2) }
        assertNull(updateChecker.getUpdate(versions, preferencesGetter = not4))
        assertNull(updateChecker.getUpdate(versions, preferencesGetter = not3))
        assertEquals(version3, updateChecker.getUpdate(versions, preferencesGetter = not2))
    }

    @Test
    fun betaVersionOnlyReturnedWhenAllowed() {
        val version3 = version3.copy(releaseChannels = betaChannels)
        val versions = listOf(version3, version2, version1)
        // beta not allowed, so 2 returned
        assertEquals(version2, updateChecker.getUpdate(versions))
        // now beta is allowed, so 3 returned
        assertEquals(version3, getWithAllowReleaseChannels(versions, betaChannels))
    }

    @Test
    fun emptyReleaseChannelsAlwaysIncluded() {
        val version3 = version3.copy(releaseChannels = emptyList())
        val versions = listOf(version3, version2, version1)
        // version with empty release channels gets returned
        assertEquals(version3, updateChecker.getUpdate(versions))
        // version with empty release channels gets returned when allowing also beta
        assertEquals(
            version3,
            updateChecker.getUpdate(versions, allowedReleaseChannels = betaChannels)
        )
        // version with empty release channels gets returned when allow list is empty
        assertEquals(version3, getWithAllowReleaseChannels(versions, emptyList()))
        assertEquals(this.version3, getWithAllowReleaseChannels(this.versions, emptyList()))
    }

    @Test
    fun onlyAllowedReleaseChannelsGetIncluded() {
        val version3 = version3.copy(releaseChannels = listOf("a"))
        val version2 = version2.copy(releaseChannels = listOf("a", "b", "c"))
        val versions = listOf(version3, version2, version1)
        // only stable version gets returned
        assertEquals(version1, getWithAllowReleaseChannels(versions, null))
        // as long as "a" is included, 3 gets returned
        assertEquals(version3, getWithAllowReleaseChannels(versions, listOf("a")))
        assertEquals(version3, getWithAllowReleaseChannels(versions, listOf("a", "b")))
        assertEquals(version3, getWithAllowReleaseChannels(versions, listOf("a", "b", "z")))
        // as long as "b" or "c" is included, 2 gets returned
        assertEquals(version2, getWithAllowReleaseChannels(versions, listOf("b")))
        assertEquals(version2, getWithAllowReleaseChannels(versions, listOf("b", "z")))
        assertEquals(version2, getWithAllowReleaseChannels(versions, listOf("c")))
        assertEquals(version2, getWithAllowReleaseChannels(versions, listOf("c", "z")))
        // if neither "a", "b" or "c" is included, 1 gets returned
        assertEquals(version1, getWithAllowReleaseChannels(versions, listOf("x", "y", "z")))
    }

    @Test
    fun multipleSignersNotSupported() {
        val version = version3.copy(signer = signerV2.copy(hasMultipleSigners = true))
        val versions = listOf(version)
        assertNull(updateChecker.getUpdate(versions))
    }

    @Test
    fun onlyAllowedSignersGetIncluded() {
        val version3 = version3.copy(signer = SignerV2(listOf("foo", "bar")))
        val version2 = version2.copy(signer = signerV2)
        val versions = listOf(version3, version2, version1)
        val v2Set = signerV2.sha256.toMutableSet()
        // 3 gets returned if at least one of its signers are allowed, or all are allowed
        assertEquals(version3, updateChecker.getUpdate(versions, { setOf("foo") }))
        assertEquals(version3, updateChecker.getUpdate(versions, { setOf("bar") }))
        assertEquals(version3, updateChecker.getUpdate(versions, { v2Set + "bar" }))
        assertEquals(version3, updateChecker.getUpdate(versions, { setOf("foo", "bar") }))
        assertEquals(version3, updateChecker.getUpdate(versions, { setOf("foo", "bar", "42") }))
        assertEquals(version3, updateChecker.getUpdate(versions, { null }))
        // 2 gets returned if at least one of its signers are allowed
        assertEquals(version2, updateChecker.getUpdate(versions, { v2Set }))
        assertEquals(version2, updateChecker.getUpdate(versions, { v2Set + "foo bar" }))
        // empty set means no signers are allowed, only works for packages without "signer"
        assertEquals(version1, updateChecker.getUpdate(versions, { emptySet() }))
        // packages without "signer" entries get through everything
        assertEquals(version1, updateChecker.getUpdate(versions, { setOf("no version") }))
        // if no matching sig can be found, no version gets returned
        assertNull(updateChecker.getUpdate(listOf(version3, version2), { setOf("no version") }))
    }

    @Test
    fun installedVulnerableVersionAlwaysReturned() {
        val version3 = version3.copy(hasKnownVulnerability = true)
        val versions = listOf(version3, version2, version1)
        assertEquals(
            version3,
            updateChecker.getUpdate(versions, includeKnownVulnerabilities = true)
        )
        assertEquals(
            version3,
            updateChecker.getUpdate(
                versions,
                installedVersionCode = 3,
                includeKnownVulnerabilities = true,
            )
        )
        assertEquals(
            version3,
            updateChecker.getUpdate(
                versions,
                installedVersionCode = 2,
                includeKnownVulnerabilities = true,
            )
        )
        // when not asking for known vulnerabilities, version3 isn't returned (no update here)
        assertNull(
            updateChecker.getUpdate(versions, installedVersionCode = 3)
        )
    }

    private fun getWithAllowReleaseChannels(
        versions: List<Version>,
        releaseChannels: List<String>?,
    ): Version? {
        val v1 = updateChecker.getUpdate(versions, allowedReleaseChannels = releaseChannels)
        assertEquals(v1,
            updateChecker.getUpdate(versions) {
                AppPreferences(releaseChannels = releaseChannels ?: emptyList())
            }
        )
        assertEquals(v1,
            updateChecker.getUpdate(versions, allowedReleaseChannels = releaseChannels) {
                AppPreferences(releaseChannels = releaseChannels ?: emptyList())
            }
        )
        return v1
    }

    private data class Version(
        override val versionCode: Long,
        override val signer: SignerV2? = null,
        override val releaseChannels: List<String>? = null,
        // the manifest is only needed for compatibility checking which we can test differently
        override val packageManifest: PackageManifest = object : PackageManifest {
            override val minSdkVersion: Int? = null
            override val maxSdkVersion: Int? = null
            override val featureNames: List<String>? = null
            override val nativecode: List<String>? = null
        },
        override val hasKnownVulnerability: Boolean = false,
    ) : PackageVersion

    private data class AppPreferences(
        override val ignoreVersionCodeUpdate: Long = 0,
        override val releaseChannels: List<String> = emptyList(),
    ) : PackagePreference

}

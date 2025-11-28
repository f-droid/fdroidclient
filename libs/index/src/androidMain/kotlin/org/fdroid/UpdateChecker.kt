package org.fdroid

import android.content.pm.PackageInfo
import android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
import androidx.core.content.pm.PackageInfoCompat
import org.fdroid.index.IndexUtils.getPackageSigner
import org.fdroid.index.v2.PackageVersion

public interface PackagePreference {
    public val ignoreVersionCodeUpdate: Long
    public val releaseChannels: List<String>
}

public class UpdateChecker(
    private val compatibilityChecker: CompatibilityChecker,
) {

    /**
     * Returns a [PackageVersion] for the given [packageInfo] that is the suggested update
     * or null if there is no suitable update in [versions].
     *
     * @param versions a **sorted** list of [PackageVersion] with highest version code first.
     * @param packageInfo needs to be retrieved with [GET_SIGNING_CERTIFICATES]
     * @param releaseChannels optional list of release channels to consider on top of stable.
     * If this is null or empty, only versions without channel (stable) will be considered.
     * @param preferencesGetter an optional way to consider additional per app preferences
     * @param includeKnownVulnerabilities if true,
     * versions with the [PackageInfo.getLongVersionCode] will be returned
     * if [PackageVersion.hasKnownVulnerability] is true, even without real update.
     */
    public fun <T : PackageVersion> getUpdate(
        versions: List<T>,
        packageInfo: PackageInfo,
        releaseChannels: List<String>? = null,
        includeKnownVulnerabilities: Boolean = false,
        preferencesGetter: (() -> PackagePreference?)? = null,
    ): T? = getUpdate(
        versions = versions,
        allowedSignersGetter = {
            // always gives us the oldest signer, even if they rotated certs by now
            @Suppress("DEPRECATION")
            packageInfo.signatures?.map { getPackageSigner(it.toByteArray()) }?.toSet()
        },
        installedVersionCode = PackageInfoCompat.getLongVersionCode(packageInfo),
        allowedReleaseChannels = releaseChannels,
        preferencesGetter = preferencesGetter,
        includeKnownVulnerabilities = includeKnownVulnerabilities,
    )

    /**
     * Returns the [PackageVersion] that is suggested for a new installation
     * or null if there is no suitable candidate in [versions].
     *
     * @param versions a **sorted** list of [PackageVersion] with highest version code first.
     * @param preferredSigner The SHA-256 hash of the signing certificate in lower-case hex.
     * Only versions from this signer will be considered for installation.
     * @param releaseChannels optional list of release channels to consider on top of stable.
     * If this is null or empty, only versions without channel (stable) will be considered.
     * @param preferencesGetter an optional way to consider additional per app preferences
     */
    public fun <T : PackageVersion> getSuggestedVersion(
        versions: List<T>,
        preferredSigner: String?,
        releaseChannels: List<String>? = null,
        preferencesGetter: (() -> PackagePreference?)? = null,
    ): T? = getUpdate(
        versions = versions,
        allowedSignersGetter = preferredSigner?.let { { setOf(it) } },
        allowedReleaseChannels = releaseChannels,
        preferencesGetter = preferencesGetter,
    )

    /**
     * Returns the [PackageVersion] that is the suggested update
     * for the given [installedVersionCode] or suggested for new installed if the given code is 0,
     * or null if there is no suitable candidate in [versions].
     *
     * @param versions a **sorted** list of [PackageVersion] with highest version code first.
     * @param allowedSignersGetter should return set of SHA-256 hashes of the signing certificates
     * in lower-case hex. Only versions from these signers will be considered for installation.
     * This is is null or returns null, all signers will be allowed.
     * If the set of signers is empty, no signers will be allowed, i.e. only apps without signer.
     * @param allowedReleaseChannels optional list of release channels to consider on top of stable.
     * If this is null or empty, only versions without channel (stable) will be considered.
     * @param preferencesGetter an optional way to consider additional per app preferences.
     * @param includeKnownVulnerabilities if true, versions with the [installedVersionCode]
     * will be returned if [PackageVersion.hasKnownVulnerability] is true, even without real update.
     */
    public fun <T : PackageVersion> getUpdate(
        versions: List<T>,
        allowedSignersGetter: (() -> Set<String>?)? = null,
        installedVersionCode: Long = 0,
        allowedReleaseChannels: List<String>? = null,
        includeKnownVulnerabilities: Boolean = false,
        preferencesGetter: (() -> PackagePreference?)? = null,
    ): T? = getUpdates(
        versions = versions,
        allowedSignersGetter = allowedSignersGetter,
        installedVersionCode = installedVersionCode,
        allowedReleaseChannels = allowedReleaseChannels,
        includeKnownVulnerabilities = includeKnownVulnerabilities,
        preferencesGetter = preferencesGetter,
    ).firstOrNull() // just return matching update with highest version code, don't look at others

    /**
     * Same as [getUpdate], but gets a list of all possible updates
     * beginning from highest version code.
     *
     * This usually isn't useful unless you need to pick a certain update with your own criteria.
     */
    public fun <T : PackageVersion> getUpdates(
        versions: List<T>,
        allowedSignersGetter: (() -> Set<String>?)? = null,
        installedVersionCode: Long = 0,
        allowedReleaseChannels: List<String>? = null,
        includeKnownVulnerabilities: Boolean = false,
        preferencesGetter: (() -> PackagePreference?)? = null,
    ): Sequence<T> = sequence {
        // getting signers is rather expensive, so we only do that when there's update candidates
        val allowedSigners by lazy { allowedSignersGetter?.let { it() } }
        versions.iterator().forEach versions@{ version ->
            // if the installed version has a known vulnerability, we return it as well
            if (includeKnownVulnerabilities &&
                version.versionCode == installedVersionCode &&
                version.hasKnownVulnerability
            ) yield(version)
            // if version code is not higher than installed skip package as list is sorted
            if (version.versionCode <= installedVersionCode) return@sequence
            // we don't support versions that have multiple signers
            if (version.signer?.hasMultipleSigners == true) return@versions
            // skip incompatible versions
            if (!compatibilityChecker.isCompatible(version.packageManifest)) return@versions
            // check if we should ignore this version code
            val packagePreference = preferencesGetter?.let { it() }
            val ignoreVersionCode = packagePreference?.ignoreVersionCodeUpdate ?: 0
            if (ignoreVersionCode >= version.versionCode) return@versions
            // check if release channel of version is allowed
            val hasAllowedReleaseChannel = hasAllowedReleaseChannel(
                allowedReleaseChannels = allowedReleaseChannels?.toMutableSet() ?: LinkedHashSet(),
                versionReleaseChannels = version.releaseChannels?.toSet(),
                packagePreference = packagePreference,
            )
            if (!hasAllowedReleaseChannel) return@versions
            // check if this version's signer is allowed
            val versionSigners = version.signer?.sha256?.toSet()
            // F-Droid allows versions without a signer entry, allow those and if no allowed signers
            if (versionSigners != null && allowedSigners != null) {
                if (versionSigners.intersect(allowedSigners!!).isEmpty()) return@versions
            }
            // no need to see other versions, we got the highest version code per sorting
            yield(version)
        }
    }

    private fun hasAllowedReleaseChannel(
        allowedReleaseChannels: MutableSet<String>,
        versionReleaseChannels: Set<String>?,
        packagePreference: PackagePreference?,
    ): Boolean {
        // no channels (aka stable version) is always allowed
        if (versionReleaseChannels.isNullOrEmpty()) return true

        // add release channels from package preferences into the ones we allow
        val extraChannels = packagePreference?.releaseChannels
        if (!extraChannels.isNullOrEmpty()) {
            allowedReleaseChannels.addAll(extraChannels)
        }
        // if allowed releases channels are empty (only stable) don't consider this version
        if (allowedReleaseChannels.isEmpty()) return false
        // don't consider version with non-matching release channel
        if (allowedReleaseChannels.intersect(versionReleaseChannels).isEmpty()) return false
        // one of the allowed channels is present in this version
        return true
    }

}

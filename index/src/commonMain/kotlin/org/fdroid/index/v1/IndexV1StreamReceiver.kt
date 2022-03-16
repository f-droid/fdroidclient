package org.fdroid.index.v1

import org.fdroid.index.v2.AntiFeatureV2
import org.fdroid.index.v2.CategoryV2
import org.fdroid.index.v2.MetadataV2
import org.fdroid.index.v2.PackageVersionV2
import org.fdroid.index.v2.ReleaseChannelV2
import org.fdroid.index.v2.RepoV2

@Deprecated("Use IndexV2 instead")
public interface IndexV1StreamReceiver {

    public fun receive(repoId: Long, repo: RepoV2, version: Int, certificate: String?)
    public fun receive(repoId: Long, packageId: String, m: MetadataV2)
    public fun receive(repoId: Long, packageId: String, v: Map<String, PackageVersionV2>)

    public fun updateRepo(
        repoId: Long,
        antiFeatures: Map<String, AntiFeatureV2>,
        categories: Map<String, CategoryV2>,
        releaseChannels: Map<String, ReleaseChannelV2>,
    )

    /**
     * Updates [MetadataV2.preferredSigner] with the given [preferredSigner]
     * for the given [packageId].
     */
    public fun updateAppMetadata(repoId: Long, packageId: String, preferredSigner: String?)

}

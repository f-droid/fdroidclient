package org.fdroid.index.v1

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.fdroid.index.DEFAULT_LOCALE
import org.fdroid.index.v2.AntiFeatureV2
import org.fdroid.index.v2.CategoryV2
import org.fdroid.index.v2.FileV2
import org.fdroid.index.v2.MirrorV2
import org.fdroid.index.v2.ReleaseChannelV2
import org.fdroid.index.v2.RepoV2

@Serializable
public data class IndexV1(
    val repo: RepoV1,
    val requests: Requests = Requests(emptyList(), emptyList()),
    val apps: List<AppV1> = emptyList(),
    val packages: Map<String, List<PackageV1>> = emptyMap(),
)

@Serializable
public data class RepoV1(
    val timestamp: Long,
    val version: Int,
    @SerialName("maxage")
    val maxAge: Int? = null, // missing in izzy repo
    val name: String,
    val icon: String,
    val address: String,
    val description: String,
    val mirrors: List<String> = emptyList(), // missing in izzy repo
) {
    public fun toRepoV2(
        locale: String = DEFAULT_LOCALE,
        antiFeatures: Map<String, AntiFeatureV2>,
        categories: Map<String, CategoryV2>,
        releaseChannels: Map<String, ReleaseChannelV2>,
    ): RepoV2 = RepoV2(
        name = mapOf(locale to name),
        icon = mapOf(locale to FileV2("/icons/$icon")),
        address = address,
        webBaseUrl = null,
        description = mapOf(locale to description),
        mirrors = mirrors.map { MirrorV2(it) },
        timestamp = timestamp,
        antiFeatures = antiFeatures,
        categories = categories,
        releaseChannels = releaseChannels,
    )
}

@Serializable
public data class Requests(
    val install: List<String>,
    val uninstall: List<String>,
)

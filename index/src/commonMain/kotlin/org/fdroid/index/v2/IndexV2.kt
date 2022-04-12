package org.fdroid.index.v2

import kotlinx.serialization.Serializable

@Serializable
public data class EntryV2(
    val timestamp: Long,
    val version: Long,
    val maxAge: Int,
    val index: FileV2,
    val diffs: Map<String, FileV2>,
)

@Serializable
public data class FileV2(
    val name: String,
    val sha256: String? = null,
    val size: Long? = null,
)

@Serializable
public data class IndexV2(
    val repo: RepoV2,
    val packages: Map<String, PackageV2> = emptyMap(),
)

@Serializable
public data class RepoV2(
    val name: String,
    val icon: FileV2? = null,
    val address: String,
    val webBaseUrl: String? = null,
    val description: LocalizedTextV2 = emptyMap(),
    val mirrors: List<MirrorV2> = emptyList(),
    val timestamp: Long,
    val antiFeatures: Map<String, AntiFeatureV2> = emptyMap(),
    val categories: Map<String, CategoryV2> = emptyMap(),
    val releaseChannels: Map<String, ReleaseChannelV2> = emptyMap(),
)

public typealias LocalizedTextV2 = Map<String, String>
public typealias LocalizedFileV2 = Map<String, FileV2>
public typealias LocalizedFileListV2 = Map<String, List<FileV2>>

@Serializable
public data class MirrorV2(
    val url: String,
    val location: String? = null,
)

@Serializable
public data class AntiFeatureV2(
    val icon: FileV2? = null,
    val name: LocalizedTextV2,
    val description: LocalizedTextV2 = emptyMap(),
)

@Serializable
public data class CategoryV2(
    val icon: FileV2? = null,
    val name: LocalizedTextV2,
    val description: LocalizedTextV2 = emptyMap(),
)

@Serializable
public data class ReleaseChannelV2(
    val name: LocalizedTextV2,
    val description: LocalizedTextV2 = emptyMap(),
)

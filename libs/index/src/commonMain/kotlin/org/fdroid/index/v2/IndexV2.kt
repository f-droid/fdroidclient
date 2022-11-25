package org.fdroid.index.v2

import kotlinx.serialization.Serializable

@Serializable
public data class Entry(
    val timestamp: Long,
    val version: Long,
    val maxAge: Int? = null,
    val index: EntryFileV2,
    val diffs: Map<String, EntryFileV2> = emptyMap(),
) {
    /**
     * @return the diff for the given [timestamp] or null if none exists
     * in which case the full [index] should be used.
     */
    public fun getDiff(timestamp: Long): EntryFileV2? {
        return diffs[timestamp.toString()]
    }
}

@Serializable
public data class EntryFileV2(
    val name: String,
    val sha256: String,
    val size: Long,
    val numPackages: Int,
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
) {
    public fun walkFiles(fileConsumer: (FileV2?) -> Unit) {
        repo.walkFiles(fileConsumer)
        packages.values.forEach { it.walkFiles(fileConsumer) }
    }
}

@Serializable
public data class RepoV2(
    val name: LocalizedTextV2 = emptyMap(),
    val icon: LocalizedFileV2 = emptyMap(),
    val address: String,
    val webBaseUrl: String? = null,
    val description: LocalizedTextV2 = emptyMap(),
    val mirrors: List<MirrorV2> = emptyList(),
    val timestamp: Long,
    val antiFeatures: Map<String, AntiFeatureV2> = emptyMap(),
    val categories: Map<String, CategoryV2> = emptyMap(),
    val releaseChannels: Map<String, ReleaseChannelV2> = emptyMap(),
) {
    public fun walkFiles(fileConsumer: (FileV2?) -> Unit) {
        icon.values.forEach { fileConsumer(it) }
        antiFeatures.values.forEach { fileConsumer(it.icon) }
        categories.values.forEach { fileConsumer(it.icon) }
    }
}

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

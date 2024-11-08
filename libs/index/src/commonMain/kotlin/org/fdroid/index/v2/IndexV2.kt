package org.fdroid.index.v2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.fdroid.IndexFile
import org.fdroid.index.IndexParser.json

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
    override val name: String,
    override val sha256: String,
    override val size: Long,
    @SerialName("ipfsCIDv1")
    override val ipfsCidV1: String? = null,
    val numPackages: Int,
) : IndexFile {
    public companion object {
        public fun deserialize(string: String): EntryFileV2 {
            return json.decodeFromString(string)
        }
    }

    public override fun serialize(): String {
        return json.encodeToString(this)
    }
}

@Serializable
public data class FileV2(
    override val name: String,
    override val sha256: String? = null,
    override val size: Long? = null,
    @SerialName("ipfsCIDv1")
    override val ipfsCidV1: String? = null,
) : IndexFile {
    public companion object {
        @JvmStatic
        public fun deserialize(string: String?): FileV2? {
            // we've seen serialized FileV2 objects becoming an empty string after parcelizing them,
            // so we need to account for null *and* empty string here.
            if (string.isNullOrEmpty()) return null
            return json.decodeFromString(string)
        }

        @JvmStatic
        public fun fromPath(path: String): FileV2 = FileV2(path)
    }

    public override fun serialize(): String {
        return json.encodeToString(this)
    }
}

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
        antiFeatures.values.forEach { it.icon.values.forEach { icon -> fileConsumer(icon) } }
        categories.values.forEach { it.icon.values.forEach { icon -> fileConsumer(icon) } }
    }
}

public typealias LocalizedTextV2 = Map<String, String>
public typealias LocalizedFileV2 = Map<String, FileV2>
public typealias LocalizedFileListV2 = Map<String, List<FileV2>>

@Serializable
public data class MirrorV2(
    val url: String,
    val countryCode: String? = null,
)

@Serializable
public data class AntiFeatureV2(
    val icon: LocalizedFileV2 = emptyMap(),
    val name: LocalizedTextV2,
    val description: LocalizedTextV2 = emptyMap(),
)

@Serializable
public data class CategoryV2(
    val icon: LocalizedFileV2 = emptyMap(),
    val name: LocalizedTextV2,
    val description: LocalizedTextV2 = emptyMap(),
)

@Serializable
public data class ReleaseChannelV2(
    val name: LocalizedTextV2,
    val description: LocalizedTextV2 = emptyMap(),
)

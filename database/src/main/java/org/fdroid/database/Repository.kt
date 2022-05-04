package org.fdroid.database

import androidx.core.os.LocaleListCompat
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.Relation
import org.fdroid.index.IndexUtils.getFingerprint
import org.fdroid.index.v2.AntiFeatureV2
import org.fdroid.index.v2.CategoryV2
import org.fdroid.index.v2.FileV2
import org.fdroid.index.v2.LocalizedFileV2
import org.fdroid.index.v2.LocalizedTextV2
import org.fdroid.index.v2.MirrorV2
import org.fdroid.index.v2.ReleaseChannelV2
import org.fdroid.index.v2.RepoV2

@Entity
public data class CoreRepository(
    @PrimaryKey(autoGenerate = true) val repoId: Long = 0,
    val name: LocalizedTextV2 = emptyMap(),
    val icon: LocalizedFileV2?,
    val address: String,
    val webBaseUrl: String? = null,
    val timestamp: Long,
    val version: Int?,
    val maxAge: Int?,
    val description: LocalizedTextV2 = emptyMap(),
    val certificate: String?,
)

internal fun RepoV2.toCoreRepository(
    repoId: Long = 0,
    version: Int,
    certificate: String? = null,
) = CoreRepository(
    repoId = repoId,
    name = name,
    icon = icon,
    address = address,
    webBaseUrl = webBaseUrl,
    timestamp = timestamp,
    version = version,
    maxAge = null,
    description = description,
    certificate = certificate,
)

public data class Repository(
    @Embedded internal val repository: CoreRepository,
    @Relation(
        parentColumn = "repoId",
        entityColumn = "repoId",
    )
    internal val mirrors: List<Mirror>,
    @Relation(
        parentColumn = "repoId",
        entityColumn = "repoId",
    )
    val antiFeatures: List<AntiFeature>,
    @Relation(
        parentColumn = "repoId",
        entityColumn = "repoId",
    )
    val categories: List<Category>,
    @Relation(
        parentColumn = "repoId",
        entityColumn = "repoId",
    )
    val releaseChannels: List<ReleaseChannel>,
    @Relation(
        parentColumn = "repoId",
        entityColumn = "repoId",
    )
    internal val preferences: RepositoryPreferences,
) {
    val repoId: Long get() = repository.repoId
    internal val name: LocalizedTextV2 get() = repository.name
    internal val icon: LocalizedFileV2? get() = repository.icon
    val address: String get() = repository.address
    val webBaseUrl: String? get() = repository.webBaseUrl
    val timestamp: Long get() = repository.timestamp
    val version: Int get() = repository.version ?: 0
    internal val description: LocalizedTextV2 get() = repository.description
    val certificate: String? get() = repository.certificate

    val weight: Int get() = preferences.weight
    val enabled: Boolean get() = preferences.enabled
    val lastUpdated: Long? get() = preferences.lastUpdated
    val lastETag: String? get() = preferences.lastETag
    val userMirrors: List<String> get() = preferences.userMirrors ?: emptyList()
    val disabledMirrors: List<String> get() = preferences.disabledMirrors ?: emptyList()
    val username: String? get() = preferences.username
    val password: String? get() = preferences.password
    val isSwap: Boolean get() = preferences.isSwap

    @delegate:Ignore
    val fingerprint: String? by lazy {
        certificate?.let { getFingerprint(it) }
    }

    /**
     * Returns official and user-added mirrors without the [disabledMirrors].
     */
    public fun getMirrors(): List<org.fdroid.download.Mirror> {
        return getAllMirrors(true).filter {
            !disabledMirrors.contains(it.baseUrl)
        }
    }

    /**
     * Returns all mirrors, including [disabledMirrors].
     */
    @JvmOverloads
    public fun getAllMirrors(includeUserMirrors: Boolean = true): List<org.fdroid.download.Mirror> {
        // FIXME decide whether we need to add our own address here
        return listOf(org.fdroid.download.Mirror(address)) + mirrors.map {
            it.toDownloadMirror()
        } + if (includeUserMirrors) userMirrors.map {
            org.fdroid.download.Mirror(it)
        } else emptyList()
    }

    public fun getName(localeList: LocaleListCompat): String? = name.getBestLocale(localeList)
    public fun getDescription(localeList: LocaleListCompat): String? =
        description.getBestLocale(localeList)

    public fun getIcon(localeList: LocaleListCompat): FileV2? = icon.getBestLocale(localeList)
}

@Entity(
    primaryKeys = ["repoId", "url"],
    foreignKeys = [ForeignKey(
        entity = CoreRepository::class,
        parentColumns = ["repoId"],
        childColumns = ["repoId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
public data class Mirror(
    val repoId: Long,
    val url: String,
    val location: String? = null,
) {
    public fun toDownloadMirror(): org.fdroid.download.Mirror = org.fdroid.download.Mirror(
        baseUrl = url,
        location = location,
    )
}

internal fun MirrorV2.toMirror(repoId: Long) = Mirror(
    repoId = repoId,
    url = url,
    location = location,
)

@Entity(
    primaryKeys = ["repoId", "id"],
    foreignKeys = [ForeignKey(
        entity = CoreRepository::class,
        parentColumns = ["repoId"],
        childColumns = ["repoId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
public data class AntiFeature(
    val repoId: Long,
    val id: String,
    @Embedded(prefix = "icon_") val icon: FileV2? = null,
    val name: LocalizedTextV2,
    val description: LocalizedTextV2,
)

internal fun Map<String, AntiFeatureV2>.toRepoAntiFeatures(repoId: Long) = map {
    AntiFeature(
        repoId = repoId,
        id = it.key,
        icon = it.value.icon,
        name = it.value.name,
        description = it.value.description,
    )
}

@Entity(
    primaryKeys = ["repoId", "id"],
    foreignKeys = [ForeignKey(
        entity = CoreRepository::class,
        parentColumns = ["repoId"],
        childColumns = ["repoId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
public data class Category(
    val repoId: Long,
    val id: String,
    @Embedded(prefix = "icon_") val icon: FileV2? = null,
    val name: LocalizedTextV2,
    val description: LocalizedTextV2,
)

internal fun Map<String, CategoryV2>.toRepoCategories(repoId: Long) = map {
    Category(
        repoId = repoId,
        id = it.key,
        icon = it.value.icon,
        name = it.value.name,
        description = it.value.description,
    )
}

@Entity(
    primaryKeys = ["repoId", "id"],
    foreignKeys = [ForeignKey(
        entity = CoreRepository::class,
        parentColumns = ["repoId"],
        childColumns = ["repoId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
public data class ReleaseChannel(
    val repoId: Long,
    val id: String,
    @Embedded(prefix = "icon_") val icon: FileV2? = null,
    val name: LocalizedTextV2,
    val description: LocalizedTextV2,
)

internal fun Map<String, ReleaseChannelV2>.toRepoReleaseChannel(repoId: Long) = map {
    ReleaseChannel(
        repoId = repoId,
        id = it.key,
        name = it.value.name,
        description = it.value.description,
    )
}

@Entity
public data class RepositoryPreferences(
    @PrimaryKey internal val repoId: Long,
    val weight: Int,
    val enabled: Boolean = true,
    val lastUpdated: Long? = System.currentTimeMillis(),
    val lastETag: String? = null,
    val userMirrors: List<String>? = null,
    val disabledMirrors: List<String>? = null,
    val username: String? = null,
    val password: String? = null,
    val isSwap: Boolean = false, // TODO remove
)

/**
 * A [Repository] which the [FDroidDatabase] gets pre-populated with.
 */
public data class InitialRepository(
    val name: String,
    val address: String,
    val description: String,
    val certificate: String,
    val version: Int,
    val enabled: Boolean,
    val weight: Int,
)

package org.fdroid.database

import androidx.core.os.LocaleListCompat
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation
import org.fdroid.index.v2.AntiFeatureV2
import org.fdroid.index.v2.CategoryV2
import org.fdroid.index.v2.FileV2
import org.fdroid.index.v2.LocalizedTextV2
import org.fdroid.index.v2.MirrorV2
import org.fdroid.index.v2.ReleaseChannelV2
import org.fdroid.index.v2.RepoV2

@Entity
data class CoreRepository(
    @PrimaryKey(autoGenerate = true) val repoId: Long = 0,
    val name: String,
    @Embedded(prefix = "icon_") val icon: FileV2?,
    val address: String,
    val timestamp: Long,
    val description: LocalizedTextV2 = emptyMap(),
    val certificate: String?,
)

fun RepoV2.toCoreRepository(repoId: Long = 0, certificate: String? = null) = CoreRepository(
    repoId = repoId,
    name = name,
    icon = icon,
    address = address,
    timestamp = timestamp,
    description = description,
    certificate = certificate,
)

data class Repository(
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
    val name: String get() = repository.name
    val icon: FileV2? get() = repository.icon
    val address: String get() = repository.address
    val timestamp: Long get() = repository.timestamp
    val description: LocalizedTextV2 get() = repository.description
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

    @JvmOverloads
    fun getMirrors(includeUserMirrors: Boolean = true) = mirrors.map {
        it.toDownloadMirror()
    } + listOf(org.fdroid.download.Mirror(address)) + // FIXME decide whether we need to add this
        if (includeUserMirrors) userMirrors.map {
            org.fdroid.download.Mirror(it)
        } else emptyList()

    fun getDescription(localeList: LocaleListCompat) = description.getBestLocale(localeList)
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
data class Mirror(
    val repoId: Long,
    val url: String,
    val location: String? = null,
) {
    fun toDownloadMirror() = org.fdroid.download.Mirror(
        baseUrl = url,
        location = location,
    )
}

fun MirrorV2.toMirror(repoId: Long) = Mirror(
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
data class AntiFeature(
    val repoId: Long,
    val id: String,
    @Embedded(prefix = "icon_") val icon: FileV2? = null,
    val name: LocalizedTextV2,
    val description: LocalizedTextV2,
)

fun Map<String, AntiFeatureV2>.toRepoAntiFeatures(repoId: Long) = map {
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
data class Category(
    val repoId: Long,
    val id: String,
    @Embedded(prefix = "icon_") val icon: FileV2? = null,
    val name: LocalizedTextV2,
    val description: LocalizedTextV2,
)

fun Map<String, CategoryV2>.toRepoCategories(repoId: Long) = map {
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
data class ReleaseChannel(
    val repoId: Long,
    val id: String,
    @Embedded(prefix = "icon_") val icon: FileV2? = null,
    val name: LocalizedTextV2,
    val description: LocalizedTextV2,
)

fun Map<String, ReleaseChannelV2>.toRepoReleaseChannel(repoId: Long) = map {
    ReleaseChannel(
        repoId = repoId,
        id = it.key,
        name = it.value.name,
        description = it.value.description,
    )
}

@Entity
data class RepositoryPreferences(
    @PrimaryKey internal val repoId: Long,
    val weight: Int,
    val enabled: Boolean = true,
    val lastUpdated: Long? = System.currentTimeMillis(), // TODO set this after repo updates
    val lastETag: String? = null,
    val userMirrors: List<String>? = null,
    val disabledMirrors: List<String>? = null,
    val username: String? = null,
    val password: String? = null,
    val isSwap: Boolean = false, // TODO remove
)

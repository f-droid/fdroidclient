package org.fdroid.database

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
)

fun RepoV2.toCoreRepository(repoId: Long = 0) = CoreRepository(
    repoId = repoId,
    name = name,
    icon = icon,
    address = address,
    timestamp = timestamp,
    description = description,
)

data class Repository(
    @Embedded val repository: CoreRepository,
    @Relation(
        parentColumn = "repoId",
        entityColumn = "repoId",
    )
    val mirrors: List<Mirror>,
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
)

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
)

fun MirrorV2.toMirror(repoId: Long) = Mirror(
    repoId = repoId,
    url = url,
    location = location,
)

@Entity(
    primaryKeys = ["repoId", "name"],
    foreignKeys = [ForeignKey(
        entity = CoreRepository::class,
        parentColumns = ["repoId"],
        childColumns = ["repoId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class AntiFeature(
    val repoId: Long,
    val name: String,
    @Embedded(prefix = "icon_") val icon: FileV2? = null,
    val description: LocalizedTextV2,
)

fun Map<String, AntiFeatureV2>.toRepoAntiFeatures(repoId: Long) = map {
    AntiFeature(
        repoId = repoId,
        name = it.key,
        icon = it.value.icon,
        description = it.value.description,
    )
}

@Entity(
    primaryKeys = ["repoId", "name"],
    foreignKeys = [ForeignKey(
        entity = CoreRepository::class,
        parentColumns = ["repoId"],
        childColumns = ["repoId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class Category(
    val repoId: Long,
    val name: String,
    @Embedded(prefix = "icon_") val icon: FileV2? = null,
    val description: LocalizedTextV2,
)

fun Map<String, CategoryV2>.toRepoCategories(repoId: Long) = map {
    Category(
        repoId = repoId,
        name = it.key,
        icon = it.value.icon,
        description = it.value.description,
    )
}

@Entity(
    primaryKeys = ["repoId", "name"],
    foreignKeys = [ForeignKey(
        entity = CoreRepository::class,
        parentColumns = ["repoId"],
        childColumns = ["repoId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class ReleaseChannel(
    val repoId: Long,
    val name: String,
    @Embedded(prefix = "icon_") val icon: FileV2? = null,
    val description: LocalizedTextV2,
)

fun Map<String, ReleaseChannelV2>.toRepoReleaseChannel(repoId: Long) = map {
    ReleaseChannel(
        repoId = repoId,
        name = it.key,
        description = it.value.description,
    )
}

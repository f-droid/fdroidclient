package org.fdroid.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import org.fdroid.index.v2.Author
import org.fdroid.index.v2.Donation
import org.fdroid.index.v2.FileV2
import org.fdroid.index.v2.LocalizedFileListV2
import org.fdroid.index.v2.LocalizedFileV2
import org.fdroid.index.v2.LocalizedTextV2
import org.fdroid.index.v2.MetadataV2
import org.fdroid.index.v2.Screenshots

@Entity(
    primaryKeys = ["repoId", "packageId"],
    foreignKeys = [ForeignKey(
        entity = CoreRepository::class,
        parentColumns = ["repoId"],
        childColumns = ["repoId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class AppMetadata(
    val repoId: Long,
    val packageId: String,
    val added: Long,
    val lastUpdated: Long,
    val name: LocalizedTextV2? = null,
    val summary: LocalizedTextV2? = null,
    val description: LocalizedTextV2? = null,
    val webSite: String? = null,
    val changelog: String? = null,
    val license: String? = null,
    val sourceCode: String? = null,
    val issueTracker: String? = null,
    val translation: String? = null,
    val preferredSigner: String? = null,
    val video: LocalizedTextV2? = null,
    @Embedded(prefix = "author_") val author: Author? = Author(),
    @Embedded(prefix = "donation_") val donation: Donation? = Donation(),
    val categories: List<String>? = null,
)

fun MetadataV2.toAppMetadata(repoId: Long, packageId: String) = AppMetadata(
    repoId = repoId,
    packageId = packageId,
    added = added,
    lastUpdated = lastUpdated,
    name = name,
    summary = summary,
    description = description,
    webSite = webSite,
    changelog = changelog,
    license = license,
    sourceCode = sourceCode,
    issueTracker = issueTracker,
    translation = translation,
    preferredSigner = preferredSigner,
    video = video,
    author = if (author?.isNull == true) null else author,
    donation = if (donation?.isNull == true) null else donation,
    categories = categories,
)

data class App(
    val metadata: AppMetadata,
    val icon: LocalizedFileV2? = null,
    val featureGraphic: LocalizedFileV2? = null,
    val promoGraphic: LocalizedFileV2? = null,
    val tvBanner: LocalizedFileV2? = null,
    val screenshots: Screenshots? = null,
)

@Entity(
    primaryKeys = ["repoId", "packageId", "type", "locale"],
    foreignKeys = [ForeignKey(
        entity = AppMetadata::class,
        parentColumns = ["repoId", "packageId"],
        childColumns = ["repoId", "packageId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class LocalizedFile(
    val repoId: Long,
    val packageId: String,
    val type: String,
    val locale: String,
    val name: String,
    val sha256: String? = null,
    val size: Long? = null,
)

fun LocalizedFileV2.toLocalizedFile(
    repoId: Long,
    packageId: String,
    type: String,
): List<LocalizedFile> = map { (locale, file) ->
    LocalizedFile(
        repoId = repoId,
        packageId = packageId,
        type = type,
        locale = locale,
        name = file.name,
        sha256 = file.sha256,
        size = file.size,
    )
}

fun List<LocalizedFile>.toLocalizedFileV2(type: String): LocalizedFileV2? = filter { file ->
    file.type == type
}.associate { file ->
    file.locale to FileV2(
        name = file.name,
        sha256 = file.sha256,
        size = file.size,
    )
}.ifEmpty { null }

@Entity(
    primaryKeys = ["repoId", "packageId", "type", "locale", "name"],
    foreignKeys = [ForeignKey(
        entity = AppMetadata::class,
        parentColumns = ["repoId", "packageId"],
        childColumns = ["repoId", "packageId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class LocalizedFileList(
    val repoId: Long,
    val packageId: String,
    val type: String,
    val locale: String,
    val name: String,
    val sha256: String? = null,
    val size: Long? = null,
)

fun LocalizedFileListV2.toLocalizedFileList(
    repoId: Long,
    packageId: String,
    type: String,
): List<LocalizedFileList> = flatMap { (locale, files) ->
    files.map { file ->
        LocalizedFileList(
            repoId = repoId,
            packageId = packageId,
            type = type,
            locale = locale,
            name = file.name,
            sha256 = file.sha256,
            size = file.size,
        )
    }
}

fun List<LocalizedFileList>.toLocalizedFileListV2(type: String): LocalizedFileListV2? {
    val map = HashMap<String, List<FileV2>>()
    iterator().forEach { file ->
        if (file.type != type) return@forEach
        val list = map.getOrPut(file.locale) { ArrayList() } as ArrayList
        list.add(FileV2(
            name = file.name,
            sha256 = file.sha256,
            size = file.size,
        ))
    }
    return map.ifEmpty { null }
}

package org.fdroid.database

import androidx.core.os.LocaleListCompat
import androidx.room.DatabaseView
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Relation
import org.fdroid.database.Converters.fromStringToLocalizedTextV2
import org.fdroid.database.Converters.fromStringToMapOfLocalizedTextV2
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
public data class AppMetadata(
    val repoId: Long,
    val packageId: String,
    val added: Long,
    val lastUpdated: Long,
    val name: LocalizedTextV2? = null,
    val summary: LocalizedTextV2? = null,
    val description: LocalizedTextV2? = null,
    val localizedName: String? = null,
    val localizedSummary: String? = null,
    val webSite: String? = null,
    val changelog: String? = null,
    val license: String? = null,
    val sourceCode: String? = null,
    val issueTracker: String? = null,
    val translation: String? = null,
    val preferredSigner: String? = null, // TODO use platformSig if an APK matches it
    val video: LocalizedTextV2? = null,
    @Embedded(prefix = "author_") val author: Author? = Author(),
    @Embedded(prefix = "donation_") val donation: Donation? = Donation(),
    val categories: List<String>? = null,
    /**
     * Whether the app is compatible with the current device.
     * This value will be computed and is always false until that happened.
     * So to always get correct data, this MUST happen within the same transaction
     * that adds the [AppMetadata].
     */
    val isCompatible: Boolean,
)

internal fun MetadataV2.toAppMetadata(
    repoId: Long,
    packageId: String,
    isCompatible: Boolean = false,
) = AppMetadata(
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
    isCompatible = isCompatible,
)

public data class App(
    val metadata: AppMetadata,
    val icon: LocalizedFileV2? = null,
    val featureGraphic: LocalizedFileV2? = null,
    val promoGraphic: LocalizedFileV2? = null,
    val tvBanner: LocalizedFileV2? = null,
    val screenshots: Screenshots? = null,
) {
    public fun getName(localeList: LocaleListCompat): String? =
        metadata.name.getBestLocale(localeList)

    public fun getSummary(localeList: LocaleListCompat): String? =
        metadata.summary.getBestLocale(localeList)

    public fun getDescription(localeList: LocaleListCompat): String? =
        metadata.description.getBestLocale(localeList)

    public fun getVideo(localeList: LocaleListCompat): String? =
        metadata.video.getBestLocale(localeList)

    public fun getIcon(localeList: LocaleListCompat): FileV2? = icon.getBestLocale(localeList)
    public fun getFeatureGraphic(localeList: LocaleListCompat): FileV2? =
        featureGraphic.getBestLocale(localeList)

    public fun getPromoGraphic(localeList: LocaleListCompat): FileV2? =
        promoGraphic.getBestLocale(localeList)

    public fun getTvBanner(localeList: LocaleListCompat): FileV2? =
        tvBanner.getBestLocale(localeList)

    // TODO remove ?.map { it.name } when client can handle FileV2
    public fun getPhoneScreenshots(localeList: LocaleListCompat): List<String> =
        screenshots?.phone.getBestLocale(localeList)?.map { it.name } ?: emptyList()

    public fun getSevenInchScreenshots(localeList: LocaleListCompat): List<String> =
        screenshots?.sevenInch.getBestLocale(localeList)?.map { it.name } ?: emptyList()

    public fun getTenInchScreenshots(localeList: LocaleListCompat): List<String> =
        screenshots?.tenInch.getBestLocale(localeList)?.map { it.name } ?: emptyList()

    public fun getTvScreenshots(localeList: LocaleListCompat): List<String> =
        screenshots?.tv.getBestLocale(localeList)?.map { it.name } ?: emptyList()

    public fun getWearScreenshots(localeList: LocaleListCompat): List<String> =
        screenshots?.wear.getBestLocale(localeList)?.map { it.name } ?: emptyList()
}

public data class AppOverviewItem(
    public val repoId: Long,
    public val packageId: String,
    public val added: Long,
    public val lastUpdated: Long,
    internal val name: LocalizedTextV2? = null,
    internal val summary: LocalizedTextV2? = null,
    @Relation(
        parentColumn = "packageId",
        entityColumn = "packageId",
    )
    internal val localizedIcon: List<LocalizedIcon>? = null,
) {
    public fun getName(localeList: LocaleListCompat): String? = name.getBestLocale(localeList)
    public fun getSummary(localeList: LocaleListCompat): String? = summary.getBestLocale(localeList)
    public fun getIcon(localeList: LocaleListCompat): String? =
        localizedIcon?.toLocalizedFileV2().getBestLocale(localeList)?.name
}

public data class AppListItem @JvmOverloads constructor(
    public val repoId: Long,
    public val packageId: String,
    internal val name: String?,
    internal val summary: String?,
    internal val antiFeatures: String?,
    @Relation(
        parentColumn = "packageId",
        entityColumn = "packageId",
    )
    internal val localizedIcon: List<LocalizedIcon>?,
    /**
     * If true, this this app has at least one version that is compatible with this device.
     */
    public val isCompatible: Boolean,
    /**
     * The name of the installed version, null if this app is not installed.
     */
    @Ignore
    public val installedVersionName: String? = null,
    @Ignore
    public val installedVersionCode: Long? = null,
) {
    public fun getName(localeList: LocaleListCompat): String? {
        // queries for this class return a larger number, so we convert on demand
        return fromStringToLocalizedTextV2(name).getBestLocale(localeList)
    }

    public fun getSummary(localeList: LocaleListCompat): String? {
        // queries for this class return a larger number, so we convert on demand
        return fromStringToLocalizedTextV2(summary).getBestLocale(localeList)
    }

    public fun getAntiFeatureNames(): List<String> {
        return fromStringToMapOfLocalizedTextV2(antiFeatures)?.map { it.key } ?: emptyList()
    }

    public fun getIcon(localeList: LocaleListCompat): String? =
        localizedIcon?.toLocalizedFileV2().getBestLocale(localeList)?.name
}

public data class UpdatableApp(
    public val packageId: String,
    public val installedVersionCode: Long,
    public val upgrade: AppVersion,
    internal val name: LocalizedTextV2? = null,
    public val summary: String? = null,
    @Relation(
        parentColumn = "packageId",
        entityColumn = "packageId",
    )
    internal val localizedIcon: List<LocalizedIcon>? = null,
) {
    public fun getName(localeList: LocaleListCompat): String? = name.getBestLocale(localeList)
    public fun getIcon(localeList: LocaleListCompat): FileV2? =
        localizedIcon?.toLocalizedFileV2().getBestLocale(localeList)
}

internal fun <T> Map<String, T>?.getBestLocale(localeList: LocaleListCompat): T? {
    if (isNullOrEmpty()) return null
    val firstMatch = localeList.getFirstMatch(keys.toTypedArray()) ?: error("not empty: $keys")
    val tag = firstMatch.toLanguageTag()
    // try first matched tag first (usually has region tag, e.g. de-DE)
    return get(tag) ?: run {
        // split away region tag and try language only
        val langTag = tag.split('-')[0]
        // try language, then English and then just take the first of the list
        get(langTag) ?: get("en-US") ?: get("en") ?: values.first()
    }
}

internal interface IFile {
    val type: String
    val locale: String
    val name: String
    val sha256: String?
    val size: Long?
}

@Entity(
    primaryKeys = ["repoId", "packageId", "type", "locale"],
    foreignKeys = [ForeignKey(
        entity = AppMetadata::class,
        parentColumns = ["repoId", "packageId"],
        childColumns = ["repoId", "packageId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
internal data class LocalizedFile(
    val repoId: Long,
    val packageId: String,
    override val type: String,
    override val locale: String,
    override val name: String,
    override val sha256: String? = null,
    override val size: Long? = null,
) : IFile

internal fun LocalizedFileV2.toLocalizedFile(
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

internal fun List<IFile>.toLocalizedFileV2(type: String? = null): LocalizedFileV2? {
    return (if (type != null) filter { file -> file.type == type } else this).associate { file ->
        file.locale to FileV2(
            name = file.name,
            sha256 = file.sha256,
            size = file.size,
        )
    }.ifEmpty { null }
}

// TODO write test that ensures that in case of the same locale,
//  only the one from the repo with higher weight is returned
@DatabaseView("""SELECT * FROM LocalizedFile
    JOIN RepositoryPreferences AS prefs USING (repoId)
    WHERE type='icon' GROUP BY repoId, packageId, locale HAVING MAX(prefs.weight)""")
public data class LocalizedIcon(
    val repoId: Long,
    val packageId: String,
    override val type: String,
    override val locale: String,
    override val name: String,
    override val sha256: String? = null,
    override val size: Long? = null,
) : IFile

@Entity(
    primaryKeys = ["repoId", "packageId", "type", "locale", "name"],
    foreignKeys = [ForeignKey(
        entity = AppMetadata::class,
        parentColumns = ["repoId", "packageId"],
        childColumns = ["repoId", "packageId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
internal data class LocalizedFileList(
    val repoId: Long,
    val packageId: String,
    val type: String,
    val locale: String,
    val name: String,
    val sha256: String? = null,
    val size: Long? = null,
)

internal fun LocalizedFileListV2.toLocalizedFileList(
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

internal fun List<LocalizedFileList>.toLocalizedFileListV2(type: String): LocalizedFileListV2? {
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

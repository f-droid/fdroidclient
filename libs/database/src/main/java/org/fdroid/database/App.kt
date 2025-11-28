package org.fdroid.database

import android.content.res.Resources
import androidx.core.os.ConfigurationCompat.getLocales
import androidx.core.os.LocaleListCompat
import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.Ignore
import androidx.room.Relation
import org.fdroid.LocaleChooser.getBestLocale
import org.fdroid.database.Converters.fromStringToMapOfLocalizedTextV2
import org.fdroid.index.v2.FileV2
import org.fdroid.index.v2.LocalizedFileListV2
import org.fdroid.index.v2.LocalizedFileV2
import org.fdroid.index.v2.LocalizedTextV2
import org.fdroid.index.v2.MetadataV2
import org.fdroid.index.v2.Screenshots

public interface MinimalApp {
    public val repoId: Long
    public val packageName: String
    public val name: String?
    public val summary: String?
    public fun getIcon(localeList: LocaleListCompat): FileV2?
}

/**
 * The detailed metadata for an app.
 * Almost all fields are optional.
 * This largely represents [MetadataV2] in a database table.
 */
@Entity(
    tableName = AppMetadata.TABLE,
    primaryKeys = ["repoId", "packageName"],
    foreignKeys = [ForeignKey(
        entity = CoreRepository::class,
        parentColumns = ["repoId"],
        childColumns = ["repoId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
public data class AppMetadata(
    public val repoId: Long,
    public val packageName: String,
    public val added: Long,
    public val lastUpdated: Long,
    public val name: LocalizedTextV2? = null,
    public val summary: LocalizedTextV2? = null,
    public val description: LocalizedTextV2? = null,
    public val localizedName: String? = null,
    public val localizedSummary: String? = null,
    public val webSite: String? = null,
    public val changelog: String? = null,
    public val license: String? = null,
    public val sourceCode: String? = null,
    public val issueTracker: String? = null,
    public val translation: String? = null,
    public val preferredSigner: String? = null,
    public val video: LocalizedTextV2? = null,
    public val authorName: String? = null,
    public val authorEmail: String? = null,
    public val authorWebSite: String? = null,
    public val authorPhone: String? = null,
    public val donate: List<String>? = null,
    public val liberapayID: String? = null,
    public val liberapay: String? = null,
    public val openCollective: String? = null,
    public val bitcoin: String? = null,
    public val litecoin: String? = null,
    public val flattrID: String? = null,
    public val categories: List<String>? = null,
    /**
     * Whether the app is compatible with the current device.
     * This value will be computed and is always false until that happened.
     * So to always get correct data, this MUST happen within the same transaction
     * that adds the [AppMetadata].
     */
    public val isCompatible: Boolean,
) {
    internal companion object {
        const val TABLE = "AppMetadata"
    }
}

internal fun MetadataV2.toAppMetadata(
    repoId: Long,
    packageName: String,
    isCompatible: Boolean = false,
    locales: LocaleListCompat = getLocales(Resources.getSystem().configuration),
) = AppMetadata(
    repoId = repoId,
    packageName = packageName,
    added = added,
    lastUpdated = lastUpdated,
    name = name.zero(),
    summary = summary.zero(),
    description = description.zero(),
    localizedName = name.getBestLocale(locales),
    localizedSummary = summary.getBestLocale(locales),
    webSite = webSite,
    changelog = changelog,
    license = license,
    sourceCode = sourceCode,
    issueTracker = issueTracker,
    translation = translation,
    preferredSigner = preferredSigner,
    video = video,
    authorName = authorName,
    authorEmail = authorEmail,
    authorWebSite = authorWebSite,
    authorPhone = authorPhone,
    donate = donate,
    liberapayID = liberapayID,
    liberapay = liberapay,
    openCollective = openCollective,
    bitcoin = bitcoin,
    litecoin = litecoin,
    flattrID = flattrID,
    categories = categories,
    isCompatible = isCompatible,
)

/**
 * Introduce zero whitespace for CJK (Chinese, Japanese, Korean) languages.
 * This is needed, because the sqlite tokenizers available to us either handle those languages
 * or do diacritics removals.
 * Since we can't remove diacritics here ourselves,
 * we help the tokenizer for CJK languages instead.
 */
internal fun LocalizedTextV2?.zero(): LocalizedTextV2? {
    if (this == null) return null
    return toMutableMap().mapValues { (locale, text) ->
        if (locale.startsWith("zh") || locale.startsWith("ja") || locale.startsWith("ko")) {
            StringBuilder().apply {
                text.forEachIndexed { i, char ->
                    if (Character.isIdeographic(char.code) && i + 1 < text.length) {
                        append(char)
                        append("\u200B")
                    } else {
                        append(char)
                    }
                }
            }.toString()
        } else {
            text
        }
    }
}

@Entity(tableName = AppMetadataFts.TABLE)
@Fts4(
    contentEntity = AppMetadata::class,
    // make FTS for non-ASCII characters case insensitive, CJK languages are handled separately,
    // because there's no tokenizer available that handles everything
    tokenizer = FtsOptions.TOKENIZER_UNICODE61,
    // can't use remove_diacritics=2 because it is SDK_INT >=30
    // see: https://www.twisterrob.net/blog/2023/10/sqlite-unicode61-remove-diacritics-2.html
    // separators=. is mainly for package name search
    // tokenchars=- is so that searching for F-Droid works as expected
    tokenizerArgs = ["remove_diacritics=1", "separators=.", "tokenchars=-"],
    notIndexed = ["repoId"],
)
internal data class AppMetadataFts(
    val repoId: Long,
    val name: String? = null,
    val summary: String? = null,
    val description: String? = null,
    val authorName: String? = null,
    val packageName: String,
) {
    internal companion object {
        const val TABLE = "AppMetadataFts"
    }
}

/**
 * A class to represent all data of an App.
 * It combines the metadata and localized filed such as icons and screenshots.
 */
@ConsistentCopyVisibility
public data class App internal constructor(
    @Embedded public val metadata: AppMetadata,
    @Relation(
        parentColumn = "packageName",
        entityColumn = "packageName",
    )
    private val localizedFiles: List<LocalizedFile>? = null,
    @Relation(
        parentColumn = "packageName",
        entityColumn = "packageName",
    )
    private val localizedFileLists: List<LocalizedFileList>? = null,
) : MinimalApp {
    public override val repoId: Long get() = metadata.repoId
    override val packageName: String get() = metadata.packageName
    public val authorName: String? get() = metadata.authorName
    internal val icon: LocalizedFileV2? get() = getLocalizedFile("icon")
    internal val featureGraphic: LocalizedFileV2? get() = getLocalizedFile("featureGraphic")
    internal val promoGraphic: LocalizedFileV2? get() = getLocalizedFile("promoGraphic")
    internal val tvBanner: LocalizedFileV2? get() = getLocalizedFile("tvBanner")
    internal val screenshots: Screenshots?
        get() = if (localizedFileLists.isNullOrEmpty()) null else Screenshots(
            phone = getLocalizedFileList("phone"),
            sevenInch = getLocalizedFileList("sevenInch"),
            tenInch = getLocalizedFileList("tenInch"),
            wear = getLocalizedFileList("wear"),
            tv = getLocalizedFileList("tv"),
        ).takeIf { !it.isNull }

    private fun getLocalizedFile(type: String): LocalizedFileV2? {
        return localizedFiles?.filter { localizedFile ->
            localizedFile.repoId == metadata.repoId && localizedFile.type == type
        }?.toLocalizedFileV2()
    }

    private fun getLocalizedFileList(type: String): LocalizedFileListV2? {
        val map = HashMap<String, List<FileV2>>()
        localizedFileLists?.iterator()?.forEach { file ->
            if (file.repoId != metadata.repoId || file.type != type) return@forEach
            val list = map.getOrPut(file.locale) { ArrayList() } as ArrayList
            list.add(
                FileV2(
                    name = file.name,
                    sha256 = file.sha256,
                    size = file.size,
                    ipfsCidV1 = file.ipfsCidV1,
                )
            )
        }
        return map.ifEmpty { null }
    }

    public override val name: String? get() = metadata.localizedName
    public override val summary: String? get() = metadata.localizedSummary
    public fun getDescription(localeList: LocaleListCompat): String? =
        metadata.description.getBestLocale(localeList)

    public fun getVideo(localeList: LocaleListCompat): String? =
        metadata.video.getBestLocale(localeList)

    public override fun getIcon(localeList: LocaleListCompat): FileV2? =
        icon.getBestLocale(localeList)

    public fun getFeatureGraphic(localeList: LocaleListCompat): FileV2? =
        featureGraphic.getBestLocale(localeList)

    public fun getPromoGraphic(localeList: LocaleListCompat): FileV2? =
        promoGraphic.getBestLocale(localeList)

    public fun getTvBanner(localeList: LocaleListCompat): FileV2? =
        tvBanner.getBestLocale(localeList)

    public fun getPhoneScreenshots(localeList: LocaleListCompat): List<FileV2> =
        screenshots?.phone.getBestLocale(localeList) ?: emptyList()

    public fun getSevenInchScreenshots(localeList: LocaleListCompat): List<FileV2> =
        screenshots?.sevenInch.getBestLocale(localeList) ?: emptyList()

    public fun getTenInchScreenshots(localeList: LocaleListCompat): List<FileV2> =
        screenshots?.tenInch.getBestLocale(localeList) ?: emptyList()

    public fun getTvScreenshots(localeList: LocaleListCompat): List<FileV2> =
        screenshots?.tv.getBestLocale(localeList) ?: emptyList()

    public fun getWearScreenshots(localeList: LocaleListCompat): List<FileV2> =
        screenshots?.wear.getBestLocale(localeList) ?: emptyList()
}

/**
 * A lightweight variant of [App] with minimal data, usually used to provide an overview of apps
 * without going into all details that get presented on a dedicated screen.
 * The reduced data footprint helps with fast loading many items at once.
 *
 * It includes [antiFeatureKeys] so some clients can apply filters to them.
 */
@ConsistentCopyVisibility
public data class AppOverviewItem internal constructor(
    public override val repoId: Long,
    public override val packageName: String,
    public val added: Long,
    public val lastUpdated: Long,
    @ColumnInfo(name = "localizedName")
    @Deprecated("Use getName() method instead.")
    public override val name: String? = null,
    @ColumnInfo(name = "localizedSummary")
    @Deprecated("Use getSummary() method instead.")
    public override val summary: String? = null,
    @ColumnInfo(name = "name")
    internal val internalName: LocalizedTextV2? = null,
    @ColumnInfo(name = "summary")
    internal val internalSummary: LocalizedTextV2? = null,
    public val categories: List<String>? = null,
    internal val antiFeatures: Map<String, LocalizedTextV2>? = null,
    @Relation(
        parentColumn = "packageName",
        entityColumn = "packageName",
    )
    internal val localizedIcon: List<LocalizedIcon>? = null,
    /**
     * If true, this this app has at least one version that is compatible with this device.
     */
    public val isCompatible: Boolean,
) : MinimalApp {
    public fun getName(localeList: LocaleListCompat): String? {
        return internalName.getBestLocale(localeList)
    }

    public fun getSummary(localeList: LocaleListCompat): String? {
        return internalSummary.getBestLocale(localeList)
    }

    public override fun getIcon(localeList: LocaleListCompat): FileV2? {
        return localizedIcon?.filter { icon ->
            icon.repoId == repoId
        }?.toLocalizedFileV2().getBestLocale(localeList)
    }

    public val antiFeatureKeys: List<String> get() = antiFeatures?.map { it.key } ?: emptyList()
}

/**
 * Similar to [AppOverviewItem], this is a lightweight version of [App]
 * meant to show a list of apps.
 *
 * There is additional information about [installedVersionCode] and [installedVersionName]
 * as well as [isCompatible].
 *
 * It includes [antiFeatureKeys] of the highest version, so some clients can apply filters to them.
 */
@ConsistentCopyVisibility
public data class AppListItem internal constructor(
    public override val repoId: Long,
    public override val packageName: String,
    @ColumnInfo(name = "localizedName")
    public override val name: String? = null,
    @ColumnInfo(name = "localizedSummary")
    public override val summary: String? = null,
    public val lastUpdated: Long,
    public val categories: List<String>? = null,
    internal val antiFeatures: String?,
    @Relation(
        parentColumn = "packageName",
        entityColumn = "packageName",
    )
    internal val localizedIcon: List<LocalizedIcon>?,
    /**
     * If true, this this app has at least one version that is compatible with this device.
     */
    public val isCompatible: Boolean,
    /**
     * The signer, this app prefers to use for new installs.
     */
    public val preferredSigner: String? = null,
    /**
     * The name of the installed version, null if this app is not installed.
     */
    @get:Ignore
    public val installedVersionName: String? = null,
    /**
     * The version code of the installed version, null if this app is not installed.
     */
    @get:Ignore
    public val installedVersionCode: Long? = null,
) : MinimalApp {
    @delegate:Ignore
    private val antiFeaturesDecoded by lazy {
        fromStringToMapOfLocalizedTextV2(antiFeatures)
    }

    public override fun getIcon(localeList: LocaleListCompat): FileV2? {
        return localizedIcon?.filter { icon ->
            icon.repoId == repoId
        }?.toLocalizedFileV2().getBestLocale(localeList)
    }

    public val antiFeatureKeys: List<String>
        get() = antiFeaturesDecoded?.map { it.key } ?: emptyList()

    public fun getAntiFeatureReason(antiFeatureKey: String, localeList: LocaleListCompat): String? {
        return antiFeaturesDecoded?.get(antiFeatureKey)?.getBestLocale(localeList)
    }
}

/**
 * An app that has an [update] available.
 * It is meant to display available updates in the UI.
 */
@ConsistentCopyVisibility
public data class UpdatableApp internal constructor(
    public override val repoId: Long,
    public override val packageName: String,
    public val installedVersionCode: Long,
    public val installedVersionName: String,
    public val update: AppVersion,
    @Deprecated("Use AppWithIssue instead: UpdateInOtherRepo")
    public val isFromPreferredRepo: Boolean,
    /**
     * If true, this is not necessarily an update (contrary to the class name),
     * but an app with the `KnownVuln` anti-feature.
     */
    @Deprecated("Use AppWithIssue instead: KnownVulnerability")
    public val hasKnownVulnerability: Boolean,
    public override val name: String? = null,
    public override val summary: String? = null,
    internal val localizedIcon: List<LocalizedIcon>? = null,
) : MinimalApp {
    public override fun getIcon(localeList: LocaleListCompat): FileV2? {
        return localizedIcon?.filter { icon ->
            icon.repoId == update.repoId
        }?.toLocalizedFileV2().getBestLocale(localeList)
    }
}

internal interface IFile {
    val type: String
    val locale: String
    val name: String
    val sha256: String?
    val size: Long?
    val ipfsCidV1: String?
}

@Entity(
    tableName = LocalizedFile.TABLE,
    primaryKeys = ["repoId", "packageName", "type", "locale"],
    foreignKeys = [ForeignKey(
        entity = AppMetadata::class,
        parentColumns = ["repoId", "packageName"],
        childColumns = ["repoId", "packageName"],
        onDelete = ForeignKey.CASCADE,
    )],
)
internal data class LocalizedFile(
    val repoId: Long,
    val packageName: String,
    override val type: String,
    override val locale: String,
    override val name: String,
    override val sha256: String? = null,
    override val size: Long? = null,
    override val ipfsCidV1: String? = null,
) : IFile {
    internal companion object {
        const val TABLE = "LocalizedFile"
    }
}

internal fun LocalizedFileV2.toLocalizedFile(
    repoId: Long,
    packageName: String,
    type: String,
): List<LocalizedFile> = map { (locale, file) ->
    LocalizedFile(
        repoId = repoId,
        packageName = packageName,
        type = type,
        locale = locale,
        name = file.name,
        sha256 = file.sha256,
        size = file.size,
        ipfsCidV1 = file.ipfsCidV1,
    )
}

internal fun List<IFile>.toLocalizedFileV2(): LocalizedFileV2? = associate { file ->
    file.locale to FileV2(
        name = file.name,
        sha256 = file.sha256,
        size = file.size,
        ipfsCidV1 = file.ipfsCidV1,
    )
}.ifEmpty { null }

// We can't restrict this query further (e.g. only from enabled repos or max weight),
// because we are using this via @Relation on packageName for specific repos.
// When filtering the result for only the repoId we are interested in, we'd get no icons.
@DatabaseView(
    viewName = LocalizedIcon.TABLE,
    value = "SELECT * FROM ${LocalizedFile.TABLE} WHERE type='icon'",
)
internal data class LocalizedIcon(
    val repoId: Long,
    val packageName: String,
    override val type: String,
    override val locale: String,
    override val name: String,
    override val sha256: String? = null,
    override val size: Long? = null,
    override val ipfsCidV1: String? = null,
) : IFile {
    internal companion object {
        const val TABLE = "LocalizedIcon"
    }
}

@Entity(
    tableName = LocalizedFileList.TABLE,
    primaryKeys = ["repoId", "packageName", "type", "locale", "name"],
    foreignKeys = [ForeignKey(
        entity = AppMetadata::class,
        parentColumns = ["repoId", "packageName"],
        childColumns = ["repoId", "packageName"],
        onDelete = ForeignKey.CASCADE,
    )],
)
internal data class LocalizedFileList(
    val repoId: Long,
    val packageName: String,
    val type: String,
    val locale: String,
    val name: String,
    val sha256: String? = null,
    val size: Long? = null,
    val ipfsCidV1: String? = null,
) {
    internal companion object {
        const val TABLE = "LocalizedFileList"
    }
}

internal fun LocalizedFileListV2.toLocalizedFileList(
    repoId: Long,
    packageName: String,
    type: String,
): List<LocalizedFileList> = flatMap { (locale, files) ->
    files.map { file -> file.toLocalizedFileList(repoId, packageName, type, locale) }
}

internal fun FileV2.toLocalizedFileList(
    repoId: Long,
    packageName: String,
    type: String,
    locale: String,
) = LocalizedFileList(
    repoId = repoId,
    packageName = packageName,
    type = type,
    locale = locale,
    name = name,
    sha256 = sha256,
    size = size,
    ipfsCidV1 = ipfsCidV1,
)

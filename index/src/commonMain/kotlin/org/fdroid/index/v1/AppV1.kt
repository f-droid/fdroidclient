package org.fdroid.index.v1

import kotlinx.serialization.Serializable
import org.fdroid.index.DEFAULT_LOCALE
import org.fdroid.index.mapValuesNotNull
import org.fdroid.index.v2.FileV2
import org.fdroid.index.v2.LocalizedFileListV2
import org.fdroid.index.v2.LocalizedFileV2
import org.fdroid.index.v2.LocalizedTextV2
import org.fdroid.index.v2.MetadataV2
import org.fdroid.index.v2.Screenshots

@Serializable
public data class AppV1(
    val categories: List<String> = emptyList(), // missing in wind repo
    val antiFeatures: List<String> = emptyList(),
    val summary: String? = null,
    val description: String? = null,
    val changelog: String? = null,
    val translation: String? = null,
    val issueTracker: String? = null,
    val sourceCode: String? = null,
    val binaries: String? = null,
    val name: String? = null,
    val authorName: String? = null,
    val authorEmail: String? = null,
    val authorWebSite: String? = null,
    val authorPhone: String? = null,
    val donate: String? = null,
    val liberapayID: String? = null,
    val liberapay: String? = null,
    val openCollective: String? = null,
    val bitcoin: String? = null,
    val litecoin: String? = null,
    val flattrID: String? = null,
    val suggestedVersionName: String? = null, // missing in guardian project repo
    val suggestedVersionCode: String? = null, // missing in wind repo
    val license: String,
    val webSite: String? = null,
    val added: Long? = null, // missing in wind repo,
    val icon: String? = null,
    val packageName: String,
    val lastUpdated: Long? = null, // missing in wind repo,
    val localized: Map<String, Localized>? = null,
    val allowedAPKSigningKeys: List<String>? = null, // guardian repo only, not needed for client?
) {
    public fun toMetadataV2(
        preferredSigner: String?,
        locale: String = DEFAULT_LOCALE,
    ): MetadataV2 = MetadataV2(
        name = getLocalizedTextV2(name, locale) { it.name },
        summary = getLocalizedTextV2(summary, locale) { it.summary },
        description = getLocalizedTextV2(description, locale) { it.description },
        added = added ?: 0,
        lastUpdated = lastUpdated ?: 0,
        webSite = webSite,
        changelog = changelog,
        license = license,
        sourceCode = sourceCode,
        issueTracker = issueTracker,
        translation = translation,
        preferredSigner = preferredSigner,
        categories = categories,
        authorName = authorName,
        authorEmail = authorEmail,
        authorWebSite = authorWebSite,
        authorPhone = authorPhone,
        donate = if (donate == null) emptyList() else listOf(donate),
        liberapayID = liberapayID,
        liberapay = liberapay,
        openCollective = openCollective,
        bitcoin = bitcoin,
        litecoin = litecoin,
        flattrID = flattrID,
        icon = localized.toLocalizedFileV2 { it.icon }
            ?: icon?.let { mapOf(locale to FileV2("/icons/$it")) },
        featureGraphic = localized.toLocalizedFileV2 { it.featureGraphic },
        promoGraphic = localized.toLocalizedFileV2 { it.promoGraphic },
        tvBanner = localized.toLocalizedFileV2 { it.tvBanner },
        video = localized.toLocalizedTextV2 { it.video },
        screenshots = Screenshots(
            phone = localized.toLocalizedFileListV2("phoneScreenshots") {
                it.phoneScreenshots
            },
            sevenInch = localized.toLocalizedFileListV2("sevenInchScreenshots") {
                it.sevenInchScreenshots
            },
            tenInch = localized.toLocalizedFileListV2("tenInchScreenshots") {
                it.tenInchScreenshots
            },
            wear = localized.toLocalizedFileListV2("wearScreenshots") {
                it.wearScreenshots
            },
            tv = localized.toLocalizedFileListV2("tvScreenshots") {
                it.tvScreenshots
            },
        ).takeIf { !it.isNull },
    )

    private fun getLocalizedTextV2(
        s: String?,
        locale: String,
        selector: (Localized) -> String?,
    ): LocalizedTextV2? {
        return if (s == null) localized?.toLocalizedTextV2(selector) else mapOf(locale to s)
    }

    private fun Map<String, Localized>?.toLocalizedTextV2(
        selector: (Localized) -> String?,
    ): LocalizedTextV2? {
        if (this == null) return null
        return mapValuesNotNull { selector(it.value) }
    }

    private fun Map<String, Localized>?.toLocalizedFileV2(
        selector: (Localized) -> String?,
    ): LocalizedFileV2? {
        if (this == null) return null
        return mapValuesNotNull {
            selector(it.value)?.let { file ->
                FileV2("/$packageName/${it.key}/$file")
            }
        }
    }

    private fun Map<String, Localized>?.toLocalizedFileListV2(
        kind: String,
        selector: (Localized) -> List<String>?,
    ): LocalizedFileListV2? {
        if (this == null) return null
        return mapValuesNotNull {
            selector(it.value)?.map { file ->
                FileV2("/$packageName/${it.key}/$kind/$file")
            }
        }
    }
}

@Serializable
public data class Localized(
    val description: String? = null,
    val name: String? = null,
    val icon: String? = null,
    val whatsNew: String? = null,
    val video: String? = null,
    val phoneScreenshots: List<String>? = null,
    val sevenInchScreenshots: List<String>? = null,
    val tenInchScreenshots: List<String>? = null,
    val wearScreenshots: List<String>? = null,
    val tvScreenshots: List<String>? = null,
    val featureGraphic: String? = null,
    val promoGraphic: String? = null,
    val tvBanner: String? = null,
    val summary: String? = null,
)

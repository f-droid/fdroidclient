package org.fdroid.index.v1

import kotlinx.serialization.Serializable

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
)

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

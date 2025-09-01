package org.fdroid.ui.details

import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import androidx.annotation.VisibleForTesting
import androidx.core.os.LocaleListCompat
import org.fdroid.database.App
import org.fdroid.database.AppMetadata
import org.fdroid.database.AppPrefs
import org.fdroid.database.AppVersion
import org.fdroid.database.Repository
import org.fdroid.download.DownloadRequest
import org.fdroid.download.getDownloadRequest
import org.fdroid.index.RELEASE_CHANNEL_BETA
import org.fdroid.index.v2.PackageVersion
import org.fdroid.install.SessionInstallManager
import org.fdroid.ui.categories.CategoryItem

data class AppDetailsItem(
    val app: AppMetadata,
    val actions: AppDetailsActions,
    /**
     * The ID of the repo that is currently set as preferred.
     * Note that the repository ID of this [app] may be different.
     */
    val preferredRepoId: Long = app.repoId,
    /**
     * A list of [Repository]s the app is in. If this is empty, the list doesn't matter,
     * because the user only has one repo.
     */
    val repositories: List<Repository> = emptyList(),
    val name: String,
    val summary: String? = null,
    val description: String? = null,
    val icon: DownloadRequest? = null,
    val featureGraphic: DownloadRequest? = null,
    val phoneScreenshots: List<DownloadRequest> = emptyList(),
    val categories: List<CategoryItem>? = null,
    val versions: List<PackageVersion>? = null,
    val installedVersion: PackageVersion? = null,
    /**
     * Needed, because the [installedVersion] may not be available, e.g. too old.
     */
    val installedVersionCode: Long? = null,
    /**
     * The currently suggested version for installation.
     */
    val suggestedVersion: PackageVersion? = null,
    /**
     * Similar to [suggestedVersion], but doesn't obey [appPrefs] for ignoring versions.
     * This is useful for (un-)ignoring this version.
     */
    val possibleUpdate: PackageVersion? = null,
    val appPrefs: AppPrefs? = null,
    val whatsNew: String? = null,
    val antiFeatures: List<AntiFeature>? = null,
    /**
     * true if this app from this repository has no versions with a
     * compatible signer. This means that the app is installed, but does not receive updates either
     * because the signer in the repo has changed or a wrong repo is set as preferred.
     */
    val noCompatibleVersions: Boolean = false,
    val authorHasMoreThanOneApp: Boolean = false,
) {
    constructor(
        repository: Repository,
        preferredRepoId: Long,
        repositories: List<Repository>,
        dbApp: App,
        actions: AppDetailsActions,
        versions: List<AppVersion>?,
        installedVersion: AppVersion?,
        installedVersionCode: Long?,
        suggestedVersion: AppVersion?,
        possibleUpdate: AppVersion?,
        appPrefs: AppPrefs?,
        noCompatibleVersions: Boolean,
        authorHasMoreThanOneApp: Boolean,
        localeList: LocaleListCompat,
    ) : this(
        app = dbApp.metadata,
        actions = actions,
        preferredRepoId = preferredRepoId,
        repositories = repositories,
        name = dbApp.name ?: "Unknown App",
        summary = dbApp.summary,
        description = getHtmlDescription(dbApp.getDescription(localeList)),
        icon = dbApp.getIcon(localeList)?.getDownloadRequest(repository),
        featureGraphic = dbApp.getFeatureGraphic(localeList)?.getDownloadRequest(repository),
        phoneScreenshots = dbApp.getPhoneScreenshots(localeList).mapNotNull {
            it.getDownloadRequest(repository)
        },
        categories = dbApp.metadata.categories?.mapNotNull { categoryId ->
            val category = repository.getCategories()[categoryId] ?: return@mapNotNull null
            CategoryItem(
                id = category.id,
                name = category.getName(localeList) ?: "Unknown Category",
            )
        },
        versions = versions,
        installedVersion = installedVersion,
        installedVersionCode = installedVersionCode,
        suggestedVersion = suggestedVersion,
        possibleUpdate = possibleUpdate,
        appPrefs = appPrefs,
        whatsNew = installedVersion?.getWhatsNew(localeList),
        antiFeatures = installedVersion?.getAntiFeatures(repository, localeList)
            ?: suggestedVersion?.getAntiFeatures(repository, localeList)
            ?: versions?.first()?.getAntiFeatures(repository, localeList),
        noCompatibleVersions = noCompatibleVersions,
        authorHasMoreThanOneApp = authorHasMoreThanOneApp,
    )

    /**
     * True if the app is installed (and has a launch intent)
     * and thus the 'Open' button should be shown.
     */
    val showOpenButton: Boolean get() = actions.launchIntent != null
    val allowsBetaVersions: Boolean
        get() = appPrefs?.releaseChannels?.contains(RELEASE_CHANNEL_BETA) == true

    val ignoresAllUpdates: Boolean get() = appPrefs?.ignoreAllUpdates == true

    /**
     * True if the update from [possibleUpdate] is being ignored
     * and not already ignoring all updates anyway.
     */
    val ignoresCurrentUpdate: Boolean
        get() {
            if (ignoresAllUpdates) return false
            val prefs = appPrefs ?: return false
            val updateVersionCode = possibleUpdate?.versionCode ?: return false
            return actions.ignoreThisUpdate != null && prefs.shouldIgnoreUpdate(updateVersionCode)
        }

    /**
     * Specifies what main button should be shown.
     */
    val mainButtonState: MainButtonState
        get() {
            return if (installedVersionCode == null) { // app is not installed
                if (suggestedVersion == null) MainButtonState.NONE
                else MainButtonState.INSTALL
            } else { // app is installed
                if (suggestedVersion == null ||
                    suggestedVersion.versionCode <= installedVersionCode
                ) MainButtonState.NONE
                else MainButtonState.UPDATE
            }
        }

    /**
     * True if this app has warnings, we need to show to the user.
     */
    val showWarnings: Boolean get() = oldTargetSdk || noCompatibleVersions

    /**
     * True if the targetSdk of the suggested version is so old
     * that auto updates for this app are not available (due to system restrictions).
     */
    val oldTargetSdk: Boolean
        get() {
            val targetSdk = suggestedVersion?.packageManifest?.targetSdkVersion
            // auto-updates are only available on SDK 31 and up
            return if (targetSdk != null && SDK_INT >= 31) {
                !SessionInstallManager.isTargetSdkSupported(targetSdk)
            } else {
                false
            }
        }
    val showAuthorContact: Boolean get() = app.authorEmail != null || app.authorWebSite != null
    val showDonate: Boolean
        get() = !app.donate.isNullOrEmpty() ||
            app.liberapay != null ||
            app.openCollective != null ||
            app.litecoin != null ||
            app.bitcoin != null
    val liberapayUri = app.liberapay?.let { "https://liberapay.com/$it/donate" }
    val openCollectiveUri = app.openCollective?.let { "https://opencollective.com/$it/donate" }
    val litecoinUri = app.litecoin?.let { "litecoin:$it" }
    val bitcoinUri = app.bitcoin?.let { "bitcoin:$it" }
}

class AppDetailsActions(
    val allowBetaVersions: () -> Unit,
    val ignoreAllUpdates: (() -> Unit)? = null,
    val ignoreThisUpdate: (() -> Unit)? = null,
    val shareApk: (() -> Unit)? = null,
    val uninstallApp: (() -> Unit)? = null,
    val launchIntent: Intent? = null,
    val shareIntent: Intent? = null,
)

enum class MainButtonState {
    NONE,
    INSTALL,
    UPDATE,
}

data class AntiFeature(
    val id: String,
    val icon: DownloadRequest? = null,
    val name: String = id,
    val reason: String? = null,
)

private fun AppVersion?.getAntiFeatures(
    repository: Repository,
    localeList: LocaleListCompat,
): List<AntiFeature>? {
    return this?.antiFeatureKeys?.mapNotNull { key ->
        val antiFeature = repository.getAntiFeatures()[key] ?: return@mapNotNull null
        AntiFeature(
            id = key,
            icon = antiFeature.getIcon(localeList)?.getDownloadRequest(repository),
            name = antiFeature.getName(localeList) ?: key,
            reason = getAntiFeatureReason(key, localeList),
        )
    }
}

@VisibleForTesting
internal fun getHtmlDescription(description: String?): String? {
    return description?.replace("</?h[1-6]>".toRegex(), "")
        ?.replace("(\\s)(https://\\S+[^\\s.])([\\s\\n.])".toRegex()) {
            val prefix = it.groups[1]?.value ?: it.value
            val url = it.groups[2]?.value ?: it.value
            val suffix = it.groups[3]?.value ?: it.value
            "$prefix<a href=\"$url\">$url</a>$suffix"
        }?.replace("(?<!</p>|ul>|</li>)\n".toRegex(), "<br>\n")
}

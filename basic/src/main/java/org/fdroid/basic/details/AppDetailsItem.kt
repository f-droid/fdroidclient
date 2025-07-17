package org.fdroid.basic.details

import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.core.os.LocaleListCompat
import org.fdroid.basic.download.getDownloadRequest
import org.fdroid.basic.install.SessionInstallManager
import org.fdroid.database.App
import org.fdroid.database.AppMetadata
import org.fdroid.database.AppPrefs
import org.fdroid.database.AppVersion
import org.fdroid.database.Repository
import org.fdroid.download.DownloadRequest
import org.fdroid.index.RELEASE_CHANNEL_BETA
import org.fdroid.index.v2.PackageManifest
import org.fdroid.index.v2.PackageVersion
import org.fdroid.index.v2.SignerV2
import java.util.concurrent.TimeUnit.DAYS

enum class MainButtonState {
    NONE, INSTALL, UPDATE
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
        description = dbApp.getDescription(localeList)
            ?.replace(" (https://\\S+) ".toRegex()) {
                val url = it.groups[1]?.value ?: it.value
                "<a href=\"$url\">$url</a>"
            }
            ?.replace("\n", "<br>"),
        icon = dbApp.getIcon(localeList)?.getDownloadRequest(repository),
        featureGraphic = dbApp.getFeatureGraphic(localeList)?.getDownloadRequest(repository),
        phoneScreenshots = dbApp.getPhoneScreenshots(localeList).mapNotNull {
            it.getDownloadRequest(repository)
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

data class AntiFeature(
    val id: String,
    val icon: DownloadRequest? = null,
    val name: String = id,
    val reason: String? = null,
)

// TODO exclude from release builds, @Preview only?
val testVersion1 = object : PackageVersion {
    override val versionCode: Long = 42
    override val versionName: String = "42.23.0-alpha1337-33d2252b90"
    override val added: Long = System.currentTimeMillis() - DAYS.toMillis(4)
    override val size: Long? = 1024 * 1024 * 42
    override val signer: SignerV2? = SignerV2(
        listOf("271721a9cddc96660336c19a39ae3cca4375072c80d3c8170860c333d2252b90")
    )
    override val releaseChannels: List<String>? = null
    override val packageManifest: PackageManifest = object : PackageManifest {
        override val minSdkVersion: Int? = null
        override val maxSdkVersion: Int? = null
        override val featureNames: List<String>? = null
        override val nativecode: List<String>? = listOf("amd64", "x86")
        override val targetSdkVersion: Int? = 13
    }
    override val hasKnownVulnerability: Boolean = false
}
val testVersion2 = object : PackageVersion {
    override val versionCode: Long = 23
    override val versionName: String = "23.42.0"
    override val added: Long = System.currentTimeMillis() - DAYS.toMillis(4)
    override val size: Long? = 1024 * 1024 * 23
    override val signer: SignerV2? = SignerV2(
        listOf("271721a9cddc96660336c19a39ae3cca4375072c80d3c8170860c333d2252b90")
    )
    override val releaseChannels: List<String>? = null
    override val packageManifest: PackageManifest = object : PackageManifest {
        override val minSdkVersion: Int? = null
        override val maxSdkVersion: Int? = null
        override val featureNames: List<String>? = null
        override val nativecode: List<String>? = null
        override val targetSdkVersion: Int? = 13
    }
    override val hasKnownVulnerability: Boolean = false
}
val testApp = AppDetailsItem(
    app = AppMetadata(
        repoId = 1,
        packageName = "org.schabi.newpipe",
        added = 1441756800000,
        lastUpdated = 1747214796000,
        webSite = "https://newpipe.net",
        changelog = "https://github.com/TeamNewPipe/NewPipe/releases",
        license = "GPL-3.0-or-later",
        sourceCode = "https://github.com/TeamNewPipe/NewPipe",
        issueTracker = "https://github.com/TeamNewPipe/NewPipe/issues",
        translation = "https://hosted.weblate.org/projects/newpipe/",
        preferredSigner = "cb84069bd68116bafae5ee4ee5b08a567aa6d898404e7cb12f9e756df5cf5cab",
        video = null,
        authorName = "Team NewPipe",
        authorEmail = "team@newpipe.net",
        authorWebSite = "https://newpipe.net",
        authorPhone = "123456",
        donate = listOf("https://newpipe.net/donate"),
        liberapayID = null,
        liberapay = "TeamNewPipe",
        openCollective = "TeamNewPipe",
        bitcoin = "TeamNewPipe",
        litecoin = "TeamNewPipe",
        flattrID = null,
        categories = listOf("Internet", "Multimedia"),
        isCompatible = true,
    ),
    actions = AppDetailsActions({}, {}, {}, {}, {}, Intent(), Intent()),
    appPrefs = AppPrefs("org.schabi.newpipe"),
    name = "New Pipe",
    summary = "Lightweight YouTube frontend",
    description = "NewPipe does not use any Google framework libraries, or the YouTube API. " +
        "It only parses the website in order to gain the information it needs. " +
        "Therefore this app can be used on devices without Google Services installed. " +
        "Also, you don't need a YouTube account to use NewPipe, and it's FLOSS.\n\n" +
        LoremIpsum(128).values.joinToString(" "),
    antiFeatures = listOf(
        AntiFeature(
            id = "NonFreeNet",
            icon = null,
            name = "This app promotes or depends entirely on a non-free network service",
            reason = "Depends on Youtube for videos.",
        ),
        AntiFeature(
            id = "FooBar",
            icon = null,
            name = "This app promotes or depends entirely on a non-free network service",
            reason = "Depends on Youtube for videos.",
        ),
    ),
    whatsNew = "This release fixes YouTube only providing a 360p stream.\n\n" +
        "Note that the solution employed in this version is likely temporary, " +
        "and in the long run the SABR video protocol needs to be implemented, " +
        "but TeamNewPipe members are currently busy so any help would be greatly appreciated! " +
        "https://github.com/TeamNewPipe/NewPipe/issues/12248",
    authorHasMoreThanOneApp = true,
    versions = listOf(
        testVersion1,
        testVersion2,
    ),
    installedVersion = testVersion2,
    suggestedVersion = testVersion1,
    possibleUpdate = testVersion1,
)

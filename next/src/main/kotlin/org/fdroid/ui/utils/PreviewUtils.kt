package org.fdroid.ui.utils

import android.content.Intent
import androidx.annotation.RestrictTo
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import org.fdroid.database.AppListSortOrder
import org.fdroid.database.AppMetadata
import org.fdroid.database.AppPrefs
import org.fdroid.database.KnownVulnerability
import org.fdroid.database.NotAvailable
import org.fdroid.database.Repository
import org.fdroid.download.Mirror
import org.fdroid.index.IndexFormatVersion
import org.fdroid.index.v2.PackageManifest
import org.fdroid.index.v2.PackageVersion
import org.fdroid.index.v2.SignerV2
import org.fdroid.install.InstallConfirmationState
import org.fdroid.install.InstallState
import org.fdroid.ui.apps.AppUpdateItem
import org.fdroid.ui.apps.AppWithIssueItem
import org.fdroid.ui.apps.InstalledAppItem
import org.fdroid.ui.apps.InstallingAppItem
import org.fdroid.ui.apps.MyAppsInfo
import org.fdroid.ui.apps.MyAppsModel
import org.fdroid.ui.categories.CategoryItem
import org.fdroid.ui.details.AntiFeature
import org.fdroid.ui.details.AppDetailsActions
import org.fdroid.ui.details.AppDetailsItem
import org.fdroid.ui.details.VersionItem
import org.fdroid.ui.lists.AppListActions
import org.fdroid.ui.lists.AppListInfo
import org.fdroid.ui.lists.AppListModel
import org.fdroid.ui.lists.AppListType
import org.fdroid.ui.repositories.RepositoryInfo
import org.fdroid.ui.repositories.RepositoryItem
import org.fdroid.ui.repositories.RepositoryModel
import org.fdroid.ui.repositories.details.ArchiveState
import org.fdroid.ui.repositories.details.OfficialMirrorItem
import org.fdroid.ui.repositories.details.RepoDetailsActions
import org.fdroid.ui.repositories.details.RepoDetailsInfo
import org.fdroid.ui.repositories.details.RepoDetailsModel
import org.fdroid.ui.repositories.details.UserMirrorItem
import java.util.concurrent.TimeUnit.DAYS

object Names {
    val randomName: String get() = names.random()
    val names = listOf(
        "Anstop",
        "PipePipe",
        "A2DP Volume",
        "Com-Phone Story Maker",
        "Lightning",
        "BitAC - Bitcoin Address Checker",
        "Text Launcher",
        "Polaris",
        "Chubby Click - Metronome",
        "SUSI.AI",
        "Moon Phase",
        "Export Contacts",
        "Import Contacts",
        "DNG Processor",
        "Rootless Pixel Launcher",
        "AndroDNS",
        "androidVNC",
        "PrBoom For Android",
        "FakeStandby",
        "eBooks",
        "ANONguard",
        "Acrylic Paint",
        "Immich",
    )
}

val testVersion1 = object : PackageVersion {
    override val versionCode: Long = 42
    override val versionName: String = "42.23.0-alpha1337-33d2252b90"
    override val added: Long = System.currentTimeMillis() - DAYS.toMillis(4)
    override val size: Long = 1024 * 1024 * 42
    override val signer: SignerV2 = SignerV2(
        listOf("271721a9cddc96660336c19a39ae3cca4375072c80d3c8170860c333d2252b90")
    )
    override val releaseChannels: List<String>? = null
    override val packageManifest: PackageManifest = object : PackageManifest {
        override val minSdkVersion: Int = 2
        override val targetSdkVersion: Int = 13
        override val maxSdkVersion: Int? = null
        override val featureNames: List<String>? = null
        override val nativecode: List<String> = listOf("amd64", "x86")
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
        override val targetSdkVersion: Int = 13
        override val maxSdkVersion: Int = 99
        override val featureNames: List<String>? = null
        override val nativecode: List<String>? = null
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
    actions = AppDetailsActions(
        installAction = { _, _, _ -> },
        requestUserConfirmation = { _, _ -> },
        checkUserConfirmation = { _, _ -> },
        cancelInstall = {},
        onUninstallResult = { _, _ -> },
        onRepoChanged = {},
        onPreferredRepoChanged = {},
        allowBetaVersions = {},
        ignoreAllUpdates = {},
        ignoreThisUpdate = {},
        shareApk = Intent(),
        uninstallIntent = Intent(),
        launchIntent = Intent(),
        shareIntent = Intent(),
    ),
    installState = InstallState.Unknown,
    appPrefs = AppPrefs("org.schabi.newpipe"),
    name = "New Pipe",
    summary = "Lightweight YouTube frontend",
    description = "NewPipe does not use any Google framework libraries, or the YouTube API. " +
        "It only parses the website in order to gain the information it needs. " +
        "Therefore this app can be used on devices without Google Services installed. " +
        "Also, you don't need a YouTube account to use NewPipe, and it's FLOSS.\n\n" +
        LoremIpsum(128).values.joinToString(" "),
    categories = listOf(
        CategoryItem("Multimedia", "Multimedia"),
        CategoryItem("Internet", "Internet"),
    ),
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
        VersionItem(
            testVersion1,
            isInstalled = false,
            isSuggested = true,
            isCompatible = true,
            isSignerCompatible = true,
            showInstallButton = true,
        ),
        VersionItem(
            testVersion1,
            isInstalled = false,
            isSuggested = false,
            isCompatible = true,
            isSignerCompatible = false,
            showInstallButton = false,
        ),
        VersionItem(
            testVersion2,
            isInstalled = false,
            isSuggested = false,
            isCompatible = false,
            isSignerCompatible = true,
            showInstallButton = true,
        ),
        VersionItem(
            testVersion2,
            isInstalled = true,
            isSuggested = false,
            isCompatible = true,
            isSignerCompatible = true,
            showInstallButton = false,
        ),
    ),
    installedVersion = testVersion2,
    suggestedVersion = null,
    possibleUpdate = testVersion1,
    proxy = null,
)

fun getPreviewVersion(versionName: String, size: Long? = null) = object : PackageVersion {
    override val versionCode: Long = 23
    override val versionName: String = versionName
    override val added: Long = System.currentTimeMillis() - DAYS.toMillis(3)
    override val size: Long? = size
    override val signer: SignerV2? = null
    override val releaseChannels: List<String>? = null
    override val packageManifest: PackageManifest = object : PackageManifest {
        override val minSdkVersion: Int? = null
        override val maxSdkVersion: Int? = null
        override val featureNames: List<String>? = null
        override val nativecode: List<String>? = null
        override val targetSdkVersion: Int? = null
    }
    override val hasKnownVulnerability: Boolean = false
}

fun getAppListInfo(model: AppListModel) = object : AppListInfo {
    override val model: AppListModel = model
    override val actions: AppListActions = object : AppListActions {
        override fun toggleFilterVisibility() {}
        override fun sortBy(sort: AppListSortOrder) {}
        override fun toggleFilterIncompatible() {}
        override fun addCategory(categoryId: String) {}
        override fun removeCategory(categoryId: String) {}
        override fun addRepository(repoId: Long) {}
        override fun removeRepository(repoId: Long) {}
        override fun saveFilters() {}
        override fun clearFilters() {}
        override fun onSearch(query: String) {}
        override fun onOnboardingSeen() {}
    }
    override val list: AppListType = AppListType.New("New")
    override val showFilters: Boolean = false
    override val showOnboarding: Boolean = false
}

fun getMyAppsInfo(model: MyAppsModel): MyAppsInfo = object : MyAppsInfo {
    override val model = model
    override fun refresh() {}
    override fun updateAll() {}
    override fun changeSortOrder(sort: AppListSortOrder) {}
    override fun search(query: String) {}
    override fun confirmAppInstall(packageName: String, state: InstallConfirmationState) {}
}

@RestrictTo(RestrictTo.Scope.TESTS)
internal val myAppsModel = MyAppsModel(
    appUpdates = listOf(
        AppUpdateItem(
            repoId = 1,
            packageName = "B1",
            name = "App Update 123",
            installedVersionName = "1.0.1",
            update = getPreviewVersion("1.1.0", 123456789),
            whatsNew = "This is new, all is new, nothing old.",
        ),
        AppUpdateItem(
            repoId = 2,
            packageName = "B2",
            name = Names.randomName,
            installedVersionName = "3.0.1",
            update = getPreviewVersion("3.1.0", 9876543),
            whatsNew = null,
        )
    ),
    installingApps = listOf(
        InstallingAppItem(
            packageName = "A1",
            installState = InstallState.Downloading(
                name = "Installing App 1",
                versionName = "1.0.4",
                currentVersionName = null,
                lastUpdated = 23,
                iconDownloadRequest = null,
                downloadedBytes = 25,
                totalBytes = 100,
                startMillis = System.currentTimeMillis(),
            )
        )
    ),
    appsWithIssue = listOf(
        AppWithIssueItem(
            packageName = "C1",
            name = Names.randomName,
            installedVersionName = "1",
            issue = KnownVulnerability(true),
            lastUpdated = System.currentTimeMillis() - DAYS.toMillis(5)
        ),
        AppWithIssueItem(
            packageName = "C2",
            name = Names.randomName,
            installedVersionName = "2",
            issue = NotAvailable,
            lastUpdated = System.currentTimeMillis() - DAYS.toMillis(7)
        ),
    ),
    installedApps = listOf(
        InstalledAppItem(
            packageName = "D1",
            name = Names.randomName,
            installedVersionName = "1",
            lastUpdated = System.currentTimeMillis() - DAYS.toMillis(1)
        ),
        InstalledAppItem(
            packageName = "D2",
            name = Names.randomName,
            installedVersionName = "2",
            lastUpdated = System.currentTimeMillis() - DAYS.toMillis(2)
        ),
        InstalledAppItem(
            packageName = "D3",
            name = Names.randomName,
            installedVersionName = "3",
            lastUpdated = System.currentTimeMillis() - DAYS.toMillis(3)
        )
    ),
    sortOrder = AppListSortOrder.NAME,
)

fun getRepositoriesInfo(
    model: RepositoryModel,
    currentRepositoryId: Long? = null,
): RepositoryInfo = object : RepositoryInfo {
    override val model: RepositoryModel = model
    override val currentRepositoryId: Long? = currentRepositoryId
    override fun onOnboardingSeen() {}
    override fun onRepositorySelected(repositoryItem: RepositoryItem) {}
    override fun onRepositoryEnabled(repoId: Long, enabled: Boolean) {}
    override fun onAddRepo() {}
    override fun onRepositoryMoved(fromRepoId: Long, toRepoId: Long) {}
    override fun onRepositoriesFinishedMoving(fromRepoId: Long, toRepoId: Long) {}
}

fun getRepoDetailsInfo(
    model: RepoDetailsModel = RepoDetailsModel(
        repo = getRepository(),
        numberApps = 42,
        officialMirrors = listOf(
            OfficialMirrorItem(
                mirror = Mirror(baseUrl = "https://mirror.example.com/fdroid/repo"),
                isEnabled = true,
                isRepoAddress = true,
            ),
            OfficialMirrorItem(
                mirror = Mirror("https://mirror.example.com/foo/bar/fdroid/repo", "de"),
                isEnabled = false,
                isRepoAddress = false,
            ),
        ),
        userMirrors = listOf(
            UserMirrorItem(Mirror("https://mirror.example.com/fdroid/repo"), true),
            UserMirrorItem(Mirror("https://mirror.example.com/foo/bar/fdroid/repo"), false),
        ),
        archiveState = ArchiveState.LOADING,
        showOnboarding = false,
        proxy = null,
    ),
) = object : RepoDetailsInfo {
    override val model = model
    override val actions: RepoDetailsActions = object : RepoDetailsActions {
        override fun deleteRepository(repoId: Long) {}
        override fun updateUsernameAndPassword(
            repoId: Long,
            username: String,
            password: String,
        ) {
        }

        override fun setMirrorEnabled(
            repoId: Long,
            mirror: Mirror,
            enabled: Boolean,
        ) {
        }

        override fun deleteUserMirror(repoId: Long, mirror: Mirror) {}
        override fun setArchiveRepoEnabled(enabled: Boolean) {}
        override fun onOnboardingSeen() {}
    }
}

fun getRepository(address: String = "https://example.org/repo") = Repository(
    repoId = 42L,
    address = address,
    timestamp = 42L,
    formatVersion = IndexFormatVersion.ONE,
    certificate = "010203",
    version = 20001L,
    weight = 42,
    lastUpdated = 1337,
    username = "foo",
    password = "bar",
)

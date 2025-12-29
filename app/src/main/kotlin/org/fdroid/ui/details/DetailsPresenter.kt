package org.fdroid.ui.details

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.core.content.pm.PackageInfoCompat.getLongVersionCode
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.asFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.fdroid.UpdateChecker
import org.fdroid.database.App
import org.fdroid.database.AppPrefs
import org.fdroid.database.AppVersion
import org.fdroid.database.FDroidDatabase
import org.fdroid.database.Repository
import org.fdroid.download.NetworkState
import org.fdroid.index.RepoManager
import org.fdroid.install.ApkFileProvider
import org.fdroid.install.AppInstallManager
import org.fdroid.install.InstallState
import org.fdroid.repo.RepoPreLoader
import org.fdroid.settings.SettingsManager
import org.fdroid.ui.apps.AppWithIssueItem
import org.fdroid.utils.sha256

private const val TAG = "DetailsPresenter"

// TODO write tests for this function
//  see: https://github.com/cashapp/molecule?tab=readme-ov-file#testing
@Composable
fun DetailsPresenter(
    db: FDroidDatabase,
    scope: CoroutineScope,
    repoManager: RepoManager,
    repoPreLoader: RepoPreLoader,
    updateChecker: UpdateChecker,
    settingsManager: SettingsManager,
    appInstallManager: AppInstallManager,
    viewModel: AppDetailsViewModel,
    packageInfoFlow: StateFlow<AppInfo?>,
    currentRepoIdFlow: StateFlow<Long?>,
    appsWithIssuesFlow: StateFlow<List<AppWithIssueItem>?>,
    networkStateFlow: StateFlow<NetworkState>,
): AppDetailsItem? {
    val packagePair = packageInfoFlow.collectAsState().value ?: return null
    val packageName = packagePair.packageName
    val packageInfo = packagePair.packageInfo
    val currentRepoId = currentRepoIdFlow.collectAsState().value
    val appsWithIssues = appsWithIssuesFlow.collectAsState().value
    val appDao = db.getAppDao()
    val app = produceState<App?>(null, currentRepoId) {
        withContext(scope.coroutineContext) {
            if (currentRepoId == null) {
                val flow = appDao.getApp(packageName).asFlow()
                flow.collect { value = it }
            } else {
                value = appDao.getApp(currentRepoId, packageName)
            }
        }
    }.value ?: return null
    val repo = produceState<Repository?>(null) {
        withContext(scope.coroutineContext) {
            value = repoManager.getRepository(app.repoId)
        }
    }.value ?: return null
    val repositories = produceState(emptyList(), packageName) {
        withContext(scope.coroutineContext) {
            val repos = appDao.getRepositoryIdsForApp(packageName).mapNotNull { repoId ->
                repoManager.getRepository(repoId)
            }
            // show repo chooser only if
            // * app is in more than one repo, or
            // * app is from a non-default repo
            value = if (repos.size > 1) repos
            else if (repo.address in repoPreLoader.defaultRepoAddresses) emptyList()
            else repos
        }
    }.value
    val installState =
        appInstallManager.getAppFlow(packageName).collectAsState(InstallState.Unknown).value

    val versions = produceState<List<AppVersion>?>(null, currentRepoId) {
        withContext(scope.coroutineContext) {
            if (currentRepoId == null) {
                db.getVersionDao().getAppVersions(app.repoId, packageName).asFlow().collect {
                    value = it
                }
            } else {
                db.getVersionDao().getAppVersions(currentRepoId, packageName).asFlow().collect {
                    value = it
                }
            }
        }
    }.value
    val appPrefs = produceState<AppPrefs?>(null, packageName) {
        withContext(scope.coroutineContext) {
            db.getAppPrefsDao().getAppPrefs(packageName).asFlow().collect { value = it }
        }
    }.value
    val preferredRepoId = remember(packageName, appPrefs) {
        appPrefs?.preferredRepoId ?: app.repoId // DB loads preferred repo first, so we remember it
    }

    val installedSigner = remember(packageInfo?.packageName) {
        @Suppress("DEPRECATION") // so far we had issues with the new way of getting sigs
        packageInfo?.signatures?.get(0)?.let {
            sha256(it.toByteArray())
        }
    }
    val suggestedVersion = remember(versions, appPrefs, installedSigner) {
        if (versions == null || appPrefs == null) {
            null
        } else {
            updateChecker.getSuggestedVersion(
                versions = versions,
                preferredSigner = installedSigner ?: app.metadata.preferredSigner,
                releaseChannels = appPrefs.releaseChannels,
                preferencesGetter = { appPrefs },
            )
        }
    }
    val possibleUpdate = remember(versions, appPrefs) {
        if (versions == null || appPrefs == null) {
            null
        } else {
            updateChecker.getUpdate(
                versions = versions,
                allowedSignersGetter = app.metadata.preferredSigner?.let { { setOf(it) } },
                allowedReleaseChannels = appPrefs.releaseChannels,
                // ignoring existing preferences to include ignored versions
                preferencesGetter = null,
            )
        }
    }
    val installedVersionCode = packageInfo?.let {
        getLongVersionCode(packageInfo)
    }
    val installedVersion = packageInfo?.let {
        versions?.find { it.versionCode == installedVersionCode }
    }
    val authorName = app.authorName
    val authorHasMoreThanOneApp = if (authorName == null) false else {
        produceState(false) {
            withContext(scope.coroutineContext) {
                db.getAppDao().hasAuthorMoreThanOneApp(authorName).asFlow().collect { value = it }
            }
        }.value
    }
    val issue = remember(appsWithIssues) {
        appsWithIssues?.find { it.packageName == packageName }?.issue
    }
    val locales = LocaleListCompat.getDefault()
    Log.d(TAG, "Presenting app details:")
    Log.d(TAG, "   app '${app.name}' ($packageName) in ${repo.address}")
    Log.d(TAG, "   versions: ${versions?.size}")
    Log.d(TAG, "   appPrefs: $appPrefs")
    Log.d(TAG, "   installState: $installState")
    return AppDetailsItem(
        repository = repo,
        preferredRepoId = preferredRepoId,
        repositories = repositories,
        dbApp = app,
        actions = AppDetailsActions(
            installAction = viewModel::install,
            requestUserConfirmation = viewModel::requestUserConfirmation,
            checkUserConfirmation = viewModel::checkUserConfirmation,
            cancelInstall = viewModel::cancelInstall,
            onUninstallResult = viewModel::onUninstallResult,
            onRepoChanged = viewModel::onRepoChanged,
            onPreferredRepoChanged = viewModel::onPreferredRepoChanged,
            allowBetaVersions = viewModel::allowBetaUpdates,
            ignoreAllUpdates = if (installedVersionCode == null) {
                null
            } else {
                viewModel::ignoreAllUpdates
            },
            ignoreThisUpdate = if (installedVersionCode == null ||
                possibleUpdate == null ||
                possibleUpdate.versionCode <= installedVersionCode
            ) {
                null
            } else {
                viewModel::ignoreThisUpdate
            },
            shareApk = if (installedVersionCode == null) {
                null
            } else {
                ApkFileProvider.getIntent(packageName)
            },
            uninstallIntent = packageInfo?.let {
                Intent(Intent.ACTION_DELETE).apply {
                    setData(Uri.fromParts("package", it.packageName, null))
                    putExtra(Intent.EXTRA_RETURN_RESULT, true)
                }
            },
            launchIntent = packagePair.launchIntent,
            shareIntent = getShareIntent(repo, packageName, app.name ?: ""),
        ),
        installState = installState,
        networkState = networkStateFlow.collectAsState().value,
        versions = versions?.map { version ->
            val signerCompatible = installedSigner == null ||
                version.signer?.sha256?.first() == installedSigner
            VersionItem(
                version = version,
                isInstalled = installedVersion == version,
                isSuggested = suggestedVersion == version,
                isCompatible = version.isCompatible,
                isSignerCompatible = signerCompatible,
                showInstallButton = if (!signerCompatible || installState.showProgress) {
                    false
                } else {
                    (installedVersion?.versionCode ?: 0) < version.versionCode
                },
            )
        },
        installedVersion = installedVersion,
        installedVersionCode = installedVersionCode,
        installedVersionName = packageInfo?.versionName,
        suggestedVersion = suggestedVersion,
        possibleUpdate = possibleUpdate,
        appPrefs = appPrefs,
        issue = issue,
        authorHasMoreThanOneApp = authorHasMoreThanOneApp,
        localeList = locales,
        proxy = settingsManager.proxyConfig,
    )
}

private fun getShareIntent(
    repo: Repository,
    packageName: String,
    appName: String,
): Intent? {
    val webBaseUrl = repo.webBaseUrl ?: return null
    val shareUri = webBaseUrl.toUri().buildUpon().appendPath(packageName).build()
    val uriIntent = Intent(Intent.ACTION_SEND).apply {
        setType("text/plain")
        putExtra(Intent.EXTRA_SUBJECT, appName)
        putExtra(Intent.EXTRA_TITLE, appName)
        putExtra(Intent.EXTRA_TEXT, shareUri.toString())
    }
    return Intent.createChooser(uriIntent, appName)
}

package org.fdroid.basic.details

import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.core.content.pm.PackageInfoCompat.getLongVersionCode
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.asFlow
import kotlinx.coroutines.flow.StateFlow
import org.fdroid.UpdateChecker
import org.fdroid.basic.ui.sha256
import org.fdroid.database.FDroidDatabase
import org.fdroid.index.RepoManager

private const val TAG = "DetailsPresenter"

// TODO write tests for this function
//  see: https://github.com/cashapp/molecule?tab=readme-ov-file#testing
@Composable
fun DetailsPresenter(
    db: FDroidDatabase,
    repoManager: RepoManager,
    updateChecker: UpdateChecker,
    viewModel: AppDetailsViewModel,
    packageInfoFlow: StateFlow<AppInfo?>,
): AppDetailsItem? {
    val packagePair = packageInfoFlow.collectAsState().value ?: return null
    val packageName = packagePair.packageName
    val app = db.getAppDao().getApp(packageName).asFlow().collectAsState(null).value
        ?: return null
    val repo = repoManager.getRepository(app.repoId) ?: return null
    val preferredRepoId = remember(packageName) {
        app.repoId // DB loads preferred repo first, so we remember it
    }
    val repositories = remember(packageName) {
        db.getAppDao().getRepositoryIdsForApp(packageName).mapNotNull { repoId ->
            repoManager.getRepository(repoId)
        }
    }

    val versions =
        db.getVersionDao().getAppVersions(packageName).asFlow().collectAsState(null).value
    val appPrefs = db.getAppPrefsDao().getAppPrefs(packageName).asFlow().collectAsState(null).value
    val suggestedVersion = if (versions == null || appPrefs == null) {
        null
    } else {
        updateChecker.getSuggestedVersion(
            versions = versions,
            preferredSigner = app.metadata.preferredSigner,
            releaseChannels = appPrefs.releaseChannels,
            preferencesGetter = { appPrefs },
        )
    }
    val possibleUpdate = if (versions == null || appPrefs == null) {
        null
    } else {
        updateChecker.getUpdate(
            versions = versions,
            allowedSignersGetter = app.metadata.preferredSigner?.let { { setOf(it) } },
            allowedReleaseChannels = appPrefs.releaseChannels,
            preferencesGetter = null, // ignoring existing preferences to include ignored versions
        )
    }
    val installedVersionCode = packagePair.packageInfo?.let {
        getLongVersionCode(packagePair.packageInfo)
    }
    val installedVersion = packagePair.packageInfo?.let {
        versions?.find { it.versionCode == installedVersionCode }
    }
    val installedSigner = packagePair.packageInfo?.signatures?.get(0)?.let {
        sha256(it.toByteArray())
    }
    val noCompatibleVersions = if (packagePair.packageInfo != null && versions != null) {
        // return true of no version has same signer
        versions.none { version ->
            version.manifest.signer?.sha256?.get(0) == installedSigner
        }
    } else {
        false
    }
    val authorName = app.authorName
    val authorHasMoreThanOneApp = if (authorName == null) false else {
        db.getAppDao().hasAuthorMoreThanOneApp(authorName).asFlow().collectAsState(false).value
    }
    val locales = LocaleListCompat.getDefault()
    Log.d(TAG, "Presenting app details:")
    Log.d(TAG, "   app '${app.name}' ($packageName) in ${repo.address}")
    Log.d(TAG, "   versions: ${versions?.size}")
    Log.d(TAG, "   appPrefs: $appPrefs")
    return AppDetailsItem(
        repository = repo,
        preferredRepoId = preferredRepoId,
        repositories = repositories, // TODO maybe use emptyList() when only in F-Droid repo
        dbApp = app,
        actions = AppDetailsActions(
            allowBetaVersions = viewModel::allowBetaUpdates,
            ignoreAllUpdates = if (installedVersionCode == null) {
                null
            } else {
                viewModel::ignoreAllUpdates
            },
            ignoreThisUpdate = if (installedVersionCode == null || possibleUpdate == null ||
                possibleUpdate.versionCode <= installedVersionCode
            ) {
                null
            } else {
                viewModel::ignoreThisUpdate
            },
            shareApk = null, // TODO
            uninstallApp = null, // TODO
            launchIntent = packagePair.launchIntent,
            shareIntent = getShareIntent(repo, packageName, app.name ?: ""),
        ),
        versions = versions,
        installedVersion = installedVersion,
        installedVersionCode = installedVersionCode,
        suggestedVersion = suggestedVersion,
        possibleUpdate = possibleUpdate,
        appPrefs = appPrefs,
        noCompatibleVersions = noCompatibleVersions,
        authorHasMoreThanOneApp = authorHasMoreThanOneApp,
        localeList = locales,
    )
}

private fun getShareIntent(
    repo: org.fdroid.database.Repository,
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

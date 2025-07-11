package org.fdroid.basic.details

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_SIGNATURES
import app.cash.molecule.RecompositionMode.Immediate
import app.cash.molecule.launchMolecule
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.fdroid.UpdateChecker
import org.fdroid.basic.ui.main.apps.MinimalApp
import org.fdroid.basic.utils.IoDispatcher
import org.fdroid.database.FDroidDatabase
import org.fdroid.index.RepoManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppDetailsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val scope: CoroutineScope,
    private val db: FDroidDatabase,
    private val repoManager: RepoManager,
    private val updateChecker: UpdateChecker,
) {
    private val packageInfoFlow = MutableStateFlow<AppInfo?>(null)

    val appDetails: StateFlow<AppDetailsItem?> = scope.launchMolecule(
        context = Dispatchers.IO, mode = Immediate,
    ) {
        DetailsPresenter(
            db = db,
            repoManager = repoManager,
            updateChecker = updateChecker,
            packageInfoFlow = packageInfoFlow,
        )
    }

    fun setAppDetails(minimalApp: MinimalApp?) {
        packageInfoFlow.value = null
        val packageManager = context.packageManager
        if (minimalApp != null) scope.launch {
            val packageInfo = try {
                packageManager.getPackageInfo(minimalApp.packageName, GET_SIGNATURES)
            } catch (_: PackageManager.NameNotFoundException) {
                null
            }
            packageInfoFlow.value = if (packageInfo == null) {
                AppInfo(minimalApp.packageName)
            } else {
                val intent = packageManager.getLaunchIntentForPackage(minimalApp.packageName)
                AppInfo(minimalApp.packageName, packageInfo, intent)
            }
        }
    }
}

class AppInfo(
    val packageName: String,
    val packageInfo: PackageInfo? = null,
    val launchIntent: Intent? = null,
)

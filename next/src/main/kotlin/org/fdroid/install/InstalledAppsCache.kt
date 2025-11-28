package org.fdroid.install

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.EXTRA_REPLACING
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_SIGNATURES
import androidx.annotation.UiThread
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.fdroid.utils.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstalledAppsCache @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioScope: CoroutineScope,
) : BroadcastReceiver() {

    private val log = KotlinLogging.logger { }
    private val packageManager = context.packageManager
    private val _installedApps = MutableStateFlow<Map<String, PackageInfo>>(emptyMap())
    val installedApps = _installedApps.asStateFlow()
    private var loadJob: Job? = null

    init {
        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }
        context.registerReceiver(this, intentFilter)
        loadInstalledApps()
    }

    @UiThread
    private fun loadInstalledApps() {
        if (loadJob?.isActive == true) {
            // TODO this may give us a stale cache if an app was changed
            //  while the system had already assembled the data, but we didn't return yet
            log.warn { "Already loading apps, not loading again." }
            return
        }
        loadJob = ioScope.launch {
            log.info { "Loading installed apps..." }
            @Suppress("DEPRECATION") // we'll use this as long as it works, new one was broken
            val installedPackages = packageManager.getInstalledPackages(GET_SIGNATURES)
            _installedApps.update { installedPackages.associateBy { it.packageName } }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.`package` != null) {
            // we have seen duplicate intents on Android 15, need to check other versions
            log.warn { "Ignoring intent with package: $intent" }
            return
        }
        when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED -> onPackageAdded(intent)
            Intent.ACTION_PACKAGE_REMOVED -> onPackageRemoved(intent)
            else -> log.error { "Unknown broadcast received: $intent" }
        }
    }

    private fun onPackageAdded(intent: Intent) {
        val replacing = intent.getBooleanExtra(EXTRA_REPLACING, false)
        log.info { "onPackageAdded($intent) ${intent.data} replacing: $replacing" }
        val packageName = intent.data?.schemeSpecificPart
            ?: error("No package name in ACTION_PACKAGE_ADDED")

        try {
            @Suppress("DEPRECATION") // we'll use this as long as it works, new one was broken
            val packageInfo = packageManager.getPackageInfo(packageName, GET_SIGNATURES)
            // even if the app got replaced, we need to update packageInfo for new version code
            _installedApps.update {
                it.toMutableMap().apply {
                    put(packageName, packageInfo)
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            // Broadcasts don't always get delivered on time. So when this broadcast arrives,
            // the user may already have uninstalled the app.
            log.warn(e) { "Maybe broadcast was late? App not installed anymore: " }
        }
    }

    private fun onPackageRemoved(intent: Intent) {
        val replacing = intent.getBooleanExtra(EXTRA_REPLACING, false)
        log.info { "onPackageRemoved($intent) ${intent.data} replacing: $replacing" }
        val packageName = intent.data?.schemeSpecificPart
            ?: error("No package name in ACTION_PACKAGE_REMOVED")
        if (!replacing) _installedApps.update { apps ->
            apps.toMutableMap().apply {
                remove(packageName)
            }
        }
    }
}

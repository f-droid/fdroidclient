package org.fdroid.ui

import android.content.Intent
import android.content.Intent.ACTION_MAIN
import android.content.Intent.ACTION_SHOW_APP_INFO
import android.content.Intent.EXTRA_PACKAGE_NAME
import android.os.Build.VERSION.SDK_INT
import androidx.core.util.Consumer
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import mu.KotlinLogging

class IntentRouter(private val backStack: NavBackStack<NavKey>) : Consumer<Intent> {
    private val log = KotlinLogging.logger { }
    private val packageNameRegex = "[A-Za-z\\d_.]+".toRegex()

    companion object {
        const val ACTION_MY_APPS = "org.fdroid.action.MY_APPS"
    }

    override fun accept(value: Intent) {
        val intent = value
        log.info { "Incoming intent: $intent" }
        val uri = intent.data
        if (ACTION_MAIN == intent.action) {
            // launcher intent, do nothing
        } else if (ACTION_SHOW_APP_INFO == intent.action) { // App Details
            val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: return
            if (packageName.matches(packageNameRegex)) {
                backStack.add(NavigationKey.AppDetails(packageName))
            } else {
                log.warn { "Malformed package name: $packageName" }
            }
        } else if (uri != null) {
            val packagesUrlRegex = "^/([a-z][a-z][a-zA-Z_-]*/)?packages/[A-Za-z\\d_.]+/?$".toRegex()
            if (uri.scheme == "market" && uri.host == "details") {
                val packageName = uri.getQueryParameter("id") ?: return
                if (packageName.matches(packageNameRegex)) {
                    backStack.add(NavigationKey.AppDetails(packageName))
                } else {
                    log.warn { "Malformed package name: $packageName" }
                }
            } else if (uri.path?.matches(packagesUrlRegex) == true) {
                val packageName = uri.lastPathSegment ?: return
                backStack.add(NavigationKey.AppDetails(packageName))
            } else if (uri.scheme == "fdroidrepos" ||
                uri.scheme == "FDROIDREPOS" ||
                (uri.scheme == "https" && uri.host == "fdroid.link")
            ) {
                backStack.add(NavigationKey.AddRepo(uri.toString()))
            }
        } else if (ACTION_MY_APPS == intent.action) {
            val lastOnBackStack = backStack.lastOrNull()
            if (lastOnBackStack !is NavigationKey.MyApps) {
                if (lastOnBackStack is NavigationKey.Discover) {
                    // reset back stack when going to My Apps from Discover
                    while (backStack.isNotEmpty()) backStack.removeLastOrNull()
                }
                backStack.add(NavigationKey.MyApps)
            }
        } else {
            log.warn { "Unknown intent: $intent - uri: $uri $SDK_INT" }
        }
    }
}

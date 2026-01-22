package org.fdroid.ui.navigation

import android.content.Intent
import android.content.Intent.ACTION_MAIN
import android.content.Intent.ACTION_SHOW_APP_INFO
import android.content.Intent.EXTRA_PACKAGE_NAME
import androidx.core.util.Consumer
import mu.KotlinLogging

class IntentRouter(private val navigator: Navigator) : Consumer<Intent> {
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
                navigator.navigate(NavigationKey.AppDetails(packageName))
            } else {
                log.warn { "Malformed package name: $packageName" }
            }
        } else if (uri != null) {
            val packagesUrlRegex = "^/([a-z][a-z][a-zA-Z_-]*/)?packages/[A-Za-z\\d_.]+/?$".toRegex()
            if (uri.scheme == "market" && uri.host == "details") {
                val packageName = uri.getQueryParameter("id") ?: return
                if (packageName.matches(packageNameRegex)) {
                    navigator.navigate(NavigationKey.AppDetails(packageName))
                } else {
                    log.warn { "Malformed package name: $packageName" }
                }
            } else if (uri.path?.matches(packagesUrlRegex) == true) {
                val packageName = uri.lastPathSegment ?: return
                navigator.navigate(NavigationKey.AppDetails(packageName))
            } else if (uri.scheme == "fdroidrepos" ||
                uri.scheme == "FDROIDREPOS" ||
                (uri.scheme == "https" && uri.host == "fdroid.link")
            ) {
                navigator.navigate(NavigationKey.AddRepo(uri.toString()))
            }
        } else if (ACTION_MY_APPS == intent.action) {
            val lastOnBackStack = navigator.last
            if (lastOnBackStack !is NavigationKey.MyApps) {
                navigator.navigate(NavigationKey.MyApps)
            }
        } else {
            log.warn { "Unknown intent: $intent - uri: $uri" }
        }
    }
}

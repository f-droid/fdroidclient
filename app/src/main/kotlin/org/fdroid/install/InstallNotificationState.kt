package org.fdroid.install

import android.content.Context
import androidx.annotation.StringRes
import org.fdroid.R
import kotlin.math.roundToInt

data class InstallNotificationState(
    val apps: List<AppState>,
    val numBytesDownloaded: Long,
    val numTotalBytes: Long,
) {

    constructor() : this(emptyList(), 0, 0)

    val percent: Int? = if (numTotalBytes > 0) {
        ((numBytesDownloaded.toFloat() / numTotalBytes) * 100).roundToInt()
    } else {
        null
    }

    /**
     * Returns true if there are apps that have an installation in progress which could be
     * waiting for user confirmation or downloading, or waiting for system installer.
     */
    val isInProgress: Boolean = apps.any { it.category != AppStateCategory.INSTALLED }

    /**
     * Returns true if there is at least one app either downloading for actually installing.
     * If there are only apps that have been installed already or are waiting for user confirmation,
     * this will return false.
     */
    val isInstallingSomeApp: Boolean = apps.any { it.category == AppStateCategory.INSTALLING }

    /**
     * Returns true if *all* apps being installed are updates to existing apps.
     */
    private val isUpdatingApps: Boolean = apps.all { it.currentVersionName != null }

    val numInstalled: Int get() = apps.count { it.category == AppStateCategory.INSTALLED }

    fun getTitle(context: Context): String {
        // can briefly show as foreground service notification, before we update real state
        if (apps.isEmpty()) return context.getString(R.string.installing)

        val titleRes = if (isUpdatingApps) {
            R.plurals.notification_updating_title
        } else {
            R.plurals.notification_installing_title
        }
        val numActiveApps: Int = apps.count { it.category != AppStateCategory.INSTALLED }
        val installTitle = context.resources.getQuantityString(
            titleRes,
            numActiveApps,
            numActiveApps,
        )
        val needsUserConfirmation =
            apps.find { it.category == AppStateCategory.NEEDS_CONFIRMATION } != null
        return if (needsUserConfirmation) {
            val s = context.getString(R.string.notification_installing_confirmation)
            "$s $installTitle"
        } else {
            installTitle
        }
    }

    fun getBigText(context: Context): String {
        // split app apps into their categories
        val installing = mutableListOf<AppState>()
        val toConfirm = mutableListOf<AppState>()
        val installed = mutableListOf<AppState>()
        apps.forEach { appState ->
            when (appState.category) {
                AppStateCategory.INSTALLING -> installing.add(appState)
                AppStateCategory.NEEDS_CONFIRMATION -> toConfirm.add(appState)
                AppStateCategory.INSTALLED -> installed.add(appState)
            }
        }
        val sb = StringBuilder()
        fun printApps(@StringRes titleRes: Int, list: List<AppState>, showTitle: Boolean = true) {
            if (list.isEmpty()) return
            if (showTitle) {
                if (sb.isNotEmpty()) sb.append("\n⠀\n")
                sb.append(context.getString(titleRes))
            }
            sb.append("\n")
            list.forEach { appState ->
                sb.append("• ").append(appState.displayStr).append("\n")
            }
        }

        val showInstallTitle = toConfirm.isNotEmpty() || installed.isNotEmpty()
        printApps(R.string.notification_installing_section_confirmation, toConfirm)
        printApps(R.string.notification_installing_section_installing, installing, showInstallTitle)
        printApps(R.string.notification_installing_section_installed, installed)
        return sb.toString()
    }

    fun getSuccessTitle(context: Context): String {
        return context.resources.getQuantityString(
            R.plurals.notification_update_success_title,
            numInstalled,
            numInstalled,
        )
    }

    fun getSuccessBigText(): String {
        val sb = StringBuilder()
        apps.forEach { appState ->
            if (appState.category == AppStateCategory.INSTALLED) {
                sb.append(appState.displayStr).append("\n")
            }
        }
        return sb.toString()
    }
}

data class AppState(
    val packageName: String,
    val category: AppStateCategory,
    val name: String,
    val installVersionName: String,
    val currentVersionName: String?,
) {
    val displayStr: String
        get() {
            val versionStr = if (currentVersionName == null) {
                installVersionName
            } else {
                "$currentVersionName → $installVersionName"
            }
            return "$name $versionStr"
        }
}

enum class AppStateCategory {
    INSTALLING,
    NEEDS_CONFIRMATION,
    INSTALLED
}

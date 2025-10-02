package org.fdroid.install

import android.app.PendingIntent
import org.fdroid.download.DownloadRequest

sealed class InstallState(val showProgress: Boolean) {
    data object Unknown : InstallState(false)
    data class Starting(
        override val name: String,
        override val versionName: String,
        override val currentVersionName: String? = null,
        override val lastUpdated: Long,
        override val iconDownloadRequest: DownloadRequest? = null,
    ) : InstallStateWithInfo(true)

    data class PreApproved(
        override val name: String,
        override val versionName: String,
        override val currentVersionName: String?,
        override val lastUpdated: Long,
        override val iconDownloadRequest: DownloadRequest?,
        val result: PreApprovalResult,
    ) : InstallStateWithInfo(true)

    data class Downloading(
        override val name: String,
        override val versionName: String,
        override val currentVersionName: String?,
        override val lastUpdated: Long,
        override val iconDownloadRequest: DownloadRequest?,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val startMillis: Long,
    ) : InstallStateWithInfo(true) {
        val progress: Float get() = downloadedBytes / totalBytes.toFloat()
    }

    data class Installing(
        override val name: String,
        override val versionName: String,
        override val currentVersionName: String?,
        override val lastUpdated: Long,
        override val iconDownloadRequest: DownloadRequest?,
    ) : InstallStateWithInfo(true)

    data class UserConfirmationNeeded(
        override val name: String,
        override val versionName: String,
        override val currentVersionName: String?,
        override val lastUpdated: Long,
        override val iconDownloadRequest: DownloadRequest?,
        val sessionId: Int,
        val intent: PendingIntent,
        val progress: Float,
    ) : InstallStateWithInfo(true) {
        constructor(
            state: InstallStateWithInfo,
            sessionId: Int,
            intent: PendingIntent,
            progress: Float
        ) : this(
            name = state.name,
            versionName = state.versionName,
            currentVersionName = state.currentVersionName,
            lastUpdated = state.lastUpdated,
            iconDownloadRequest = state.iconDownloadRequest,
            sessionId = sessionId,
            intent = intent,
            progress = progress
        )
    }

    data class Installed(
        override val name: String,
        override val versionName: String,
        override val currentVersionName: String?,
        override val lastUpdated: Long,
        override val iconDownloadRequest: DownloadRequest?,
    ) : InstallStateWithInfo(false)

    data object UserAborted : InstallState(false)
    data class Error(val msg: String?) : InstallState(false)

    data object Uninstalled : InstallState(false)
}

sealed class InstallStateWithInfo(showProgress: Boolean) : InstallState(showProgress) {
    abstract val name: String
    abstract val versionName: String
    abstract val currentVersionName: String?
    abstract val lastUpdated: Long
    abstract val iconDownloadRequest: DownloadRequest?
}

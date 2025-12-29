package org.fdroid.install

import android.app.PendingIntent
import org.fdroid.database.AppVersion
import org.fdroid.database.Repository
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

    data class PreApprovalConfirmationNeeded(
        private val state: InstallStateWithInfo,
        val version: AppVersion,
        val repo: Repository,
        override val sessionId: Int,
        override val intent: PendingIntent,
    ) : InstallConfirmationState() {
        override val name: String = state.name
        override val versionName: String = state.versionName
        override val currentVersionName: String? = state.currentVersionName
        override val lastUpdated: Long = state.lastUpdated
        override val iconDownloadRequest: DownloadRequest? = state.iconDownloadRequest
    }

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
        override val sessionId: Int,
        override val intent: PendingIntent,
        val progress: Float,
    ) : InstallConfirmationState() {
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

    data class Error(
        val msg: String?,
        override val name: String,
        override val versionName: String,
        override val currentVersionName: String?,
        override val lastUpdated: Long,
        override val iconDownloadRequest: DownloadRequest?,
    ) : InstallStateWithInfo(false) {
        constructor(msg: String?, s: InstallStateWithInfo) : this(
            msg = msg,
            name = s.name,
            versionName = s.versionName,
            currentVersionName = s.currentVersionName,
            lastUpdated = s.lastUpdated,
            iconDownloadRequest = s.iconDownloadRequest,
        )
    }

    data object Uninstalled : InstallState(false)
}

sealed class InstallStateWithInfo(showProgress: Boolean) : InstallState(showProgress) {
    abstract val name: String
    abstract val versionName: String
    abstract val currentVersionName: String?
    abstract val lastUpdated: Long
    abstract val iconDownloadRequest: DownloadRequest?
}

sealed class InstallConfirmationState() : InstallStateWithInfo(true) {
    abstract val sessionId: Int
    abstract val intent: PendingIntent
}

package org.fdroid.install

import android.app.PendingIntent

sealed class InstallState(val showProgress: Boolean) {
    data object Unknown : InstallState(false)
    data object Starting : InstallState(true)
    data class PreApproved(val result: PreApprovalResult) : InstallState(true)
    data class Downloading(
        val sessionId: Int?,
        val downloadedBytes: Long,
        val totalBytes: Long,
    ) : InstallState(true)

    data class Installing(val sessionId: Int?) : InstallState(true)
    data class UserConfirmationNeeded(
        val sessionId: Int,
        val intent: PendingIntent,
        val progress: Float,
    ) : InstallState(true)

    data object PreApprovalFailed : InstallState(true)

    data object Installed : InstallState(false)
    data object UserAborted : InstallState(false)
    data class Error(val msg: String?) : InstallState(false)

    data object Uninstalled : InstallState(false)
}

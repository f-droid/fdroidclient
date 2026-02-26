package org.fdroid.install

import android.app.PendingIntent

sealed interface PreApprovalResult {
  data object NotSupported : PreApprovalResult

  data object UserAborted : PreApprovalResult

  data class UserConfirmationRequired(val sessionId: Int, val intent: PendingIntent) :
    PreApprovalResult

  data class Success(val sessionId: Int) : PreApprovalResult

  data class Error(val errorMsg: String?) : PreApprovalResult
}

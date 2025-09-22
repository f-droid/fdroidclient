package org.fdroid.install

sealed interface PreApprovalResult {
    data object NotSupported : PreApprovalResult
    data object UserAborted : PreApprovalResult
    data class Success(val sessionId: Int) : PreApprovalResult
    data class Error(val errorMsg: String?) : PreApprovalResult
}

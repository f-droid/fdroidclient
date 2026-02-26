package org.fdroid.ui.crash

import org.acra.config.RetryPolicy
import org.acra.sender.ReportSender

class NoRetryPolicy : RetryPolicy {
  override fun shouldRetrySend(
    senders: List<ReportSender>,
    failedSenders: List<RetryPolicy.FailedSender>,
  ): Boolean {
    return false
  }
}

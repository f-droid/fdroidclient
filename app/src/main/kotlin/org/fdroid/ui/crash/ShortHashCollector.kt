package org.fdroid.ui.crash

import android.content.Context
import kotlin.math.min
import org.acra.ReportField
import org.acra.builder.ReportBuilder
import org.acra.collector.BaseReportFieldCollector
import org.acra.config.CoreConfiguration
import org.acra.data.CrashReportData

class ShortHashCollector : BaseReportFieldCollector(ReportField.CUSTOM_DATA) {

  override fun collect(
    reportField: ReportField,
    context: Context,
    config: CoreConfiguration,
    reportBuilder: ReportBuilder,
    target: CrashReportData,
  ) {
    val hash = getStackTraceHash(reportBuilder.exception)
    if (hash != null) {
      target.put("STACK_TRACE_SHORT_HASH", hash)
    }
  }

  private fun getStackTraceHash(th: Throwable?): String? {
    if (th == null) return null
    val res = StringBuilder()
    var cause = th
    while (cause != null) {
      // we only consider the first 3 stack trace elements of each cause,
      // because we found the bottom often includes irrelevant changes
      val numElements = min(3, cause.stackTrace.size)
      val stackTraceElements = cause.stackTrace.toMutableList().subList(0, numElements)
      stackTraceElements.forEachIndexed { i, e ->
        res.append(e.className)
        res.append(e.methodName)
        // first element also includes line number to differentiate between crashes in same method
        if (i == 0) res.append(e.lineNumber)
      }
      cause = cause.cause
    }
    return Integer.toHexString(res.toString().hashCode())
  }
}

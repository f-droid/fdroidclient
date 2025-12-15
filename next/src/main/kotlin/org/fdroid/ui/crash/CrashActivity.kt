package org.fdroid.ui.crash

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import mu.KotlinLogging
import org.acra.ReportField
import org.acra.dialog.CrashReportDialogHelper
import org.fdroid.ui.FDroidContent
import java.io.IOException

class CrashActivity : ComponentActivity() {
    private val log = KotlinLogging.logger {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val helper = CrashReportDialogHelper(this, intent)
        setContent {
            FDroidContent {
                Crash(
                    onCancel = {
                        helper.cancelReports()
                        finishAfterTransition()
                    },
                    onSend = { comment, userEmail ->
                        helper.sendCrash(comment, userEmail)
                        finishAfterTransition()
                    },
                    onSave = { uri, comment ->
                        onSave(helper, uri, comment)
                    },
                )
            }
        }
    }

    private fun onSave(helper: CrashReportDialogHelper, uri: Uri, comment: String): Boolean {
        return try {
            val crashData = helper.reportData.apply {
                if (comment.isNotBlank()) {
                    put(ReportField.USER_COMMENT, comment)
                }
            }
            contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                outputStream.write(crashData.toJSON().encodeToByteArray())
            } ?: throw IOException("Could not open $uri")
            true
        } catch (e: Exception) {
            log.error(e) { "Error saving log: " }
            false
        }
    }
}

package org.fdroid.ui.panic

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlin.system.exitProcess

class ExitActivity : AppCompatActivity() {
    companion object {
        fun exitAndRemoveFromRecentApps(activity: AppCompatActivity) {
            activity.runOnUiThread({
                val intent = Intent(activity, ExitActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                            or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                            or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            or Intent.FLAG_ACTIVITY_NO_ANIMATION
                    )
                }
                activity.startActivity(intent)
            })
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finishAndRemoveTask()
        exitProcess(0)
    }
}

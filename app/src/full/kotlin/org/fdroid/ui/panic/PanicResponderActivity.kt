package org.fdroid.ui.panic

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import info.guardianproject.panic.Panic
import info.guardianproject.panic.PanicResponder
import mu.KotlinLogging

/**
 * This [AppCompatActivity] is purely to run events in response to a panic trigger.
 * It needs to be an `AppCompatActivity` rather than a [android.app.Service]
 * so that it can fetch some of the required information about what sent the
 * [Intent].  This is therefore an `AppCompatActivity` without any UI, which
 * is a special case in Android.  All the code must be in
 * [onCreate] and [finish] must be called at the end of
 * that method.
 *
 * @see PanicResponder.receivedTriggerFromConnectedApp
 */
@AndroidEntryPoint
class PanicResponderActivity : AppCompatActivity() {

    private val log = KotlinLogging.logger { }
    private val viewModel: PanicSettingsViewModel by viewModels()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = getIntent()
        if (!Panic.isTriggerIntent(intent)) {
            finish()
            return
        }

        // received intent from panic app
        log.info { "Received Panic Trigger..." }

        // FIXME: we should ideally also check the signature of the connected app,
        //  not only the package name as it could be a fake app occupying the same package name
        val receivedTriggerFromConnectedApp = PanicResponder.receivedTriggerFromConnectedApp(this)

        if (receivedTriggerFromConnectedApp) {
            if (viewModel.resetRepos) {
                viewModel.resetDb()
            }
        }

        // exit and clear, if not deactivated
        if (viewModel.exitApp) {
            ExitActivity.exitAndRemoveFromRecentApps(this)
            finishAndRemoveTask()
        } else {
            finish()
        }
    }
}

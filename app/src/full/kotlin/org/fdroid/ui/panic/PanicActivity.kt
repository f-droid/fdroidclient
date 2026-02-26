package org.fdroid.ui.panic

import android.os.Bundle
import android.view.WindowManager.LayoutParams.FLAG_SECURE
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import info.guardianproject.panic.PanicResponder
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_DYNAMIC_COLORS
import org.fdroid.settings.SettingsManager
import org.fdroid.ui.FDroidContent
import javax.inject.Inject

@AndroidEntryPoint
class PanicActivity : AppCompatActivity() {

    private val log = KotlinLogging.logger { }
    private val viewModel: PanicSettingsViewModel by viewModels()

    @Inject
    lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // observe preventScreenshots setting and react to changes
        lifecycleScope.launch {
            // this flow doesn't change when we are paused, so we keep collecting it
            settingsManager.preventScreenshotsFlow.collect { preventScreenshots ->
                if (preventScreenshots) {
                    window?.addFlags(FLAG_SECURE)
                } else {
                    window?.clearFlags(FLAG_SECURE)
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.appFlow.drop(1).collect { packageName ->
                    log.info { "Setting panic trigger package name..." }
                    PanicResponder.setTriggerPackageName(this@PanicActivity, packageName)
                }
            }
        }
        enableEdgeToEdge()
        setContent {
            val dynamicColors = settingsManager.dynamicColorFlow.collectAsStateWithLifecycle(
                PREF_DEFAULT_DYNAMIC_COLORS
            ).value
            val viewModel = hiltViewModel<PanicSettingsViewModel>()
            FDroidContent(
                dynamicColors = dynamicColors,
            ) {
                PanicSettings(
                    prefsFlow = viewModel.prefsFlow,
                    state = viewModel.state.collectAsStateWithLifecycle().value,
                    onBackClicked = { onBackPressedDispatcher.onBackPressed() },
                )
            }
        }
    }
}

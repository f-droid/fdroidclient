package org.fdroid.ui.panic

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager.LayoutParams.FLAG_SECURE
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.edit
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import info.guardianproject.panic.Panic
import info.guardianproject.panic.PanicResponder
import info.guardianproject.panic.PanicResponder.PREF_TRIGGER_PACKAGE_NAME
import javax.inject.Inject
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.fdroid.R
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_DYNAMIC_COLORS
import org.fdroid.settings.SettingsManager
import org.fdroid.ui.FDroidContent

@AndroidEntryPoint
class PanicActivity : AppCompatActivity() {

  private val log = KotlinLogging.logger {}
  private val viewModel: PanicSettingsViewModel by viewModels()
  private var showPanicDialog by mutableStateOf(false)
  private var panicAppName by mutableStateOf("")

  @Inject lateinit var settingsManager: SettingsManager

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    checkIntent()

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
      val dynamicColors =
        settingsManager.dynamicColorFlow
          .collectAsStateWithLifecycle(PREF_DEFAULT_DYNAMIC_COLORS)
          .value
      val viewModel = hiltViewModel<PanicSettingsViewModel>()
      FDroidContent(dynamicColors = dynamicColors) {
        PanicSettings(
          prefsFlow = viewModel.prefsFlow,
          state = viewModel.state.collectAsStateWithLifecycle().value,
          onBackClicked = { onBackPressedDispatcher.onBackPressed() },
        )
      }
      if (showPanicDialog) {
        PanicConfirmationDialog(
          panicAppName = panicAppName,
          onConfirm = {
            val pkg = getCallerPackageName()
            if (pkg != null) {
              PanicResponder.setTriggerPackageName(this@PanicActivity, pkg)
              val prefs = getSharedPreferences("${packageName}_preferences", MODE_PRIVATE)
              prefs.edit { putString(PREF_TRIGGER_PACKAGE_NAME, pkg) }
              viewModel.setTriggerPackageName(pkg)
            } else {
              // it's not clear what can be done in this state, so currently it's a no-op
            }
            setResult(RESULT_OK)
            showPanicDialog = false
          },
          onDismiss = {
            setResult(RESULT_CANCELED)
            showPanicDialog = false
            finish()
          },
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    // set the activity's intent to the new one received
    setIntent(intent)
    checkIntent()
  }

  private fun checkIntent() {
    // check intent to see if this was triggered by the panic app settings ui
    val intent = getIntent()
    if (intent != null) {
      if (Panic.ACTION_CONNECT == intent.action) {
        // then check settings to see if we're coming from a new app
        val prefs = getSharedPreferences("${packageName}_preferences", MODE_PRIVATE)
        val packagePref: String =
          prefs.getString(PREF_TRIGGER_PACKAGE_NAME, null) ?: Panic.PACKAGE_NAME_NONE
        val callerPackageName: String? = getCallerPackageName()
        if (callerPackageName != packagePref) {
          showPanicDialog = true

          // get app name if possible
          if (callerPackageName != null) {
            try {
              panicAppName =
                packageManager.getApplicationLabel(
                  packageManager.getApplicationInfo(callerPackageName, 0)
                ) as String
            } catch (e: PackageManager.NameNotFoundException) {
              log.error(e) { "Panic app label not found: " }
              panicAppName = getString(R.string.panic_app_unknown_app)
            }
          } else {
            panicAppName = getString(R.string.panic_app_unknown_app)
          }
        }
      } else if (Panic.ACTION_DISCONNECT == intent.action) {
        // action not supported by previous fdroid version
        log.warn { "Panic app disconnect action not supported " }
      }
    }
  }

  private fun getCallerPackageName(): String? {
    // attempt various methods of determining the package that started this activity
    if (callingPackage != null) {
      return callingPackage
    }
    if (callingActivity?.packageName != null) {
      return callingActivity?.packageName
    }
    if (intent.getStringExtra(Intent.EXTRA_PACKAGE_NAME) != null) {
      return intent.getStringExtra(Intent.EXTRA_PACKAGE_NAME)
    }
    return null
  }
}

@Composable
fun PanicConfirmationDialog(
  panicAppName: String,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = {},
    properties =
      DialogProperties(
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
      ),
    title = { Text(stringResource(R.string.panic_app_dialog_title)) },
    text = { Text(stringResource(R.string.panic_app_dialog_message, panicAppName)) },
    confirmButton = {
      TextButton(onClick = onConfirm) { Text(stringResource(android.R.string.ok)) }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
    },
  )
}

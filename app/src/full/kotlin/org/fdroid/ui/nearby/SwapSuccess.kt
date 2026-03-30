package org.fdroid.ui.nearby

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.fdroid.R
import org.fdroid.install.InstallConfirmationState
import org.fdroid.install.InstallState
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.utils.BigLoadingIndicator

object SwapSuccessBinder {
  @JvmStatic
  fun bind(composeView: ComposeView, viewModel: SwapSuccessViewModel) {
    composeView.setViewCompositionStrategy(
      ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
    )
    composeView.setContent {
      FDroidContent {
        val model by viewModel.model.collectAsStateWithLifecycle()
        val appToConfirm = model.appToConfirm
        LaunchedEffect(appToConfirm?.packageName, appToConfirm?.installState) {
          val state = appToConfirm?.installState as? InstallConfirmationState ?: return@LaunchedEffect
          viewModel.confirmAppInstall(appToConfirm.packageName, state)
        }
        SwapSuccess(
          model = model,
          onInstall = viewModel::install,
          onCancel = viewModel::cancelInstall,
          onCheckUserConfirmation = viewModel::checkUserConfirmation,
        )
      }
    }
  }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SwapSuccess(
  model: SwapSuccessModel,
  onInstall: (String) -> Unit,
  onCancel: (String) -> Unit,
  onCheckUserConfirmation: (String, InstallState.UserConfirmationNeeded) -> Unit,
) {
  val lifecycleOwner = LocalLifecycleOwner.current
  val currentAppToConfirm = model.appToConfirm
  var numChecks by remember { mutableIntStateOf(0) }
  DisposableEffect(
    lifecycleOwner,
    currentAppToConfirm?.packageName,
    currentAppToConfirm?.installState
  ) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        when (val state = currentAppToConfirm?.installState) {
            is InstallState.UserConfirmationNeeded if numChecks < 3 -> {
              Log.i("SwapSuccessScreen", "Resumed ($numChecks). Checking user confirmation... $state")
              numChecks += 1
              onCheckUserConfirmation(currentAppToConfirm.packageName, state)
            }
          is InstallState.UserConfirmationNeeded -> {
            Log.i("SwapSuccessScreen", "Cancel installation after repeated confirmation checks")
            onCancel(currentAppToConfirm.packageName)
          }
          else -> {
            numChecks = 0
          }
        }
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  when {
    model.loading -> BigLoadingIndicator()
    model.apps.isEmpty() -> {
      Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(stringResource(R.string.swap_connecting))
      }
    }
    else -> {
      LazyColumn {
        items(model.apps, key = { it.packageName }) { app ->
          SwapSuccessAppRow(app = app, onInstall = onInstall, onCancel = onCancel)
        }
      }
    }
  }
}

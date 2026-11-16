package org.fdroid.ui.onboarding

import android.Manifest.permission.POST_NOTIFICATIONS
import android.os.Build.VERSION.SDK_INT
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.settings.MainOnboardingState
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.navigation.NavigationKey
import org.fdroid.ui.utils.FDroidButton
import org.fdroid.ui.utils.launchSafe
import org.fdroid.ui.utils.mainOnboardingState

@Composable
fun MainOnboardingPermissions(
  mainOnboardingState: MainOnboardingState,
  reCheckPermissions: () -> Unit,
  onNav: (NavigationKey) -> Unit,
) {
  Column(
    modifier =
      Modifier.fillMaxSize()
        .safeDrawingPadding()
        .padding(vertical = 16.dp)
        .verticalScroll(rememberScrollState()),
    verticalArrangement = spacedBy(16.dp),
  ) {
    StepIndicators(
      totalSteps = 2,
      currentStep = 1,
      modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally),
    )
    Text(
      text = stringResource(R.string.onboarding_permission_title),
      style = MaterialTheme.typography.headlineMedium,
      modifier = Modifier.padding(horizontal = 16.dp),
    )
    Text(
      text = stringResource(R.string.onboarding_permission_summary),
      style = MaterialTheme.typography.bodyMedium,
      modifier = Modifier.padding(horizontal = 16.dp),
    )
    val intentLauncher =
      rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { reCheckPermissions() },
      )
    ListItem(
      supportingContent = {
        Text(text = stringResource(R.string.onboarding_permission_install_apps_summary))
      },
      trailingContent = {
        Switch(
          checked = mainOnboardingState.permissionState.canRequestPackageInstalls,
          onCheckedChange = null,
        )
      },
      modifier =
        Modifier.clickable {
          if (!mainOnboardingState.permissionState.canRequestPackageInstalls) {
            intentLauncher.launchSafe(
              mainOnboardingState.permissionState.requestPackageInstallsIntent,
            )
          }
        },
    ) {
      Text(text = stringResource(R.string.onboarding_permission_install_apps_title))
    }
    val notificationLauncher =
      rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { reCheckPermissions() },
      )
    ListItem(
      supportingContent = {
        Text(text = stringResource(R.string.onboarding_permission_notifications_summary))
      },
      trailingContent = {
        Switch(
          checked = mainOnboardingState.permissionState.hasNotificationPermission,
          onCheckedChange = null,
        )
      },
      modifier =
        Modifier.clickable {
          if (SDK_INT >= 33 && !mainOnboardingState.permissionState.hasNotificationPermission) {
            notificationLauncher.launchSafe(POST_NOTIFICATIONS)
          }
        },
    ) {
      Text(text = stringResource(R.string.onboarding_permission_notifications_title))
    }
    ListItem(
      supportingContent = {
        Text(text = stringResource(R.string.onboarding_permission_doze_summary))
      },
      trailingContent = {
        Switch(
          checked = mainOnboardingState.permissionState.isIgnoringBatteryOptimizations,
          onCheckedChange = null,
        )
      },
      modifier =
        Modifier.clickable {
          if (!mainOnboardingState.permissionState.isIgnoringBatteryOptimizations) {
            intentLauncher.launchSafe(
              mainOnboardingState.permissionState.requestIgnoreBatteryOptimizationsIntent,
            )
          }
        },
    ) {
      Text(text = stringResource(R.string.onboarding_permission_doze_title))
    }
    Spacer(Modifier.weight(1f))
    if (mainOnboardingState.permissionState.acceptable) {
      FDroidButton(
        text = stringResource(R.string.onboarding_button_finish),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        onClick = { onNav(NavigationKey.Discover) },
      )
    } else {
      TextButton(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        onClick = { onNav(NavigationKey.Discover) },
      ) {
        Text(text = stringResource(R.string.onboarding_button_skip))
      }
    }
  }
}

@Composable
@PreviewLightDark
private fun Preview() {
  FDroidContent {
    MainOnboardingPermissions(
      mainOnboardingState = mainOnboardingState,
      reCheckPermissions = {},
      onNav = {},
    )
  }
}

@Composable
@PreviewLightDark
private fun PreviewSkip() {
  FDroidContent {
    MainOnboardingPermissions(
      mainOnboardingState.copy(
        permissionState =
          mainOnboardingState.permissionState.copy(
            hasNotificationPermission = false,
            isIgnoringBatteryOptimizations = false,
          ),
      ),
      reCheckPermissions = {},
      onNav = {},
    )
  }
}

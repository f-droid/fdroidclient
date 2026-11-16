package org.fdroid.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.android.tools.screenshot.PreviewTest
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.utils.mainOnboardingState

@Composable
@PreviewTest
@PreviewLightDark
private fun MainOnboardingPermissionsTest() {
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

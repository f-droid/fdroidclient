package org.fdroid.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.android.tools.screenshot.PreviewTest
import org.fdroid.download.NetworkState
import org.fdroid.ui.FDroidContent

@Preview
@Composable
@PreviewTest
private fun WifiTest() = FDroidContent {
  MainOnboardingWelcome(
    networkState = NetworkState(isOnline = true, isMetered = false, warnWhenMetered = true),
    onNav = {},
  )
}

@Composable
@PreviewTest
@PreviewLightDark
private fun MeteredTest() = FDroidContent {
  MainOnboardingWelcome(
    networkState = NetworkState(isOnline = true, isMetered = true, warnWhenMetered = true),
    onNav = {},
  )
}

@Preview
@Composable
@PreviewTest
private fun OfflineTest() = FDroidContent {
  MainOnboardingWelcome(
    networkState = NetworkState(isOnline = false, isMetered = false, warnWhenMetered = true),
    onNav = {},
  )
}

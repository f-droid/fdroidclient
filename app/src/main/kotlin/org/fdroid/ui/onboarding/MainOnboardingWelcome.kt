package org.fdroid.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.download.NetworkState
import org.fdroid.repo.RepoUpdateWorker
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.navigation.NavigationKey
import org.fdroid.ui.utils.FDroidButton
import org.fdroid.ui.utils.SelectableCard

@Composable
fun MainOnboardingWelcome(
  networkState: NetworkState,
  onNav: (NavigationKey) -> Unit,
) {
  Column(
    modifier =
      Modifier.fillMaxSize()
        .safeDrawingPadding()
        .padding(16.dp)
        .verticalScroll(rememberScrollState()),
    verticalArrangement = spacedBy(16.dp),
  ) {
    var downloadWhenMetered by remember { mutableStateOf(true) }
    StepIndicators(
      totalSteps = 2,
      currentStep = 0,
      modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally),
    )
    Text(
      text = stringResource(R.string.onboarding_title),
      style = MaterialTheme.typography.headlineMedium,
    )
    Text(
      text = stringResource(R.string.onboarding_intro),
      style = MaterialTheme.typography.bodyMedium,
    )
    if (networkState.isOnline) {
      Text(
        text = stringResource(R.string.onboarding_online),
        style = MaterialTheme.typography.bodyMedium,
      )
    } else {
      Text(
        text = stringResource(R.string.first_start_offline),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
      )
    }
    if (networkState.isOnline && networkState.showWarningDialog) {
      SelectableCard(
        selected = downloadWhenMetered,
        modifier = Modifier.padding(top = 16.dp),
        onClick = { downloadWhenMetered = true },
      ) {
        Column(
          verticalArrangement = spacedBy(8.dp),
          modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
        ) {
          Text(
            text = stringResource(R.string.onboarding_metered_title),
            style = MaterialTheme.typography.titleMedium,
          )
          Text(
            text = stringResource(R.string.onboarding_metered_summary),
            style = MaterialTheme.typography.bodyMedium,
          )
        }
      }
      SelectableCard(selected = !downloadWhenMetered, onClick = { downloadWhenMetered = false }) {
        Column(
          verticalArrangement = spacedBy(8.dp),
          modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
        ) {
          Text(
            text = stringResource(R.string.onboarding_wifi_title),
            style = MaterialTheme.typography.titleMedium,
          )
          Text(
            text = stringResource(R.string.onboarding_wifi_summary),
            style = MaterialTheme.typography.bodyMedium,
          )
        }
      }
    }
    Spacer(Modifier.weight(1f))
    val context = LocalContext.current
    FDroidButton(
      text = stringResource(R.string.next),
      modifier = Modifier.fillMaxWidth(),
      onClick = {
        if (networkState.isOnline && (downloadWhenMetered || !networkState.showWarningDialog)) {
          RepoUpdateWorker.updateNow(context)
        }
        onNav(NavigationKey.MainOnboardingPermissions)
      },
    )
  }
}

@Composable
fun StepIndicators(
  totalSteps: Int,
  currentStep: Int,
  modifier: Modifier = Modifier,
  activeColor: Color = MaterialTheme.colorScheme.primary,
  inactiveColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
) {
  Row(
    modifier = modifier,
    horizontalArrangement = spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    repeat(totalSteps) { index ->
      val isSelected = index == currentStep
      val width by
        animateDpAsState(
          targetValue = if (isSelected) 24.dp else 8.dp,
          label = "IndicatorWidth",
        )
      val color by
        animateColorAsState(
          targetValue = if (isSelected) activeColor else inactiveColor,
          label = "IndicatorColor",
        )
      Box(modifier = Modifier.height(8.dp).width(width).clip(CircleShape).background(color))
    }
  }
}

@Composable
@PreviewLightDark
private fun PreviewWifi() {
  FDroidContent {
    MainOnboardingWelcome(
      networkState = NetworkState(isOnline = true, isMetered = false, warnWhenMetered = true),
      onNav = {},
    )
  }
}

@Composable
@PreviewLightDark
private fun PreviewMetered() {
  FDroidContent {
    MainOnboardingWelcome(
      networkState = NetworkState(isOnline = true, isMetered = true, warnWhenMetered = true),
      onNav = {},
    )
  }
}

@Composable
@PreviewLightDark
private fun PreviewOffline() {
  FDroidContent {
    MainOnboardingWelcome(
      networkState = NetworkState(isOnline = false, isMetered = false, warnWhenMetered = true),
      onNav = {},
    )
  }
}

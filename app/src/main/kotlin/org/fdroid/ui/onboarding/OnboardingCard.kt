package org.fdroid.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.ui.FDroidContent

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun OnboardingPopupCard(
  title: String,
  message: String,
  modifier: Modifier = Modifier,
  buttonText: String = stringResource(R.string.got_it),
  onGotIt: () -> Unit = {},
) {
  val focusRequester = remember { FocusRequester() }
  OnboardingCard(
    title = title,
    message = message,
    modifier = modifier.widthIn(max = TooltipDefaults.richTooltipMaxWidth),
    buttonText = buttonText,
    buttonModifier = Modifier.focusRequester(focusRequester),
    onGotIt = onGotIt,
  )
  LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@Composable
fun OnboardingCard(
  title: String,
  message: String,
  modifier: Modifier = Modifier,
  buttonText: String = stringResource(R.string.got_it),
  buttonModifier: Modifier = Modifier,
  onGotIt: () -> Unit = {},
) {
  ElevatedCard(modifier = modifier) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleSmall,
      modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
    )
    Text(
      text = message,
      style = MaterialTheme.typography.bodyMedium,
      modifier = Modifier.padding(horizontal = 16.dp),
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
      TextButton(
        onClick = onGotIt,
        modifier = buttonModifier.padding(vertical = 8.dp, horizontal = 16.dp),
      ) {
        Text(text = buttonText)
      }
    }
  }
}

@Preview
@Composable
private fun Preview() {
  FDroidContent {
    OnboardingCard(
      title = "Filter",
      message =
        "Here you can apply filters to the list of apps," +
          " e.g. showing only apps within a certain category or repository. " +
          "Changing the sort order is also possible.",
      modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
    ) {}
  }
}

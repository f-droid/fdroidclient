package org.fdroid.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.viktormykhailiv.compose.hints.Hint
import com.viktormykhailiv.compose.hints.HintProperties
import com.viktormykhailiv.compose.hints.rememberHint
import org.fdroid.R
import org.fdroid.ui.navigation.NavigationKey

@Composable
fun getMainOnboardingHint(onOnboardingSeen: () -> Unit, onNav: (NavigationKey) -> Unit): Hint {
  return rememberHint(HintProperties(dismissOnClickOutside = false)) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
      ElevatedCard(modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)) {
        Text(
          text = stringResource(R.string.panic_hide_migration_title),
          style = MaterialTheme.typography.titleSmall,
          modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
        )
        Text(
          text = stringResource(R.string.panic_hide_migration_message),
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.padding(horizontal = 16.dp),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          TextButton(
            onClick = {
              onNav(NavigationKey.Settings)
              onOnboardingSeen()
            },
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
          ) {
            Text(text = stringResource(R.string.panic_hide_migration_button))
          }
        }
      }
    }
  }
}

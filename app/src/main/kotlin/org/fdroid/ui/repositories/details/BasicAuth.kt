package org.fdroid.ui.repositories.details

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.utils.FDroidOutlineButton
import org.fdroid.ui.utils.VerticalAnimatedVisibility

@Composable
fun BasicAuth(
  username: String,
  modifier: Modifier = Modifier,
  onEditCredentials: (String, String) -> Unit,
) {
  val usernameState = rememberTextFieldState(initialText = username)
  val passwordState = rememberTextFieldState()
  var showSaveButton by remember { mutableStateOf(true) }
  Column(verticalArrangement = spacedBy(8.dp), modifier = modifier.fillMaxWidth().imePadding()) {
    Text(
      text = stringResource(R.string.repo_basic_auth_title),
      style = MaterialTheme.typography.titleMedium,
    )
    TextField(
      state = usernameState,
      label = { Text(stringResource(R.string.repo_basicauth_username)) },
    )
    TextField(
      state = passwordState,
      label = { Text(stringResource(R.string.repo_basicauth_password)) },
    )
    VerticalAnimatedVisibility(
      visible = showSaveButton,
      modifier = Modifier.align(Alignment.End),
    ) {
      FDroidOutlineButton(
        text = stringResource(R.string.repo_basicauth_edit),
        onClick = {
          val username = usernameState.text.toString()
          val password = passwordState.text.toString()
          onEditCredentials(username, password)
          showSaveButton = false
        },
        imageVector = Icons.Default.Save,
      )
    }
  }
}

@Composable
@Preview
fun BasicAuthCardPreview() {
  FDroidContent { BasicAuth("username", Modifier.padding(16.dp)) { _, _ -> } }
}

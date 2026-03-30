package org.fdroid.ui.nearby

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import org.fdroid.R
import org.fdroid.fdroid.nearby.SwapService
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.utils.BackButton

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun NearbyStart(onBackClicked: () -> Unit) {
  val context = LocalContext.current
  Scaffold(
    topBar = { TopAppBar(navigationIcon = { BackButton(onClick = onBackClicked) }, title = {}) }
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .padding(innerPadding)
          .padding(top = 16.dp, start = 16.dp, end = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = spacedBy(16.dp),
    ) {
      Text(stringResource(R.string.nearby_splash__download_apps_from_people_nearby))
      Button(
        onClick = {
          val i = Intent(context, SwapService::class.java)
          ContextCompat.startForegroundService(context, i)
        }
      ) {
        Text(stringResource(R.string.nearby_splash__find_people_button))
      }
    }
  }
}

@Preview
@Composable
private fun Preview() {
  FDroidContent { NearbyStart({}) }
}

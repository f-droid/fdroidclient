package org.fdroid.ui.details

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import org.fdroid.R
import org.fdroid.database.AppVersion
import org.fdroid.database.KnownVulnerability
import org.fdroid.database.NoCompatibleSigner
import org.fdroid.database.NotAvailable
import org.fdroid.database.UpdateInOtherRepo
import org.fdroid.index.v2.ANTI_FEATURE_KNOWN_VULNERABILITY
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.utils.testApp

@Composable
fun AppDetailsWarnings(item: AppDetailsItem, modifier: Modifier = Modifier) {
  val (color, string) =
    when {
      // app issues take priority
      item.issue != null ->
        when (item.issue) {
          // apps has a known security vulnerability
          is KnownVulnerability -> {
            val details =
              item.versions?.firstNotNullOfOrNull { versionItem ->
                (versionItem.version as? AppVersion)?.getAntiFeatureReason(
                  antiFeatureKey = ANTI_FEATURE_KNOWN_VULNERABILITY,
                  localeList = LocaleListCompat.getDefault(),
                )
              }
            Pair(
              MaterialTheme.colorScheme.errorContainer,
              if (details.isNullOrBlank()) {
                stringResource(R.string.antiknownvulnlist)
              } else {
                stringResource(R.string.antiknownvulnlist) + ":\n\n" + details
              },
            )
          }
          is NoCompatibleSigner ->
            Pair(
              MaterialTheme.colorScheme.errorContainer,
              if (item.issue.repoIdWithCompatibleSigner == null) {
                stringResource(R.string.app_no_compatible_signer)
              } else {
                stringResource(R.string.app_no_compatible_signer_in_this_repo)
              },
            )
          is UpdateInOtherRepo ->
            Pair(
              MaterialTheme.colorScheme.inverseSurface,
              stringResource(R.string.app_issue_update_other_repo),
            )
          NotAvailable ->
            Pair(MaterialTheme.colorScheme.errorContainer, stringResource(R.string.error))
        }
      // app is outright incompatible
      item.isIncompatible ->
        Pair(
          MaterialTheme.colorScheme.errorContainer,
          stringResource(R.string.app_no_compatible_versions),
        )
      // app targets old targetSdk, not a deal breaker, but worth flagging, no auto-update
      item.oldTargetSdk ->
        Pair(MaterialTheme.colorScheme.inverseSurface, stringResource(R.string.app_no_auto_update))
      else -> return
    }
  ElevatedCard(
    colors = CardDefaults.elevatedCardColors(containerColor = color),
    modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
  ) {
    WarningRow(text = string)
  }
}

@Composable
private fun WarningRow(text: String) {
  Row(
    horizontalArrangement = spacedBy(16.dp),
    verticalAlignment = CenterVertically,
    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
  ) {
    Icon(Icons.Default.WarningAmber, null)
    Text(text, style = MaterialTheme.typography.bodyLarge)
  }
}

@Preview
@Composable
fun AppDetailsWarningsPreview() {
  FDroidContent { Column { AppDetailsWarnings(testApp) } }
}

@Preview
@Composable
private fun KnownVulnPreview() {
  FDroidContent {
    Column {
      AppDetailsWarnings(testApp.copy(issue = KnownVulnerability(true)))
      AppDetailsWarnings(testApp.copy(issue = KnownVulnerability(false)))
    }
  }
}

@Preview
@Composable
private fun IncompatiblePreview() {
  FDroidContent {
    Column {
      AppDetailsWarnings(
        testApp.copy(versions = listOf(testApp.versions!!.first().copy(isCompatible = false)))
      )
    }
  }
}

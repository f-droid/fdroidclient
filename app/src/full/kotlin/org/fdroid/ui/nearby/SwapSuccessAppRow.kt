package org.fdroid.ui.nearby

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.install.InstallState
import org.fdroid.install.InstallStateWithInfo
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.apps.VersionLine
import org.fdroid.ui.utils.AsyncShimmerImage

@Composable
fun SwapSuccessAppRow(
  app: SwapSuccessItem,
  onInstall: (String) -> Unit,
  onCancel: (String) -> Unit,
) {
  ListItem(
    leadingContent = {
      AsyncShimmerImage(
        model = app.iconModel,
        error = painterResource(R.drawable.ic_repo_app_default),
        contentDescription = null,
        modifier = Modifier.size(48.dp),
      )
    },
    headlineContent = { Text(app.name) },
    supportingContent = {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (app.hasUpdate) {
          VersionLine(app.installedVersionName, app.versionName)
        } else {
          Text(app.versionName)
        }
        val errorState = app.installState as? InstallState.Error
        if (errorState?.msg != null) {
          Text(errorState.msg, color = MaterialTheme.colorScheme.error)
        }
      }
    },
    trailingContent = {
      Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        when (val state = app.installState) {
          is InstallState.Installed -> {
            Text(stringResource(R.string.app_installed))
          }
          is InstallState.Error -> {}
          is InstallState.Downloading -> {
            Box(contentAlignment = Alignment.Center) {
              IconButton(onClick = { onCancel(app.packageName) }) {
                Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = stringResource(R.string.cancel),
                )
              }
              CircularProgressIndicator(progress = { state.progress })
            }
          }
          is InstallStateWithInfo -> {
            Box(contentAlignment = Alignment.Center) {
              IconButton(onClick = { onCancel(app.packageName) }) {
                Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = stringResource(R.string.cancel),
                )
              }
              CircularProgressIndicator()
            }
          }
          else -> {
            if (app.isInstalled && !app.hasUpdate) {
              Text(stringResource(R.string.app_installed))
            } else {
              Button(onClick = { onInstall(app.packageName) }) {
                Text(
                  stringResource(
                    if (app.hasUpdate) R.string.menu_upgrade else R.string.menu_install
                  )
                )
              }
            }
          }
        }
      }
    },
  )
}

@Preview
@Composable
private fun SwapSuccessAppRowPreview() {
  val now = System.currentTimeMillis()
  FDroidContent {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      SwapSuccessAppRow(
        app =
          previewSwapSuccessUiItem(
            packageName = "org.example.install",
            name = "Fresh Install",
            versionName = "1.2.0",
            versionCode = 120L,
            installedVersionName = null,
            installedVersionCode = null,
            installState = InstallState.Unknown,
          ),
        onInstall = {},
        onCancel = {},
      )
      SwapSuccessAppRow(
        app =
          previewSwapSuccessUiItem(
            packageName = "org.example.update",
            name = "Update Available",
            versionName = "2.0.0",
            versionCode = 200L,
            installedVersionName = "1.9.0",
            installedVersionCode = 190L,
            installState = InstallState.Unknown,
          ),
        onInstall = {},
        onCancel = {},
      )
      SwapSuccessAppRow(
        app =
          previewSwapSuccessUiItem(
            packageName = "org.example.installed",
            name = "Already Installed",
            versionName = "3.1.0",
            versionCode = 310L,
            installedVersionName = "3.1.0",
            installedVersionCode = 310L,
            installState = InstallState.Unknown,
          ),
        onInstall = {},
        onCancel = {},
      )
      SwapSuccessAppRow(
        app =
          previewSwapSuccessUiItem(
            packageName = "org.example.downloading",
            name = "Downloading",
            versionName = "4.0.0",
            versionCode = 400L,
            installedVersionName = "3.5.0",
            installedVersionCode = 350L,
            installState =
              InstallState.Downloading(
                name = "Downloading",
                versionName = "4.0.0",
                currentVersionName = "3.5.0",
                lastUpdated = now,
                iconModel = null,
                downloadedBytes = 40,
                totalBytes = 100,
                startMillis = now,
              ),
          ),
        onInstall = {},
        onCancel = {},
      )
      SwapSuccessAppRow(
        app =
          previewSwapSuccessUiItem(
            packageName = "org.example.installing",
            name = "Installing",
            versionName = "5.0.0",
            versionCode = 500L,
            installedVersionName = null,
            installedVersionCode = null,
            installState =
              InstallState.Installing(
                name = "Installing",
                versionName = "5.0.0",
                currentVersionName = null,
                lastUpdated = now,
                iconModel = null,
              ),
          ),
        onInstall = {},
        onCancel = {},
      )
      SwapSuccessAppRow(
        app =
          previewSwapSuccessUiItem(
            packageName = "org.example.error",
            name = "Failed Install",
            versionName = "6.0.0",
            versionCode = 600L,
            installedVersionName = "5.8.0",
            installedVersionCode = 580L,
            installState =
              InstallState.Error(
                msg = "Signature mismatch",
                name = "Failed Install",
                versionName = "6.0.0",
                currentVersionName = "5.8.0",
                lastUpdated = now,
                iconModel = null,
              ),
          ),
        onInstall = {},
        onCancel = {},
      )
      SwapSuccessAppRow(
        app =
          previewSwapSuccessUiItem(
            packageName = "org.example.installedstate",
            name = "Installed State",
            versionName = "7.0.0",
            versionCode = 700L,
            installedVersionName = "7.0.0",
            installedVersionCode = 700L,
            installState =
              InstallState.Installed(
                name = "Installed State",
                versionName = "7.0.0",
                currentVersionName = "6.9.0",
                lastUpdated = now,
                iconModel = null,
              ),
          ),
        onInstall = {},
        onCancel = {},
      )
    }
  }
}

@Preview(locale = "fa")
@Composable
private fun SwapSuccessAppRowRtlPreview() {
  FDroidContent {
    SwapSuccessAppRow(
      app =
        previewSwapSuccessUiItem(
          packageName = "org.example.rtl",
          name = "برنامه نمونه",
          versionName = "2.0.0-beta",
          versionCode = 200L,
          installedVersionName = "1.9.0-alpha",
          installedVersionCode = 190L,
          installState = InstallState.Unknown,
        ),
      onInstall = {},
      onCancel = {},
    )
  }
}

private fun previewSwapSuccessUiItem(
  packageName: String,
  name: String,
  versionName: String,
  versionCode: Long,
  installedVersionName: String?,
  installedVersionCode: Long?,
  installState: InstallState,
): SwapSuccessItem =
  SwapSuccessItem(
    packageName = packageName,
    name = name,
    versionName = versionName,
    versionCode = versionCode,
    installedVersionName = installedVersionName,
    installedVersionCode = installedVersionCode,
    iconModel = null,
    installState = installState,
  )


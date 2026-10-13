package org.fdroid.ui.details

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PermDeviceInformation
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType.Companion.PrimaryNotEditable
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.material3.SheetValue.Hidden
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.enterAlwaysScrollBehavior
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Bullet
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.fdroid.R
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.utils.ExpandableSection
import org.fdroid.ui.utils.testApp

@Composable
fun PermissionsSection(
  versions: List<VersionItem>,
  modifier: Modifier = Modifier,
) {
  var showScreen by remember { mutableStateOf(false) }
  val version = versions.find { it.isSuggested } ?: versions.first()
  ExpandableSection(
    icon = rememberVectorPainter(Icons.Default.PermDeviceInformation),
    title = stringResource(R.string.app_details_permissions_section),
    modifier = modifier,
  ) {
    Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)) {
      val sensitivePerms = version.permissions.filter { it.type == PermissionType.SENSITIVE }
      val numSensitive = sensitivePerms.size
      Text(
        text =
          if (numSensitive == 0) {
            stringResource(R.string.app_details_permissions_sensitive_none)
          } else {
            pluralStringResource(
              R.plurals.app_details_permissions_sensitive,
              numSensitive,
              numSensitive,
            )
          }
      )
      if (numSensitive > 0) {
        Text(
          text =
            buildAnnotatedString {
              withBulletList(bullet = Bullet.Default.copy(padding = 8.sp)) {
                sensitivePerms.forEach { perm ->
                  withBulletListItem { append(perm.label ?: perm.name) }
                }
              }
            },
          modifier = Modifier.padding(top = 8.dp),
        )
      }
      Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
        TextButton(onClick = { showScreen = true }) {
          Text(
            text =
              stringResource(
                R.string.app_details_permissions_button,
                version.permissions.size,
              )
          )
          Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
          Icon(imageVector = Icons.AutoMirrored.Default.ArrowForward, contentDescription = null)
        }
      }
    }
  }
  if (showScreen) {
    PermissionsSheet(version = version, versions = versions, onDismiss = { showScreen = false })
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionsSheet(
  version: VersionItem,
  versions: List<VersionItem>,
  initiallyHidden: Boolean = !LocalInspectionMode.current,
  onDismiss: () -> Unit,
) {
  val sheetState =
    rememberBottomSheetState(
      initialValue = if (initiallyHidden) Hidden else Expanded,
      enabledValues = setOf(Hidden, Expanded),
    )
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    dragHandle = {},
    contentWindowInsets = { WindowInsets() },
    properties = ModalBottomSheetProperties(),
  ) {
    var currentVersion by remember { mutableStateOf(version) }
    val scrollBehavior = enterAlwaysScrollBehavior(rememberTopAppBarState())
    Scaffold(
      topBar = {
        TopAppBar(
          navigationIcon = {
            IconButton(onClick = onDismiss) {
              Icon(
                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                contentDescription = stringResource(R.string.back),
              )
            }
          },
          title = {
            Text(
              stringResource(R.string.app_details_permissions_section),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          },
          actions = {
            VersionDropDown(
              currentVersion,
              versions,
              onVersionChanged = { currentVersion = it },
              modifier = Modifier.padding(end = 16.dp),
            )
          },
          scrollBehavior = scrollBehavior,
        )
      },
      modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { paddingValues ->
      LazyColumn(contentPadding = paddingValues) {
        item(contentType = "dropdown") {}
        item(contentType = "warning") {
          ElevatedCard(
            colors =
              CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
              ),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 16.dp),
          ) {
            Row(
              horizontalArrangement = spacedBy(16.dp),
              verticalAlignment = CenterVertically,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
              Icon(Icons.Default.Info, null)
              Text(
                stringResource(R.string.app_details_permissions_warning),
                style = MaterialTheme.typography.bodyLarge,
              )
            }
          }
        }
        items(
          items = currentVersion.permissions.sortedBy { it.type.ordinal },
          contentType = { "permission" },
        ) { permission ->
          PermissionsRow(permission)
        }
      }
    }
  }
}

@Composable
private fun VersionDropDown(
  currentVersion: VersionItem,
  versions: List<VersionItem>,
  onVersionChanged: (VersionItem) -> Unit,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }
  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = it },
    modifier = modifier.minimumInteractiveComponentSize(),
  ) {
    Row(
      modifier =
        Modifier.menuAnchor(PrimaryNotEditable)
          .clickable { expanded = true }
          .border(
            width = 1.dp,
            color = OutlinedTextFieldDefaults.colors().unfocusedIndicatorColor,
            shape = OutlinedTextFieldDefaults.shape,
          )
          .padding(4.dp),
      verticalAlignment = CenterVertically,
    ) {
      Text(
        text = currentVersion.version.versionName,
        overflow = TextOverflow.MiddleEllipsis,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        modifier = Modifier.padding(start = 6.dp),
      )
      Icon(
        imageVector = Icons.Default.ArrowDropDown,
        contentDescription = null,
        modifier = Modifier.padding(start = 2.dp),
      )
    }
    ExposedDropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
    ) {
      versions.forEach { versionItem ->
        DropdownMenuItem(
          text = {
            Text(
              versionItem.version.versionName,
              style = MaterialTheme.typography.labelMedium,
            )
          },
          onClick = {
            onVersionChanged(versionItem)
            expanded = false
          },
        )
      }
    }
  }
}

@Composable
private fun PermissionsRow(permission: Permission) {
  ListItem(
    overlineContent = { Text(text = permission.name, fontFamily = FontFamily.Monospace) },
    supportingContent = {
      Text(
        text = (permission.description ?: "") + "\nType: ${permission.type}",
        style =
          LocalTextStyle.current.copy(
            hyphens = Hyphens.Auto,
            lineBreak = LineBreak.Paragraph.copy(strategy = LineBreak.Strategy.HighQuality),
          ),
      )
    },
  ) {
    val text =
      if (permission.label != null && permission.label != permission.name) {
        permission.label
      } else {
        permission.name.substringAfter("android.permission.")
      }
    Text(text = text)
  }
}

@Composable
@PreviewLightDark
private fun Preview() {
  FDroidContent { PermissionsSection(testApp.versions!!) }
}

@Composable
@PreviewLightDark
private fun PreviewNoSensitivePermissions() {
  FDroidContent { PermissionsSection(testApp.versions!!.subList(1, 3)) }
}

@Composable
@PreviewLightDark
private fun PreviewScreen() {
  FDroidContent {
    Box(modifier = Modifier.fillMaxSize()) {
      PermissionsSheet(testApp.versions!!.first(), testApp.versions, initiallyHidden = false) {}
    }
  }
}

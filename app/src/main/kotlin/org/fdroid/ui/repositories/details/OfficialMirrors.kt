package org.fdroid.ui.repositories.details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.download.Mirror
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.utils.ExpandableSection

@Composable
fun OfficialMirrors(
  mirrors: List<OfficialMirrorItem>,
  setMirrorEnabled: (Mirror, Boolean) -> Unit,
) {
  ExpandableSection(
    icon = rememberVectorPainter(Icons.Default.Public),
    title = stringResource(R.string.repo_official_mirrors),
    modifier = Modifier.padding(horizontal = 16.dp),
  ) {
    Column {
      mirrors.forEach { m ->
        OfficialMirrorRow(
          item = m,
          setMirrorEnabled = { m, enabled -> setMirrorEnabled(m, enabled) },
        )
      }
    }
  }
}

@Composable
private fun OfficialMirrorRow(
  item: OfficialMirrorItem,
  setMirrorEnabled: (Mirror, Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  ListItem(
    leadingContent = { Text(text = item.emoji, modifier = Modifier.width(20.dp)) },
    headlineContent = { Text(item.url) },
    trailingContent = { Switch(checked = item.isEnabled, onCheckedChange = null) },
    colors = ListItemDefaults.colors(MaterialTheme.colorScheme.background),
    modifier =
      modifier.toggleable(
        value = item.isEnabled,
        role = Role.Switch,
        onValueChange = { checked -> setMirrorEnabled(item.mirror, checked) },
      ),
  )
}

@Preview
@Composable
fun OfficialMirrorsPreview() {
  FDroidContent {
    val mirrors =
      listOf(
          OfficialMirrorItem(
            mirror = Mirror(baseUrl = "https://mirror.example.com/fdroid/repo"),
            isEnabled = true,
            isRepoAddress = true,
          ),
          OfficialMirrorItem(
            mirror = Mirror("https://mirror.example.com/foo/bar/fdroid/repo", "de"),
            isEnabled = false,
            isRepoAddress = false,
          ),
          OfficialMirrorItem(
            mirror = Mirror("https://foobar.onion"),
            isEnabled = true,
            isRepoAddress = false,
          ),
          OfficialMirrorItem(
            mirror =
              Mirror("https://mirror.example.org/with/a/very/long/url/that/wraps/repo", "fr"),
            isEnabled = true,
            isRepoAddress = false,
          ),
          OfficialMirrorItem(
            mirror = Mirror("https://mirror.example.net/repo"),
            isEnabled = false,
            isRepoAddress = false,
          ),
        )
        .sorted()
    OfficialMirrors(mirrors = mirrors, setMirrorEnabled = { _, _ -> })
  }
}

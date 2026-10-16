package org.fdroid.ui.lists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CrisisAlert
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter.Companion.tint
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.utils.AsyncShimmerImage
import org.fdroid.ui.utils.getAppListInfo
import org.fdroid.ui.utils.getAppListModel

data class AntiFeatureItem(
  val id: String,
  val name: String,
  val iconModel: Any?,
  val description: String? = null,
)

@Composable
fun AntiFeatureRow(
  antiFeature: AntiFeatureItem,
  filteredAntiFeatureIds: Set<String>,
  actions: AppListActions,
) {
  val isChecked = antiFeature.id !in filteredAntiFeatureIds
  ListItem(
    leadingContent = {
      AsyncShimmerImage(
        model = antiFeature.iconModel,
        colorFilter = tint(MaterialTheme.colorScheme.onSurfaceVariant),
        error = rememberVectorPainter(Icons.Default.CrisisAlert),
        contentDescription = null,
        modifier = Modifier.size(24.dp).semantics { hideFromAccessibility() },
      )
    },
    supportingContent =
      antiFeature.description?.let { description ->
        {
          Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
          )
        }
      },
    trailingContent = {
      Switch(
        checked = isChecked,
        onCheckedChange = { selected ->
          if (selected) actions.removeAntiFeature(antiFeature.id)
          else actions.addAntiFeature(antiFeature.id)
        },
      )
    },
    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    verticalAlignment = Alignment.CenterVertically,
    modifier =
      Modifier.clickable {
        if (isChecked) actions.addAntiFeature(antiFeature.id)
        else actions.removeAntiFeature(antiFeature.id)
      },
  ) {
    Text(antiFeature.name)
  }
}

@Composable
@PreviewLightDark
private fun Preview() {
  FDroidContent {
    AntiFeatureRow(
      antiFeature =
        AntiFeatureItem(
          id = "test",
          name = "Test Anti-Feature",
          iconModel = null,
          description = "This is a test anti-feature for preview purposes.",
        ),
      filteredAntiFeatureIds = emptySet(),
      actions = getAppListInfo(getAppListModel()).actions,
    )
  }
}

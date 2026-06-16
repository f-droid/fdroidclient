package org.fdroid.ui.categories

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewDynamicColors
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.lists.AppListType
import org.fdroid.ui.navigation.NavigationKey
import org.fdroid.ui.utils.ExpandIconArrow

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun CategoryGroupRow(
  group: CategoryGroup,
  categories: List<CategoryItem>,
  onNav: (NavKey) -> Unit,
) {
  val isPreview = LocalInspectionMode.current
  var expanded by rememberSaveable { mutableStateOf(isPreview) }
  val onClick = { expanded = !expanded }
  ListItem(
    leadingContent = {
      Box(
        contentAlignment = Center,
        modifier =
          Modifier.size(40.dp)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.primaryContainer),
      ) {
        Icon(
          imageVector = group.imageVector,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.semantics { hideFromAccessibility() },
        )
      }
    },
    supportingContent = {
      Text(text = stringResource(group.summary), maxLines = 2, overflow = TextOverflow.Ellipsis)
    },
    trailingContent = { IconButton(onClick = onClick) { ExpandIconArrow(expanded) } },
    onClick = onClick,
  ) {
    Text(text = stringResource(group.name), maxLines = 2, overflow = TextOverflow.Ellipsis)
  }
  AnimatedVisibility(
    visible = expanded,
    enter = fadeIn() + expandVertically(),
    exit = fadeOut() + shrinkVertically(),
  ) {
    ChipFlowRow(modifier = Modifier.padding(8.dp)) {
      categories.forEach { categoryItem ->
        CategoryChip(
          categoryItem = categoryItem,
          onClick = {
            val type = AppListType.Category(categoryItem.name, categoryItem.id)
            val navKey = NavigationKey.AppList(type)
            onNav(navKey)
          },
        )
      }
    }
  }
}

@Composable
@PreviewLightDark
private fun Preview() {
  FDroidContent {
    val categories =
      listOf(
        CategoryItem("App Store & Updater", "App Store & Updater"),
        CategoryItem("Browser", "Browser"),
        CategoryItem("Calendar & Agenda", "Calendar & Agenda"),
      )
    Column { CategoryGroupRow(CategoryGroups.productivity, categories) {} }
  }
}

@Composable
@PreviewDynamicColors
private fun PreviewDynamicColors() {
  FDroidContent(dynamicColors = true) {
    val categories =
      listOf(
        CategoryItem("App Store & Updater", "App Store & Updater"),
        CategoryItem("Browser", "Browser"),
        CategoryItem("Calendar & Agenda", "Calendar & Agenda"),
      )
    Column { CategoryGroupRow(CategoryGroups.productivity, categories) {} }
  }
}

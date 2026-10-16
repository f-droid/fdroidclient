package org.fdroid.ui.lists

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CrisisAlert
import androidx.compose.material.icons.filled.PhonelinkErase
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter.Companion.tint
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.database.AppListSortOrder
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.categories.CategoryChip
import org.fdroid.ui.categories.ChipFlowRow
import org.fdroid.ui.categories.chipHeight
import org.fdroid.ui.icons.PackageVariant
import org.fdroid.ui.utils.AsyncShimmerImage
import org.fdroid.ui.utils.ExpandableSection
import org.fdroid.ui.utils.getAppListInfo
import org.fdroid.ui.utils.getAppListModel

@Composable
fun AppsFilter(info: AppListInfo, modifier: Modifier = Modifier) {
  val scrollState = rememberScrollState()
  Column(modifier = modifier.verticalScroll(scrollState)) {
    // Header
    Row(
      horizontalArrangement = spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
      modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Default.Sort,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.semantics { hideFromAccessibility() },
      )
      Text(
        text = stringResource(R.string.sort_title),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.weight(1f),
      )
    }
    ChipFlowRow(modifier = Modifier.padding(start = 16.dp)) {
      // Sort by setting
      val byNameSelected = info.model.sortBy == AppListSortOrder.NAME
      FilterChip(
        selected = byNameSelected,
        modifier = Modifier.height(chipHeight),
        leadingIcon = {
          if (byNameSelected) {
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = stringResource(R.string.filter_selected),
            )
          } else {
            Icon(Icons.Default.SortByAlpha, null)
          }
        },
        label = { Text(stringResource(R.string.sort_by_name)) },
        onClick = { if (!byNameSelected) info.actions.sortBy(AppListSortOrder.NAME) },
      )
      val byLatestSelected = info.model.sortBy == AppListSortOrder.LAST_UPDATED
      FilterChip(
        selected = byLatestSelected,
        modifier = Modifier.height(chipHeight),
        leadingIcon = {
          if (byLatestSelected) {
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = stringResource(R.string.filter_selected),
            )
          } else {
            Icon(Icons.Default.AccessTime, null)
          }
        },
        label = { Text(stringResource(R.string.sort_by_latest)) },
        onClick = { if (!byLatestSelected) info.actions.sortBy(AppListSortOrder.LAST_UPDATED) },
      )
      // Incompatible filter
      FilterChip(
        selected = info.model.filterIncompatible,
        modifier = Modifier.height(chipHeight),
        leadingIcon = {
          if (info.model.filterIncompatible) {
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = stringResource(R.string.filter_selected),
            )
          } else {
            Icon(Icons.Default.PhonelinkErase, null)
          }
        },
        label = { Text(stringResource(R.string.filter_only_compatible)) },
        onClick = info.actions::toggleFilterIncompatible,
      )
    }
    TextButton(
      onClick = info.actions::saveFilters,
      modifier = modifier.align(Alignment.End).padding(horizontal = 16.dp),
    ) {
      Icon(Icons.Default.Save, null)
      Spacer(Modifier.width(ButtonDefaults.IconSpacing))
      Text(text = stringResource(R.string.filter_button_save))
    }
    // Filter apps by section
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    Text(
      text = stringResource(R.string.filter_intro),
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
    // Categories
    val categories = info.model.categories
    if (!categories.isNullOrEmpty())
      ExpandableSection(
        icon = rememberVectorPainter(Icons.Default.Category),
        title = stringResource(R.string.main_menu__categories),
        initiallyExpanded = info.model.filteredCategoryIds.isNotEmpty(),
        iconTint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        collapsedSummary = {
          val selectedCategories =
            categories.filter { it.id in info.model.filteredCategoryIds }.map { it.name }
          if (selectedCategories.isNotEmpty()) {
            Text(
              text = selectedCategories.joinToString(", "),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
            )
          }
        },
      ) {
        ChipFlowRow(modifier = modifier.fillMaxWidth()) {
          categories.forEach { item ->
            val isSelected = item.id in info.model.filteredCategoryIds
            CategoryChip(
              categoryItem = item,
              selected = isSelected,
              onSelected = {
                if (isSelected) {
                  info.actions.removeCategory(item.id)
                } else {
                  info.actions.addCategory(item.id)
                }
              },
            )
          }
        }
      }
    // Repositories
    if (info.model.repositories.isNotEmpty())
      ExpandableSection(
        icon = rememberVectorPainter(PackageVariant),
        title = stringResource(R.string.app_details_repositories),
        initiallyExpanded = info.model.filteredRepositoryIds.isNotEmpty(),
        iconTint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        collapsedSummary = {
          val selectedRepos =
            info.model.repositories
              .filter { it.repoId in info.model.filteredRepositoryIds }
              .map { it.name }
          if (selectedRepos.isNotEmpty()) {
            Text(
              text = selectedRepos.joinToString(", "),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
            )
          }
        },
      ) {
        ChipFlowRow(modifier = modifier.fillMaxWidth()) {
          info.model.repositories.forEach { repo ->
            val selected = repo.repoId in info.model.filteredRepositoryIds
            FilterChip(
              selected = selected,
              modifier = Modifier.height(chipHeight),
              leadingIcon = {
                if (selected) {
                  Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.filter_selected),
                  )
                } else
                  AsyncShimmerImage(
                    model = repo.icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp).semantics { hideFromAccessibility() },
                  )
              },
              label = { Text(repo.name) },
              onClick = {
                if (selected) {
                  info.actions.removeRepository(repo.repoId)
                } else {
                  info.actions.addRepository(repo.repoId)
                }
              },
            )
          }
        }
      }
    // Anti-Features
    val antiFeatures = info.model.antiFeatures
    if (!antiFeatures.isNullOrEmpty())
      ExpandableSection(
        icon = rememberVectorPainter(Icons.Default.WarningAmber),
        title = stringResource(R.string.filter_antifeatures),
        initiallyExpanded = info.model.filteredAntiFeatureIds.isNotEmpty(),
        iconTint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
      ) {
        ChipFlowRow(modifier = modifier.fillMaxWidth()) {
          antiFeatures.forEach { item ->
            val isSelected = item.id in info.model.filteredAntiFeatureIds
            FilterChip(
              selected = isSelected,
              modifier = Modifier.height(chipHeight),
              leadingIcon = {
                if (isSelected) {
                  Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = stringResource(R.string.filter_selected),
                  )
                } else
                  AsyncShimmerImage(
                    model = item.iconModel,
                    colorFilter = tint(MaterialTheme.colorScheme.onSurfaceVariant),
                    error = rememberVectorPainter(Icons.Default.CrisisAlert),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp).semantics { hideFromAccessibility() },
                  )
              },
              label = { Text(item.name) },
              colors =
                FilterChipDefaults.filterChipColors()
                  .copy(selectedContainerColor = MaterialTheme.colorScheme.errorContainer),
              onClick = {
                if (isSelected) {
                  info.actions.removeAntiFeature(item.id)
                } else {
                  info.actions.addAntiFeature(item.id)
                }
              },
            )
          }
        }
      }
    // clear all filters
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    TextButton(
      onClick = info.actions::clearFilters,
      modifier = modifier.align(Alignment.End).padding(horizontal = 16.dp),
    ) {
      Icon(Icons.Default.Clear, null)
      Spacer(Modifier.width(ButtonDefaults.IconSpacing))
      Text(stringResource(R.string.filter_button_clear_all))
    }
  }
}

@Composable
@Preview(device = "spec:width=1080px,height=4000px,dpi=440")
private fun Preview() {
  FDroidContent {
    val info = getAppListInfo(getAppListModel())
    AppsFilter(info)
  }
}

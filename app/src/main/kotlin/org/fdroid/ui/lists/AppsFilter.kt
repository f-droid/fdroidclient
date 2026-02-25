package org.fdroid.ui.lists

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import org.fdroid.ui.categories.CategoryItem
import org.fdroid.ui.categories.ChipFlowRow
import org.fdroid.ui.categories.chipHeight
import org.fdroid.ui.icons.PackageVariant
import org.fdroid.ui.utils.AsyncShimmerImage
import org.fdroid.ui.utils.getAppListInfo
import org.fdroid.ui.utils.repoItems
import kotlin.random.Random

@Composable
fun AppsFilter(
    info: AppListInfo,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    Column(modifier = modifier.verticalScroll(scrollState)) {
        FilterHeader(
            icon = Icons.AutoMirrored.Default.Sort,
            text = stringResource(R.string.sort_title),
        )
        ChipFlowRow(
            modifier = Modifier.padding(start = 16.dp)
        ) {
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
                label = {
                    Text(stringResource(R.string.sort_by_name))
                },
                onClick = {
                    if (!byNameSelected) info.actions.sortBy(AppListSortOrder.NAME)
                },
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
                label = {
                    Text(stringResource(R.string.sort_by_latest))
                },
                onClick = {
                    if (!byLatestSelected) info.actions.sortBy(AppListSortOrder.LAST_UPDATED)
                },
            )
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
                label = {
                    Text(stringResource(R.string.filter_only_compatible))
                },
                onClick = info.actions::toggleFilterIncompatible,
            )
        }
        TextButton(
            onClick = info.actions::saveFilters,
            modifier = modifier
                .align(Alignment.End)
                .padding(horizontal = 16.dp),
        ) {
            Icon(Icons.Default.Save, null)
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text(
                text = stringResource(R.string.filter_button_save)
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text(
            text = stringResource(R.string.filter_intro),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        // Categories
        val categories = info.model.categories
        if (!categories.isNullOrEmpty()) FilterSection(
            icon = Icons.Default.Category,
            title = stringResource(R.string.main_menu__categories),
            initiallyExpanded = info.model.filteredCategoryIds.isNotEmpty(),
            onCollapsed = {
                info.model.filteredCategoryIds.forEach {
                    info.actions.removeCategory(it)
                }
            },
        ) {
            categories.forEach { item ->
                val isSelected = item.id in info.model.filteredCategoryIds
                CategoryChip(categoryItem = item, selected = isSelected, onSelected = {
                    if (isSelected) {
                        info.actions.removeCategory(item.id)
                    } else {
                        info.actions.addCategory(item.id)
                    }
                })
            }
        }
        // Repositories
        if (info.model.repositories.isNotEmpty()) FilterSection(
            icon = PackageVariant,
            title = stringResource(R.string.app_details_repositories),
            initiallyExpanded = info.model.filteredRepositoryIds.isNotEmpty(),
            onCollapsed = {
                info.model.filteredRepositoryIds.forEach {
                    info.actions.removeRepository(it)
                }
            },
        ) {
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
                        } else AsyncShimmerImage(
                            model = repo.icon,
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .semantics { hideFromAccessibility() },
                        )
                    },
                    label = {
                        Text(repo.name)
                    },
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
        // Anti-Features
        val antiFeatures = info.model.antiFeatures
        if (!antiFeatures.isNullOrEmpty()) FilterSection(
            icon = Icons.Default.WarningAmber,
            title = stringResource(R.string.filter_antifeatures),
            initiallyExpanded = info.model.filteredAntiFeatureIds.isNotEmpty(),
            onCollapsed = {
                info.model.filteredAntiFeatureIds.forEach {
                    info.actions.removeAntiFeature(it)
                }
            },
        ) {
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
                        } else AsyncShimmerImage(
                            model = item.iconModel,
                            error = rememberVectorPainter(Icons.Default.CrisisAlert),
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .semantics { hideFromAccessibility() },
                        )
                    },
                    label = {
                        Text(item.name)
                    },
                    colors = FilterChipDefaults.filterChipColors()
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
        // clear all filters
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        TextButton(
            onClick = info.actions::clearFilters,
            modifier = modifier
                .align(Alignment.End)
                .padding(horizontal = 16.dp),
        ) {
            Icon(Icons.Default.Clear, null)
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.filter_button_clear_all))
        }
    }
}

@Composable
@Preview
private fun Preview() {
    FDroidContent {
        val model = AppListModel(
            apps = listOf(
                AppListItem(1, "1", "This is app 1", "It has summary 2", 0, false, true),
                AppListItem(2, "2", "This is app 2", "It has summary 2", 0, true, true),
            ),
            showFilterBadge = true,
            sortBy = AppListSortOrder.NAME,
            filterIncompatible = Random.nextBoolean(),
            categories = listOf(
                CategoryItem("App Store & Updater", "App Store & Updater"),
                CategoryItem("Browser", "Browser"),
                CategoryItem("Calendar & Agenda", "Calendar & Agenda"),
                CategoryItem("Cloud Storage & File Sync", "Cloud Storage & File Sync"),
                CategoryItem("Connectivity", "Connectivity"),
                CategoryItem("Development", "Development"),
                CategoryItem("doesn't exist", "Foo bar"),
            ),
            filteredCategoryIds = setOf("Browser"),
            antiFeatures = listOf(
                AntiFeatureItem("foo1", "bar1", null),
                AntiFeatureItem("foo2", "bar2", null),
                AntiFeatureItem("foo3", "bar3", null),
            ),
            filteredAntiFeatureIds = setOf("foo2"),
            repositories = repoItems,
            filteredRepositoryIds = setOf(2),
        )
        val info = getAppListInfo(model)
        AppsFilter(info)
    }
}

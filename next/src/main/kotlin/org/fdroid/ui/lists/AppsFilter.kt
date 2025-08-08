package org.fdroid.ui.lists

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.database.AppListSortOrder
import org.fdroid.fdroid.ui.theme.FDroidContent
import org.fdroid.next.R
import org.fdroid.ui.categories.CategoryCard
import org.fdroid.ui.categories.CategoryItem
import org.fdroid.ui.icons.PackageVariant
import org.fdroid.ui.repositories.RepositoryItem
import org.fdroid.ui.utils.AsyncShimmerImage

interface AppListInfo {
    val model: AppListModel

    fun toggleFilterVisibility()
    fun sortBy(sort: AppListSortOrder)
    fun addCategory(categoryId: String)
    fun removeCategory(categoryId: String)
    fun addRepository(repoId: Long)
    fun removeRepository(repoId: Long)
    fun onSearch(query: String)
}

@Composable
fun AppsFilter(
    info: AppListInfo,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.verticalScroll(scrollState)) {
        FilterHeader(Icons.AutoMirrored.Default.Sort, "Sorting")
        FlowRow(
            horizontalArrangement = spacedBy(8.dp),
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            val byNameSelected = info.model.sortBy == AppListSortOrder.NAME
            FilterChip(
                selected = byNameSelected,
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
                    if (!byNameSelected) info.sortBy(AppListSortOrder.NAME)
                },
            )
            val byLatestSelected = info.model.sortBy == AppListSortOrder.LAST_UPDATED
            FilterChip(
                selected = byLatestSelected,
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
                    if (!byLatestSelected) info.sortBy(AppListSortOrder.LAST_UPDATED)
                },
            )

        }
        val categories = info.model.categories
        if (categories != null) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            FilterHeader(Icons.Default.Category, "Categories")
            FlowRow(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                categories.forEach { item ->
                    val isSelected = item.id in info.model.filteredCategoryIds
                    CategoryCard(item, selected = isSelected, onSelected = {
                        if (isSelected) {
                            info.removeCategory(item.id)
                        } else {
                            info.addCategory(item.id)
                        }
                    })
                }
            }
        }
        if (info.model.repositories.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            FilterHeader(PackageVariant, "Repositories")
            FlowRow(
                horizontalArrangement = spacedBy(8.dp),
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                info.model.repositories.forEach { repo ->
                    val selected = repo.repoId in info.model.filteredRepositoryIds
                    FilterChip(
                        selected = selected,
                        leadingIcon = {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = stringResource(R.string.filter_selected),
                                )
                            } else AsyncShimmerImage(
                                model = repo.icon,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(repo.name)
                        },
                        onClick = {
                            if (selected) {
                                info.removeRepository(repo.repoId)
                            } else {
                                info.addRepository(repo.repoId)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterHeader(icon: ImageVector, text: String) {
    Row(
        horizontalArrangement = spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
@Preview
private fun Preview() {
    FDroidContent {
        val model = AppListModel(
            list = AppListType.New("New"),
            apps = listOf(
                AppListItem(1, "1", "This is app 1", "It has summary 2", 0, null),
                AppListItem(2, "2", "This is app 2", "It has summary 2", 0, null),
            ),
            areFiltersShown = true,
            sortBy = AppListSortOrder.NAME,
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
            repositories = listOf(
                RepositoryItem(
                    repoId = 1,
                    address = "http://example.org",
                    name = "F-Droid",
                    icon = null,
                    timestamp = 42,
                    lastUpdated = null,
                    weight = 1,
                    enabled = true,
                ),
                RepositoryItem(
                    repoId = 2,
                    address = "http://example.org",
                    name = "Guardian Project Repository",
                    icon = null,
                    timestamp = 42,
                    lastUpdated = null,
                    weight = 2,
                    enabled = true,
                ),
            ),
            filteredRepositoryIds = setOf(2),
        )
        val info = object : AppListInfo {
            override val model: AppListModel = model
            override fun toggleFilterVisibility() {}
            override fun sortBy(sort: AppListSortOrder) {}
            override fun addCategory(categoryId: String) {}
            override fun removeCategory(categoryId: String) {}
            override fun addRepository(repoId: Long) {}
            override fun removeRepository(repoId: Long) {}
            override fun onSearch(query: String) {}
        }
        AppsFilter(info)
    }
}

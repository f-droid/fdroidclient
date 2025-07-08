package org.fdroid.basic.ui.main.lists

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.fdroid.basic.ui.main.discover.FilterModel
import org.fdroid.basic.ui.main.discover.Sort

interface FilterInfo {
    val model: FilterModel

    fun toggleFilterVisibility()
    fun sortBy(sort: Sort)
    fun addCategory(category: String)
    fun removeCategory(category: String)
}

@Composable
fun ColumnScope.AppsFilter(
    filter: FilterInfo,
    addedRepos: MutableList<String>,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(filter.model.areFiltersShown) {
        FlowRow(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = spacedBy(16.dp),
        ) {
            var sortByMenuExpanded by remember { mutableStateOf(false) }
            var repoMenuExpanded by remember { mutableStateOf(false) }
            var categoryMenuExpanded by remember { mutableStateOf(false) }
            FilterChip(
                selected = false,
                leadingIcon = {
                    val vector = when (filter.model.sortBy) {
                        Sort.NAME -> Icons.Filled.SortByAlpha
                        Sort.LATEST -> Icons.Filled.AccessTime
                    }
                    Icon(vector, null, modifier = Modifier.size(FilterChipDefaults.IconSize))
                },
                trailingIcon = {
                    Icon(Icons.Filled.ArrowDropDown, null)
                },
                label = {
                    val s = when (filter.model.sortBy) {
                        Sort.NAME -> "Sort by name"
                        Sort.LATEST -> "Sort by latest"
                    }
                    Text(s)
                    DropdownMenu(
                        expanded = sortByMenuExpanded,
                        onDismissRequest = { sortByMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sort by name") },
                            leadingIcon = {
                                Icon(Icons.Filled.SortByAlpha, null)
                            },
                            onClick = {
                                filter.sortBy(Sort.NAME)
                                sortByMenuExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Sort by latest") },
                            leadingIcon = {
                                Icon(Icons.Filled.AccessTime, null)
                            },
                            onClick = {
                                filter.sortBy(Sort.LATEST)
                                sortByMenuExpanded = false
                            },
                        )
                    }
                },
                onClick = { sortByMenuExpanded = !sortByMenuExpanded },
            )
            if (!filter.model.allCategories.isNullOrEmpty()) FilterChip(
                selected = false,
                leadingIcon = {
                    Icon(
                        Icons.Filled.Add,
                        null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                    )
                },
                label = {
                    Text("Category")
                    DropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = { categoryMenuExpanded = false },
                    ) {
                        filter.model.allCategories?.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    filter.addCategory(category.id)
                                    categoryMenuExpanded = false
                                },
                            )
                        }
                    }
                },
                onClick = { categoryMenuExpanded = !categoryMenuExpanded },
            )
            FilterChip(
                selected = false,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                    )
                },
                label = {
                    Text("Repository")
                    DropdownMenu(
                        expanded = repoMenuExpanded,
                        onDismissRequest = { repoMenuExpanded = false },
                    ) {
                        val repos = listOf(
                            "F-Droid",
                            "Guardian Project",
                            "IzzyOnDroid",
                        )
                        repos.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    addedRepos.add(category)
                                    repoMenuExpanded = false
                                },
                            )
                        }
                    }
                },
                onClick = { repoMenuExpanded = !repoMenuExpanded },
            )
            filter.model.addedCategories.forEach { category ->
                FilterChip(
                    selected = true,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                        )
                    },
                    label = { Text(category) },
                    onClick = { filter.removeCategory(category) }
                )
            }
            addedRepos.forEach { repo ->
                FilterChip(
                    selected = true,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                        )
                    },
                    label = { Text(repo) },
                    onClick = { addedRepos.remove(repo) }
                )
            }
        }
    }
}

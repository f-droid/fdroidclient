package org.fdroid.basic.ui.main.apps

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.fdroid.basic.ui.main.NUM_ITEMS
import org.fdroid.basic.ui.main.Sort

@Composable
fun AppList(
    onlyInstalledApps: Boolean,
    sortBy: Sort,
    addedCategories: List<String>,
    categories: List<String>,
    currentItem: AppNavigationItem?,
    onItemClick: (AppNavigationItem) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.then(
            if (currentItem == null) Modifier
            else Modifier.selectableGroup()
        ),
    ) {
        repeat(NUM_ITEMS) { idx ->
            if (onlyInstalledApps && idx % 2 > 0) return@repeat
            val i = if (sortBy == Sort.NAME) idx else NUM_ITEMS - idx
            val category = categories.getOrElse(i) { categories.random() }
            if (addedCategories.isNotEmpty() && category !in addedCategories) return@repeat
            item {
                val navItem = AppNavigationItem(
                    packageName = "$i",
                    name = "App $i",
                    summary = "Summary of the app • $category",
                )
                val isSelected = currentItem?.packageName == navItem.packageName
                val interactionModifier = if (currentItem == null) {
                    Modifier.clickable(
                        onClick = { onItemClick(navItem) }
                    )
                } else {
                    Modifier.selectable(
                        selected = isSelected,
                        onClick = { onItemClick(navItem) }
                    )
                }
                ListItem(
                    headlineContent = { Text(navItem.name) },
                    supportingContent = { Text(navItem.summary) },
                    leadingContent = {
                        BadgedBox(badge = {
                            if (i <= 3) Icon(
                                imageVector = Icons.Filled.NewReleases,
                                tint = MaterialTheme.colorScheme.primary,
                                contentDescription = null, modifier = Modifier.size(24.dp),
                            )
                        }) {
                            Icon(
                                Icons.Filled.Android,
                                tint = MaterialTheme.colorScheme.secondary,
                                contentDescription = null,
                            )
                        }
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            Color.Transparent
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 8.dp,
                            vertical = 4.dp
                        )
                        .then(interactionModifier)
                )
            }
        }
        item {
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
        }
    }
}

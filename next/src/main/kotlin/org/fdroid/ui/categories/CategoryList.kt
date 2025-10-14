package org.fdroid.ui.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import org.fdroid.R
import org.fdroid.fdroid.ui.theme.FDroidContent
import org.fdroid.ui.NavigationKey
import org.fdroid.ui.lists.AppListType

@Composable
fun CategoryList(
    categoryMap: Map<CategoryGroup, List<CategoryItem>>?,
    onNav: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    if (categoryMap != null) Column(
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.main_menu__categories),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
        )
        // we'll sort the groups here, because before we didn't have the context to get names
        val context = LocalContext.current
        val sortedMap = remember(categoryMap) {
            val comparator = compareBy<CategoryGroup> { context.getString(it.name) }
            categoryMap.toSortedMap(comparator)
        }
        sortedMap.forEach { (group, categories) ->
            Text(
                text = stringResource(group.name),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(4.dp),
            )
            FlowRow(
                horizontalArrangement = Arrangement.Start,
            ) {
                categories.forEach { category ->
                    CategoryChip(category, {
                        val type = AppListType.Category(category.name, category.id)
                        val navKey = NavigationKey.AppList(type)
                        onNav(navKey)
                    })
                }
            }
        }
    }
}

@Preview
@Composable
fun CategoryListPreview() {
    FDroidContent {
        val categories = mapOf(
            CategoryGroups.productivity to listOf(
                CategoryItem("App Store & Updater", "App Store & Updater"),
                CategoryItem("Browser", "Browser"),
                CategoryItem("Calendar & Agenda", "Calendar & Agenda"),
            ),
            CategoryGroups.media to listOf(
                CategoryItem("Cloud Storage & File Sync", "Cloud Storage & File Sync"),
                CategoryItem("Connectivity", "Connectivity"),
                CategoryItem("Development", "Development"),
                CategoryItem("doesn't exist", "Foo bar"),
            )
        )
        CategoryList(categories, {})
    }
}

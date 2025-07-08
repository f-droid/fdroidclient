package org.fdroid.basic.ui.categories

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.basic.R
import org.fdroid.fdroid.ui.theme.FDroidContent
import kotlin.math.max

@Composable
fun CategoryList(categories: List<Category>?, modifier: Modifier = Modifier) {
    if (categories != null) Column {
        val state = rememberLazyGridState()
        LazyVerticalGrid(
            state = state,
            columns = GridCells.Adaptive(150.dp),
            contentPadding = PaddingValues(12.dp),
            modifier = modifier,
        ) {
            item(key = "header", span = {
                GridItemSpan(max(1, state.layoutInfo.maxSpan))
            }) {
                Text(
                    text = stringResource(R.string.main_menu__categories),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
                )
            }
            items(categories, key = { it.name }) { category ->
                CategoryCard(category)
            }
        }
    }
}

@Preview
@Composable
fun CategoryListPreview() {
    FDroidContent {
        val categories = listOf(
            Category("App Store & Updater", "App Store & Updater"),
            Category("Browser", "Browser"),
            Category("Calendar & Agenda", "Calendar & Agenda"),
            Category("Cloud Storage & File Sync", "Cloud Storage & File Sync"),
            Category("Connectivity", "Connectivity"),
            Category("Development", "Development"),
            Category("doesn't exist", "Foo bar"),
        )
        CategoryList(categories)
    }
}

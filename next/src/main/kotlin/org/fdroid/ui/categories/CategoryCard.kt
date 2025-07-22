package org.fdroid.ui.categories

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.ui.NavigationKey
import org.fdroid.ui.lists.AppListType
import org.fdroid.fdroid.ui.theme.FDroidContent

@Composable
fun CategoryCard(
    categoryItem: CategoryItem,
    onNav: (NavigationKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    AssistChip(
        onClick = {
            val type = AppListType.Category(categoryItem.name, categoryItem.id)
            val navKey = NavigationKey.AppList(type)
            onNav(navKey)
        },
        leadingIcon = {
            Image(
                imageVector = categoryItem.imageVector,
                contentDescription = null,
                contentScale = ContentScale.None,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                modifier = Modifier.padding(end = 8.dp),
            )
        },
        label = {
            Text(
                categoryItem.name,
                maxLines = 2,
                style = MaterialTheme.typography.bodySmall,
                overflow = TextOverflow.Ellipsis,
            )
        },
        modifier = modifier.padding(horizontal = 4.dp)
    )
}

@Preview
@Composable
fun CategoryCardPreview() {
    FDroidContent {
        CategoryCard(CategoryItem("VPN & Proxy", "VPN & Proxy"), {})
    }
}

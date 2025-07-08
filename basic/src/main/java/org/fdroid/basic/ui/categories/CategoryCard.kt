package org.fdroid.basic.ui.categories

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
import org.fdroid.fdroid.ui.theme.FDroidContent

@Composable
fun CategoryCard(category: Category) {
    AssistChip(
        onClick = {},
        leadingIcon = {
            Image(
                imageVector = category.imageVector,
                contentDescription = null,
                contentScale = ContentScale.None,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                modifier = Modifier.padding(end = 8.dp),
            )
        },
        label = {
            Text(
                category.name,
                maxLines = 2,
                style = MaterialTheme.typography.bodySmall,
                overflow = TextOverflow.Ellipsis,
            )
        },
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Preview
@Composable
fun CategoryCardPreview() {
    FDroidContent {
        CategoryCard(Category("VPN & Proxy", "VPN & Proxy"))
    }
}

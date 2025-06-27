package org.fdroid.basic.ui.main.discover

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons.Default
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.basic.Category
import org.fdroid.fdroid.ui.theme.FDroidContent

@Composable
fun CategoryCard(category: Category) {
    Card(modifier = Modifier.padding(8.dp).height(80.dp)) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp).fillMaxHeight(),
        ) {
            Text(
                category.name,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Image(
                imageVector = category.imageVector,
                contentDescription = null,
                contentScale = ContentScale.None,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                modifier = Modifier.padding(start = 16.dp),
            )
        }
    }
}

@Preview
@Composable
fun CategoryCardPreview() {
    FDroidContent {
        CategoryCard(Category("VPN & Proxy", Default.VpnLock))
    }
}

package org.fdroid.ui.discover

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.utils.AsyncShimmerImage
import org.fdroid.ui.utils.InstalledBadge
import org.fdroid.ui.utils.Names

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppCarousel(
    title: String,
    apps: List<AppDiscoverItem>,
    modifier: Modifier = Modifier,
    onTitleTap: () -> Unit,
    onAppTap: (AppDiscoverItem) -> Unit,
) {
    Column(
        verticalArrangement = spacedBy(8.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onTitleTap)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                modifier = Modifier.semantics { hideFromAccessibility() },
                contentDescription = null,
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = spacedBy(16.dp),
        ) {
            items(apps, key = { it.packageName }) { app ->
                AppBox(app, onAppTap)
            }
        }
    }
}

@Composable
fun AppBox(app: AppDiscoverItem, onAppTap: (AppDiscoverItem) -> Unit) {
    Column(
        verticalArrangement = spacedBy(8.dp),
        modifier = Modifier
            .width(80.dp)
            .clickable { onAppTap(app) },
    ) {
        BadgedBox(badge = { if (app.isInstalled) InstalledBadge() }) {
            AsyncShimmerImage(
                model = app.imageModel,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(76.dp)
                    .clip(MaterialTheme.shapes.large)
                    .semantics { hideFromAccessibility() },
            )
        }
        Text(
            text = app.name,
            style = MaterialTheme.typography.bodySmall,
            minLines = 2,
            maxLines = 2,
        )
    }
}

@Preview
@Composable
fun AppCarouselPreview() {
    val apps = listOf(
        AppDiscoverItem("1", Names.randomName, false),
        AppDiscoverItem("2", Names.randomName, true),
        AppDiscoverItem("3", Names.randomName, false),
        AppDiscoverItem("4", Names.randomName, false),
        AppDiscoverItem("5", Names.randomName, false),
    )
    FDroidContent {
        AppCarousel("Preview Apps", apps, onTitleTap = {}) {}
    }
}

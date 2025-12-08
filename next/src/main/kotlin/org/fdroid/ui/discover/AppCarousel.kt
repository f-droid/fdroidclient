package org.fdroid.ui.discover

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.fdroid.ui.theme.FDroidContent
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
    val carouselState = rememberCarouselState { apps.size }
    Column(modifier = modifier) {
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
        HorizontalUncontainedCarousel(
            state = carouselState,
            itemWidth = 80.dp,
            itemSpacing = 8.dp,
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) { index ->
            AppBox(apps[index], onAppTap)
        }
    }
}

@Composable
fun AppBox(app: AppDiscoverItem, onAppTap: (AppDiscoverItem) -> Unit) {
    Column(
        verticalArrangement = spacedBy(8.dp),
        modifier = Modifier
            .padding(8.dp)
            .clickable { onAppTap(app) },
    ) {
        BadgedBox(badge = { if (app.isInstalled) InstalledBadge() }) {
            AsyncShimmerImage(
                model = app.imageModel,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .requiredSize(76.dp)
                    .clip(MaterialTheme.shapes.medium)
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
        AppDiscoverItem("", Names.randomName, false),
        AppDiscoverItem("", Names.randomName, true),
        AppDiscoverItem("", Names.randomName, false),
        AppDiscoverItem("", Names.randomName, false),
        AppDiscoverItem("", Names.randomName, false),
    )
    FDroidContent {
        AppCarousel("Preview Apps", apps, onTitleTap = {}) {}
    }
}

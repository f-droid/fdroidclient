package org.fdroid.basic.ui.main.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Android
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.fdroid.basic.ui.Names
import org.fdroid.fdroid.ui.theme.FDroidContent

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
        app.iconDownloadRequest?.let {
            AsyncImage( // TODO error handling
                app.iconDownloadRequest,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .requiredSize(76.dp)
                    .clip(MaterialTheme.shapes.medium),
            )
        } ?: Icon(
            Icons.Filled.Android,
            tint = MaterialTheme.colorScheme.secondary,
            contentDescription = null,
            modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .size(76.dp)
                .background(Color.White)
                .padding(8.dp),
        )
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
        AppDiscoverItem("", Names.randomName, isNew = true),
        AppDiscoverItem("", Names.randomName, isNew = false),
        AppDiscoverItem("", Names.randomName, isNew = false),
        AppDiscoverItem("", Names.randomName, isNew = false),
        AppDiscoverItem("", Names.randomName, isNew = false),
    )
    FDroidContent {
        AppCarousel("Preview Apps", apps, onTitleTap = {}) {}
    }
}

package org.fdroid.basic.ui.main.details

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter.Companion.tint
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.fdroid.basic.R
import org.fdroid.basic.ui.Names.names
import org.fdroid.basic.ui.asRelativeTimeString
import org.fdroid.basic.ui.main.apps.MinimalApp
import org.fdroid.basic.ui.main.discover.AppNavigationItem
import org.fdroid.fdroid.ui.theme.FDroidContent

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun AppDetails(
    appItem: MinimalApp?,
    modifier: Modifier = Modifier,
) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)
    if (appItem == null) CircularProgressIndicator()
    else Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
                title = {
                    if (topAppBarState.overlappedFraction == 1f) {
                        Text(appItem.name ?: "unknown app")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { /* do something */ }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Localized description"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* do something */ }) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Localized description"
                        )
                    }
                    IconButton(onClick = { /* do something */ }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Localized description"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        val item = newPipe
        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth(),
        ) {
            item.featureGraphic?.let { featureGraphic ->
                AsyncImage(
                    model = featureGraphic.name,
                    contentDescription = "",
                    contentScale = ContentScale.FillWidth,
                    onError = {
                        // TODO Spacer(modifier = Modifier.padding(innerPadding))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 196.dp)
                        .padding(bottom = 8.dp),
                    error = rememberVectorPainter(Icons.Default.Error),
                )
            } ?: Spacer(modifier = Modifier.padding(innerPadding))
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp),
                horizontalArrangement = spacedBy(8.dp),
            ) {
                item.icon?.let { icon ->
                    AsyncImage(
                        model = icon.name,
                        contentDescription = "",
                        placeholder = rememberVectorPainter(Icons.Default.Image),
                        error = rememberVectorPainter(Icons.Default.Error),
                        modifier = Modifier.size(64.dp),
                    )
                }
                Column {
                    Text(
                        item.app.name ?: "Unknown app",
                        style = MaterialTheme.typography.headlineMediumEmphasized
                    )
                    item.app.metadata.authorName?.let { authorName ->
                        Text(
                            text = "By $authorName",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    val lastUpdated = item.app.metadata.lastUpdated.asRelativeTimeString()
                    Text(
                        text = "Last updated: $lastUpdated (11.91 MB)",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            item.app.summary?.let { summary ->
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyLargeEmphasized,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            Row(
                horizontalArrangement = spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Button(onClick = {}, modifier = Modifier.weight(1f)) {
                    Text("Install")
                }
            }
            item.whatsNew?.let { whatsNew ->
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = "What's new",
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    Text(
                        text = whatsNew,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
            item.description?.let { description ->
                Text(
                    text = description,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            val carouselState = rememberCarouselState { item.phoneScreenshots.size }
            HorizontalUncontainedCarousel(
                state = carouselState,
                itemWidth = 135.dp,
                itemSpacing = 0.dp,
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .padding(vertical = 8.dp)
            ) { index ->
                names
                AsyncImage(
                    model = item.phoneScreenshots[index].name,
                    contentDescription = "",
                    placeholder = rememberVectorPainter(Icons.Default.Image),
                    error = rememberVectorPainter(Icons.Default.Error),
                    modifier = Modifier.widthIn(min = 135.dp)
                )
            }
            // Anti-Features
            Text(
                text = stringResource(R.string.antifeatures),
                style = MaterialTheme.typography.titleMediumEmphasized,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            item.antiFeatures?.forEach { antiFeature ->
                ListItem(
                    leadingContent = {
                        antiFeature.icon?.let { icon ->
                            AsyncImage(
                                model = icon.name,
                                contentDescription = "",
                                colorFilter = tint(MaterialTheme.colorScheme.onBackground),
                                error = rememberVectorPainter(Icons.Filled.Error),
                                modifier = Modifier.size(48.dp),
                            )
                        }
                    },
                    headlineContent = {
                        Text(
                            text = antiFeature.name,
                            style = MaterialTheme.typography.bodyMediumEmphasized,
                        )
                    },
                    supportingContent = {
                        antiFeature.reason?.let {
                            Text(
                                text = antiFeature.reason,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
    }
}

@Preview
@Composable
fun AppDetailsPreview() {
    FDroidContent {
        val item = AppNavigationItem(
            packageName = "foo",
            name = "bar",
            summary = "This is a nice app!",
            isNew = false,
        )
        AppDetails(item)
    }
}

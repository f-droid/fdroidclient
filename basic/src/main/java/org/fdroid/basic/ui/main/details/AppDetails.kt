package org.fdroid.basic.ui.main.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter.Companion.tint
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign.Companion.Center
import androidx.compose.ui.text.style.TextOverflow
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
@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalSharedTransitionApi::class
)
fun AppDetails(
    appItem: MinimalApp?,
    onBackNav: (() -> Unit)?,
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
                    if (onBackNav != null) IconButton(onClick = onBackNav) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Localized description",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* do something */ }) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Localized description",
                        )
                    }
                    var expanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Localized description",
                        )
                    }
                    AppDetailsMenu(expanded) { expanded = false }
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
                        .graphicsLayer { alpha = 0.5f }
                        .drawWithContent {
                            val colors = listOf(
                                Color.Black,
                                Color.Transparent
                            )
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(colors),
                                blendMode = BlendMode.DstIn
                            )
                        }
                        .padding(bottom = 8.dp),
                    error = rememberVectorPainter(Icons.Default.Error),
                )
            } ?: Spacer(modifier = Modifier.padding(innerPadding))
            // Header
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
            // Summary
            item.app.summary?.let { summary ->
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyLargeEmphasized,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            // Repo Chooser
            RepoChooser(
                repos = listOf("F-Droid", "Guardian Project"),
                currentRepoId = 0L,
                preferredRepoId = 0L,
                onRepoChanged = {},
                onPreferredRepoChanged = {},
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            // Main Buttons
            Row(
                horizontalArrangement = spacedBy(8.dp, CenterHorizontally),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                OutlinedButton(onClick = {}, modifier = Modifier.weight(1f)) {
                    Text("Open")
                }
                Button(onClick = {}, modifier = Modifier.weight(1f)) {
                    Text("Update")
                }
            }
            // Warnings
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Column {
                    Row(
                        horizontalArrangement = spacedBy(8.dp),
                        verticalAlignment = CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Icon(Icons.Default.WarningAmber, "")
                        Text(
                            "Can not update this app, because no compatible versions available in repository.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    HorizontalDivider()
                    Row(
                        horizontalArrangement = spacedBy(8.dp),
                        verticalAlignment = CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Icon(Icons.Default.WarningAmber, "")
                        Text(
                            "Auto-update not available, because app targets old version of Android.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
            // What's New
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
            // Description
            var descriptionExpanded by remember { mutableStateOf(false) }
            item.description?.let { description ->
                AnimatedVisibility(descriptionExpanded) {
                    Text(
                        text = description,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(top = 8.dp),
                    )
                }
                AnimatedVisibility(!descriptionExpanded) {
                    Text(
                        text = description,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(top = 8.dp),
                    )
                }
                TextButton(onClick = { descriptionExpanded = !descriptionExpanded }) {
                    Text(
                        text = if (descriptionExpanded) "Less" else "More",
                        textAlign = Center,
                        maxLines = if (descriptionExpanded) Int.MAX_VALUE else 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp),
                    )
                }
            }
            // Anti-features
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                ExpandableSection(
                    icon = rememberVectorPainter(Icons.Default.WarningAmber),
                    title = "This app has anti-features",
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    item.antiFeatures?.forEach { antiFeature ->
                        ListItem(
                            leadingContent = {
                                antiFeature.icon?.let { icon ->
                                    AsyncImage(
                                        model = icon.name,
                                        contentDescription = "",
                                        colorFilter = tint(MaterialTheme.colorScheme.onBackground),
                                        error = rememberVectorPainter(Icons.Filled.Error),
                                        modifier = Modifier.size(32.dp),
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
            // Screenshots
            val carouselState = rememberCarouselState { item.phoneScreenshots.size }
            HorizontalUncontainedCarousel(
                state = carouselState,
                itemWidth = 120.dp,
                itemSpacing = 4.dp,
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
                    contentScale = ContentScale.Fit,
                    placeholder = rememberVectorPainter(Icons.Default.Image),
                    error = rememberVectorPainter(Icons.Default.Error),
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.medium),
                )
            }
            // Donate card
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "Donate",
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                item.app.metadata.donate?.forEach { donation ->
                    AppDetailsLink(
                        icon = Icons.Default.Link,
                        title = donation,
                        url = donation,
                        modifier = modifier.padding(horizontal = 16.dp),
                    )
                }
                item.app.metadata.liberapay?.let { liberapay ->
                    AppDetailsLink(
                        icon = Icons.Default.ChangeHistory,
                        title = "LiberaPay",
                        url = "https://liberapay.com/$liberapay",
                        modifier = modifier.padding(horizontal = 16.dp),
                    )
                }
            }
            // Description (expandable section)
            var sectionExpanded by rememberSaveable { mutableStateOf(false) }
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    verticalAlignment = CenterVertically,
                    horizontalArrangement = spacedBy(8.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clickable(onClick = { sectionExpanded = !sectionExpanded })
                ) {
                    Icon(
                        Icons.AutoMirrored.Default.Notes,
                        contentDescription = null,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        AnimatedVisibility(!sectionExpanded) {
                            Text(
                                text = item.description ?: "nope",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    IconButton(onClick = { sectionExpanded = !sectionExpanded }) {
                        Icon(
                            imageVector = if (sectionExpanded) {
                                Icons.Default.KeyboardArrowUp
                            } else {
                                Icons.Default.KeyboardArrowDown
                            },
                            contentDescription = null,
                        )
                    }
                }
                AnimatedVisibility(sectionExpanded) {
                    Text(item.description ?: "nope")
                }
            }
            // Links
            ExpandableSection(
                icon = rememberVectorPainter(Icons.Default.Link),
                title = stringResource(R.string.links),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    item.app.metadata.webSite?.let { webSite ->
                        AppDetailsLink(
                            icon = Icons.Default.Home,
                            title = stringResource(R.string.menu_website),
                            url = webSite,
                        )
                    }
                    item.app.metadata.issueTracker?.let { issueTracker ->
                        AppDetailsLink(
                            icon = Icons.Default.EditNote,
                            title = stringResource(R.string.menu_issues),
                            url = issueTracker,
                        )
                    }
                    item.app.metadata.changelog?.let { changelog ->
                        AppDetailsLink(
                            icon = Icons.Default.ChangeHistory,
                            title = stringResource(R.string.menu_changelog),
                            url = changelog,
                        )
                    }
                    item.app.metadata.license?.let { license ->
                        AppDetailsLink(
                            icon = License,
                            title = stringResource(R.string.menu_license, license),
                            url = "https://spdx.org/licenses/$license",
                        )
                    }
                    item.app.metadata.translation?.let { translation ->
                        AppDetailsLink(
                            icon = Icons.Default.Translate,
                            title = stringResource(R.string.menu_translation),
                            url = translation,
                        )
                    }
                    item.app.metadata.sourceCode?.let { sourceCode ->
                        AppDetailsLink(
                            icon = Icons.Default.Code,
                            title = stringResource(R.string.menu_source),
                            url = sourceCode,
                        )
                    }
                }
            }
            // Donation (expandable section)
            ExpandableSection(
                icon = rememberVectorPainter(Icons.Default.MonetizationOn),
                title = "Donation links",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    item.app.metadata.donate?.forEach { donation ->
                        AppDetailsLink(
                            icon = Icons.Default.Link,
                            title = donation,
                            url = donation,
                        )
                    }
                    item.app.metadata.liberapay?.let { liberapay ->
                        AppDetailsLink(
                            icon = Icons.Default.ChangeHistory,
                            title = "LiberaPay",
                            url = "https://liberapay.com/$liberapay",
                        )
                    }
                }
            }
            // Versions
            ExpandableSection(
                icon = rememberVectorPainter(Icons.Default.AccessTime),
                title = stringResource(R.string.versions),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text("TODO")
            }
            // Permissions
            ExpandableSection(
                icon = rememberVectorPainter(Icons.Default.Security),
                title = stringResource(R.string.permissions),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text("Do we still want to list all permissions here?")
            }
            // Developer contact
            ExpandableSection(
                icon = rememberVectorPainter(Icons.Default.Person),
                title = "Developer contact",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    item.app.metadata.authorWebSite?.let { authorWebSite ->
                        AppDetailsLink(
                            icon = Icons.Default.Home,
                            title = stringResource(R.string.menu_website),
                            url = authorWebSite,
                        )
                    }
                    item.app.metadata.authorEmail?.let { authorEmail ->
                        AppDetailsLink(
                            icon = Icons.Default.Mail,
                            title = stringResource(R.string.menu_email),
                            url = authorEmail,
                        )
                    }
                    item.app.metadata.authorPhone?.let { authorPhone ->
                        AppDetailsLink(
                            icon = Icons.Default.Call,
                            title = "Call the author",
                            url = authorPhone,
                        )
                    }
                }
            }
            // More apps by dev
            Button(
                onClick = {},
                modifier = Modifier
                    .align(CenterHorizontally)
                    .padding(bottom = 16.dp),
            ) {
                Text("More apps by ${item.app.metadata.authorName}")
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
        AppDetails(item, { })
    }
}

package org.fdroid.basic.ui.main.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign.Companion.Center
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.fdroid.basic.R
import org.fdroid.basic.details.AppDetailsItem
import org.fdroid.basic.details.testApp
import org.fdroid.basic.ui.Names.names
import org.fdroid.basic.ui.icons.License
import org.fdroid.fdroid.ui.theme.FDroidContent

@Composable
@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalSharedTransitionApi::class
)
fun AppDetails(
    appItem: AppDetailsItem?,
    onBackNav: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)
    if (appItem == null) Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        LoadingIndicator(Modifier.size(128.dp))
    } else Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
                title = {
                    if (topAppBarState.overlappedFraction == 1f) {
                        Text(appItem.name)
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
        val item = appItem
        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            // Header is taking care of top innerPadding
            AppDetailsHeader(item, innerPadding)
            AnimatedVisibility(item.showWarnings) { AppDetailsWarnings(item) }
            // What's New
            if (item.installedVersion != null &&
                (item.whatsNew != null || item.app.changelog != null)
            ) {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = "What's new",
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    if (item.whatsNew != null) SelectionContainer {
                        Text(
                            text = item.whatsNew,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    } else if (item.app.changelog != null) {
                        Text(
                            text = buildAnnotatedString {
                                withLink(LinkAnnotation.Url(item.app.changelog!!)) {
                                    append(item.app.changelog)
                                }
                            },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
            }
            // Description
            var descriptionExpanded by remember { mutableStateOf(false) }
            item.description?.let { description ->
                val htmlDescription = AnnotatedString.fromHtml(description)
                AnimatedVisibility(descriptionExpanded) {
                    SelectionContainer {
                        Text(
                            text = htmlDescription,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(top = 8.dp),
                        )
                    }
                }
                AnimatedVisibility(!descriptionExpanded) {
                    Text(
                        text = htmlDescription,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
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
            if (!item.antiFeatures.isNullOrEmpty()) {
                AntiFeatures(item)
            }
            // Screenshots
            if (item.phoneScreenshots.isNotEmpty()) {
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
                        model = item.phoneScreenshots[index],
                        contentDescription = "",
                        contentScale = ContentScale.Fit,
                        placeholder = rememberVectorPainter(Icons.Default.Image),
                        error = rememberVectorPainter(Icons.Default.Error),
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium),
                    )
                }
            }
            // Donate card
            if (item.showDonate) ElevatedCard(
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
                item.app.donate?.forEach { donation ->
                    AppDetailsLink(
                        icon = Icons.Default.Link,
                        title = donation,
                        url = donation,
                        modifier = modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp),
                    )
                }
                item.liberapayUri?.let { liberapayUri ->
                    AppDetailsLink(
                        icon = Icons.Default.ChangeHistory,
                        title = "LiberaPay",
                        url = liberapayUri,
                        modifier = modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp),
                    )
                }
                item.openCollectiveUri?.let { openCollectiveUri ->
                    AppDetailsLink(
                        icon = Icons.Default.Groups,
                        title = "OpenCollective",
                        url = openCollectiveUri,
                        modifier = modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp),
                    )
                }
                item.bitcoinUri?.let { bitcoinUri ->
                    AppDetailsLink(
                        icon = Icons.Default.CurrencyBitcoin,
                        title = "Bitcoin",
                        url = bitcoinUri,
                        modifier = modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp),
                    )
                }
                item.litecoinUri?.let { litecoinUri ->
                    AppDetailsLink(
                        icon = Icons.Default.CurrencyBitcoin,
                        title = "Litecoin",
                        url = litecoinUri,
                        modifier = modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp),
                    )
                }
            }
            // Links
            ExpandableSection(
                icon = rememberVectorPainter(Icons.Default.Link),
                title = stringResource(R.string.links),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    item.app.webSite?.let { webSite ->
                        AppDetailsLink(
                            icon = Icons.Default.Home,
                            title = stringResource(R.string.menu_website),
                            url = webSite,
                        )
                    }
                    item.app.issueTracker?.let { issueTracker ->
                        AppDetailsLink(
                            icon = Icons.Default.EditNote,
                            title = stringResource(R.string.menu_issues),
                            url = issueTracker,
                        )
                    }
                    item.app.changelog?.let { changelog ->
                        AppDetailsLink(
                            icon = Icons.Default.ChangeHistory,
                            title = stringResource(R.string.menu_changelog),
                            url = changelog,
                        )
                    }
                    item.app.license?.let { license ->
                        AppDetailsLink(
                            icon = License,
                            title = stringResource(R.string.menu_license, license),
                            url = "https://spdx.org/licenses/$license",
                        )
                    }
                    item.app.translation?.let { translation ->
                        AppDetailsLink(
                            icon = Icons.Default.Translate,
                            title = stringResource(R.string.menu_translation),
                            url = translation,
                        )
                    }
                    item.app.sourceCode?.let { sourceCode ->
                        AppDetailsLink(
                            icon = Icons.Default.Code,
                            title = stringResource(R.string.menu_source),
                            url = sourceCode,
                        )
                    }
                }
            }
            // Versions
            if (!item.versions.isNullOrEmpty()) {
                Versions(item)
            }
            // Developer contact
            if (item.showAuthorContact) ExpandableSection(
                icon = rememberVectorPainter(Icons.Default.Person),
                title = "Developer contact",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    item.app.authorWebSite?.let { authorWebSite ->
                        AppDetailsLink(
                            icon = Icons.Default.Home,
                            title = stringResource(R.string.menu_website),
                            url = authorWebSite,
                        )
                    }
                    item.app.authorEmail?.let { authorEmail ->
                        AppDetailsLink(
                            icon = Icons.Default.Mail,
                            title = stringResource(R.string.menu_email),
                            url = authorEmail,
                        )
                    }
                }
            }
            // More apps by dev
            if (item.authorHasMoreThanOneApp) Button(
                onClick = {},
                modifier = Modifier
                    .align(CenterHorizontally)
                    .padding(bottom = 16.dp),
            ) {
                val s =
                    stringResource(
                        R.string.app_details_more_apps_by_author,
                        item.app.authorName!!
                    )
                Text(s)
            }
        }
    }
}

@Preview
@Composable
fun AppDetailsLoadingPreview() {
    FDroidContent {
        AppDetails(null, { })
    }
}

@Preview
@Composable
fun AppDetailsPreview() {
    FDroidContent {
        AppDetails(testApp, { })
    }
}

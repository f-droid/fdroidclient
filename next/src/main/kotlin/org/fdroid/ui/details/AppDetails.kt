package org.fdroid.ui.details

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
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
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import androidx.core.os.LocaleListCompat
import org.fdroid.LocaleChooser.getBestLocale
import org.fdroid.R
import org.fdroid.fdroid.ui.theme.FDroidContent
import org.fdroid.install.InstallState
import org.fdroid.ui.NavigationKey
import org.fdroid.ui.categories.CategoryChip
import org.fdroid.ui.icons.License
import org.fdroid.ui.lists.AppListType
import org.fdroid.ui.utils.AsyncShimmerImage
import org.fdroid.ui.utils.BigLoadingIndicator
import org.fdroid.ui.utils.ExpandableSection
import org.fdroid.ui.utils.testApp

@Composable
@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalSharedTransitionApi::class
)
fun AppDetails(
    item: AppDetailsItem?,
    onNav: (NavigationKey) -> Unit,
    onBackNav: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val topAppBarState = rememberTopAppBarState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)
    if (item == null) BigLoadingIndicator()
    else Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AppDetailsTopAppBar(item, topAppBarState, scrollBehavior, onBackNav)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        // react to install state changes
        LaunchedEffect(item.installState) {
            val state = item.installState
            if (state is InstallState.UserConfirmationNeeded) {
                Log.i("AppDetails", "Requesting user confirmation... $state")
                item.actions.requestUserConfirmation(item.app.packageName, state)
            } else if (state is InstallState.Error) {
                val msg = context.getString(R.string.install_error_notify_title, state.msg ?: "")
                snackbarHostState.showSnackbar(msg)
            }
        }
        val scrollState = rememberScrollState()
        Column(
            modifier = modifier
                .verticalScroll(scrollState)
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
                        text = stringResource(R.string.whats_new_title),
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
                        text = if (descriptionExpanded) {
                            stringResource(R.string.less)
                        } else {
                            stringResource(R.string.more)
                        },
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
                    AsyncShimmerImage(
                        model = item.phoneScreenshots[index],
                        contentDescription = "",
                        contentScale = ContentScale.Fit,
                        placeholder = rememberVectorPainter(Icons.Default.Image),
                        error = rememberVectorPainter(Icons.Default.Error),
                        modifier = Modifier
                            .size(120.dp, 240.dp)
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
                    text = stringResource(R.string.donate_title),
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
                        title = stringResource(R.string.menu_bitcoin),
                        url = bitcoinUri,
                        modifier = modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp),
                    )
                }
                item.litecoinUri?.let { litecoinUri ->
                    AppDetailsLink(
                        icon = Icons.Default.CurrencyBitcoin,
                        title = stringResource(R.string.menu_litecoin),
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
                    item.app.video?.getBestLocale(LocaleListCompat.getDefault())?.let { video ->
                        AppDetailsLink(
                            icon = Icons.Default.OndemandVideo,
                            title = stringResource(R.string.menu_video),
                            url = video,
                        )
                    }
                }
            }
            // Versions
            if (!item.versions.isNullOrEmpty()) {
                Versions(item) { scrollState.scrollTo(0) }
            }
            // Developer contact
            if (item.showAuthorContact) ExpandableSection(
                icon = rememberVectorPainter(Icons.Default.Person),
                title = stringResource(R.string.developer_contact),
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
            if (!item.categories.isNullOrEmpty()) ExpandableSection(
                icon = rememberVectorPainter(Icons.Default.Category),
                title = stringResource(R.string.main_menu__categories),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                initiallyExpanded = true,
            ) {
                FlowRow(modifier = Modifier.padding(start = 16.dp)) {
                    item.categories.forEach { item ->
                        CategoryChip(item, onSelected = {
                            val categoryNav = AppListType.Category(item.name, item.id)
                            onNav(NavigationKey.AppList(categoryNav))
                        })
                    }
                }
            }
            // More apps by dev
            if (item.authorHasMoreThanOneApp) {
                val authorName = item.app.authorName!!
                val title = stringResource(R.string.app_list_author, authorName)
                Button(
                    onClick = {
                        onNav(NavigationKey.AppList(AppListType.Author(title, authorName)))
                    },
                    modifier = Modifier
                        .align(CenterHorizontally)
                        .padding(bottom = 16.dp),
                ) {
                    val s = stringResource(R.string.app_details_more_apps_by_author, authorName)
                    Text(s)
                }
            }
        }
    }
}

@Preview
@Composable
fun AppDetailsLoadingPreview() {
    FDroidContent {
        AppDetails(null, { }, {})
    }
}

@Preview
@Composable
fun AppDetailsPreview() {
    FDroidContent {
        AppDetails(testApp, { }, {})
    }
}

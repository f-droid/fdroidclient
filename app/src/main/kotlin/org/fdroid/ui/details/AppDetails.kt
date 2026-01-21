package org.fdroid.ui.details

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppSettingsAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign.Companion.Center
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import org.fdroid.LocaleChooser.getBestLocale
import org.fdroid.R
import org.fdroid.install.InstallState
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.categories.CategoryChip
import org.fdroid.ui.icons.License
import org.fdroid.ui.icons.Litecoin
import org.fdroid.ui.lists.AppListType
import org.fdroid.ui.navigation.NavigationKey
import org.fdroid.ui.utils.BigLoadingIndicator
import org.fdroid.ui.utils.ExpandableSection
import org.fdroid.ui.utils.testApp

@Composable
@OptIn(
    ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class
)
fun AppDetails(
    item: AppDetailsItem?,
    onNav: (NavigationKey) -> Unit,
    onBackNav: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val topAppBarState = rememberTopAppBarState()
    var showInstallError by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)
    if (item == null) BigLoadingIndicator()
    else Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AppDetailsTopAppBar(item, topAppBarState, scrollBehavior, onBackNav)
        },
    ) { innerPadding ->
        // react to install state changes
        LaunchedEffect(item.installState) {
            val state = item.installState
            if (state is InstallState.UserConfirmationNeeded) {
                Log.i("AppDetails", "Requesting user confirmation... $state")
                item.actions.requestUserConfirmation(state)
            } else if (state is InstallState.Error) {
                showInstallError = true
            }
        }
        val scrollState = rememberScrollState()
        var size by remember { mutableStateOf(IntSize.Zero) }
        Column(
            modifier = modifier
                .verticalScroll(scrollState)
                .fillMaxWidth()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .onGloballyPositioned { coordinates ->
                    size = coordinates.size
                }
        ) {
            // Header is taking care of top innerPadding
            AppDetailsHeader(item, innerPadding)
            AnimatedVisibility(item.showWarnings) {
                AppDetailsWarnings(item, Modifier.padding(horizontal = 16.dp))
            }
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
            item.description?.let { description ->
                val maxLines = 3
                val textMeasurer = rememberTextMeasurer()
                val allowExpand = remember(size.width, description) {
                    textMeasurer.measure(
                        text = description,
                        constraints = Constraints.fixedWidth(size.width),
                    ).lineCount > maxLines
                }
                var descriptionExpanded by remember(allowExpand) {
                    // not expanded (false) by default,
                    // but expanded (true) when expanding not allowed
                    mutableStateOf(!allowExpand)
                }
                val htmlDescription = AnnotatedString.fromHtml(description)
                AnimatedVisibility(
                    visible = descriptionExpanded,
                    modifier = Modifier
                        .semantics { liveRegion = LiveRegionMode.Polite },
                ) {
                    SelectionContainer {
                        Text(
                            text = htmlDescription,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(top = 8.dp),
                        )
                    }
                }
                if (allowExpand) {
                    AnimatedVisibility(!descriptionExpanded) {
                        Text(
                            text = htmlDescription,
                            maxLines = maxLines,
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
            }
            // Anti-features
            if (!item.antiFeatures.isNullOrEmpty()) {
                AntiFeatures(item.antiFeatures)
            }
            // Screenshots
            if (item.phoneScreenshots.isNotEmpty()) {
                Screenshots(item.networkState.isMetered, item.phoneScreenshots)
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
                        icon = Litecoin,
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
                        CategoryChip(item, onClick = {
                            val categoryNav = AppListType.Category(item.name, item.id)
                            onNav(NavigationKey.AppList(categoryNav))
                        })
                    }
                }
            }
            ExpandableSection(
                icon = rememberVectorPainter(Icons.Default.AppSettingsAlt),
                title = stringResource(R.string.technical_info),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                TechnicalInfo(item)
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
    if (showInstallError && item != null && item.installState is InstallState.Error) AlertDialog(
        onDismissRequest = { showInstallError = false },
        containerColor = MaterialTheme.colorScheme.errorContainer,
        title = {
            Text(stringResource(R.string.install_error_notify_title, item.name))
        },
        text = {
            if (item.installState.msg == null) {
                Text(stringResource(R.string.app_details_install_error_text))
            } else {
                ExpandableSection(
                    icon = null,
                    title = stringResource(R.string.app_details_install_error_text)
                ) {
                    SelectionContainer {
                        Text(
                            text = item.installState.msg,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { showInstallError = false }) {
                Text(stringResource(R.string.ok))
            }
        },
    )
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

package org.fdroid.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.enterAlwaysScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.BuildConfig.VERSION_NAME
import org.fdroid.R
import org.fdroid.ui.utils.openUriSafe

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun About(onBackClicked: (() -> Unit)?) {
    val scrollBehavior = enterAlwaysScrollBehavior(rememberTopAppBarState())
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (onBackClicked != null) IconButton(onClick = onBackClicked) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                title = {
                    Text(stringResource(R.string.about_title_full))
                },
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { paddingValues ->
        AboutContent(Modifier.fillMaxSize(), paddingValues)
    }
}

@Composable
fun AboutContent(modifier: Modifier = Modifier, paddingValues: PaddingValues = PaddingValues()) {
    val scrollableState = rememberScrollState()
    Box(
        modifier = modifier.verticalScroll(scrollableState)
    ) {
        Column(
            verticalArrangement = spacedBy(8.dp),
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            AboutHeader()
            AboutText()
        }
    }
}

@Composable
private fun AboutHeader(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = null, // decorative element
            modifier = Modifier
                .fillMaxWidth(0.25f)
                .aspectRatio(1f)
                .semantics { hideFromAccessibility() }
        )
        SelectionContainer {
            Text(
                text = "${stringResource(R.string.about_version)} $VERSION_NAME",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .alpha(0.75f)
            )
        }
    }
}

@Composable
private fun AboutText() {
    SelectionContainer {
        Text(
            text = stringResource(R.string.about_text),
            textAlign = TextAlign.Justify,
            style = MaterialTheme.typography.bodyLarge.copy(hyphens = Hyphens.Auto),
            modifier = Modifier.padding(top = 16.dp),
        )
    }
    val uriHandler = LocalUriHandler.current
    Text(
        text = stringResource(R.string.links),
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Justify,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(top = 8.dp)
    )
    AboutLink(
        text = stringResource(R.string.menu_website),
        icon = Icons.Default.Home,
        onClick = { uriHandler.openUriSafe("https://f-droid.org") },
    )
    AboutLink(
        text = stringResource(R.string.about_forum),
        icon = Icons.Default.Forum,
        onClick = { uriHandler.openUriSafe("https://forum.f-droid.org") },
    )
    AboutLink(
        text = stringResource(R.string.menu_translation),
        icon = Icons.Default.Translate,
        onClick = {
            uriHandler.openUriSafe("https://f-droid.org/en/docs/Translation_and_Localization/")
        },
    )
    AboutLink(
        text = stringResource(R.string.donate_title),
        icon = Icons.Default.MonetizationOn,
        onClick = { uriHandler.openUriSafe("https://f-droid.org/donate/") },
    )
    AboutLink(
        text = stringResource(R.string.about_source),
        icon = Icons.Default.Code,
        onClick = { uriHandler.openUriSafe("https://gitlab.com/fdroid/fdroidclient") },
    )
    Text(
        text = stringResource(R.string.about_license),
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.bodyLarge,
    )
    SelectionContainer {
        Text(
            text = stringResource(R.string.about_license_text),
            textAlign = TextAlign.Justify,
            style = MaterialTheme.typography.bodyLarge.copy(hyphens = Hyphens.Auto),
        )
    }
}

@Composable
private fun AboutLink(text: String, icon: ImageVector, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.semantics { hideFromAccessibility() }
        )
        Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
        Text(text = text)
    }
}

@Preview(showBackground = true)
@Composable
private fun AboutPreview() {
    FDroidContent {
        About {}
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AboutPreviewDark() {
    FDroidContent {
        About(null)
    }
}

package org.fdroid.ui.repositories.details

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.viktormykhailiv.compose.hints.rememberHint
import com.viktormykhailiv.compose.hints.rememberHintAnchorState
import com.viktormykhailiv.compose.hints.rememberHintController
import org.fdroid.R
import org.fdroid.fdroid.ui.theme.FDroidContent
import org.fdroid.ui.utils.BigLoadingIndicator
import org.fdroid.ui.utils.OnboardingCard
import org.fdroid.ui.utils.getHintOverlayColor
import org.fdroid.ui.utils.getRepoDetailsInfo

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun RepoDetails(
    info: RepoDetailsInfo,
    onShowAppsClicked: (String, Long) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val repo = info.model.repo

    val hintController = rememberHintController(
        overlay = getHintOverlayColor(),
    )
    val hint = rememberHint {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            OnboardingCard(
                title = stringResource(R.string.repo_details),
                message = stringResource(R.string.repo_details_info_text),
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
                onGotIt = {
                    info.actions.onOnboardingSeen()
                    hintController.dismiss()
                },
            )
        }
    }
    val hintAnchor = rememberHintAnchorState(hint)
    LaunchedEffect(info.model.showOnboarding) {
        if (info.model.showOnboarding) {
            hintController.show(hintAnchor)
            info.actions.onOnboardingSeen()
        }
    }

    var qrCodeDialog by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }
    if (repo != null && qrCodeDialog) QrCodeDialog({ qrCodeDialog = false }) {
        info.actions.generateQrCode(repo)
    }
    if (repo != null && deleteDialog) DeleteDialog({ deleteDialog = false }) {
        info.actions.deleteRepository(repo.repoId)
        deleteDialog = false
        onBack()
    }
    Scaffold(topBar = {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                    )
                }
            },
            title = {
                Text(stringResource(R.string.repo_details))
            },
            actions = {
                if (repo == null) return@TopAppBar
                IconButton(onClick = { info.model.shareRepo(context) }) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = stringResource(R.string.share_repository)
                    )
                }
                IconButton(onClick = { qrCodeDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = stringResource(R.string.show_repository_qr)
                    )
                }
                IconButton(onClick = { deleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete)
                    )
                }
            })
    }) { paddingValues ->
        if (repo == null) BigLoadingIndicator()
        else RepoDetailsContent(
            info = info,
            onShowAppsClicked = onShowAppsClicked,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Preview
@Composable
fun RepoDetailsScreenPreview() {
    FDroidContent {
        RepoDetails(getRepoDetailsInfo(), { _, _ -> }, {})
    }
}

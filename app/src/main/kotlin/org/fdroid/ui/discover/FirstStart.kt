package org.fdroid.ui.discover

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement.Center
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.download.NetworkState
import org.fdroid.index.IndexUpdateResult
import org.fdroid.repo.RepoUpdateFinished
import org.fdroid.repo.RepoUpdateProgress
import org.fdroid.repo.RepoUpdateState
import org.fdroid.repo.RepoUpdateWorker
import org.fdroid.ui.FDroidContent

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun FirstStart(
    networkState: NetworkState,
    repoUpdateState: RepoUpdateState?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var override by rememberSaveable { mutableStateOf(false) }
    // reset override on error, so user can press button again for re-try
    LaunchedEffect(repoUpdateState) {
        if (repoUpdateState is RepoUpdateFinished &&
            repoUpdateState.result is IndexUpdateResult.Error
        ) override = false
        // TODO it would be nice to surface normal update errors better and also let the user retry
    }
    Column(verticalArrangement = Center, modifier = modifier) {
        if ((!networkState.isOnline || networkState.isMetered) && !override) {
            // offline or metered, not overridden
            val res = if (networkState.isMetered) {
                stringResource(R.string.first_start_metered)
            } else {
                stringResource(R.string.first_start_offline)
            }
            Text(
                text = stringResource(R.string.first_start_intro),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            )
            Text(
                text = res,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            )
            Button(
                onClick = { override = true },
                modifier = Modifier.align(CenterHorizontally),
            ) {
                Text(stringResource(R.string.first_start_button))
            }
        } else {
            // happy path or user set override
            LaunchedEffect(Unit) {
                RepoUpdateWorker.updateNow(context)
            }
            Text(
                text = stringResource(R.string.first_start_loading),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            )
            // use thicker stroke for larger circle
            val stroke = Stroke(
                width = with(LocalDensity.current) {
                    6.dp.toPx()
                },
                cap = StrokeCap.Round,
            )
            val progressModifier = Modifier
                .padding(16.dp)
                .size(128.dp)
                .align(CenterHorizontally)
            // show indeterminate circle if we don't have any progress (may take a bit to start)
            val progress = (repoUpdateState as? RepoUpdateProgress)?.progress ?: 0f
            if (progress == 0f) CircularWavyProgressIndicator(
                wavelength = 24.dp,
                stroke = stroke,
                trackStroke = stroke,
                modifier = progressModifier,
            ) else {
                // animate real progress (download and DB insertion)
                val animatedProgress by animateFloatAsState(targetValue = progress)
                CircularWavyProgressIndicator(
                    progress = { animatedProgress },
                    wavelength = 24.dp,
                    stroke = stroke,
                    trackStroke = stroke,
                    modifier = progressModifier,
                )
            }
        }
    }
}

@Preview
@Composable
private fun OfflinePreview() {
    FDroidContent {
        Column {
            FirstStart(NetworkState(isOnline = false, isMetered = false), null)
        }
    }
}

@Preview
@Composable
private fun MeteredPreview() {
    FDroidContent {
        Column {
            FirstStart(NetworkState(isOnline = true, isMetered = true), null)
        }
    }
}

@Preview
@Composable
private fun UpdatePreview() {
    FDroidContent {
        Column {
            FirstStart(
                networkState = NetworkState(isOnline = true, isMetered = false),
                repoUpdateState = RepoUpdateProgress(1L, false, 0.5f),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

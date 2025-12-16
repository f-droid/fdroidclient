package org.fdroid.ui.discover

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.repo.RepoUpdateState
import org.fdroid.ui.FDroidContent

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun ColumnScope.FirstStart(repoUpdateState: RepoUpdateState?) {
    Text(
        stringResource(R.string.first_start_loading),
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
        .align(Alignment.CenterHorizontally)
    // show indeterminate circle if we don't have any progress (may take a bit to start)
    if ((repoUpdateState?.progress ?: 0f) == 0f) CircularWavyProgressIndicator(
        wavelength = 24.dp,
        stroke = stroke,
        trackStroke = stroke,
        modifier = progressModifier,
    ) else {
        // animate real progress (download and DB insertion)
        val animatedProgress by animateFloatAsState(
            targetValue = repoUpdateState?.progress ?: 0f,
            animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        )
        CircularWavyProgressIndicator(
            progress = { animatedProgress },
            wavelength = 24.dp,
            stroke = stroke,
            trackStroke = stroke,
            modifier = progressModifier,
        )
    }
}

@Preview
@Composable
private fun NullPreview() {
    FDroidContent {
        Column {
            FirstStart(null)
        }
    }
}

@Preview
@Composable
private fun DownloadPreview() {
    FDroidContent {
        Column {
            FirstStart(RepoUpdateState(1L, true, 0.25f))
        }
    }
}

@Preview
@Composable
private fun UpdatePreview() {
    FDroidContent {
        Column {
            FirstStart(RepoUpdateState(1L, false, 0.75f))
        }
    }
}

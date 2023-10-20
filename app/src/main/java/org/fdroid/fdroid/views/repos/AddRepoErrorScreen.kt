package org.fdroid.fdroid.views.repos

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.fdroid.R
import org.fdroid.fdroid.compose.ComposeUtils
import org.fdroid.fdroid.views.repos.ManageReposActivity.getDisallowInstallUnknownSourcesErrorMessage
import org.fdroid.repo.AddRepoError
import org.fdroid.repo.AddRepoError.ErrorType.INVALID_FINGERPRINT
import org.fdroid.repo.AddRepoError.ErrorType.INVALID_INDEX
import org.fdroid.repo.AddRepoError.ErrorType.IO_ERROR
import org.fdroid.repo.AddRepoError.ErrorType.UNKNOWN_SOURCES_DISALLOWED
import java.io.IOException

@Composable
fun AddRepoErrorScreen(paddingValues: PaddingValues, state: AddRepoError) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp, CenterVertically),
        horizontalAlignment = CenterHorizontally,
        modifier = Modifier
            .padding(16.dp)
            .padding(paddingValues)
            .fillMaxSize(),
    ) {
        Image(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.colors.error),
            modifier = Modifier.size(48.dp),
        )
        val title = when (state.errorType) {
            INVALID_FINGERPRINT -> stringResource(R.string.bad_fingerprint)
            UNKNOWN_SOURCES_DISALLOWED -> {
                if (LocalInspectionMode.current) {
                    stringResource(R.string.has_disallow_install_unknown_sources)
                } else {
                    getDisallowInstallUnknownSourcesErrorMessage(LocalContext.current)
                }
            }

            INVALID_INDEX -> stringResource(R.string.repo_invalid)
            IO_ERROR -> stringResource(R.string.repo_io_error)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.h5,
            textAlign = TextAlign.Center,
        )
        if (state.exception != null) Text(
            text = state.exception.toString(),
            style = MaterialTheme.typography.body1,
            modifier = Modifier.alpha(ContentAlpha.medium),
        )
    }
}

@Preview
@Composable
fun AddRepoErrorInvalidFingerprintPreview() {
    ComposeUtils.FDroidContent {
        AddRepoErrorScreen(PaddingValues(0.dp), AddRepoError(INVALID_FINGERPRINT))
    }
}

@Preview
@Composable
fun AddRepoErrorIoErrorPreview() {
    ComposeUtils.FDroidContent {
        AddRepoErrorScreen(PaddingValues(0.dp), AddRepoError(IO_ERROR, IOException("foo bar")))
    }
}

@Preview
@Composable
fun AddRepoErrorInvalidIndexPreview() {
    ComposeUtils.FDroidContent {
        AddRepoErrorScreen(
            PaddingValues(0.dp),
            AddRepoError(INVALID_INDEX, RuntimeException("foo bar"))
        )
    }
}

@Preview
@Composable
fun AddRepoErrorUnknownSourcesPreview() {
    ComposeUtils.FDroidContent {
        AddRepoErrorScreen(PaddingValues(0.dp), AddRepoError(UNKNOWN_SOURCES_DISALLOWED))
    }
}

package org.fdroid.ui.repositories.add

import android.content.Context
import android.os.Build.VERSION.SDK_INT
import android.os.UserManager
import android.os.UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.fdroid.ui.theme.FDroidContent
import org.fdroid.repo.AddRepoError
import org.fdroid.repo.AddRepoError.ErrorType.INVALID_FINGERPRINT
import org.fdroid.repo.AddRepoError.ErrorType.INVALID_INDEX
import org.fdroid.repo.AddRepoError.ErrorType.IO_ERROR
import org.fdroid.repo.AddRepoError.ErrorType.IS_ARCHIVE_REPO
import org.fdroid.repo.AddRepoError.ErrorType.UNKNOWN_SOURCES_DISALLOWED
import java.io.IOException

@Composable
fun AddRepoErrorScreen(state: AddRepoError, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp, CenterVertically),
        horizontalAlignment = CenterHorizontally,
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize(),
    ) {
        Image(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.error),
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
            IS_ARCHIVE_REPO -> stringResource(R.string.repo_error_adding_archive)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        if (state.exception != null) Text(
            text = state.exception.toString(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

fun getDisallowInstallUnknownSourcesErrorMessage(context: Context): String {
    val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
    return if (SDK_INT >= 29 &&
        userManager.hasUserRestriction(DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY)
    ) {
        context.getString(R.string.has_disallow_install_unknown_sources_globally)
    } else {
        context.getString(R.string.has_disallow_install_unknown_sources)
    }
}

@Preview
@Composable
fun AddRepoErrorInvalidFingerprintPreview() {
    FDroidContent {
        AddRepoErrorScreen(AddRepoError(INVALID_FINGERPRINT))
    }
}

@Preview
@Composable
fun AddRepoErrorIoErrorPreview() {
    FDroidContent {
        AddRepoErrorScreen(AddRepoError(IO_ERROR, IOException("foo bar")))
    }
}

@Preview
@Composable
fun AddRepoErrorInvalidIndexPreview() {
    FDroidContent {
        AddRepoErrorScreen(AddRepoError(INVALID_INDEX, RuntimeException("foo bar")))
    }
}

@Preview
@Composable
fun AddRepoErrorUnknownSourcesPreview() {
    FDroidContent {
        AddRepoErrorScreen(AddRepoError(UNKNOWN_SOURCES_DISALLOWED))
    }
}

@Preview
@Composable
fun AddRepoErrorArchivePreview() {
    FDroidContent {
        AddRepoErrorScreen(AddRepoError(IS_ARCHIVE_REPO))
    }
}

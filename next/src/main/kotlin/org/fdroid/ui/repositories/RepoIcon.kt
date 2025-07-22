package org.fdroid.ui.repositories

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.core.os.LocaleListCompat
import coil3.compose.AsyncImage
import org.fdroid.next.R
import org.fdroid.download.getDownloadRequest
import org.fdroid.database.Repository

@Composable
fun RepoIcon(repo: Repository, modifier: Modifier = Modifier) {
    AsyncImage(
        model = repo.getIcon(LocaleListCompat.getDefault())?.getDownloadRequest(repo),
        contentDescription = null,
        error = painterResource(R.drawable.ic_repo_app_default),
        placeholder = painterResource(R.drawable.ic_repo_app_default),
        modifier = modifier,
    )
}

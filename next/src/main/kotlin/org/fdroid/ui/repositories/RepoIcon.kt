package org.fdroid.ui.repositories

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.core.os.LocaleListCompat
import org.fdroid.R
import org.fdroid.database.Repository
import org.fdroid.download.getDownloadRequest
import org.fdroid.ui.utils.AsyncShimmerImage

@Composable
fun RepoIcon(repo: Repository, modifier: Modifier = Modifier) {
    AsyncShimmerImage(
        model = repo.getIcon(LocaleListCompat.getDefault())?.getDownloadRequest(repo),
        contentDescription = null,
        error = painterResource(R.drawable.ic_repo_app_default),
        modifier = modifier,
    )
}

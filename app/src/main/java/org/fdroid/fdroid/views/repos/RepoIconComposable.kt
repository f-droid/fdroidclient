package org.fdroid.fdroid.views.repos

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.core.content.res.ResourcesCompat.getDrawable
import androidx.core.os.LocaleListCompat
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import org.fdroid.database.Repository
import org.fdroid.fdroid.R
import org.fdroid.fdroid.Utils.getDownloadRequest

@Composable
@OptIn(ExperimentalGlideComposeApi::class)
fun RepoIcon(repo: Repository, modifier: Modifier = Modifier) {
    if (LocalInspectionMode.current) Image(
        painter = rememberDrawablePainter(
            getDrawable(LocalContext.current.resources, R.drawable.ic_launcher, null)
        ),
        contentDescription = null,
        modifier = modifier,
    ) else GlideImage(
        model = getDownloadRequest(repo, repo.getIcon(LocaleListCompat.getDefault())),
        contentDescription = null,
        modifier = modifier,
    ) { requestBuilder ->
        requestBuilder
            .fallback(R.drawable.ic_repo_app_default)
            .error(R.drawable.ic_repo_app_default)
    }
}

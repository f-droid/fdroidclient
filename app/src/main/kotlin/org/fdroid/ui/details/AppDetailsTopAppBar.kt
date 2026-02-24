package org.fdroid.ui.details

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import org.fdroid.R
import org.fdroid.ui.utils.BackButton
import org.fdroid.ui.utils.TopAppBarButton
import org.fdroid.ui.utils.TopAppBarOverflowButton
import org.fdroid.ui.utils.startActivitySafe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailsTopAppBar(
    item: AppDetailsItem,
    topAppBarState: TopAppBarState,
    scrollBehavior: TopAppBarScrollBehavior,
    onBackNav: (() -> Unit)?,
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
        ),
        title = {
            if (topAppBarState.overlappedFraction == 1f) {
                Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        navigationIcon = {
            if (onBackNav != null) BackButton(onClick = onBackNav)
        },
        actions = {
            val context = LocalContext.current
            item.actions.shareIntent?.let { shareIntent ->
                TopAppBarButton(
                    imageVector = Icons.Filled.Share,
                    contentDescription = stringResource(R.string.menu_share),
                    onClick = { context.startActivitySafe(shareIntent) },
                )
            }
            TopAppBarOverflowButton { onDismissRequest ->
                AppDetailsMenu(item, onDismissRequest)
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

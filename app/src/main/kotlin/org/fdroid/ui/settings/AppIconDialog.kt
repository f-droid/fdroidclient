package org.fdroid.ui.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.utils.AdaptiveIconImage

@Composable
fun AppIconDialog(
  currentAppIcon: AppIcon,
  onChangeIcon: (AppIcon) -> Unit,
  onDismissRequest: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismissRequest,
    title = { Text(stringResource(R.string.icon_title)) },
    text = {
      Column(
        horizontalAlignment = CenterHorizontally,
        verticalArrangement = spacedBy(8.dp),
      ) {
        Text(stringResource(R.string.icon_intro))
        LazyVerticalGrid(
          columns = GridCells.Adaptive(minSize = 96.dp),
          contentPadding = PaddingValues(vertical = 8.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          items(appIcons) { icon ->
            val isSelected = icon == currentAppIcon
            Column(
              horizontalAlignment = CenterHorizontally,
              verticalArrangement = spacedBy(8.dp),
              modifier =
                Modifier.width(96.dp).padding(horizontal = 8.dp).clickable { onChangeIcon(icon) },
            ) {
              val boxModifier = Modifier.size(64.dp)
              Box(
                modifier =
                  if (isSelected)
                    boxModifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                  else boxModifier
              ) {
                AdaptiveIconImage(
                  icon.iconRes,
                  modifier =
                    Modifier.align(Center)
                      .size(if (isSelected) 48.dp else 64.dp)
                      .graphicsLayer(
                        shape = CircleShape,
                        shadowElevation = if (isSelected) 4f else 8f,
                        clip = true,
                      ),
                )
              }
              Text(stringResource(icon.labelRes))
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel)) }
    },
  )
}

@Preview
@Composable
private fun Preview() {
  FDroidContent {
    Box(modifier = Modifier.fillMaxSize()) {
      AppIconDialog(
        currentAppIcon = appIcons.random(),
        onChangeIcon = {},
        onDismissRequest = {},
      )
    }
  }
}

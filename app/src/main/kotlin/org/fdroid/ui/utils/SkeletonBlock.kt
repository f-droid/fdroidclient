package org.fdroid.ui.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip

@Composable
fun SkeletonBlock(
  modifier: Modifier,
  shape: androidx.compose.ui.graphics.Shape = MaterialTheme.shapes.small,
) {
  Box(modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHighest).shimmer())
}

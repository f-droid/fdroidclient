package org.fdroid.ui.utils

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SelectableCard(
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  val primaryColor = MaterialTheme.colorScheme.primary
  val shape = RoundedCornerShape(16.dp)

  val borderColor by
    animateColorAsState(
      targetValue = if (selected) primaryColor else MaterialTheme.colorScheme.outline,
      animationSpec = tween(durationMillis = 300),
      label = "borderColor",
    )
  val backgroundColor by
    animateColorAsState(
      targetValue =
        if (selected) {
          MaterialTheme.colorScheme.primaryContainer
        } else {
          MaterialTheme.colorScheme.surfaceContainer
        },
      animationSpec = tween(durationMillis = 300),
      label = "backgroundColor",
    )

  Card(
    modifier = modifier.fillMaxWidth().clickable { onClick() },
    shape = shape,
    colors = CardDefaults.cardColors(containerColor = backgroundColor),
    border =
      BorderStroke(
        width = if (selected) 2.dp else 1.dp,
        color = borderColor,
      ),
  ) {
    content()
  }
}

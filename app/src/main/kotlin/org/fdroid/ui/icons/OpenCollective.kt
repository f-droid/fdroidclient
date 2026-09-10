package org.fdroid.ui.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.ui.FDroidContent

val OpenCollective: ImageVector
  get() {
    if (_OpenCollective != null) {
      return _OpenCollective!!
    }
    _OpenCollective =
      ImageVector.Builder(
          name = "OpenCollective",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 29f,
          viewportHeight = 29f,
        )
        .apply {
          path(
            fill = SolidColor(Color(0xFF99CFFF)),
            pathFillType = PathFillType.EvenOdd,
          ) {
            moveTo(26.009f, 6.526f)
            curveTo(27.58f, 8.789f, 28.5f, 11.537f, 28.5f, 14.5f)
            curveToRelative(0f, 2.963f, -0.92f, 5.711f, -2.491f, 7.973f)
            lineToRelative(-3.626f, -3.626f)
            curveToRelative(0.712f, -1.289f, 1.117f, -2.771f, 1.117f, -4.347f)
            curveToRelative(0f, -1.576f, -0.405f, -3.058f, -1.117f, -4.347f)
            close()
          }
          path(
            fill = SolidColor(Color(0xFF1F87FF)),
            pathFillType = PathFillType.EvenOdd,
          ) {
            moveTo(22.473f, 2.991f)
            lineTo(18.847f, 6.617f)
            curveTo(17.558f, 5.905f, 16.076f, 5.5f, 14.5f, 5.5f)
            curveToRelative(-4.97f, 0f, -9f, 4.03f, -9f, 9f)
            curveToRelative(0f, 4.97f, 4.03f, 9f, 9f, 9f)
            curveToRelative(1.576f, 0f, 3.058f, -0.405f, 4.347f, -1.117f)
            lineToRelative(3.627f, 3.626f)
            curveToRelative(-2.263f, 1.571f, -5.01f, 2.491f, -7.973f, 2.491f)
            curveToRelative(-7.732f, 0f, -14f, -6.268f, -14f, -14f)
            curveToRelative(0f, -7.732f, 6.268f, -14f, 14f, -14f)
            curveToRelative(2.963f, 0f, 5.711f, 0.92f, 7.973f, 2.491f)
            close()
          }
        }
        .build()

    return _OpenCollective!!
  }

@Suppress("ObjectPropertyName") private var _OpenCollective: ImageVector? = null

@Preview
@Composable
private fun Preview() {
  FDroidContent {
    Box(modifier = Modifier.padding(12.dp)) {
      Image(imageVector = OpenCollective, contentDescription = "")
    }
  }
}

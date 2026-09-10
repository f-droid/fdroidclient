package org.fdroid.ui.icons

import android.content.res.Configuration
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

private fun createIcon(color: Color): ImageVector {
  return ImageVector.Builder(
      name = "Taler",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 200f,
      viewportHeight = 200f,
    )
    .apply {
      path(
        fill = SolidColor(color),
        pathFillType = PathFillType.EvenOdd,
      ) {
        moveTo(57.6f, 43.4f)
        curveToRelative(-25.5f, 4.3f, -44.9f, 28f, -44.9f, 56.5f)
        curveToRelative(0f, 31.5f, 23.9f, 57.2f, 53.3f, 57.2f)
        reflectiveCurveToRelative(53.3f, -25.6f, 53.3f, -57.2f)
        curveToRelative(0f, -15.4f, -5.7f, -29.3f, -14.9f, -39.6f)
        curveToRelative(1.6f, -1.9f, 6.3f, -4.8f, 6.4f, -4.6f)
        curveToRelative(10f, 11.6f, 16.1f, 27.2f, 16.1f, 44.2f)
        curveToRelative(0f, 36f, -27.3f, 65.3f, -60.9f, 65.3f)
        curveToRelative(-33.6f, 0f, -60.9f, -29.3f, -60.9f, -65.3f)
        reflectiveCurveToRelative(27.3f, -65.3f, 60.9f, -65.3f)
        curveToRelative(1.7f, 0f, 5.7f, 0.3f, 5.5f, 0.4f)
        curveToRelative(-4.3f, 2.3f, -9.7f, 5.4f, -13.9f, 8.5f)
      }
      path(
        fill = SolidColor(color),
        pathFillType = PathFillType.EvenOdd,
      ) {
        moveTo(60.8f, 149.8f)
        curveToRelative(-13.4f, -12f, -22f, -29.9f, -22f, -50f)
        curveToRelative(0f, -36f, 27.4f, -65.2f, 61.1f, -65.2f)
        curveToRelative(1.5f, 0f, 3f, 0.1f, 4.5f, 0.2f)
        arcToRelative(67.6f, 67.6f, 0f, isMoreThanHalf = false, isPositiveArc = false, -13.4f, 8.6f)
        curveToRelative(-25.4f, 4.5f, -44.7f, 28.1f, -44.7f, 56.4f)
        curveToRelative(0f, 21.3f, 11f, 40f, 27.3f, 49.8f)
        arcToRelative(45.9f, 45.9f, 0f, isMoreThanHalf = false, isPositiveArc = true, -12.7f, 0.3f)
        close()
      }
      path(
        fill = SolidColor(color),
        pathFillType = PathFillType.EvenOdd,
      ) {
        moveTo(142.4f, 156.6f)
        curveToRelative(25.5f, -4.3f, 44.9f, -28f, 44.9f, -56.5f)
        curveToRelative(-0f, -31.5f, -23.9f, -57.2f, -53.3f, -57.2f)
        reflectiveCurveToRelative(-53.3f, 25.6f, -53.3f, 57.2f)
        curveToRelative(-0f, 15.4f, 5.7f, 29.3f, 14.9f, 39.6f)
        curveToRelative(-1.6f, 1.9f, -6.3f, 4.8f, -6.4f, 4.6f)
        curveToRelative(-10f, -11.6f, -16.1f, -27.2f, -16.1f, -44.2f)
        curveToRelative(-0f, -36f, 27.3f, -65.3f, 60.9f, -65.3f)
        curveToRelative(33.6f, -0f, 60.9f, 29.3f, 60.9f, 65.3f)
        reflectiveCurveToRelative(-27.3f, 65.3f, -60.9f, 65.3f)
        curveToRelative(-1.7f, -0f, -5.7f, -0.3f, -5.5f, -0.4f)
        curveToRelative(4.3f, -2.3f, 9.7f, -5.4f, 13.9f, -8.5f)
      }
      path(
        fill = SolidColor(color),
        pathFillType = PathFillType.EvenOdd,
      ) {
        moveTo(139.2f, 50.2f)
        curveToRelative(13.4f, 12f, 22f, 29.9f, 22f, 50f)
        curveToRelative(-0f, 36f, -27.4f, 65.2f, -61.1f, 65.2f)
        curveToRelative(-1.5f, -0f, -3f, -0.1f, -4.5f, -0.2f)
        arcToRelative(67.6f, 67.6f, 0f, isMoreThanHalf = false, isPositiveArc = false, 13.4f, -8.6f)
        curveToRelative(25.4f, -4.5f, 44.7f, -28.1f, 44.7f, -56.4f)
        curveToRelative(-0f, -21.3f, -11f, -40f, -27.3f, -49.8f)
        arcToRelative(45.9f, 45.9f, 45f, isMoreThanHalf = false, isPositiveArc = true, 12.7f, -0.3f)
        close()
      }
    }
    .build()
}

val Taler: ImageVector
  get() {
    if (_Taler != null) {
      return _Taler!!
    }
    _Taler = createIcon(Color(0xFF0042B3))
    return _Taler!!
  }

@Suppress("ObjectPropertyName") private var _Taler: ImageVector? = null

val TalerNight: ImageVector
  get() {
    if (_TalerNight != null) {
      return _TalerNight!!
    }
    _TalerNight = createIcon(Color(0xffb4c5ff))
    return _TalerNight!!
  }

@Suppress("ObjectPropertyName") private var _TalerNight: ImageVector? = null

@Preview
@Composable
private fun DayPreview() {
  FDroidContent {
    Box(modifier = Modifier.padding(12.dp)) { Image(imageVector = Taler, contentDescription = "") }
  }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NightPreview() {
  FDroidContent {
    Box(modifier = Modifier.padding(12.dp)) {
      Image(imageVector = TalerNight, contentDescription = "")
    }
  }
}

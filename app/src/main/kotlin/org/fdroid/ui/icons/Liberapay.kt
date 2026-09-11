package org.fdroid.ui.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.ui.FDroidContent

val Liberapay: ImageVector
  get() {
    if (_Liberapay != null) {
      return _Liberapay!!
    }
    _Liberapay =
      ImageVector.Builder(
          name = "Liberapay",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 80f,
          viewportHeight = 80f,
        )
        .apply {
          path(fill = SolidColor(Color(0xFFF6C915))) {
            moveTo(10f, 0f)
            lineTo(70f, 0f)
            arcTo(10f, 10f, 0f, isMoreThanHalf = false, isPositiveArc = true, 80f, 10f)
            lineTo(80f, 70f)
            arcTo(10f, 10f, 0f, isMoreThanHalf = false, isPositiveArc = true, 70f, 80f)
            lineTo(10f, 80f)
            arcTo(10f, 10f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0f, 70f)
            lineTo(0f, 10f)
            arcTo(10f, 10f, 0f, isMoreThanHalf = false, isPositiveArc = true, 10f, 0f)
            close()
          }
          path(fill = SolidColor(Color(0xFF1A171B))) {
            moveTo(32.73f, 56.265f)
            curveToRelative(-2.587f, 0f, -4.617f, -0.338f, -6.093f, -1.011f)
            reflectiveCurveToRelative(-2.531f, -1.594f, -3.171f, -2.761f)
            reflectiveCurveToRelative(-0.946f, -2.493f, -0.928f, -4.015f)
            reflectiveCurveToRelative(0.227f, -3.123f, 0.628f, -4.836f)
            lineToRelative(6.939f, -29.014f)
            lineToRelative(8.47f, -1.311f)
            lineToRelative(-7.595f, 31.473f)
            curveToRelative(-0.146f, 0.655f, -0.228f, 1.257f, -0.246f, 1.803f)
            reflectiveCurveToRelative(0.082f, 1.029f, 0.3f, 1.447f)
            reflectiveCurveToRelative(0.592f, 0.757f, 1.12f, 1.011f)
            reflectiveCurveToRelative(1.266f, 0.42f, 2.213f, 0.493f)
            close()
            moveTo(40.533f, 28.021f)
            curveToRelative(1.46f, -0.437f, 3.127f, -0.828f, 5.003f, -1.175f)
            reflectiveCurveToRelative(3.908f, -0.518f, 6.093f, -0.518f)
            reflectiveCurveToRelative(3.799f, 0.309f, 5.273f, 0.928f)
            reflectiveCurveToRelative(2.686f, 1.467f, 3.634f, 2.541f)
            reflectiveCurveToRelative(1.648f, 2.332f, 2.102f, 3.77f)
            reflectiveCurveToRelative(0.683f, 2.978f, 0.683f, 4.618f)
            curveToRelative(0f, 2.658f, -0.437f, 5.09f, -1.321f, 7.294f)
            reflectiveCurveToRelative(-2.076f, 4.108f, -3.624f, 5.71f)
            reflectiveCurveToRelative(-3.406f, 2.851f, -5.576f, 3.747f)
            reflectiveCurveToRelative(-4.523f, 1.334f, -7.072f, 1.334f)
            curveToRelative(-1.239f, 0f, -2.477f, -0.11f, -3.716f, -0.33f)
            lineToRelative(-2.459f, 9.891f)
            horizontalLineToRelative(-8.087f)
            close()
            moveTo(43.487f, 49.387f)
            curveToRelative(0.619f, 0.146f, 1.384f, 0.213f, 2.295f, 0.213f)
            curveToRelative(1.42f, 0f, 2.713f, -0.258f, 3.879f, -0.788f)
            reflectiveCurveToRelative(2.158f, -1.265f, 2.978f, -2.213f)
            reflectiveCurveToRelative(1.456f, -2.084f, 1.912f, -3.415f)
            reflectiveCurveToRelative(0.683f, -2.795f, 0.683f, -4.398f)
            reflectiveCurveToRelative(-0.347f, -2.896f, -1.038f, -3.989f)
            reflectiveCurveToRelative(-1.894f, -1.639f, -3.606f, -1.639f)
            curveToRelative(-1.167f, 0f, -2.259f, 0.109f, -3.279f, 0.328f)
            close()
          }
        }
        .build()

    return _Liberapay!!
  }

@Suppress("ObjectPropertyName") private var _Liberapay: ImageVector? = null

@Preview
@Composable
private fun Preview() {
  FDroidContent {
    Box(modifier = Modifier.padding(12.dp)) {
      Image(imageVector = Liberapay, contentDescription = "")
    }
  }
}

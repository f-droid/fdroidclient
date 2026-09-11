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

val Litecoin: ImageVector
  get() {
    if (_Litecoin != null) {
      return _Litecoin!!
    }
    _Litecoin =
      ImageVector.Builder(
          name = "Litecoin",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 6.35f,
          viewportHeight = 6.35f,
        )
        .apply {
          path(
            fill = SolidColor(Color(0xFF345D9D)),
            strokeLineWidth = 0.264733f,
          ) {
            moveTo(3.009f, 0.013f)
            curveTo(2.8072f, 0.0395f, 2.6093f, 0.0528f, 2.4096f, 0.1002f)
            curveTo(1.9008f, 0.221f, 1.4091f, 0.4834f, 1.0264f, 0.8398f)
            curveTo(0.7146f, 1.1303f, 0.4603f, 1.475f, 0.2823f, 1.8626f)
            curveTo(-0.2829f, 3.0938f, 0.0184f, 4.6192f, 1.0264f, 5.5312f)
            curveTo(1.4193f, 5.8866f, 1.8823f, 6.1267f, 2.3912f, 6.2665f)
            curveTo(2.7859f, 6.375f, 3.2141f, 6.3868f, 3.6177f, 6.3324f)
            curveTo(5.0811f, 6.1353f, 6.2818f, 4.8595f, 6.3468f, 3.3749f)
            curveTo(6.4132f, 1.8592f, 5.4265f, 0.474f, 3.9404f, 0.1025f)
            curveTo(3.6493f, 0.0297f, 3.3093f, -0.0264f, 3.009f, 0.013f)
          }
          path(
            fill = SolidColor(Color.White),
            strokeLineWidth = 0.264733f,
          ) {
            moveTo(3.3318f, 2.9231f)
            lineTo(3.8298f, 2.7663f)
            curveTo(3.8257f, 2.8561f, 3.785f, 2.9478f, 3.7599f, 3.0337f)
            curveTo(3.7493f, 3.0699f, 3.7431f, 3.1183f, 3.7078f, 3.1391f)
            curveTo(3.6145f, 3.1942f, 3.473f, 3.2171f, 3.3687f, 3.2471f)
            curveTo(3.327f, 3.2591f, 3.2567f, 3.2692f, 3.2278f, 3.3039f)
            curveTo(3.1656f, 3.3789f, 3.1478f, 3.5487f, 3.121f, 3.6423f)
            curveTo(3.0483f, 3.8958f, 2.9518f, 4.1495f, 2.8984f, 4.4077f)
            horizontalLineToRelative(1.2357f)
            horizontalLineToRelative(0.3689f)
            curveToRelative(0.0571f, 0f, 0.1295f, -0.0117f, 0.1843f, 0.006f)
            curveToRelative(0.0611f, 0.0197f, 0.068f, 0.0781f, 0.054f, 0.1323f)
            curveToRelative(-0.0357f, 0.1391f, -0.0787f, 0.2769f, -0.1184f, 0.415f)
            curveToRelative(-0.0161f, 0.0558f, -0.0269f, 0.1318f, -0.0834f, 0.1618f)
            curveToRelative(-0.0609f, 0.0323f, -0.163f, 0.0134f, -0.2301f, 0.0134f)
            horizontalLineTo(3.7652f)
            horizontalLineTo(1.7826f)
            lineTo(1.9262f, 4.6383f)
            lineTo(2.2344f, 3.5962f)
            lineTo(1.7365f, 3.753f)
            curveToRelative(0.0079f, -0.0903f, 0.0461f, -0.1806f, 0.0711f, -0.2674f)
            curveToRelative(0.0092f, -0.0322f, 0.0142f, -0.0781f, 0.0434f, -0.0989f)
            curveToRelative(0.0895f, -0.0637f, 0.2508f, -0.0842f, 0.3558f, -0.1158f)
            curveToRelative(0.0488f, -0.0147f, 0.1161f, -0.0221f, 0.1423f, -0.0717f)
            curveTo(2.4231f, 3.0593f, 2.4463f, 2.8716f, 2.4929f, 2.7202f)
            curveTo(2.6059f, 2.3526f, 2.7127f, 1.9827f, 2.8207f, 1.6136f)
            curveTo(2.8603f, 1.4783f, 2.9019f, 1.3435f, 2.9406f, 1.2079f)
            curveTo(2.9578f, 1.1478f, 2.9692f, 1.0634f, 3.0127f, 1.0162f)
            curveTo(3.0621f, 0.9625f, 3.1462f, 0.9773f, 3.2119f, 0.9773f)
            horizontalLineTo(3.7006f)
            curveToRelative(0.0678f, 0f, 0.1629f, -0.0075f, 0.1729f, 0.083f)
            curveToRelative(0.0061f, 0.0553f, -0.0275f, 0.1225f, -0.0425f, 0.1752f)
            curveTo(3.7942f, 1.365f, 3.7552f, 1.4939f, 3.7165f, 1.6228f)
            curveTo(3.5878f, 2.0521f, 3.4919f, 2.5047f, 3.3318f, 2.9231f)
          }
        }
        .build()

    return _Litecoin!!
  }

@Suppress("ObjectPropertyName") private var _Litecoin: ImageVector? = null

@Preview
@Composable
private fun Preview() {
  FDroidContent {
    Box(modifier = Modifier.padding(12.dp)) {
      Image(imageVector = Litecoin, contentDescription = "")
    }
  }
}

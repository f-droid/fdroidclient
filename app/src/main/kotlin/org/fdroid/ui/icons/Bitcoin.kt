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

val Bitcoin: ImageVector
  get() {
    if (_Bitcoin != null) {
      return _Bitcoin!!
    }
    _Bitcoin =
      ImageVector.Builder(
          name = "Bitcoin",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 6.35f,
          viewportHeight = 6.35f,
        )
        .apply {
          path(fill = SolidColor(Color(0xFFF79313))) {
            moveTo(3.051f, 0.0021f)
            curveTo(2.8252f, 0.0317f, 2.6038f, 0.0414f, 2.3812f, 0.1f)
            curveTo(1.715f, 0.2755f, 1.1172f, 0.6617f, 0.6868f, 1.2005f)
            curveTo(0.4503f, 1.4967f, 0.2661f, 1.8407f, 0.1537f, 2.2027f)
            curveTo(0.0472f, 2.5452f, -0.0156f, 2.9097f, 2.0E-4f, 3.2693f)
            curveTo(0.0468f, 4.3346f, 0.6013f, 5.3166f, 1.5131f, 5.8816f)
            curveTo(1.8013f, 6.0601f, 2.1211f, 6.1881f, 2.4507f, 6.2662f)
            curveTo(2.8226f, 6.3544f, 3.2084f, 6.3709f, 3.5868f, 6.3238f)
            curveTo(4.6065f, 6.1969f, 5.5262f, 5.5448f, 5.9965f, 4.6335f)
            curveTo(6.2414f, 4.159f, 6.363f, 3.6189f, 6.3499f, 3.0857f)
            curveTo(6.3157f, 1.6865f, 5.3107f, 0.4366f, 3.9539f, 0.0974f)
            curveTo(3.7471f, 0.0457f, 3.5365f, 0.0141f, 3.3238f, 0.0048f)
            curveTo(3.2349f, 9.0E-4f, 3.1398f, -0.0096f, 3.051f, 0.0021f)
          }
          path(fill = SolidColor(Color.White)) {
            moveTo(3.3933f, 1.1906f)
            lineTo(3.2544f, 1.7562f)
            curveToRelative(0.0875f, 0.0071f, 0.1957f, 0.0335f, 0.2778f, 0.0645f)
            curveToRelative(0.0289f, -0.1398f, 0.0677f, -0.2782f, 0.1023f, -0.4167f)
            curveToRelative(0.0066f, -0.0264f, 0.016f, -0.1252f, 0.043f, -0.1355f)
            curveToRelative(0.0232f, -0.0089f, 0.0694f, 0.0124f, 0.0928f, 0.0183f)
            curveToRelative(0.0827f, 0.0207f, 0.166f, 0.0395f, 0.248f, 0.0626f)
            curveToRelative(-0.0261f, 0.1937f, -0.123f, 0.3807f, -0.1389f, 0.5755f)
            curveToRelative(0.0639f, 0.0096f, 0.1294f, 0.0448f, 0.1885f, 0.0702f)
            curveToRelative(0.2056f, 0.0883f, 0.4213f, 0.2308f, 0.4901f, 0.4557f)
            curveToRelative(0.0897f, 0.2932f, -0.071f, 0.7503f, -0.4207f, 0.7789f)
            curveToRelative(0.0349f, 0.0378f, 0.0924f, 0.0599f, 0.1339f, 0.0912f)
            curveToRelative(0.0908f, 0.0682f, 0.1637f, 0.1541f, 0.2043f, 0.2611f)
            curveToRelative(0.0452f, 0.119f, 0.038f, 0.25f, 0.0111f, 0.3721f)
            curveTo(4.4275f, 4.2225f, 4.2722f, 4.4616f, 4.0035f, 4.5538f)
            curveTo(3.7557f, 4.6387f, 3.4706f, 4.5965f, 3.2196f, 4.5492f)
            curveTo(3.2037f, 4.7439f, 3.1068f, 4.9309f, 3.0807f, 5.1246f)
            curveTo(2.9652f, 5.1152f, 2.8401f, 5.0717f, 2.7285f, 5.0403f)
            lineTo(2.8135f, 4.698f)
            lineTo(2.8724f, 4.4698f)
            curveTo(2.8077f, 4.4564f, 2.7425f, 4.4397f, 2.6789f, 4.4216f)
            curveTo(2.66f, 4.4163f, 2.6198f, 4.3972f, 2.601f, 4.4064f)
            curveTo(2.5727f, 4.4203f, 2.564f, 4.52f, 2.5567f, 4.5492f)
            curveTo(2.5216f, 4.6896f, 2.49f, 4.8315f, 2.4507f, 4.9709f)
            lineTo(2.1034f, 4.8865f)
            curveTo(2.1293f, 4.6944f, 2.1947f, 4.4928f, 2.2473f, 4.3061f)
            curveTo(2.0187f, 4.2417f, 1.7698f, 4.2112f, 1.5478f, 4.1275f)
            curveTo(1.58f, 3.9922f, 1.6637f, 3.8594f, 1.7165f, 3.7306f)
            curveTo(1.817f, 3.7442f, 1.9916f, 3.8445f, 2.0835f, 3.7726f)
            curveTo(2.1268f, 3.7388f, 2.1362f, 3.6865f, 2.1487f, 3.6364f)
            lineTo(2.2144f, 3.3734f)
            curveTo(2.2884f, 3.0775f, 2.3612f, 2.7813f, 2.4352f, 2.4854f)
            lineToRelative(0.062f, -0.248f)
            curveToRelative(0.0121f, -0.0485f, 0.0298f, -0.0982f, 0.0206f, -0.1488f)
            curveToRelative(-0.0239f, -0.1324f, -0.1764f, -0.1568f, -0.2854f, -0.1818f)
            curveToRelative(-0.0255f, -0.0058f, -0.1041f, -0.0115f, -0.1185f, -0.0343f)
            curveToRelative(-0.0125f, -0.0198f, 0.0121f, -0.0751f, 0.0174f, -0.0964f)
            curveToRelative(0.0224f, -0.0895f, 0.054f, -0.1815f, 0.0663f, -0.2729f)
            lineTo(2.9021f, 1.6768f)
            lineTo(2.9871f, 1.3345f)
            lineTo(3.046f, 1.1063f)
            lineTo(3.3933f, 1.1906f)
            moveTo(2.9865f, 2.8625f)
            curveToRelative(0.1422f, 0.0294f, 0.2805f, 0.0788f, 0.4266f, 0.0889f)
            curveToRelative(0.0813f, 0.0056f, 0.1632f, 0.0078f, 0.2431f, -0.0126f)
            curveTo(3.8647f, 2.8854f, 3.9752f, 2.636f, 3.8386f, 2.4557f)
            curveTo(3.7399f, 2.3254f, 3.5606f, 2.264f, 3.4082f, 2.2243f)
            curveTo(3.3488f, 2.2089f, 3.2894f, 2.1946f, 3.2296f, 2.1811f)
            curveToRelative(-0.0166f, -0.0037f, -0.0512f, -0.0182f, -0.0667f, -0.0079f)
            curveToRelative(-0.0122f, 0.0082f, -0.0138f, 0.0313f, -0.0171f, 0.0443f)
            lineTo(3.1136f, 2.3465f)
            curveTo(3.0714f, 2.5154f, 3.0097f, 2.6901f, 2.9865f, 2.8625f)
            moveToRelative(-0.0857f, 0.3574f)
            curveToRelative(-0.0283f, 0.0209f, -0.0355f, 0.1198f, -0.0439f, 0.1536f)
            curveToRelative(-0.0359f, 0.1438f, -0.0707f, 0.2879f, -0.1067f, 0.4316f)
            lineToRelative(-0.031f, 0.124f)
            curveToRelative(-0.0032f, 0.0127f, -0.0128f, 0.0344f, -0.005f, 0.0468f)
            curveToRelative(0.0093f, 0.0148f, 0.0434f, 0.0169f, 0.0589f, 0.0208f)
            lineToRelative(0.1587f, 0.0397f)
            curveToRelative(0.1454f, 0.0362f, 0.2919f, 0.0564f, 0.4415f, 0.0564f)
            curveToRelative(0.1017f, 0f, 0.2154f, -0.0172f, 0.2977f, -0.0819f)
            curveTo(3.8152f, 3.8974f, 3.8287f, 3.6702f, 3.7054f, 3.5371f)
            curveTo(3.5756f, 3.3972f, 3.3753f, 3.3287f, 3.1948f, 3.2823f)
            curveTo(3.1224f, 3.2637f, 3.0497f, 3.2452f, 2.9766f, 3.2296f)
            curveToRelative(-0.0184f, -0.0039f, -0.0588f, -0.0222f, -0.0758f, -0.0097f)
            close()
          }
        }
        .build()

    return _Bitcoin!!
  }

@Suppress("ObjectPropertyName") private var _Bitcoin: ImageVector? = null

@Preview
@Composable
private fun Preview() {
  FDroidContent {
    Box(modifier = Modifier.padding(12.dp)) {
      Image(imageVector = Bitcoin, contentDescription = "")
    }
  }
}

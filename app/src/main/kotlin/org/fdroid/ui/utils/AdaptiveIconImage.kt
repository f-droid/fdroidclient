package org.fdroid.ui.utils

import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build.VERSION.SDK_INT
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap

@Composable
fun AdaptiveIconImage(
  @DrawableRes iconId: Int,
  modifier: Modifier = Modifier,
) {
  val res = LocalResources.current
  val context = LocalContext.current

  val bitmapPainter =
    remember(iconId) {
      val drawable = ResourcesCompat.getDrawable(res, iconId, context.theme)
      if (SDK_INT >= 26 && drawable is AdaptiveIconDrawable) {
        BitmapPainter(drawable.toBitmap().asImageBitmap())
      } else {
        null
      }
    }
  val painter = bitmapPainter ?: painterResource(iconId)

  Image(
    painter = painter,
    contentDescription = null,
    modifier = modifier,
  )
}

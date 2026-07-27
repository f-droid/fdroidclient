package org.fdroid.ui.settings

import android.content.ComponentName
import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.fdroid.R

sealed class AppIcon(
  val componentName: String,
  @get:DrawableRes val iconRes: Int,
  @get:StringRes val labelRes: Int,
) {

  fun getComponentName(context: Context): ComponentName {
    val applicationContext = context.applicationContext
    return ComponentName(applicationContext, "org.fdroid$componentName")
  }

  data object Default :
    AppIcon(
      componentName = ".IconActivity",
      iconRes = R.mipmap.ic_launcher,
      labelRes = R.string.app_name,
    )

  data object Calculator :
    AppIcon(
      componentName = ".IconCalculatorActivity",
      iconRes = R.mipmap.ic_calculator,
      labelRes = R.string.icon_calculator,
    )
}

val appIcons =
  listOf(
    AppIcon.Default,
    AppIcon.Calculator,
  )

package org.fdroid.fdroid.compose

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.accompanist.themeadapter.material.createMdcTheme
import org.fdroid.fdroid.Preferences
import org.fdroid.fdroid.R
import java.util.Locale

object ComposeUtils {
    @Composable
    fun FDroidContent(content: @Composable () -> Unit) {
        val context = LocalContext.current
        val layoutDirection = LocalLayoutDirection.current
        val (colors, typography, shapes) = createMdcTheme(
            context = context,
            layoutDirection = layoutDirection,
        )
        val newColors = (colors ?: MaterialTheme.colors).let { c ->
            if (!LocalInspectionMode.current && !c.isLight && Preferences.get().isPureBlack) {
                c.copy(background = Color.Black)
            } else c
        }
        MaterialTheme(
            colors = newColors,
            typography = typography?.let {
                it.copy(
                    // adapt letter-spacing to non-compose UI
                    body1 = it.body1.copy(letterSpacing = 0.em),
                    body2 = it.body2.copy(letterSpacing = 0.em),
                    // set caption style to match MDC
                    caption = it.caption.copy(
                        color = colorResource(id = R.color.fdroid_caption),
                        fontSize = 12.sp)
                )
            } ?: MaterialTheme.typography,
            shapes = shapes ?: MaterialTheme.shapes
        ) {
            Surface(content = content)
        }
    }

    @Composable
    fun FDroidButton(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        imageVector: ImageVector? = null,
    ) {
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(32.dp),
            modifier = modifier.heightIn(min = ButtonDefaults.MinHeight)
        ) {
            if (imageVector != null) {
                Icon(
                    imageVector = imageVector,
                    contentDescription = text,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            }
            Text(text = text.uppercase(Locale.getDefault()))
        }
    }

    @Composable
    fun FDroidOutlineButton(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        imageVector: ImageVector? = null,
    ) {
        OutlinedButton(
            onClick = onClick,
            shape = RoundedCornerShape(32.dp),
            modifier = modifier.heightIn(min = ButtonDefaults.MinHeight)
        ) {
            if (imageVector != null) {
                Icon(
                    imageVector = imageVector,
                    contentDescription = text,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            }
            Text(text = text.uppercase(Locale.getDefault()))
        }
    }

    /**
     * A tiny helper for consuming Activity lifecycle events.
     *
     * copied from https://stackoverflow.com/a/66807899
     *
     * There is also an official API for consuming lifecycle events. However at the time of writing
     * it's not stable and I also couldn't find any actually working code snippets demonstrating
     * it's use. "androidx.lifecycle:lifecycle-runtime-compose"
     */
    @Composable
    fun LifecycleEventListener(onEvent: (owner: LifecycleOwner, event: Lifecycle.Event) -> Unit) {
        val eventHandler = rememberUpdatedState(onEvent)
        val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)

        DisposableEffect(lifecycleOwner.value) {
            val lifecycle = lifecycleOwner.value.lifecycle
            val observer = LifecycleEventObserver { owner, event ->
                eventHandler.value(owner, event)
            }

            lifecycle.addObserver(observer)
            onDispose {
                lifecycle.removeObserver(observer)
            }
        }
    }

    /**
     * Composable that mimics MDC TextView with `@style/CaptionText`
     */
    @Composable
    fun CaptionText(text: String) {
        Text(
            text = text,
            style = MaterialTheme.typography.caption,
            modifier = Modifier.padding(0.dp, 16.dp, 0.dp, 4.dp)
        )
    }
}

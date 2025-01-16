package org.fdroid.fdroid.compose

import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.material.color.MaterialColors

object ComposeUtils {

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
            Text(text = text)
        }
    }

    @Composable
    fun FDroidOutlineButton(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        imageVector: ImageVector? = null,
        color: Color = MaterialTheme.colors.primary,
    ) {
        OutlinedButton(
            onClick = onClick,
            shape = RoundedCornerShape(32.dp),
            modifier = modifier.heightIn(min = ButtonDefaults.MinHeight),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        ) {
            if (imageVector != null) {
                Icon(
                    imageVector = imageVector,
                    contentDescription = text,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            }
            Text(text = text, maxLines = 1)
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
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(0.dp, 16.dp, 0.dp, 4.dp)
        )
    }
}

@Composable
@ColorInt
@ReadOnlyComposable
fun colorAttribute(@AttrRes attrColor: Int): Color {
    val color = MaterialColors.getColor(LocalContext.current, attrColor, 0)
    return Color(color)
}

package org.fdroid.fdroid.compose

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.google.accompanist.themeadapter.material.createMdcTheme
import org.fdroid.fdroid.Preferences
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
                // adapt letter-spacing to non-compose UI
                it.copy(
                    body1 = it.body1.copy(letterSpacing = 0.em),
                    body2 = it.body2.copy(letterSpacing = 0.em),
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

}

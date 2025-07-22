package org.fdroid.ui.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.fdroid.ui.theme.FDroidContent

val License: ImageVector
    get() {
        if (_License != null) {
            return _License!!
        }
        _License = ImageVector.Builder(
            name = "License",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(480f, 520f)
                quadToRelative(-50f, 0f, -85f, -35f)
                reflectiveQuadToRelative(-35f, -85f)
                reflectiveQuadToRelative(35f, -85f)
                reflectiveQuadToRelative(85f, -35f)
                reflectiveQuadToRelative(85f, 35f)
                reflectiveQuadToRelative(35f, 85f)
                reflectiveQuadToRelative(-35f, 85f)
                reflectiveQuadToRelative(-85f, 35f)
                moveToRelative(0f, 320f)
                lineTo(293f, 902f)
                quadToRelative(-20f, 7f, -36.5f, -5f)
                reflectiveQuadTo(240f, 865f)
                verticalLineToRelative(-254f)
                quadToRelative(-38f, -42f, -59f, -96f)
                reflectiveQuadToRelative(-21f, -115f)
                quadToRelative(0f, -134f, 93f, -227f)
                reflectiveQuadToRelative(227f, -93f)
                reflectiveQuadToRelative(227f, 93f)
                reflectiveQuadToRelative(93f, 227f)
                quadToRelative(0f, 61f, -21f, 115f)
                reflectiveQuadToRelative(-59f, 96f)
                verticalLineToRelative(254f)
                quadToRelative(0f, 20f, -16.5f, 32f)
                reflectiveQuadTo(667f, 902f)
                close()
                moveToRelative(0f, -200f)
                quadToRelative(100f, 0f, 170f, -70f)
                reflectiveQuadToRelative(70f, -170f)
                reflectiveQuadToRelative(-70f, -170f)
                reflectiveQuadToRelative(-170f, -70f)
                reflectiveQuadToRelative(-170f, 70f)
                reflectiveQuadToRelative(-70f, 170f)
                reflectiveQuadToRelative(70f, 170f)
                reflectiveQuadToRelative(170f, 70f)
                moveTo(320f, 801f)
                lineToRelative(160f, -41f)
                lineToRelative(160f, 41f)
                verticalLineToRelative(-124f)
                quadToRelative(-35f, 20f, -75.5f, 31.5f)
                reflectiveQuadTo(480f, 720f)
                reflectiveQuadToRelative(-84.5f, -11.5f)
                reflectiveQuadTo(320f, 677f)
                close()
                moveToRelative(160f, -62f)
            }
        }.build()
        return _License!!
    }

private var _License: ImageVector? = null

@Preview
@Composable
private fun Preview() {
    FDroidContent {
        Box(modifier = Modifier.padding(12.dp)) {
            Image(imageVector = License, contentDescription = "")
        }
    }
}

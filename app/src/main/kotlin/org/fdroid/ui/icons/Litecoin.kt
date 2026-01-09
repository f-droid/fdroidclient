package org.fdroid.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Litecoin: ImageVector
    get() {
        if (_Litecoin != null) return _Litecoin!!

        _Litecoin = ImageVector.Builder(
            name = "litecoin",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF000000))
            ) {
            }
            path(
                fill = SolidColor(Color(0xFF000000))
            ) {
            }
            path(
                fill = SolidColor(Color(0xFF000000))
            ) {
                moveTo(12f, 1f)
                arcTo(11f, 11f, 0f, true, false, 23f, 12f)
                arcTo(11f, 11f, 0f, false, false, 12f, 1f)
                close()
                moveToRelative(0.178277f, 11.361877f)
                lineToRelative(-1.14417f, 3.860909f)
                horizontalLineToRelative(6.119981f)
                arcToRelative(0.31398162f, 0.31398162f, 0f, false, true, 0.300677f, 0.401791f)
                lineToRelative(-0.532172f, 1.833333f)
                arcToRelative(0.42041606f, 0.42041606f, 0f, false, true, -0.404451f, 0.303339f)
                horizontalLineTo(7.170537f)
                lineTo(8.7510885f, 13.423561f)
                lineTo(7.0029028f, 13.955733f)
                lineTo(7.3887276f, 12.707789f)
                lineTo(9.1395743f, 12.175616f)
                lineTo(11.358733f, 4.6773101f)
                arcToRelative(0.4177552f, 0.4177552f, 0f, false, true, 0.401789f, -0.305999f)
                horizontalLineToRelative(2.368167f)
                arcToRelative(0.31398162f, 0.31398162f, 0f, false, true, 0.303338f, 0.3991292f)
                lineTo(12.569424f, 11.111273f)
                lineTo(14.31761f, 10.5791f)
                lineTo(13.942429f, 11.848331f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF000000))
            ) {
            }
        }.build()

        return _Litecoin!!
    }

@Suppress("ktlint:standard:backing-property-naming")
private var _Litecoin: ImageVector? = null

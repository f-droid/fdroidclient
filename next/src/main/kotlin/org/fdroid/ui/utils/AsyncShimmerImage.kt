package org.fdroid.ui.utils

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import org.fdroid.R
import org.fdroid.fdroid.ui.theme.FDroidContent

@Composable
fun AsyncShimmerImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    colorFilter: ColorFilter? = null,
    contentScale: ContentScale = ContentScale.Fit,
    error: Painter = painterResource(R.drawable.ic_repo_app_default),
    placeholder: Painter = error,
) {
    Box(modifier = modifier) {
        SubcomposeAsyncImage(
            model = model,
            loading = {
                Image(
                    painter = placeholder,
                    contentDescription = contentDescription,
                    colorFilter = colorFilter,
                    contentScale = contentScale,
                    modifier = Modifier
                        .matchParentSize()
                        .shimmer(),
                )
            },
            error = {
                Image(
                    painter = error,
                    contentDescription = contentDescription,
                    colorFilter = colorFilter,
                    contentScale = contentScale,
                    modifier = Modifier.matchParentSize(),
                )
            },
            colorFilter = colorFilter,
            contentScale = contentScale,
            contentDescription = contentDescription,
            modifier = Modifier.matchParentSize()
        )
    }
}

@Composable
@Preview
private fun Preview() {
    FDroidContent {
        Image(
            painter = painterResource(R.drawable.ic_repo_app_default),
            contentDescription = null,
            modifier = Modifier
                .size(128.dp)
                .shimmer(),
        )
    }
}

@Composable
fun Modifier.shimmer(): Modifier {
    val shimmerColors = listOf(
        Color.Transparent,
        MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
        Color.Transparent
    )
    val transition = rememberInfiniteTransition(label = "Shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -400f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2000,
                easing = FastOutLinearInEasing,
            )
        ),
        label = "Translate"
    )
    return this.drawWithCache {
        val brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(translateAnim, 0f),
            end = Offset(translateAnim + size.width / 1.5f, size.height)
        )
        onDrawWithContent {
            drawContent()
            drawRect(
                brush = brush,
                size = size,
            )
        }
    }
}

package org.fdroid.ui.details

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.utils.AsyncShimmerImage
import org.fdroid.ui.utils.testApp

@Composable
fun Screenshots(isMetered: Boolean, phoneScreenshots: List<Any>) {
    var showEvenWhenMetered by remember { mutableStateOf(false) }
    if (isMetered && !showEvenWhenMetered) Box(
        contentAlignment = Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .padding(horizontal = 16.dp)
            .clickable { showEvenWhenMetered = true }
    ) {
        Image(
            painterResource(R.drawable.screenshots_placeholder),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .semantics { hideFromAccessibility() }
        )
        ElevatedButton(
            onClick = { showEvenWhenMetered = true },
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.screenshots_metered),
                textAlign = TextAlign.Center,
            )
        }
    } else {
        Screenshots(phoneScreenshots)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun Screenshots(phoneScreenshots: List<Any>) {
    val carouselState = rememberCarouselState { phoneScreenshots.size }
    var showScreenshot by remember { mutableStateOf<Int?>(null) }
    val screenshotIndex = showScreenshot
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (screenshotIndex != null) ModalBottomSheet(
        onDismissRequest = { showScreenshot = null },
        sheetState = sheetState,
        properties = ModalBottomSheetProperties(),
    ) {
        val pagerState = rememberPagerState(
            initialPage = screenshotIndex,
            pageCount = { phoneScreenshots.size },
        )
        Surface {
            HorizontalPager(state = pagerState) { page ->
                AsyncShimmerImage(
                    model = phoneScreenshots[page],
                    contentDescription = "",
                    contentScale = ContentScale.Fit,
                    placeholder = rememberVectorPainter(Icons.Default.Image),
                    error = rememberVectorPainter(Icons.Default.Error),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
    HorizontalUncontainedCarousel(
        state = carouselState,
        itemWidth = 120.dp,
        itemSpacing = 2.dp,
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(vertical = 8.dp)
    ) { index ->
        AsyncShimmerImage(
            model = phoneScreenshots[index],
            contentDescription = "",
            contentScale = ContentScale.Fit,
            placeholder = rememberVectorPainter(Icons.Default.Image),
            error = rememberVectorPainter(Icons.Default.Error),
            modifier = Modifier
                .size(120.dp, 240.dp)
                .clickable {
                    showScreenshot = index
                }
        )
    }
}

@Preview
@Composable
private fun Preview() {
    FDroidContent {
        Screenshots(false, testApp.phoneScreenshots)
    }
}

@Preview(widthDp = 300)
@Composable
private fun PreviewMetered() {
    FDroidContent {
        Screenshots(true, testApp.phoneScreenshots)
    }
}

package org.fdroid.basic.ui.main.discover

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.fdroid.basic.MainViewModel
import org.fdroid.basic.R
import org.fdroid.fdroid.ui.theme.FDroidContent

@Composable
fun CategoryCarousel(
    onTitleTap: () -> Unit,
    onCategoryTap: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel(),
) {
    CategoryCarousel(viewModel.categories, modifier, onTitleTap, onCategoryTap)
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CategoryCarousel(
    categories: List<Pair<String, Int>>,
    modifier: Modifier = Modifier,
    onTitleTap: () -> Unit,
    onCategoryTap: (String) -> Unit,
) {
    val carouselState = rememberCarouselState { categories.size }
    Column(modifier = modifier) {
        Row(
            verticalAlignment = CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onTitleTap)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = "Categories",
                style = MaterialTheme.typography.headlineSmall,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
            )
        }
        HorizontalUncontainedCarousel(
            state = carouselState,
            itemWidth = 80.dp,
            itemSpacing = 8.dp,
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) { index ->
            CategoryBox(categories[index], onCategoryTap)
        }
    }
}

@Composable
fun CategoryBox(category: Pair<String, Int>, onCategoryTap: (String) -> Unit) {
    Column(
        verticalArrangement = spacedBy(8.dp),
        modifier = Modifier
            .padding(8.dp)
            .clickable { onCategoryTap(category.first) },
    ) {
        Image(
            painter = painterResource(category.second),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .size(76.dp),
        )
        Text(
            text = category.first,
            style = MaterialTheme.typography.bodySmall,
            minLines = 2,
            maxLines = 2,
        )
    }
}

@Preview
@Composable
fun CategoryCarouselPreview() {
    val categories = listOf(
        Pair(stringResource(R.string.category_Time), R.drawable.category_theming),
        Pair(stringResource(R.string.category_Games), R.drawable.category_games),
        Pair(stringResource(R.string.category_Money), R.drawable.category_money),
        Pair(stringResource(R.string.category_Reading), R.drawable.category_reading),
        Pair(stringResource(R.string.category_Theming), R.drawable.category_theming),
        Pair(stringResource(R.string.category_Connectivity), R.drawable.category_connectivity),
    )
    FDroidContent {
        CategoryCarousel(categories, onTitleTap = {}) {}
    }
}

package org.fdroid.ui.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.ui.FDroidContent

private val chipHeight = 36.dp

@Composable
fun CategoryChip(
    categoryItem: CategoryItem,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    FilterChip(
        onClick = onSelected,
        leadingIcon = {
            if (selected) Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.filter_selected),
            ) else Icon(
                imageVector = categoryItem.imageVector,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { hideFromAccessibility() },
            )
        },
        label = {
            Text(
                categoryItem.name,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        selected = selected,
        modifier = modifier.padding(horizontal = 4.dp).height(chipHeight)
    )
}

@Composable
fun CategoryChip(
    categoryItem: CategoryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AssistChip(
        onClick = onClick,
        leadingIcon = {
            Icon(
                imageVector = categoryItem.imageVector,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { hideFromAccessibility() },
            )
        },
        label = {
            Text(
                text = categoryItem.name,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        modifier = modifier.padding(horizontal = 4.dp).height(chipHeight)
    )
}

@Preview
@Composable
fun CategoryCardPreview() {
    FDroidContent {
        Column {
            CategoryChip(
                CategoryItem("VPN & Proxy", "VPN & Proxy"),
                selected = true,
                onSelected = {},
            )
            CategoryChip(
                CategoryItem("VPN & Proxy", "VPN & Proxy"),
                selected = false,
                onSelected = {},
            )
            CategoryChip(
                CategoryItem("VPN & Proxy", "VPN & Proxy"),
                onClick = {},
            )
        }
    }
}

/**
 * More similar to how multiple category chips are shown in the main category list.
 * Used to show spacing between items, specifically with regards to how Android specifies
 * a minimum height for interactive elements. This leads to the conclusion that if we make
 * the chips too short, we get an artificially large vertical gap. Hence why we set
 * the height explicitly on category chips to make them align with this minimum height.
 */
@Preview
@Composable
fun CategoryCardFlowRowPreview() {
    val categories = listOf(
        CategoryItem("Cloud Storage & File Sync", "Cloud Storage & File Sync"),
        CategoryItem("Connectivity", "Connectivity"),
        CategoryItem("Development", "Development"),
        CategoryItem("doesn't exist", "Foo bar"),
    )

    FDroidContent {
        FlowRow(
            horizontalArrangement = Arrangement.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .padding(24.dp, 8.dp, 4.dp, 20.dp)
        ) {
            categories.map { category ->
                CategoryChip(category, onClick = {})
            }
        }
    }
}

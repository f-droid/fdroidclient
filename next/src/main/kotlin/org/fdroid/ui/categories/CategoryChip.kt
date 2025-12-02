package org.fdroid.ui.categories

import androidx.compose.foundation.layout.Column
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
import org.fdroid.fdroid.ui.theme.FDroidContent

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
        modifier = modifier.padding(horizontal = 4.dp)
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
        modifier = modifier.padding(horizontal = 4.dp)
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

package org.fdroid.ui.lists

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.ui.categories.ChipFlowRow

@Composable
fun FilterSection(
    icon: ImageVector,
    title: String,
    initiallyExpanded: Boolean,
    onCollapsed: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (FlowRowScope.() -> Unit),
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val onExpand = {
        expanded = !expanded
        if (!expanded) onCollapsed()
    }
    FilterHeader(icon = icon, text = title, modifier = Modifier.clickable { onExpand() }) {
        IconButton(onClick = onExpand) {
            Icon(
                imageVector = if (expanded) {
                    Icons.Default.Remove
                } else {
                    Icons.Default.Add
                },
                contentDescription = if (expanded) {
                    stringResource(R.string.collapse)
                } else {
                    stringResource(R.string.expand)
                },
            )
        }
    }
    AnimatedVisibility(expanded) {
        ChipFlowRow(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            content()
        }
    }
}

@Composable
fun FilterHeader(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    trailingContent: @Composable () -> Unit = {}
) {
    Row(
        horizontalArrangement = spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { hideFromAccessibility() },
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        trailingContent()
    }
}

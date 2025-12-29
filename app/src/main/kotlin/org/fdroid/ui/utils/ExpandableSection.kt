package org.fdroid.ui.utils

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun ExpandableSection(
    icon: Painter?,
    title: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = LocalInspectionMode.current,
    content: @Composable () -> Unit,
) {
    var sectionExpanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = spacedBy(8.dp),
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clickable(onClick = { sectionExpanded = !sectionExpanded })
        ) {
            if (icon != null) Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.semantics { hideFromAccessibility() },
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { sectionExpanded = !sectionExpanded }) {
                ExpandIconArrow(sectionExpanded)
            }
        }
        AnimatedVisibility(
            visible = sectionExpanded,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            content()
        }
    }
}

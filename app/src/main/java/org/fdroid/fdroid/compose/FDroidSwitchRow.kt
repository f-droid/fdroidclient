package org.fdroid.fdroid.compose

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.fdroid.compose.ComposeUtils.FDroidContent

@Composable
fun FDroidSwitchRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val contentAlpha = if (enabled) 1f else ContentAlpha.disabled
    Row(
        horizontalArrangement = spacedBy(8.dp),
        verticalAlignment = CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.body2,
            modifier = Modifier
                .alpha(contentAlpha)
                .weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colors.secondary,
                uncheckedThumbColor = Color.Gray,
            )
        )
    }
}

@Composable
@Preview
fun FDroidSwitchRowPreview() {
    FDroidContent {
        FDroidSwitchRow(
            text = "Important setting",
            checked = true,
        )
    }
}

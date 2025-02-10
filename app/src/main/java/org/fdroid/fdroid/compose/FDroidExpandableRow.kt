package org.fdroid.fdroid.compose

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.fdroid.R
import org.fdroid.fdroid.ui.theme.FDroidContent

@Composable
fun FDroidExpandableRow(
    text: String,
    imageVectorStart: ImageVector,
    expanded: Boolean = false,
    content: @Composable () -> Unit,
) {
    var expandedInternal by rememberSaveable { mutableStateOf(expanded) }

    val imageVectorEnd = if (expandedInternal) {
        Icons.Default.ExpandLess
    } else {
        Icons.Default.ExpandMore
    }

    Column {
        // header
        Row(
            horizontalArrangement = spacedBy(8.dp),
            verticalAlignment = CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = expandedInternal,
                    role = Role.Button,
                    onValueChange = { expandedInternal = !expandedInternal },
                )
                // add padding after toggleable to have a larger touch area
                .padding(vertical = 16.dp),
        ) {
            Icon(
                imageVector = imageVectorStart,
                contentDescription = text,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Text(
                text = text,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = imageVectorEnd,
                contentDescription = stringResource(R.string.expand),
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
        }
        // content
        if (expandedInternal) {
            content()
        }
    }
}

@Composable
@Preview
fun FDroidExpandableRowPreview() {
    FDroidContent {
        FDroidExpandableRow(
            text = "Permissions",
            imageVectorStart = Icons.Default.Lock,
            expanded = true,
        ) {
            Text("Some content here")
        }
    }
}

package org.fdroid.basic.ui.main.details

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UpdateDisabled
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun AppDetailsMenu(expanded: Boolean, onDismiss: () -> Unit) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        DropdownMenuItem(
            leadingIcon = {
                Icon(Icons.Default.UpdateDisabled, null)
            },
            text = { Text("Ignore all updates") },
            trailingIcon = {
                Checkbox(false, null)
            },
            onClick = { /* Do something... */ },
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(Icons.Default.UpdateDisabled, null)
            },
            text = { Text("Ignore this updates") },
            trailingIcon = {
                Checkbox(false, null)
            },
            onClick = { /* Do something... */ },
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(Icons.Default.Preview, null)
            },
            text = { Text("Allow beta updates") },
            trailingIcon = {
                Checkbox(false, null)
            },
            onClick = { /* Do something... */ },
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(Icons.Default.Share, null)
            },
            text = { Text("Share APK") },
            onClick = { /* Do something... */ },
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(Icons.Default.Delete, null)
            },
            text = { Text("Uninstall app") },
            onClick = { /* Do something... */ },
        )
    }
}

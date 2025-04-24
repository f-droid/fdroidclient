package org.fdroid.basic.ui.main.apps

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AppSearchInputField(
    searchBarState: SearchBarState,
    textFieldState: TextFieldState,
    toggleFilter: () -> Unit,
    showFilterBadge: Boolean
) {
    val scope = rememberCoroutineScope()
    SearchBarDefaults.InputField(
        modifier = Modifier,
        searchBarState = searchBarState,
        textFieldState = textFieldState,
        onSearch = {
            scope.launch { searchBarState.animateToCollapsed() }
        },
        placeholder = { Text("Search...") },
        leadingIcon = {
            if (searchBarState.currentValue == SearchBarValue.Expanded) {
                IconButton(
                    onClick = { scope.launch { searchBarState.animateToCollapsed() } }
                ) {
                    Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                }
            } else {
                Icon(Icons.Default.Search, contentDescription = null)
            }
        },
        trailingIcon = {
            if (searchBarState.currentValue == SearchBarValue.Expanded) {
                IconButton(onClick = { textFieldState.setTextAndPlaceCursorAtEnd("") }) {
                    Icon(
                        Icons.Filled.Clear,
                        contentDescription = null,
                    )
                }
            } else Row {
                IconButton(onClick = toggleFilter) {
                    BadgedBox(badge = {
                        if (showFilterBadge) Badge(containerColor = MaterialTheme.colorScheme.secondary)
                    }) {
                        Icon(
                            Icons.Filled.FilterList,
                            contentDescription = null,
                        )
                    }
                }
            }
        }
    )
}

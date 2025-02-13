package org.fdroid.basic.ui.main.apps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopSearchBar
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AppsSearch(
    onlyInstalledApps: Boolean,
    addedCategories: List<String>,
    addedRepos: List<String>,
    toggleFilter: () -> Unit,
) {
    val textFieldState = rememberTextFieldState()
    val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
    val searchBarState = rememberSearchBarState()
    val scope = rememberCoroutineScope()
    val inputField = @Composable {
        AppSearchInputField(
            searchBarState = searchBarState,
            textFieldState = textFieldState,
            toggleFilter = toggleFilter,
            showFilterBadge = addedRepos.isNotEmpty() || addedCategories.isNotEmpty() ||
                onlyInstalledApps,
        )
    }
    TopSearchBar(
        modifier = Modifier,
        state = searchBarState,
        scrollBehavior = scrollBehavior,
        windowInsets = WindowInsets.systemBars,
        inputField = inputField,
    )
    ExpandedFullScreenSearchBar(
        state = searchBarState,
        inputField = inputField,
    ) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            repeat(4) { idx ->
                val resultText = "Suggestion $idx"
                ListItem(headlineContent = { Text(resultText) },
                    supportingContent = { Text("Additional info") },
                    leadingContent = {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .clickable {
                            textFieldState.setTextAndPlaceCursorAtEnd(resultText)
                            scope.launch { searchBarState.animateToCollapsed() }
                        }
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp,
                            vertical = 4.dp
                        )
                )
            }
        }
    }
}

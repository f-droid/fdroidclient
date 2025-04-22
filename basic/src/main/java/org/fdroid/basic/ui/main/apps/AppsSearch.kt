package org.fdroid.basic.ui.main.apps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExpandedDockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.TopSearchBar
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AppsSearch(
    showFilterBadge: Boolean,
    toggleFilter: () -> Unit,
    onItemClick: (AppNavigationItem) -> Unit,
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
            showFilterBadge = showFilterBadge,
        )
    }
    TopSearchBar(
        modifier = Modifier,
        state = searchBarState,
        scrollBehavior = scrollBehavior,
        windowInsets = WindowInsets.systemBars,
        inputField = inputField,
    )
    ExpandedDockedSearchBar(
        state = searchBarState,
        inputField = inputField,
    ) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            repeat(4) { i ->
                val navItem = AppNavigationItem(
                    packageName = "$i",
                    name = "App $i",
                    summary = "Summary of the app",
                    isNew = false,
                )
                AppItem(
                    name = navItem.name,
                    summary = navItem.summary,
                    isNew = navItem.isNew,
                    isSelected = false,
                    modifier = Modifier
                        .clickable {
                            scope.launch {
                                searchBarState.animateToCollapsed()
                                onItemClick(navItem)
                            }
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

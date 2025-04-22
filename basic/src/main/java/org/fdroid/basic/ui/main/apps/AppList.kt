package org.fdroid.basic.ui.main.apps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AppList(
    apps: List<AppNavigationItem>,
    currentItem: AppNavigationItem?,
    onItemClick: (AppNavigationItem) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.then(
            if (currentItem == null) Modifier
            else Modifier.selectableGroup()
        ),
    ) {
        items(apps) { navItem ->
            val isSelected = currentItem?.packageName == navItem.packageName
            val interactionModifier = if (currentItem == null) {
                Modifier.clickable(
                    onClick = { onItemClick(navItem) }
                )
            } else {
                Modifier.selectable(
                    selected = isSelected,
                    onClick = { onItemClick(navItem) }
                )
            }
            val modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .then(interactionModifier)
            AppItem(navItem.name, navItem.summary, navItem.isNew, isSelected, modifier)
        }
        item {
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
        }
    }
}

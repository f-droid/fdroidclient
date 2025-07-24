package org.fdroid.ui.repositories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
fun RepositoriesList(
    repositories: List<RepositoryItem>,
    currentRepositoryId: Long?,
    onRepositorySelected: (RepositoryItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = spacedBy(8.dp),
        modifier = modifier.then(
            if (currentRepositoryId == null) Modifier
            else Modifier.selectableGroup()
        ),
    ) {
        items(repositories) { repoItem ->
            val isSelected = currentRepositoryId == repoItem.repoId
            val interactionModifier = if (currentRepositoryId == null) {
                Modifier.clickable(
                    onClick = { onRepositorySelected(repoItem) }
                )
            } else {
                Modifier.selectable(
                    selected = isSelected,
                    onClick = { onRepositorySelected(repoItem) }
                )
            }
            RepositoryRow(
                repoItem = repoItem,
                isSelected = isSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(interactionModifier),
            )
        }
    }
}

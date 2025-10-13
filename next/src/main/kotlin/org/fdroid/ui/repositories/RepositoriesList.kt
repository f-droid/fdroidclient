package org.fdroid.ui.repositories

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import org.fdroid.ui.utils.DraggableItem
import org.fdroid.ui.utils.dragContainer
import org.fdroid.ui.utils.rememberDragDropState

@Composable
fun RepositoriesList(
    info: RepositoryInfo,
    modifier: Modifier = Modifier,
) {
    val repositories = info.model.repositories ?: return
    val currentRepositoryId = info.currentRepositoryId
    val listState = rememberLazyListState()
    val dragDropState = rememberDragDropState(
        lazyListState = listState,
        onMove = info::onRepositoryMoved,
        onEnd = { from, to ->
            from as? Long ?: error("from $from was not a repoId")
            to as? Long ?: error("to $to was not a repoId")
            info.onRepositoriesFinishedMoving(from, to)
        },
    )
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = spacedBy(8.dp),
        modifier = modifier
            .then(
                if (repositories.size > 1) Modifier.dragContainer(dragDropState)
                else Modifier
            )
            .then(
                if (currentRepositoryId == null) Modifier
                else Modifier.selectableGroup()
            ),
    ) {
        itemsIndexed(
            items = repositories,
            key = { _, item -> item.repoId },
        ) { index, repoItem ->
            val isSelected = currentRepositoryId == repoItem.repoId
            val interactionModifier = if (currentRepositoryId == null) {
                Modifier.clickable(
                    onClick = { info.onRepositorySelected(repoItem) }
                )
            } else {
                Modifier.selectable(
                    selected = isSelected,
                    onClick = { info.onRepositorySelected(repoItem) }
                )
            }
            DraggableItem(dragDropState, index) { isDragging ->
                val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp)
                RepositoryRow(
                    repoItem = repoItem,
                    isSelected = isSelected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(interactionModifier)
                        .let {
                            if (isDragging) it.dropShadow(
                                shape = RoundedCornerShape(4.dp),
                                shadow = Shadow(
                                    radius = 8.dp,
                                    offset = DpOffset(x = elevation, elevation)
                                )
                            ) else it
                        }
                )
            }
        }
    }
}

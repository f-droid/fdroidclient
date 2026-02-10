package org.fdroid.ui.history

import org.fdroid.history.HistoryEvent

data class HistoryItem(
    val event: HistoryEvent,
    val iconModel: Any,
)

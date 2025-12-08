package org.fdroid.ui.discover

class AppDiscoverItem(
    val packageName: String,
    val name: String,
    val isInstalled: Boolean,
    val imageModel: Any? = null,
    val lastUpdated: Long = -1,
)

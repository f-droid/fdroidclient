package org.fdroid.ui.lists

data class AppListItem(
  val repoId: Long,
  val packageName: String,
  val name: String,
  val summary: String,
  val lastUpdated: Long,
  val isInstalled: Boolean,
  val isCompatible: Boolean,
  val iconModel: Any? = null,
  val categoryIds: Set<String>? = null,
  val antiFeatureIds: Set<String> = emptySet(),
)

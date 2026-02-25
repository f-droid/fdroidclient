package org.fdroid.ui.lists

import kotlinx.serialization.Serializable

@Serializable
sealed class AppListType {
  abstract val title: String

  @Serializable data class New(override val title: String) : AppListType()

  @Serializable data class RecentlyUpdated(override val title: String) : AppListType()

  @Serializable data class MostDownloaded(override val title: String) : AppListType()

  @Serializable data class All(override val title: String) : AppListType()

  @Serializable
  data class Category(override val title: String, val categoryId: String) : AppListType()

  @Serializable data class Repository(override val title: String, val repoId: Long) : AppListType()

  @Serializable
  data class Author(override val title: String, val authorName: String) : AppListType()
}

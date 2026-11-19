package org.fdroid.ui.discover

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import org.fdroid.R
import org.fdroid.ui.categories.CategoryList
import org.fdroid.ui.categories.CategoryListSkeleton
import org.fdroid.ui.lists.AppListType

@Composable
fun DiscoverContent(
  discoverModel: LoadedDiscoverModel,
  onListTap: (AppListType) -> Unit,
  onAppTap: (AppDiscoverItem) -> Unit,
  onNav: (NavKey) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier) {
    val listNew = AppListType.New(stringResource(R.string.app_list_new))
    DiscoverSection(
      data = discoverModel.newApps,
      isEmpty = { it.isEmpty() },
      modifier = Modifier.padding(bottom = 18.dp),
      placeholder = { AppCarouselSkeleton(listNew.title) },
    ) { apps ->
      AppCarousel(
        title = listNew.title,
        apps = apps,
        onTitleTap = { onListTap(listNew) },
        onAppTap = onAppTap,
      )
    }

    val listRecentlyUpdated =
      AppListType.RecentlyUpdated(stringResource(R.string.app_list_recently_updated))
    DiscoverSection(
      data = discoverModel.recentlyUpdatedApps,
      isEmpty = { it.isEmpty() },
      modifier = Modifier.padding(bottom = 18.dp),
      placeholder = { AppCarouselSkeleton(listRecentlyUpdated.title) },
    ) { apps ->
      AppCarousel(
        title = listRecentlyUpdated.title,
        apps = apps,
        onTitleTap = { onListTap(listRecentlyUpdated) },
        onAppTap = onAppTap,
      )
    }

    val listMostDownloaded =
      AppListType.MostDownloaded(stringResource(R.string.app_list_most_downloaded))
    DiscoverSection(
      data = discoverModel.mostDownloadedApps,
      isEmpty = { it.isEmpty() },
      modifier = Modifier.padding(bottom = 18.dp),
      placeholder = { AppCarouselSkeleton(listMostDownloaded.title) },
    ) { apps ->
      AppCarousel(
        title = listMostDownloaded.title,
        apps = apps,
        onTitleTap = { onListTap(listMostDownloaded) },
        onAppTap = onAppTap,
      )
    }

    DiscoverSection(
      data = discoverModel.categories,
      isEmpty = { it.isEmpty() },
      placeholder = { CategoryListSkeleton() },
    ) { categories ->
      CategoryList(categoryMap = categories, onNav = onNav)
    }
  }
}

@Composable
private fun <T> DiscoverSection(
  data: T?,
  isEmpty: (T) -> Boolean,
  modifier: Modifier = Modifier,
  placeholder: @Composable () -> Unit,
  content: @Composable (T) -> Unit,
) {
  AnimatedContent(
    targetState = data,
    transitionSpec = {
      (fadeIn(animationSpec = tween(500, easing = FastOutSlowInEasing)) togetherWith
          fadeOut(animationSpec = tween(500, easing = FastOutSlowInEasing)))
        .using(SizeTransform { _, _ -> tween(500, easing = FastOutSlowInEasing) })
    },
    contentKey = { value ->
      when {
        value == null -> SectionState.Loading
        isEmpty(value) -> SectionState.Empty
        else -> SectionState.Content
      }
    },
    label = "Discover section",
  ) { value ->
    when {
      value == null -> Box(modifier) { placeholder() }
      !isEmpty(value) -> Box(modifier) { content(value) }
    }
  }
}

private enum class SectionState {
  Loading,
  Empty,
  Content,
}

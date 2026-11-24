package org.fdroid.ui.discover

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.SpaceBetween
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.utils.AsyncShimmerImage
import org.fdroid.ui.utils.InstalledBadge
import org.fdroid.ui.utils.Names
import org.fdroid.ui.utils.SkeletonBlock

@Composable
fun AppCarousel(
  title: String,
  apps: List<AppDiscoverItem>,
  modifier: Modifier = Modifier,
  onTitleTap: () -> Unit,
  onAppTap: (AppDiscoverItem) -> Unit,
) {
  Column(verticalArrangement = spacedBy(10.dp), modifier = modifier) {
    Row(
      verticalAlignment = CenterVertically,
      horizontalArrangement = SpaceBetween,
      modifier =
        Modifier.fillMaxWidth().clickable(onClick = onTitleTap).padding(horizontal = 16.dp),
    ) {
      Text(text = title, style = MaterialTheme.typography.titleMediumEmphasized, fontSize = 20.sp)
      IconButton(
        onClick = onTitleTap,
        shape = CircleShape,
        colors =
          IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
          ),
        modifier = Modifier.size(48.dp).padding(6.dp),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowForward,
          modifier = Modifier.semantics { hideFromAccessibility() },
          contentDescription = null,
        )
      }
    }
    LazyRow(
      contentPadding = PaddingValues(horizontal = 16.dp),
      horizontalArrangement = spacedBy(16.dp),
    ) {
      items(apps, key = { it.packageName }) { app -> AppBox(app, onAppTap) }
    }
  }
}

@Composable
fun AppBox(app: AppDiscoverItem, onAppTap: (AppDiscoverItem) -> Unit) {
  Column(
    verticalArrangement = spacedBy(8.dp),
    modifier = Modifier.width(80.dp).clickable { onAppTap(app) },
  ) {
    BadgedBox(badge = { if (app.isInstalled) InstalledBadge() }) {
      AsyncShimmerImage(
        model = app.imageModel,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier =
          Modifier.size(76.dp).clip(MaterialTheme.shapes.large).semantics {
            hideFromAccessibility()
          },
      )
    }
    AppName(app.name)
  }
}

@Composable
private fun AppName(name: String, modifier: Modifier = Modifier) {
  Text(
    text = name,
    style = MaterialTheme.typography.bodySmall,
    minLines = 2,
    maxLines = 2,
    lineHeight = 14.sp,
    modifier = modifier,
  )
}

@Composable
fun AppCarouselSkeleton(title: String) {
  Column(verticalArrangement = spacedBy(10.dp), modifier = Modifier.progressSemantics()) {
    Row(
      verticalAlignment = CenterVertically,
      horizontalArrangement = SpaceBetween,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(min = 48.dp),
    ) {
      Text(text = title, style = MaterialTheme.typography.titleMediumEmphasized, fontSize = 20.sp)
    }
    Row(
      horizontalArrangement = spacedBy(16.dp),
      modifier = Modifier.fillMaxWidth().clipToBounds().padding(horizontal = 16.dp),
    ) {
      repeat(5) {
        Column(verticalArrangement = spacedBy(8.dp), modifier = Modifier.width(80.dp)) {
          SkeletonBlock(Modifier.size(76.dp), MaterialTheme.shapes.large)
          Box(modifier = Modifier.width(80.dp)) {
            AppName(" ", Modifier.alpha(0f).semantics { hideFromAccessibility() })
            Column(
              verticalArrangement = spacedBy(4.dp),
              modifier = Modifier.matchParentSize(),
            ) {
              SkeletonBlock(Modifier.width(75.dp).height(10.dp))
              SkeletonBlock(Modifier.width(54.dp).height(10.dp))
            }
          }
        }
      }
    }
  }
}

@Composable
@PreviewLightDark
private fun AppCarouselPreview() {
  val apps =
    listOf(
      AppDiscoverItem("1", Names.randomName, false),
      AppDiscoverItem("2", Names.randomName, true),
      AppDiscoverItem("3", Names.randomName, false),
      AppDiscoverItem("4", Names.randomName, false),
      AppDiscoverItem("5", Names.randomName, false),
    )
  FDroidContent { AppCarousel("Preview Apps", apps, onTitleTap = {}) {} }
}

@Composable
@PreviewLightDark
private fun AppCarouselLoadingPreview() {
  FDroidContent { AppCarouselSkeleton("Preview Apps") }
}

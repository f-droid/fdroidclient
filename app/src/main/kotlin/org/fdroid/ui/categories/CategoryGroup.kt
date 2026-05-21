package org.fdroid.ui.categories

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.ui.graphics.vector.ImageVector
import org.fdroid.R

data class CategoryGroup(
  val id: String,
  @get:StringRes val name: Int,
  @get:StringRes val summary: Int,
  val imageVector: ImageVector,
)

object CategoryGroups {
  val communication =
    CategoryGroup(
      id = "communication",
      name = R.string.category_group_communication,
      summary = R.string.category_group_summary_communication,
      imageVector = Icons.Default.Forum,
    )
  val device =
    CategoryGroup(
      id = "device",
      name = R.string.category_group_device,
      summary = R.string.category_group_summary_device,
      imageVector = Icons.Default.PhoneAndroid,
    )
  val games = CategoryGroup(
    id = "games",
    name = R.string.category_group_games,
    summary = R.string.category_group_summary_games,
    imageVector = Icons.Default.SportsEsports,
  )
  val interests =
    CategoryGroup(
      id = "interests",
      name = R.string.category_group_interests,
      summary = R.string.category_group_summary_interests,
      imageVector = Icons.Default.FavoriteBorder,
    )
  val media =
    CategoryGroup(
      id = "media",
      name = R.string.category_group_media,
      summary = R.string.category_group_summary_media,
      imageVector = Icons.Default.VideogameAsset,
    )
  val misc =
    CategoryGroup(
      id = "misc",
      name = R.string.category_group_misc,
      summary = R.string.category_group_summary_misc,
      imageVector = Icons.Default.Category,
    )
  val network =
    CategoryGroup(
      id = "network",
      name = R.string.category_group_network,
      summary = R.string.category_group_summary_network,
      imageVector = Icons.Default.NetworkCheck,
    )
  val productivity =
    CategoryGroup(
      id = "productivity",
      name = R.string.category_group_productivity,
      summary = R.string.category_group_summary_productivity,
      imageVector = Icons.Default.Factory,
    )
  val storage =
    CategoryGroup(
      id = "storage",
      name = R.string.category_group_storage,
      summary = R.string.category_group_summary_storage,
      imageVector = Icons.Default.SdStorage,
    )
  val tools =
    CategoryGroup(
      id = "tools",
      name = R.string.category_group_tools,
      summary = R.string.category_group_summary_tools,
      imageVector = Icons.Default.Construction,
    )
  val wallets =
    CategoryGroup(
      id = "wallets",
      name = R.string.category_group_wallets,
      summary = R.string.category_group_summary_wallets,
      imageVector = Icons.Default.Wallet,
    )
}

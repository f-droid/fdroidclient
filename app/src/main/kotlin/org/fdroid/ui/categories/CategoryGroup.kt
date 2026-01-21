package org.fdroid.ui.categories

import androidx.annotation.StringRes
import org.fdroid.R

data class CategoryGroup(
    val id: String,
    @get:StringRes
    val name: Int,
)

object CategoryGroups {
    val productivity = CategoryGroup("productivity", R.string.category_group_productivity)
    val tools = CategoryGroup("tools", R.string.category_group_tools)
    val wallets = CategoryGroup("wallets", R.string.category_group_wallets)
    val media = CategoryGroup("media", R.string.category_group_media)
    val communication = CategoryGroup("communication", R.string.category_group_communication)
    val device = CategoryGroup("device", R.string.category_group_device)
    val network = CategoryGroup("network", R.string.category_group_network)
    val storage = CategoryGroup("storage", R.string.category_group_storage)
    val interests = CategoryGroup("interests", R.string.category_group_interests)
    val misc = CategoryGroup("misc", R.string.category_group_misc)
}

package org.fdroid.ui.categories

import androidx.compose.foundation.layout.FlexBox
import androidx.compose.foundation.layout.FlexBoxScope
import androidx.compose.foundation.layout.FlexWrap
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.ui.FDroidContent

val chipHeight = 36.dp

/**
 * When presenting a list of chips (e.g. categories, repositories, sort criteria), use this to
 * ensure appropriate spacing between elements. Make sure to set the height of your chips to
 * [chipHeight] so that all chips match.
 */
@Composable
fun ChipFlexBox(modifier: Modifier = Modifier, content: @Composable FlexBoxScope.() -> Unit) {
  FlexBox(
    modifier = modifier.padding(8.dp),
    config = {
      wrap(FlexWrap.Wrap)
      gap(8.dp)
    },
    content = content,
  )
}

@Preview
@Composable
fun ChipFlexBoxFewItemsPreview() {
  val categories =
    listOf(
      CategoryItem("News", "News"),
      CategoryItem("Note", "Note"),
      CategoryItem("doesn't exist", "Oops"),
    )

  FDroidContent { ChipFlexBox { categories.forEach { category -> CategoryChip(category, {}) } } }
}

@Preview
@Composable
fun ChipFlexBoxManyItemsPreview() {
  val categories =
    listOf(
      CategoryItem("Cloud Storage & File Sync", "Cloud Storage & File Sync"),
      CategoryItem("Connectivity", "Connectivity"),
      CategoryItem("Development", "Development"),
      CategoryItem("doesn't exist", "Foo bar"),
      CategoryItem("Online Media Player", "Online Media Player"),
      CategoryItem("Pass Wallet", "Pass Wallet"),
      CategoryItem("Password & 2FA", "Password & 2FA"),
      CategoryItem("Phone & SMS", "Phone & SMS"),
      CategoryItem("Podcast", "Podcast"),
      CategoryItem("Public Transport", "Public Transport"),
      CategoryItem("Reading", "Reading"),
      CategoryItem("Recipe Manager", "Recipe Manager"),
      CategoryItem("Religion", "Religion"),
      CategoryItem("Science & Education", "Science & Education"),
    )

  FDroidContent { ChipFlexBox { categories.forEach { category -> CategoryChip(category, {}) } } }
}

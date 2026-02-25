package org.fdroid.ui.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.fdroid.R

@Composable
fun ExpandIconChevron(isExpanded: Boolean) {
  Icon(
    imageVector =
      if (isExpanded) {
        Icons.Default.ExpandLess
      } else {
        Icons.Default.ExpandMore
      },
    contentDescription =
      if (isExpanded) {
        stringResource(R.string.collapse)
      } else {
        stringResource(R.string.expand)
      },
  )
}

@Composable
fun ExpandIconArrow(isExpanded: Boolean) {
  Icon(
    imageVector =
      if (isExpanded) {
        Icons.Default.ExpandLess
      } else {
        Icons.Default.ExpandMore
      },
    contentDescription =
      if (isExpanded) {
        stringResource(R.string.collapse)
      } else {
        stringResource(R.string.expand)
      },
  )
}

package org.fdroid.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.search.SavedSearch
import org.fdroid.ui.FDroidContent

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun PastSearches(
  savedSearches: List<SavedSearch>,
  onSearch: (String) -> Unit,
  onClearSavedSearches: () -> Unit,
  modifier: Modifier = Modifier,
  paddingValues: PaddingValues = PaddingValues(),
) {
  LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = paddingValues) {
    item {
      Row {
        Text(
          text = stringResource(R.string.search_history),
          style = MaterialTheme.typography.labelLarge,
          modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp).weight(1f),
        )
        TextButton(
          onClick = onClearSavedSearches,
          modifier = Modifier.padding(horizontal = 8.dp),
        ) {
          Text(stringResource(R.string.clear))
        }
      }
    }
    items(savedSearches) { item ->
      ListItem(
        leadingContent = {
          Icon(
            Icons.Default.History,
            contentDescription = null,
            modifier =
              Modifier.clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(8.dp),
          )
        },
        onClick = { onSearch(item.query) },
        modifier = Modifier.fillMaxWidth().animateItem(),
      ) {
        Text(item.query)
      }
    }
    item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars)) }
    item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.ime)) }
  }
}

@Preview
@Composable
private fun Preview() {
  FDroidContent {
    val savedSearches =
      listOf(SavedSearch(1, "foo"), SavedSearch(2, "foo bar"), SavedSearch(3, "foobar"))
    PastSearches(
        savedSearches = savedSearches,
      onClearSavedSearches = {},
        onSearch = {},
      )
  }
}

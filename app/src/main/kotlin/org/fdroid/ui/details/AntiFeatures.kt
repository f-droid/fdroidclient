package org.fdroid.ui.details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CrisisAlert
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter.Companion.tint
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onVisibilityChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.viktormykhailiv.compose.hints.HintAnchorState
import com.viktormykhailiv.compose.hints.hintAnchor
import com.viktormykhailiv.compose.hints.rememberHint
import com.viktormykhailiv.compose.hints.rememberHintAnchorState
import org.fdroid.R
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.utils.AsyncShimmerImage
import org.fdroid.ui.utils.ExpandableSection
import org.fdroid.ui.utils.testApp

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun AntiFeatures(
  antiFeatures: List<AntiFeature>,
  hintAnchor: HintAnchorState,
  showOnboarding: Boolean,
  modifier: Modifier = Modifier,
  onShowOnboarding: () -> Unit,
) {
  var placed by remember { mutableStateOf(false) }
  var visible by remember { mutableStateOf(false) }
  ElevatedCard(
    colors =
      CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.inverseSurface),
    modifier =
      modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp)
        .hintAnchor(state = hintAnchor, shape = CardDefaults.elevatedShape)
        .onPlaced {
          if (visible && showOnboarding) {
            onShowOnboarding()
          } else {
            placed = true
          }
        }
        .onVisibilityChanged(minDurationMs = 1000) { isVisible ->
          if (isVisible && placed && showOnboarding) {
            onShowOnboarding()
          } else if (isVisible) {
            visible = true
          }
        }
        .padding(8.dp),
  ) {
    ExpandableSection(
      icon = rememberVectorPainter(Icons.Default.WarningAmber),
      title = stringResource(R.string.anti_features_title),
      modifier = Modifier.padding(start = 16.dp),
    ) {
      Column {
        antiFeatures.forEach { antiFeature ->
          ListItem(
            leadingContent = {
              AsyncShimmerImage(
                model = antiFeature.icon,
                contentDescription = "",
                colorFilter = tint(MaterialTheme.colorScheme.inverseOnSurface),
                error = rememberVectorPainter(Icons.Default.CrisisAlert),
                modifier = Modifier.size(32.dp),
              )
            },
            headlineContent = {
              Text(
                text = antiFeature.name,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                style = MaterialTheme.typography.bodyMediumEmphasized,
              )
            },
            supportingContent = {
              antiFeature.reason?.let {
                Text(
                  text = antiFeature.reason,
                  color = MaterialTheme.colorScheme.inverseOnSurface,
                  style = MaterialTheme.typography.labelMedium,
                )
              }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.padding(bottom = 8.dp),
          )
        }
      }
    }
  }
}

@Preview
@Composable
fun AntiFeaturesPreview() {
  val hint = rememberHint {}
  val hintAnchor = rememberHintAnchorState(hint)
  FDroidContent { AntiFeatures(testApp.antiFeatures!!, hintAnchor, showOnboarding = false) {} }
}

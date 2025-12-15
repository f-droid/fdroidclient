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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter.Companion.tint
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.R
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.utils.AsyncShimmerImage
import org.fdroid.ui.utils.ExpandableSection
import org.fdroid.ui.utils.testApp

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun AntiFeatures(
    antiFeatures: List<AntiFeature>,
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.inverseSurface,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        ExpandableSection(
            icon = rememberVectorPainter(Icons.Default.WarningAmber),
            title = stringResource(R.string.anti_features_title),
            modifier = Modifier.padding(horizontal = 16.dp),
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
    FDroidContent {
        AntiFeatures(testApp.antiFeatures!!)
    }
}

package org.fdroid.ui.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition.Companion.Below
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import org.fdroid.R

@Composable
fun TopAppBarButton(imageVector: ImageVector, contentDescription: String, onClick: () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(Below),
        tooltip = { PlainTooltip { Text(contentDescription) } },
        state = rememberTooltipState(),
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
            )
        }
    }
}

@Composable
fun BackButton(onClick: () -> Unit) = TopAppBarButton(
    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
    contentDescription = stringResource(R.string.back),
    onClick = onClick,
)

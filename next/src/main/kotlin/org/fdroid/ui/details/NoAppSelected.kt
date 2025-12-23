package org.fdroid.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.ui.AboutContent
import org.fdroid.ui.FDroidContent

@Composable
fun NoAppSelected() {
    Box(
        contentAlignment = Center,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AboutContent(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = 0.7f)
                .padding(top = 32.dp)
        )
    }
}

@Preview
@Composable
private fun Preview() {
    FDroidContent {
        NoAppSelected()
    }
}

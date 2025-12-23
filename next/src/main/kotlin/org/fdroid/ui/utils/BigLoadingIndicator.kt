package org.fdroid.ui.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fdroid.ui.FDroidContent

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun BigLoadingIndicator(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize()
    ) {
        LoadingIndicator(Modifier.size(128.dp))
    }
}

@Preview
@Composable
private fun Preview() {
    FDroidContent {
        BigLoadingIndicator()
    }
}

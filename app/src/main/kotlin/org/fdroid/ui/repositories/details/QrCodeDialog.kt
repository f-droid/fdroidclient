package org.fdroid.ui.repositories.details

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.fdroid.R

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun QrCodeDialog(onDismissDialog: () -> Unit, generateQrCode: suspend () -> Bitmap?) {
  val qrCodeBitmap: ImageBitmap? by produceState(null) { value = generateQrCode()?.asImageBitmap() }
  AlertDialog(
    title = { Text(text = stringResource(R.string.share_repository)) },
    text = {
      val bitmap = qrCodeBitmap
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth().heightIn(min = 128.dp),
      ) {
        if (bitmap == null) {
          LoadingIndicator()
        } else {
          Image(bitmap = bitmap, contentDescription = stringResource(R.string.swap_scan_qr))
        }
      }
    },
    onDismissRequest = onDismissDialog,
    confirmButton = { TextButton(onClick = onDismissDialog) { Text(stringResource(R.string.ok)) } },
  )
}

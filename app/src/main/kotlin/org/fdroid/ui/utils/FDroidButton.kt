package org.fdroid.ui.utils

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun FDroidButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  imageVector: ImageVector? = null,
) {
  Button(
    onClick = onClick,
    shape = RoundedCornerShape(32.dp),
    modifier = modifier.heightIn(min = ButtonDefaults.MinHeight),
  ) {
    if (imageVector != null) {
      Icon(
        imageVector = imageVector,
        contentDescription = text,
        modifier = Modifier.size(ButtonDefaults.IconSize),
      )
      Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
    }
    Text(text = text)
  }
}

@Composable
fun FDroidOutlineButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  imageVector: ImageVector? = null,
  color: Color = MaterialTheme.colorScheme.primary,
) {
  OutlinedButton(
    onClick = onClick,
    shape = RoundedCornerShape(32.dp),
    modifier = modifier.heightIn(min = ButtonDefaults.MinHeight),
    colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
  ) {
    if (imageVector != null) {
      Icon(
        imageVector = imageVector,
        contentDescription = text,
        modifier = Modifier.size(ButtonDefaults.IconSize),
      )
      Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
    }
    Text(text = text, maxLines = 1)
  }
}

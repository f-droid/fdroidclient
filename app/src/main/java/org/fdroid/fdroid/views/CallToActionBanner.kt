package org.fdroid.fdroid.views

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import org.fdroid.fdroid.Preferences
import org.fdroid.fdroid.R
import org.fdroid.fdroid.openUriSafe
import org.fdroid.fdroid.ui.theme.FDroidContent
import java.util.Calendar

/**
 * Expire at the start of September 2026, to align with the countdown on keepandroidopen.org.
 *
 * The call to action regarding Google Developer Verification and keepandroidopen.org should not
 * persist forever. Rather, we will only show it for a month or two.
 * This is in the vain hope that something changes and Google decides to let us install the software
 * we choose on our own device. If that were to happen, we don't want to keep showing this.
 *
 * @param now Allow this to be passed in for testing purposes.
 */
fun hasCallToActionExpired(now: Long = System.currentTimeMillis()): Boolean {
    val calendar = Calendar.getInstance().apply {
        set(2026, 8, 1) // 8 = Sept (0-based month)
    }
    val expiry = calendar.timeInMillis
    val hasExpired = now > expiry

    return hasExpired
}

/**
 * Broadcast a call to action message to end users.
 *
 * This should only be used in exceptional circumstances. For example, it was first
 * introduced to bring users attention to the Developer verification mandate which
 * threatens the very existence of F-Droid.
 */
@Composable
fun CallToActionBanner(dismissed: Boolean, onDismiss: () -> Unit) {
    var showBanner by remember { mutableStateOf(!dismissed) }
    val uriHandler = LocalUriHandler.current
    val url = stringResource(R.string.call_to_action_keepandroidopen_url)

    val handleDismiss = {
        onDismiss()
        showBanner = false
    }

    AnimatedVisibility(
        visible = showBanner,
        exit = slideOutVertically(),
    ) {
        Row {
            Row(
                Modifier
                    .background(colorResource(R.color.call_to_action_banner__background)),
                horizontalArrangement = spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .selectable(
                            selected = false,
                            onClick = { uriHandler.openUriSafe(url) },
                        )
                        .padding(start = 6.dp, top = 6.dp, bottom = 6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.call_to_action_keepandroidopen),
                        textAlign = TextAlign.Center,
                        color = colorResource(R.color.call_to_action_banner__text),
                        style = TextStyle(lineHeight = 1.em),
                    )
                    Text(
                        text = "https://keepandroidopen.org",
                        textAlign = TextAlign.Center,
                        color = colorResource(R.color.call_to_action_banner__link),
                        textDecoration = TextDecoration.Underline,

                        // For some reason, setting the TextStyle on the above Text() makes the font size change.
                        // To make this have the same font, we add this empty TextStyle.
                        style = TextStyle(),
                    )
                }

                IconButton(onClick = handleDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = colorResource(R.color.call_to_action_banner__text),
                    )
                }
            }
        }
    }
}

/**
 * A helper method to show [CallToActionBanner] from Java code.
 */
fun setContentCallToActionBanner(composeView: ComposeView) {
    val dismissed = hasCallToActionExpired() || Preferences.get().callToActionDismissed
    val onDismiss = {
        Preferences.get().setCallToActionDismissed()
    }

    composeView.setContent {
        FDroidContent {
            CallToActionBanner(
                dismissed = dismissed,
                onDismiss = onDismiss,
            )
        }
    }
}

@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
fun CallToActionBannerPreview() {
    FDroidContent(pureBlack = true) {
        CallToActionBanner(onDismiss = {}, dismissed = false)
    }
}

package org.fdroid.ui

import androidx.compose.runtime.Composable
import com.viktormykhailiv.compose.hints.Hint
import com.viktormykhailiv.compose.hints.HintProperties
import com.viktormykhailiv.compose.hints.rememberHint
import org.fdroid.ui.navigation.NavigationKey

@Composable
@Suppress("unused") // used in full flavor
fun getMainOnboardingHint(onOnboardingSeen: () -> Unit, onNav: (NavigationKey) -> Unit): Hint {
  return rememberHint(HintProperties(dismissOnClickOutside = true)) {}
}

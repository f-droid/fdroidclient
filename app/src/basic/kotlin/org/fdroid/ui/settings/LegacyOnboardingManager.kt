package org.fdroid.ui.settings

import javax.inject.Inject
import javax.inject.Singleton

/**
 * This class is used to check if the user has used the legacy hiding feature and if so, it will
 * switch to the new calculator icon and disable the legacy hiding feature.
 *
 * After sufficient time has passed, this class can be removed.
 */
@Singleton
class LegacyOnboardingManager @Inject constructor() {
  val showAppHidingOnboarding: Boolean = false
}

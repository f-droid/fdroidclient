package org.fdroid.basic.ui.main.apps

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class AppNavigationItem(
    val packageName: String,
    val name: String,
    val summary: String,
    val isNew: Boolean,
): Parcelable

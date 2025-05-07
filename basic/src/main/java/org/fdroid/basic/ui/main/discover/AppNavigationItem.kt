package org.fdroid.basic.ui.main.discover

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.fdroid.basic.ui.main.apps.MinimalApp

@Parcelize
class AppNavigationItem(
    override val packageName: String,
    override val name: String,
    val summary: String,
    val isNew: Boolean,
): MinimalApp, Parcelable

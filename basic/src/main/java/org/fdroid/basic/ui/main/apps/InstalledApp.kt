package org.fdroid.basic.ui.main.apps

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

interface MinimalApp {
    val packageName: String
    val name: String?
}

@Parcelize
data class InstalledApp(
    override val packageName: String,
    override val name: String,
    val versionName: String,
) : MinimalApp, Parcelable

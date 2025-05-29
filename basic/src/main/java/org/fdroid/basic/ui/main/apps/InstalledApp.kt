package org.fdroid.basic.ui.main.apps

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

interface MinimalApp {
    val packageName: String
    val name: String?
    val icon: String?
}

@Parcelize
data class InstalledApp(
    override val packageName: String,
    override val icon: String? = null,
    override val name: String,
    val versionName: String,
) : MinimalApp, Parcelable

package org.fdroid.basic.ui.main.apps

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UpdatableApp(
    override val packageName: String,
    override val name: String,
    val currentVersionName: String,
    val updateVersionName: String,
    val size: Long,
    override val icon: String? = null,
    val whatsNew: String? = null,
): MinimalApp, Parcelable

package org.fdroid.basic.ui.main.updates

data class UpdatableApp(
    val name: String,
    val currentVersionName: String,
    val updateVersionName: String,
    val size: Long,
    val whatsNew: String? = null,
)

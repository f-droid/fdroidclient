package org.fdroid.history

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class HistoryEvent {
  abstract val time: Long
  abstract val packageName: String
  abstract val name: String?
}

@Serializable
@SerialName("InstallEvent")
data class InstallEvent(
  override val time: Long,
  override val packageName: String,
  override val name: String,
  val versionName: String,
  val oldVersionName: String?,
) : HistoryEvent()

@Serializable
@SerialName("UninstallEvent")
data class UninstallEvent(
  override val time: Long,
  override val packageName: String,
  override val name: String?,
) : HistoryEvent()

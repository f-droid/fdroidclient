package org.fdroid.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.provider.Settings
import android.provider.Settings.Secure.INSTALL_NON_MARKET_APPS
import android.telephony.TelephonyManager
import androidx.annotation.WorkerThread
import androidx.core.os.LocaleListCompat
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import org.fdroid.BuildConfig.FLAVOR
import org.fdroid.settings.SettingsManager

fun sha256(bytes: ByteArray): String {
  val messageDigest: MessageDigest =
    try {
      MessageDigest.getInstance("SHA-256")
    } catch (e: NoSuchAlgorithmException) {
      throw AssertionError(e)
    }
  messageDigest.update(bytes)
  return messageDigest.digest().toHexString()
}

/** Returns true if the device is an old Xiaomi or Redmi device where SessionInstall is broken. */
@WorkerThread
fun isOldXiaomi(context: Context): Boolean {
  val isXiaomiOrRedmi =
    "Xiaomi".equals(Build.BRAND, ignoreCase = true) ||
      "Redmi".equals(Build.BRAND, ignoreCase = true)
  val isOldXiaomiOrRedmi = SDK_INT <= 31 && isXiaomiOrRedmi
  val isChineseXiaomiOrRedmi =
    SDK_INT <= 33 && isXiaomiOrRedmi && Build.VERSION.INCREMENTAL.takeLast(4).startsWith("CN")
  return (isOldXiaomiOrRedmi || isChineseXiaomiOrRedmi) &&
    listOf("com.miui.securitycenter", "com.miui.packageinstaller").any { isInstalled(context, it) }
}

@WorkerThread
fun isInstalled(context: Context, packageName: String): Boolean {
  return try {
    context.packageManager.getPackageInfo(packageName, 0)
    true
  } catch (_: PackageManager.NameNotFoundException) {
    false
  }
}

fun getLogName(context: Context): String {
  val sdf =
    SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
      timeZone = TimeZone.getTimeZone("UTC")
    }
  val time = sdf.format(Date())
  return "${context.packageName}-$time"
}

fun getCurrentLocation(context: Context): String {
  val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
  return tm.simCountryIso
    ?: tm.networkCountryIso
    ?: run {
      val localeList = LocaleListCompat.getDefault()
      localeList.get(0)?.country ?: Locale.getDefault().country
    }
}

fun isChina(context: Context): Boolean {
  val country = getCurrentLocation(context)
  return country.equals("cn", ignoreCase = true)
}

/**
 * Returns true if the user has privileged extension installed, but not yet granted unknown sources
 * permission. It also returns false, if at any time in the past, it returned false.
 *
 * Can be removed after sufficient time has passed since 2026-10.
 */
fun isMigratingFromPrivilegedExtension(
  context: Context,
  settingsManager: SettingsManager,
): Boolean {
  // if we remember that migration isn't needed bail out early
  if (!settingsManager.privilegedExtensionNeedsMigration) return false

  // check if privileged extension is installed
  try {
    context.packageManager.getApplicationInfo("org.fdroid.fdroid.privileged", 0)
  } catch (_: PackageManager.NameNotFoundException) {
    // privileged extension isn't installed, so no need for migration
    settingsManager.privilegedExtensionNeedsMigration = false
    return false
  }
  // now check if unknown sources permission is granted, if not, migration is needed
  return if (SDK_INT < 26) {
      @Suppress("DEPRECATION")
      val nonMarket = Settings.Secure.getInt(context.contentResolver, INSTALL_NON_MARKET_APPS, 0)
      nonMarket != 1 // this is 1 if unknown sources was granted
    } else {
      !context.packageManager.canRequestPackageInstalls()
    }
    .also { result ->
      // remember that migration isn't needed
      if (!result) settingsManager.privilegedExtensionNeedsMigration = false
    }
}

val isFull: Boolean
  get() = FLAVOR.startsWith("full")
val isBasic: Boolean
  get() = FLAVOR.startsWith("basic")

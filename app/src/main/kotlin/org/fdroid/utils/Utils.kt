package org.fdroid.utils

import android.content.Context
import android.telephony.TelephonyManager
import androidx.core.os.LocaleListCompat
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import org.fdroid.BuildConfig.FLAVOR

@OptIn(ExperimentalStdlibApi::class)
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

val isFull: Boolean
  get() = FLAVOR.startsWith("full")
val isBasic: Boolean
  get() = FLAVOR.startsWith("basic")

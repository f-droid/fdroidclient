package org.fdroid.basic.ui

import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import android.util.Log
import androidx.compose.ui.platform.UriHandler
import org.fdroid.index.v2.PackageManifest
import org.fdroid.index.v2.PackageVersion
import org.fdroid.index.v2.SignerV2
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.concurrent.TimeUnit

fun Context.startActivitySafe(i: Intent?) {
    if (i == null) return
    try {
        startActivity(i)
    } catch (e: Exception) {
        Log.e("Context", "Error opening $i ", e)
    }
}

fun UriHandler.openUriSafe(uri: String) {
    try {
        openUri(uri)
    } catch (e: Exception) {
        Log.e("UriHandler", "Error opening $uri ", e)
    }
}

fun getPreviewVersion(versionName: String, size: Long? = null) = object : PackageVersion {
    override val versionCode: Long = 23
    override val versionName: String = versionName
    override val added: Long = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3)
    override val size: Long? = size
    override val signer: SignerV2? = null
    override val releaseChannels: List<String>? = null
    override val packageManifest: PackageManifest = object : PackageManifest {
        override val minSdkVersion: Int? = null
        override val maxSdkVersion: Int? = null
        override val featureNames: List<String>? = null
        override val nativecode: List<String>? = null
        override val targetSdkVersion: Int? = null
    }
    override val hasKnownVulnerability: Boolean = false
}

fun Long.asRelativeTimeString(): String {
    return DateUtils.getRelativeTimeSpanString(
        this,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_ALL
    ).toString()
}

@OptIn(ExperimentalStdlibApi::class)
fun sha256(bytes: ByteArray): String {
    val messageDigest: MessageDigest = try {
        MessageDigest.getInstance("SHA-256")
    } catch (e: NoSuchAlgorithmException) {
        throw AssertionError(e)
    }
    messageDigest.update(bytes)
    return messageDigest.digest().toHexString()
}

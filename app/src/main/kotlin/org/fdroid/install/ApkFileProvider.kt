package org.fdroid.install

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.Intent.EXTRA_STREAM
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.ParcelFileDescriptor.MODE_READ_ONLY
import android.provider.MediaStore.MediaColumns
import androidx.core.net.toUri
import mu.KotlinLogging
import org.fdroid.BuildConfig.APPLICATION_ID
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class ApkFileProvider : ContentProvider() {

    companion object {
        private const val AUTHORITY = "${APPLICATION_ID}.install.ApkFileProvider"
        private const val MIME_TYPE = "application/vnd.android.package-archive"

        private fun getUri(packageName: String): Uri {
            return "content://$AUTHORITY/$packageName.apk".toUri()
        }

        fun getIntent(packageName: String) = Intent(ACTION_SEND).apply {
            setDataAndType(getUri(packageName), MIME_TYPE)
            putExtra(EXTRA_STREAM, data)
            setFlags(FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private val log = KotlinLogging.logger {}

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        log.info { "openFile $uri $mode" }
        if (mode != "r") return null

        val applicationInfo = getApplicationInfo(uri) ?: throw FileNotFoundException()
        try {
            val apkFile = File(applicationInfo.publicSourceDir)
            return ParcelFileDescriptor.open(apkFile, MODE_READ_ONLY)
        } catch (e: IOException) {
            throw FileNotFoundException(e.localizedMessage)
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        val packageName = uri.lastPathSegment ?: return null
        val applicationInfo = getApplicationInfo(uri) ?: return null
        // we don't care what they are asking for, just give them this
        val columns = arrayOf(
            MediaColumns.DISPLAY_NAME,
            MediaColumns.MIME_TYPE,
            MediaColumns.DATA,
            MediaColumns.SIZE,
        )
        return MatrixCursor(columns).apply {
            try {
                addRow(
                    arrayOf<Any?>(
                        packageName,
                        MIME_TYPE,
                        applicationInfo.publicSourceDir,
                        File(applicationInfo.publicSourceDir).length(),
                    )
                )
            } catch (e: Exception) {
                log.error(e) { "Error returning cursor: " }
                return null
            }
        }
    }

    @Throws(PackageManager.NameNotFoundException::class)
    private fun getApplicationInfo(uri: Uri): ApplicationInfo? {
        val packageManager = context?.packageManager ?: return null
        val packageName = uri.lastPathSegment?.removeSuffix(".apk") ?: return null
        return try {
            packageManager.getApplicationInfo(packageName, 0)
        } catch (e: Exception) {
            log.error(e) { "Error getting ApplicationInfo: " }
            null
        }
    }

    override fun onCreate(): Boolean = true
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun getType(uri: Uri): String = MIME_TYPE
    override fun getTypeAnonymous(uri: Uri): String = MIME_TYPE
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0
}

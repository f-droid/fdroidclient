package org.fdroid.install

import android.content.pm.ApplicationInfo.FLAG_SYSTEM
import android.provider.MediaStore.MediaColumns.DISPLAY_NAME
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mu.KotlinLogging
import org.fdroid.install.ApkFileProvider.Companion.getIntent
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.fail

@RunWith(AndroidJUnit4::class)
class ApkFileProviderTest {

    private val log = KotlinLogging.logger {}
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val pm = context.packageManager

    private val packageInfoList
        get() = pm.getInstalledPackages(0).filter {
            val info = it.applicationInfo ?: return@filter false
            (info.flags and FLAG_SYSTEM) == 0
        }.sortedBy {
            val path = it.applicationInfo?.publicSourceDir ?: return@sortedBy Long.MAX_VALUE
            File(path).length()
        }.subList(0, 3) // just test with the three smallest apps

    /**
     * Test whether reading installed APKs via our custom [android.content.ContentProvider] works.
     * It also only copies max 3 apps so it doesn't take a long time to run.
     */
    @Test
    fun testCopyFromGetUri() {
        for (packageInfo in packageInfoList) {
            val applicationInfo = packageInfo.applicationInfo ?: fail()
            val apk = File(applicationInfo.publicSourceDir)
            val uri = getIntent(packageInfo.packageName).data ?: fail()
            val test = FileOutputStream("/dev/null")
            val numBytesCopied = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.copyTo(test)
            } ?: fail()
            assertEquals(apk.length(), numBytesCopied)
            log.info {
                "${packageInfo.packageName} read $numBytesCopied bytes from ${apk.absolutePath}"
            }
        }
    }

    /**
     * Test whether querying the custom [android.content.ContentProvider] for installed APKs
     * returns the right kind of data.
     */
    @Test
    @Throws(IOException::class)
    fun testQuery() {
        for (packageInfo in packageInfoList) {
            val uri = getIntent(packageInfo.packageName).data ?: fail()
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                val name = cursor.getString(cursor.getColumnIndex(DISPLAY_NAME))
                assertEquals("${packageInfo.packageName}.apk", name)
            } ?: fail()
        }
    }
}

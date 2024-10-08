package org.fdroid.index

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat.PNG
import android.graphics.Bitmap.Config.ARGB_8888
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.system.Os.symlink
import androidx.core.content.pm.PackageInfoCompat
import org.fdroid.index.IndexUtils.toHex
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.jar.JarFile
import java.util.regex.Pattern

public abstract class IndexCreator<T>(
    protected val packageManager: PackageManager,
    protected val repoDir: File,
    protected val packageNames: Set<String>,
) {

    private val iconDir = File(repoDir, "icons")
    private val iconDirs =
        listOf("icons-120", "icons-160", "icons-240", "icons-320", "icons-480", "icons-640")
    private val nativeCodePattern = Pattern.compile("^lib/([a-z0-9-]+)/.*")

    init {
        require(repoDir.isDirectory) { "$repoDir is not a directory" }
        require(repoDir.canWrite()) { "Can not write to $repoDir" }
    }

    @Throws(IOException::class)
    public abstract fun createRepo(): T

    protected fun prepareIconFolders() {
        iconDir.mkdir()
        iconDirs.forEach { dir ->
            val file = File(repoDir, dir)
            if (!file.exists()) symlink(iconDir.absolutePath, file.absolutePath)
        }
    }

    /**
     * Extracts the icon from an APK and writes it to the repo as a PNG.
     * @return the name of the written icon file.
     */
    protected fun copyIconToRepo(packageInfo: PackageInfo): String? {
        val packageName = packageInfo.packageName
        val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
        val drawable = packageInfo.applicationInfo?.loadIcon(packageManager) ?: return null
        val bitmap: Bitmap
        if (drawable is BitmapDrawable) {
            bitmap = drawable.bitmap
        } else {
            bitmap =
                Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
        }
        val iconName = "${packageName}_$versionCode.png"
        File(iconDir, iconName).outputStream().use { outputStream ->
            bitmap.compress(PNG, 100, outputStream)
        }
        return iconName
    }

    /**
     * Symlinks the APK to the repo. Does not support split APKs.
     * @return the name of the linked/copied APK file or null if no file exists.
     *
     * Roboletric apparently does not support Os.symlink, and some devices might
     * have wonky implementations.  Copying is slower and takes more disk space,
     * but is much more reliable.  So it is a workable fallback.
     */
    protected fun copyApkToRepo(packageInfo: PackageInfo): File? {
        val appInfo = packageInfo.applicationInfo ?: return null
        val packageName = packageInfo.packageName
        val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
        val apkName = "${packageName}_$versionCode.apk"
        val apkFile = File(repoDir, apkName)
        if (apkFile.exists()) apkFile.delete()
        symlink(appInfo.publicSourceDir, apkFile.absolutePath)
        if (!apkFile.exists()) {
            File(appInfo.publicSourceDir).copyTo(apkFile)
        }
        return apkFile
    }

    protected fun hashFile(file: File): String {
        val messageDigest: MessageDigest = try {
            MessageDigest.getInstance("SHA-256")
        } catch (e: NoSuchAlgorithmException) {
            throw AssertionError(e)
        }
        file.inputStream().use { inputStream ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var bytes = inputStream.read(buffer)
            while (bytes >= 0) {
                messageDigest.update(buffer, 0, bytes)
                bytes = inputStream.read(buffer)
            }
        }
        return messageDigest.digest().toHex()
    }

    protected fun parseNativeCode(packageInfo: PackageInfo): List<String> {
        val appInfo = packageInfo.applicationInfo ?: return emptyList()
        val apkJar = JarFile(appInfo.publicSourceDir)
        val abis = HashSet<String>()
        val jarEntries = apkJar.entries()
        while (jarEntries.hasMoreElements()) {
            val jarEntry = jarEntries.nextElement()
            val matcher = nativeCodePattern.matcher(jarEntry.name)
            if (matcher.matches()) {
                val group = matcher.group(1)
                if (group != null) abis.add(group)
            }
        }
        return abis.toList()
    }

}

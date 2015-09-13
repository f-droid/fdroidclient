/*
 * Copyright (C) 2010-12  Ciaran Gultnieks, ciaran@ciarang.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.fdroid.fdroid;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.utils.StorageUtils;

import org.fdroid.fdroid.compat.FileCompat;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.SanitizedFile;
import org.xml.sax.XMLReader;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public final class Utils {

    @SuppressWarnings("UnusedDeclaration")
    private static final String TAG = "Utils";

    public static final int BUFFER_SIZE = 4096;

    // The date format used for storing dates (e.g. lastupdated, added) in the
    // database.
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

    private static final String[] FRIENDLY_SIZE_FORMAT = {
            "%.0f B", "%.0f KiB", "%.1f MiB", "%.2f GiB" };

    public static final String FALLBACK_ICONS_DIR = "/icons/";

    /*
     * @param dpiMultiplier Lets you grab icons for densities larger or
     * smaller than that of your device by some fraction. Useful, for example,
     * if you want to display a 48dp image at twice the size, 96dp, in which
     * case you'd use a dpiMultiplier of 2.0 to get an image twice as big.
     */
    public static String getIconsDir(final Context context, final double dpiMultiplier) {
        final DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        final double dpi = metrics.densityDpi * dpiMultiplier;
        if (dpi >= 640) {
            return "/icons-640/";
        }
        if (dpi >= 480) {
            return "/icons-480/";
        }
        if (dpi >= 320) {
            return "/icons-320/";
        }
        if (dpi >= 240) {
            return "/icons-240/";
        }
        if (dpi >= 160) {
            return "/icons-160/";
        }

        return "/icons-120/";
    }

    public static void copy(InputStream input, OutputStream output)
            throws IOException {
        copy(input, output, null, null);
    }

    public static void copy(InputStream input, OutputStream output,
                    ProgressListener progressListener,
                    ProgressListener.Event templateProgressEvent)
    throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead = 0;
        while (true) {
            int count = input.read(buffer);
            if (count == -1) {
                break;
            }
            if (progressListener != null) {
                bytesRead += count;
                templateProgressEvent.progress = bytesRead;
                progressListener.onProgress(templateProgressEvent);
            }
            output.write(buffer, 0, count);
        }
        output.flush();
    }

    /**
     * Attempt to symlink, but if that fails, it will make a copy of the file.
     */
    public static boolean symlinkOrCopyFileQuietly(SanitizedFile inFile, SanitizedFile outFile) {
        return FileCompat.symlink(inFile, outFile) || copyQuietly(inFile, outFile);
    }

    /**
     * Read the input stream until it reaches the end, ignoring any exceptions.
     */
    public static void consumeStream(InputStream stream) {
        final byte[] buffer = new byte[256];
        try {
            int read;
            do {
                read = stream.read(buffer);
            } while (read != -1);
        } catch (IOException e) {
            // Ignore...
        }
    }

    public static boolean copyQuietly(File inFile, File outFile) {
        InputStream input = null;
        OutputStream output = null;
        try {
            input  = new FileInputStream(inFile);
            output = new FileOutputStream(outFile);
            Utils.copy(input, output);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "I/O error when copying a file", e);
            return false;
        } finally {
            closeQuietly(output);
            closeQuietly(input);
        }
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ioe) {
            // ignore
        }
    }

    public static String getFriendlySize(int size) {
        double s = size;
        int i = 0;
        while (i < FRIENDLY_SIZE_FORMAT.length - 1 && s >= 1024) {
            s = (100 * s / 1024) / 100.0;
            i++;
        }
        return String.format(FRIENDLY_SIZE_FORMAT[i], s);
    }

    private static final String[] androidVersionNames = {
        "?",     // 0, undefined
        "1.0",   // 1
        "1.1",   // 2
        "1.5",   // 3
        "1.6",   // 4
        "2.0",   // 5
        "2.0.1", // 6
        "2.1",   // 7
        "2.2",   // 8
        "2.3",   // 9
        "2.3.3", // 10
        "3.0",   // 11
        "3.1",   // 12
        "3.2",   // 13
        "4.0",   // 14
        "4.0.3", // 15
        "4.1",   // 16
        "4.2",   // 17
        "4.3",   // 18
        "4.4",   // 19
        "4.4W",  // 20
        "5.0",   // 21
        "5.1",   // 22
        "6.0"    // 23
    };

    public static String getAndroidVersionName(int sdkLevel) {
        if (sdkLevel < 0) {
            return androidVersionNames[0];
        }
        if (sdkLevel >= androidVersionNames.length) {
            return String.format(Locale.ENGLISH, "v%d", sdkLevel);
        }
        return androidVersionNames[sdkLevel];
    }

    /* PackageManager doesn't give us the min and max sdk versions, so we have
     * to parse it */
    private static int getMinMaxSdkVersion(Context context, String packageName,
            String attrName) {
        try {
            AssetManager am = context.createPackageContext(packageName, 0).getAssets();
            XmlResourceParser xml = am.openXmlResourceParser("AndroidManifest.xml");
            int eventType = xml.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && xml.getName().equals("uses-sdk")) {
                    for (int j = 0; j < xml.getAttributeCount(); j++) {
                        if (xml.getAttributeName(j).equals(attrName)) {
                            return Integer.parseInt(xml.getAttributeValue(j));
                        }
                    }
                }
                eventType = xml.nextToken();
            }
        } catch (PackageManager.NameNotFoundException | IOException | XmlPullParserException e) {
            Log.e(TAG, "Could not get min/max sdk version", e);
        }
        return 0;
    }

    public static int getMinSdkVersion(Context context, String packageName) {
        return getMinMaxSdkVersion(context, packageName, "minSdkVersion");
    }

    public static int getMaxSdkVersion(Context context, String packageName) {
        return getMinMaxSdkVersion(context, packageName, "maxSdkVersion");
    }

    // return a fingerprint formatted for display
    public static String formatFingerprint(Context context, String fingerprint) {
        if (TextUtils.isEmpty(fingerprint)
                || fingerprint.length() != 64  // SHA-256 is 64 hex chars
                || fingerprint.matches(".*[^0-9a-fA-F].*")) // its a hex string
            return context.getString(R.string.bad_fingerprint);
        String displayFP = fingerprint.substring(0, 2);
        for (int i = 2; i < fingerprint.length(); i = i + 2)
            displayFP += " " + fingerprint.substring(i, i + 2);
        return displayFP;
    }

    @NonNull
    public static Uri getLocalRepoUri(Repo repo) {
        if (TextUtils.isEmpty(repo.address))
            return Uri.parse("http://wifi-not-enabled");
        Uri uri = Uri.parse(repo.address);
        Uri.Builder b = uri.buildUpon();
        if (!TextUtils.isEmpty(repo.fingerprint))
            b.appendQueryParameter("fingerprint", repo.fingerprint);
        String scheme = Preferences.get().isLocalRepoHttpsEnabled() ? "https" : "http";
        b.scheme(scheme);
        return b.build();
    }

    public static Uri getSharingUri(Repo repo) {
        if (TextUtils.isEmpty(repo.address))
            return Uri.parse("http://wifi-not-enabled");
        Uri localRepoUri = getLocalRepoUri(repo);
        Uri.Builder b = localRepoUri.buildUpon();
        b.scheme(localRepoUri.getScheme().replaceFirst("http", "fdroidrepo"));
        b.appendQueryParameter("swap", "1");
        if (!TextUtils.isEmpty(FDroidApp.bssid)) {
            b.appendQueryParameter("bssid", Uri.encode(FDroidApp.bssid));
            if (!TextUtils.isEmpty(FDroidApp.ssid))
                b.appendQueryParameter("ssid", Uri.encode(FDroidApp.ssid));
        }
        return b.build();
    }

    /**
     * See {@link Utils#getApkDownloadDir(android.content.Context)} for why this is "unsafe".
     */
    public static SanitizedFile getApkCacheDir(Context context) {
        final SanitizedFile apkCacheDir = new SanitizedFile(StorageUtils.getCacheDirectory(context, true), "apks");
        if (!apkCacheDir.exists()) {
            apkCacheDir.mkdir();
        }
        return apkCacheDir;
    }

    /**
     * The directory where .apk files are downloaded (and stored - if the relevant property is enabled).
     * This must be on internal storage, to prevent other apps with "write external storage" from being
     * able to change the .apk file between F-Droid requesting the Package Manger to install, and the
     * Package Manager receiving that request.
     */
    public static File getApkDownloadDir(Context context) {
        final SanitizedFile apkCacheDir = new SanitizedFile(StorageUtils.getCacheDirectory(context, false), "temp");
        if (!apkCacheDir.exists()) {
            apkCacheDir.mkdir();
        }

        // All parent directories of the .apk file need to be executable for the package installer
        // to be able to have permission to read our world-readable .apk files.
        FileCompat.setExecutable(apkCacheDir, true, false);
        return apkCacheDir;
    }

    public static String calcFingerprint(String keyHexString) {
        if (TextUtils.isEmpty(keyHexString)
                || keyHexString.matches(".*[^a-fA-F0-9].*")) {
            Log.e(TAG, "Signing key certificate was blank or contained a non-hex-digit!");
            return null;
        }
        return calcFingerprint(Hasher.unhex(keyHexString));
    }

    public static String calcFingerprint(Certificate cert) {
        if (cert == null)
            return null;
        try {
            return calcFingerprint(cert.getEncoded());
        } catch (CertificateEncodingException e) {
            return null;
        }
    }

    public static String calcFingerprint(byte[] key) {
        if (key == null)
            return null;
        if (key.length < 256) {
            Log.e(TAG, "key was shorter than 256 bytes (" + key.length + "), cannot be valid!");
            return null;
        }
        String ret = null;
        try {
            // keytool -list -v gives you the SHA-256 fingerprint
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(key);
            byte[] fingerprint = digest.digest();
            Formatter formatter = new Formatter(new StringBuilder());
            for (byte aFingerprint : fingerprint) {
                formatter.format("%02X", aFingerprint);
            }
            ret = formatter.toString();
            formatter.close();
        } catch (Exception e) {
            Log.w(TAG, "Unable to get certificate fingerprint", e);
        }
        return ret;
    }

    /**
     * There is a method {@link java.util.Locale#forLanguageTag(String)} which would be useful
     * for this, however it doesn't deal with android-specific language tags, which are a little
     * different. For example, android language tags may have an "r" before the country code,
     * such as "zh-rHK", however {@link java.util.Locale} expects them to be "zr-HK".
     */
    public static Locale getLocaleFromAndroidLangTag(String languageTag) {
        if (TextUtils.isEmpty(languageTag)) {
            return null;
        }

        final String[] parts = languageTag.split("-");
        if (parts.length == 1) {
            return new Locale(parts[0]);
        }
        if (parts.length == 2) {
            String country = parts[1];
            // Some languages have an "r" before the country as per the values folders, such
            // as "zh-rCN". As far as the Locale class is concerned, the "r" is
            // not helpful, and this should be "zh-CN". Thus, we will
            // strip the "r" when found.
            if (country.charAt(0) == 'r' && country.length() == 3) {
                country = country.substring(1);
            }
            return new Locale(parts[0], country);
        }
        Log.e(TAG, "Locale could not be parsed from language tag: " + languageTag);
        return new Locale(languageTag);
    }

    public static String getApkUrl(Apk apk) {
        return getApkUrl(apk.repoAddress, apk);
    }

    public static String getApkUrl(String repoAddress, Apk apk) {
        return repoAddress + "/" + apk.apkName.replace(" ", "%20");
    }

    public static class CommaSeparatedList implements Iterable<String> {
        private final String value;

        private CommaSeparatedList(String list) {
            value = list;
        }

        public static CommaSeparatedList make(List<String> list) {
            if (list == null || list.isEmpty())
                return null;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(list.get(i));
            }
            return new CommaSeparatedList(sb.toString());
        }

        public static CommaSeparatedList make(String[] list) {
            if (list == null || list.length == 0)
                return null;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < list.length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(list[i]);
            }
            return new CommaSeparatedList(sb.toString());
        }

        @Nullable
        public static CommaSeparatedList make(@Nullable String list) {
            if (TextUtils.isEmpty(list))
                return null;
            return new CommaSeparatedList(list);
        }

        public static String str(CommaSeparatedList instance) {
            return (instance == null ? null : instance.toString());
        }

        @Override
        public String toString() {
            return value;
        }

        public String toPrettyString() {
            return value.replaceAll(",", ", ");
        }

        @Override
        public Iterator<String> iterator() {
            TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(',');
            splitter.setString(value);
            return splitter.iterator();
        }

        public boolean contains(String v) {
            for (final String s : this) {
                if (s.equals(v))
                    return true;
            }
            return false;
        }
    }

    public static DisplayImageOptions.Builder getImageLoadingOptions() {
        return new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .imageScaleType(ImageScaleType.NONE)
                .showImageOnLoading(R.drawable.ic_repo_app_default)
                .showImageForEmptyUri(R.drawable.ic_repo_app_default)
                .displayer(new FadeInBitmapDisplayer(200, true, true, false))
                .bitmapConfig(Bitmap.Config.RGB_565);
    }

    // this is all new stuff being added
    public static String hashBytes(byte[] input, String algo) {
        try {
            MessageDigest md = MessageDigest.getInstance(algo);
            byte[] hashBytes = md.digest(input);
            String hash = toHexString(hashBytes);

            md.reset();
            return hash;
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Device does not support " + algo + " MessageDisgest algorithm");
            return null;
        }
    }

    public static String getBinaryHash(File apk, String algo) {
        FileInputStream fis = null;
        try {
            MessageDigest md = MessageDigest.getInstance(algo);
            fis = new FileInputStream(apk);
            BufferedInputStream bis = new BufferedInputStream(fis);

            byte[] dataBytes = new byte[524288];
            int nread;
            while ((nread = bis.read(dataBytes)) != -1)
                md.update(dataBytes, 0, nread);

            byte[] mdbytes = md.digest();
            return toHexString(mdbytes);
        } catch (IOException e) {
            Log.e(TAG, "Error reading \"" + apk.getAbsolutePath()
                    + "\" to compute " + algo + " hash.", e);
            return null;
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Device does not support " + algo + " MessageDisgest algorithm");
            return null;
        } finally {
            closeQuietly(fis);
        }
    }

    /**
     * Computes the base 16 representation of the byte array argument.
     *
     * @param bytes an array of bytes.
     * @return the bytes represented as a string of hexadecimal digits.
     */
    public static String toHexString(byte[] bytes) {
        BigInteger bi = new BigInteger(1, bytes);
        return String.format("%0" + (bytes.length << 1) + "X", bi);
    }

    public static int parseInt(String str, int fallback) {
        if (str == null || str.length() == 0) {
            return fallback;
        }
        int result;
        try {
            result = Integer.parseInt(str);
        } catch (NumberFormatException e) {
            result = fallback;
        }
        return result;
    }

    public static Date parseDate(String str, Date fallback) {
        if (str == null || str.length() == 0) {
            return fallback;
        }
        Date result;
        try {
            result = DATE_FORMAT.parse(str);
        } catch (ParseException e) {
            result = fallback;
        }
        return result;
    }

    public static String formatDate(Date date, String fallback) {
        if (date == null) {
            return fallback;
        }
        return DATE_FORMAT.format(date);
    }

    // Need this to add the unimplemented support for ordered and unordered
    // lists to Html.fromHtml().
    public static class HtmlTagHandler implements Html.TagHandler {
        int listNum;

        @Override
        public void handleTag(boolean opening, String tag, Editable output,
                XMLReader reader) {
            switch (tag) {
            case "ul":
                if (opening)
                    listNum = -1;
                else
                    output.append('\n');
                break;
            case "ol":
                if (opening)
                    listNum = 1;
                else
                    output.append('\n');
                break;
            case "li":
                if (opening) {
                    if (listNum == -1) {
                        output.append("\t• ");
                    } else {
                        output.append("\t").append(Integer.toString(listNum)).append(". ");
                        listNum++;
                    }
                } else {
                    output.append('\n');
                }
                break;
            }
        }
    }

    /**
     * Remove all files from the {@param directory} either beginning with {@param startsWith}
     * or ending with {@param endsWith}. Note that if the SD card is not ready, then the
     * cache directory will probably not be available. In this situation no files will be
     * deleted (and thus they may still exist after the SD card becomes available).
     */
    public static void deleteFiles(@Nullable File directory, @Nullable String startsWith, @Nullable String endsWith) {

        if (directory == null) {
            return;
        }

        final File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        if (startsWith != null) {
            debugLog(TAG, "Cleaning up files in " + directory + " that start with \"" + startsWith + "\"");
        }

        if (endsWith != null) {
            debugLog(TAG, "Cleaning up files in " + directory + " that end with \"" + endsWith + "\"");
        }

        for (File f : files) {
            if ((startsWith != null && f.getName().startsWith(startsWith))
                || (endsWith != null && f.getName().endsWith(endsWith))) {
                if (!f.delete()) {
                    Log.w(TAG, "Couldn't delete cache file " + f);
                }
            }
        }
    }

    public static void debugLog(String tag, String msg) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, msg);
        }
    }

    public static void debugLog(String tag, String msg, Throwable tr) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, msg, tr);
        }
    }

    // Try to get the version name of the client. Return null on failure.
    public static String getVersionName(Context context) {
        String versionName = null;
        try {
            versionName = context.getPackageManager()
                .getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not get client version name", e);
        }
        return versionName;
    }

}

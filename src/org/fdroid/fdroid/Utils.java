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

import android.content.pm.PackageInfo;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import com.nostra13.universalimageloader.utils.StorageUtils;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.text.SimpleDateFormat;
import java.security.MessageDigest;
import java.util.*;

import org.fdroid.fdroid.data.Repo;

public final class Utils {

    public static final int BUFFER_SIZE = 4096;

    // The date format used for storing dates (e.g. lastupdated, added) in the
    // database.
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

    private static final String[] FRIENDLY_SIZE_FORMAT = {
            "%.0f B", "%.0f KiB", "%.1f MiB", "%.2f GiB" };

    public static final SimpleDateFormat LOG_DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);

    public static String getIconsDir(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        String iconsDir;
        if (metrics.densityDpi >= 640) {
            iconsDir = "/icons-640/";
        } else if (metrics.densityDpi >= 480) {
            iconsDir = "/icons-480/";
        } else if (metrics.densityDpi >= 320) {
            iconsDir = "/icons-320/";
        } else if (metrics.densityDpi >= 240) {
            iconsDir = "/icons-240/";
        } else if (metrics.densityDpi >= 160) {
            iconsDir = "/icons-160/";
        } else {
            iconsDir = "/icons-120/";
        }
        return iconsDir;
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

    public static String getAndroidVersionName(int sdkLevel) {
        if (sdkLevel < 1) return null;
        switch (sdkLevel) {
            case 19: return "4.4";
            case 18: return "4.3";
            case 17: return "4.2";
            case 16: return "4.1";
            case 15: return "4.0.3";
            case 14: return "4.0";
            case 13: return "3.2";
            case 12: return "3.1";
            case 11: return "3.0";
            case 10: return "2.3.3";
            case 9: return "2.3";
            case 8: return "2.2";
            case 7: return "2.1";
            case 6: return "2.0.1";
            case 5: return "2.0";
            case 4: return "1.6";
            case 3: return "1.5";
            case 2: return "1.1";
            case 1: return "1.0";
            default: return "?";
        }
    }

    public static int countSubstringOccurrence(File file, String substring) throws IOException {
        int count = 0;
        FileReader input = null;
        try {
            int currentSubstringIndex = 0;
            char[] buffer = new char[4096];

            input = new FileReader(file);
            int numRead = input.read(buffer);
            while(numRead != -1) {

                for (char c : buffer) {
                    if (c == substring.charAt(currentSubstringIndex)) {
                        currentSubstringIndex ++;
                        if (currentSubstringIndex == substring.length()) {
                            count ++;
                            currentSubstringIndex = 0;
                        }
                    } else {
                        currentSubstringIndex = 0;
                    }
                }
                numRead = input.read(buffer);
            }
        } finally {
            closeQuietly(input);
        }
        return count;
    }

    // return a fingerprint formatted for display
    public static String formatFingerprint(String fingerprint) {
        if (fingerprint.length() != 62)  // SHA-256 is 62 hex chars
            return "BAD FINGERPRINT";
        String displayFP = fingerprint.substring(0, 2);
        for (int i = 2; i < fingerprint.length(); i = i + 2)
            displayFP += " " + fingerprint.substring(i, i + 2);
        return displayFP;
    }

    public static File getApkCacheDir(Context context) {
        File apkCacheDir = new File(
                StorageUtils.getCacheDirectory(context, true), "apks");
        if (!apkCacheDir.exists()) {
            apkCacheDir.mkdir();
        }
        return apkCacheDir;
    }

    public static Map<String, PackageInfo> getInstalledApps(Context context) {
        return installedApkCache.getApks(context);
    }

    public static void clearInstalledApksCache() {
        installedApkCache.emptyCache();
    }

    public static String calcFingerprint(String keyHexString) {
        if (TextUtils.isEmpty(keyHexString))
            return null;
        else
            return calcFingerprint(Hasher.unhex(keyHexString));
    }

    public static String calcFingerprint(Certificate cert) {
        try {
            return calcFingerprint(cert.getEncoded());
        } catch (CertificateEncodingException e) {
            return null;
        }
    }

    public static String calcFingerprint(byte[] key) {
        String ret = null;
        try {
            // keytool -list -v gives you the SHA-256 fingerprint
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(key);
            byte[] fingerprint = digest.digest();
            Formatter formatter = new Formatter(new StringBuilder());
            for (int i = 1; i < fingerprint.length; i++) {
                formatter.format("%02X", fingerprint[i]);
            }
            ret = formatter.toString();
            formatter.close();
        } catch (Exception e) {
            Log.w("FDroid", "Unable to get certificate fingerprint.\n"
                    + Log.getStackTraceString(e));
        }
        return ret;
    }

    public static class CommaSeparatedList implements Iterable<String> {
        private String value;

        private CommaSeparatedList(String list) {
            value = list;
        }

        public static CommaSeparatedList make(List<String> list) {
            if (list == null || list.size() == 0)
                return null;
            else {
                StringBuilder sb = new StringBuilder();
                for(int i = 0; i < list.size(); i ++) {
                    if (i > 0) {
                        sb.append(',');
                    }
                    sb.append(list.get(i));
                }
                return new CommaSeparatedList(sb.toString());
            }
        }

        public static CommaSeparatedList make(String list) {
            if (list == null || list.length() == 0)
                return null;
            else
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
            for (String s : this) {
                if (s.equals(v))
                    return true;
            }
            return false;
        }
    }

    private static InstalledApkCache installedApkCache = null;

    /**
     * We do a lot of querying of the installed app's. As a result, we like
     * to cache this information quite heavily (and flush the cache when new
     * apps are installed). The caching implementation needs to be setup like
     * this so that it is possible to mock for testing purposes.
     */
    public static void setupInstalledApkCache(InstalledApkCache cache) {
        installedApkCache = cache;
    }

    public static class InstalledApkCache {

        protected Map<String, PackageInfo> installedApks = null;

        protected Map<String, PackageInfo> buildAppList(Context context) {
            Map<String, PackageInfo> info = new HashMap<String, PackageInfo>();
            Log.d("FDroid", "Reading installed packages");
            List<PackageInfo> installedPackages = context.getPackageManager().getInstalledPackages(0);
            for (PackageInfo appInfo : installedPackages) {
                info.put(appInfo.packageName, appInfo);
            }
            return info;
        }

        public Map<String, PackageInfo> getApks(Context context) {
            if (installedApks == null) {
                installedApks = buildAppList(context);
            }
            return installedApks;
        }

        public void emptyCache() {
            installedApks = null;
        }

    }


}

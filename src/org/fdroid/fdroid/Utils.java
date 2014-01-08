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

import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.security.MessageDigest;
import java.util.Formatter;
import java.util.Locale;

import android.content.Context;

import com.nostra13.universalimageloader.utils.StorageUtils;

public final class Utils {

    public static final int BUFFER_SIZE = 4096;

    private static final String[] FRIENDLY_SIZE_FORMAT = {
            "%.0f B", "%.0f KiB", "%.1f MiB", "%.2f GiB" };

    public static final SimpleDateFormat LOG_DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);



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
        BufferedReader reader = null;
        try {

            reader = new BufferedReader(new FileReader(file));
            while(true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                count += countSubstringOccurrence(line, substring);
            }

        } finally {
            closeQuietly(reader);
        }
        return count;
    }

    /**
     * Thanks to http://stackoverflow.com/a/767910
     */
    public static int countSubstringOccurrence(String toSearch, String substring) {
        int count = 0;
        int index = 0;
        while (true) {
            index = toSearch.indexOf(substring, index);
            if (index == -1){
                break;
            }
            count ++;
            index += substring.length();
        }
        return count;
    }

    public static String formatFingerprint(DB.Repo repo) {
        return formatFingerprint(repo.pubkey);
    }

    public static String formatFingerprint(String key) {
        String fingerprintString;
        if (key == null) {
            return "";
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(Hasher.unhex(key));
            byte[] fingerprint = digest.digest();
            Formatter formatter = new Formatter(new StringBuilder());
            formatter.format("%02X", fingerprint[0]);
            for (int i = 1; i < fingerprint.length; i++) {
                formatter.format(i % 5 == 0 ? " %02X" : ":%02X",
                        fingerprint[i]);
            }
            fingerprintString = formatter.toString();
            formatter.close();
        } catch (Exception e) {
            Log.w("FDroid", "Unable to get certificate fingerprint.\n"
                    + Log.getStackTraceString(e));
            fingerprintString = "";
        }
        return fingerprintString;
    }
    
    public static File getApkCacheDir(Context context) {
        File apkCacheDir = new File(
                StorageUtils.getCacheDirectory(context, true), "apks");
        if (!apkCacheDir.exists()) {
            apkCacheDir.mkdir();
        }
        return apkCacheDir;
    }

}

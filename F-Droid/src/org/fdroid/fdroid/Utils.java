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
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.XmlResourceParser;
import android.net.Uri;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import com.nostra13.universalimageloader.utils.StorageUtils;
import org.fdroid.fdroid.data.Repo;
import org.xml.sax.XMLReader;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.text.SimpleDateFormat;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

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

    /**
     * use symlinks if they are available, otherwise fall back to copying
     */
    public static boolean symlinkOrCopyFile(File inFile, File outFile) {
        if (new File("/system/bin/ln").exists()) {
            return symlink(inFile, outFile);
        } else {
            return copy(inFile, outFile);
        }
    }

    public static boolean symlink(File inFile, File outFile) {
        int exitCode = -1;
        try {
            Process sh = Runtime.getRuntime().exec("sh");
            OutputStream out = sh.getOutputStream();
            String command = "/system/bin/ln -s " + inFile + " " + outFile
                    + "\nexit\n";
            out.write(command.getBytes("ASCII"));

            final char buf[] = new char[40];
            InputStreamReader reader = new InputStreamReader(sh.getInputStream());
            while (reader.read(buf) != -1)
                throw new IOException("stdout: " + new String(buf));
            reader = new InputStreamReader(sh.getErrorStream());
            while (reader.read(buf) != -1)
                throw new IOException("stderr: " + new String(buf));

            exitCode = sh.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return exitCode == 0;
    }

    public static boolean copy(File inFile, File outFile) {
        InputStream input = null;
        OutputStream output = null;
        try {
            input = new FileInputStream(inFile);
            output = new FileOutputStream(outFile);
            Utils.copy(input, output);
            output.close();
            input.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
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
        "5.0"    // 21
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

    /* PackageManager doesn't give us minSdkVersion, so we have to parse it */
    public static int getMinSdkVersion(Context context, String packageName) {
        try {
            AssetManager am = context.createPackageContext(packageName, 0).getAssets();
            XmlResourceParser xml = am.openXmlResourceParser("AndroidManifest.xml");
            int eventType = xml.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (xml.getName().equals("uses-sdk")) {
                        for (int j = 0; j < xml.getAttributeCount(); j++) {
                            if (xml.getAttributeName(j).equals("minSdkVersion")) {
                                return Integer.parseInt(xml.getAttributeValue(j));
                            }
                        }
                    }
                }
                eventType = xml.nextToken();
            }
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }
        return 8; // some kind of hopeful default
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
        if (TextUtils.isEmpty(fingerprint)
                || fingerprint.length() != 64  // SHA-256 is 64 hex chars
                || fingerprint.matches(".*[^0-9a-fA-F].*")) // its a hex string
            return "BAD FINGERPRINT";
        String displayFP = fingerprint.substring(0, 2);
        for (int i = 2; i < fingerprint.length(); i = i + 2)
            displayFP += " " + fingerprint.substring(i, i + 2);
        return displayFP;
    }

    public static Uri getSharingUri(Context context, Repo repo) {
        if (TextUtils.isEmpty(repo.address))
            return Uri.parse("http://wifi-not-enabled");
        Uri uri = Uri.parse(repo.address.replaceFirst("http", "fdroidrepo"));
        Uri.Builder b = uri.buildUpon();
        b.appendQueryParameter("swap", "1");
        if (!TextUtils.isEmpty(repo.fingerprint))
            b.appendQueryParameter("fingerprint", repo.fingerprint);
        if (!TextUtils.isEmpty(FDroidApp.bssid)) {
            b.appendQueryParameter("bssid", Uri.encode(FDroidApp.bssid));
            if (!TextUtils.isEmpty(FDroidApp.ssid))
                b.appendQueryParameter("ssid", Uri.encode(FDroidApp.ssid));
        }
        return b.build();
    }

    public static File getApkCacheDir(Context context) {
        File apkCacheDir = new File(
                StorageUtils.getCacheDirectory(context, true), "apks");
        if (!apkCacheDir.exists()) {
            apkCacheDir.mkdir();
        }
        return apkCacheDir;
    }

    public static String calcFingerprint(String keyHexString) {
        if (TextUtils.isEmpty(keyHexString)
                || keyHexString.matches(".*[^a-fA-F0-9].*")) {
            Log.e("FDroid", "Signing key certificate was blank or contained a non-hex-digit!");
            return null;
        } else
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
        if (key.length < 256) {
            Log.e("FDroid", "key was shorter than 256 bytes (" + key.length + "), cannot be valid!");
            return null;
        }
        try {
            // keytool -list -v gives you the SHA-256 fingerprint
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(key);
            byte[] fingerprint = digest.digest();
            Formatter formatter = new Formatter(new StringBuilder());
            for (int i = 0; i < fingerprint.length; i++) {
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
                for (int i = 0; i < list.size(); i++) {
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

    // this is all new stuff being added
    public static String hashBytes(byte[] input, String algo) {
        try {
            MessageDigest md = MessageDigest.getInstance(algo);
            byte[] hashBytes = md.digest(input);
            String hash = toHexString(hashBytes);

            md.reset();
            return hash;
        } catch (NoSuchAlgorithmException e) {
            Log.e("FDroid", "Device does not support " + algo + " MessageDisgest algorithm");
            return null;
        }
    }

    public static String getBinaryHash(File apk, String algo) {
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        try {
            MessageDigest md = MessageDigest.getInstance(algo);
            fis = new FileInputStream(apk);
            bis = new BufferedInputStream(fis);

            byte[] dataBytes = new byte[524288];
            int nread = 0;

            while ((nread = bis.read(dataBytes)) != -1)
                md.update(dataBytes, 0, nread);

            byte[] mdbytes = md.digest();
            return toHexString(mdbytes);
        } catch (IOException e) {
            Log.e("FDroid", "Error reading \"" + apk.getAbsolutePath()
                    + "\" to compute " + algo + " hash.");
            return null;
        } catch (NoSuchAlgorithmException e) {
            Log.e("FDroid", "Device does not support " + algo + " MessageDisgest algorithm");
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


    // Need this to add the unimplemented support for ordered and unordered
    // lists to Html.fromHtml().
    public static class HtmlTagHandler implements Html.TagHandler {
        int listNum;

        @Override
        public void handleTag(boolean opening, String tag, Editable output,
                XMLReader reader) {
            if (tag.equals("ul")) {
                if (opening)
                    listNum = -1;
                else
                    output.append('\n');
            } else if (opening && tag.equals("ol")) {
                if (opening)
                    listNum = 1;
                else
                    output.append('\n');
            } else if (tag.equals("li")) {
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
            }
        }
    }

    public static <T> boolean arrayContains(final T[] array, final T v) {
        for (final T e : array) {
            if (e == v || v != null && v.equals(e)) {
                return true;
            }
        }
        return false;
    }

}

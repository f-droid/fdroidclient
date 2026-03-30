/*
 * Copyright (C) 2010-12  Ciaran Gultnieks, ciaran@ciarang.com
 * Copyright (C) 2019 Michael Pöhn, michael.poehn@fsfe.org
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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.DisplayCompat;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.encode.Contents;
import com.google.zxing.encode.QRCodeEncoder;

import org.fdroid.BuildConfig;
import org.fdroid.R;
import org.fdroid.database.Repository;
import org.fdroid.download.Mirror;
import org.fdroid.fdroid.compat.FileCompat;
import org.fdroid.fdroid.data.SanitizedFile;
import org.fdroid.fdroid.net.TreeUriDownloader;
import org.fdroid.index.v2.FileV2;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import vendored.org.apache.commons.codec.digest.DigestUtils;

public final class Utils {

    private static final String TAG = "Utils";

    private static final int BUFFER_SIZE = 4096;

    private static final String[] FRIENDLY_SIZE_FORMAT = {
            "%.0f B", "%.0f KiB", "%.1f MiB", "%.2f GiB",
    };

    private static Pattern safePackageNamePattern;

    private static Handler toastHandler;

    public static String getDisallowInstallUnknownSourcesErrorMessage(Context context) {
        UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        if (Build.VERSION.SDK_INT >= 29
                && userManager.hasUserRestriction(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY)) {
            return context.getString(R.string.has_disallow_install_unknown_sources_globally);
        }
        return context.getString(R.string.has_disallow_install_unknown_sources);
    }

    @NonNull
    public static Uri getUri(String repoAddress, String... pathElements) {
        /*
         * Storage Access Framework URLs have this wacky URL-encoded path within the URL path.
         *
         * i.e.
         * content://authority/tree/313E-1F1C%3A/document/313E-1F1C%3Aguardianproject.info%2Ffdroid%2Frepo
         *
         * Currently don't know a better way to identify these than by content:// prefix,
         * seems the Android SDK expects apps to consider them as opaque identifiers.
         *
         * Note: This hack works for the external storage documents provider for now,
         *       but will most likely fail for other providers.
         *       Using DocumentFile off the UiThread can be used to build path Uris reliably.
         */
        if (repoAddress.startsWith("content://")) {
            StringBuilder result = new StringBuilder(repoAddress);
            for (String element : pathElements) {
                result.append(TreeUriDownloader.ESCAPED_SLASH);
                result.append(element);
            }
            return Uri.parse(result.toString());
        } else { // Normal URL
            Uri.Builder result = Uri.parse(repoAddress).buildUpon();
            for (String element : pathElements) {
                result.appendPath(element);
            }
            return result.build();
        }
    }

    /**
     * Returns the repository address. Usually this is {@link Repository#getAddress()},
     * but in case of a content:// repo, we need to take its local Uri instead,
     * so we can know that we need to use different downloaders for non-HTTP locations.
     */
    public static String getRepoAddress(Repository repository) {
        List<Mirror> mirrors = repository.getMirrors();
        // check if we need to account for non-HTTP mirrors
        String nonHttpUri = null;
        for (Mirror m : mirrors) {
            if (ContentResolver.SCHEME_CONTENT.equals(m.getUrl().getProtocol().getName())
                    || ContentResolver.SCHEME_FILE.equals(m.getUrl().getProtocol().getName())) {
                nonHttpUri = m.getBaseUrl();
                break;
            }
        }
        // return normal canonical URL, if this is a pure HTTP repo
        if (nonHttpUri == null) {
            String address = repository.getAddress();
            if (address.endsWith("/")) return address.substring(0, address.length() - 1);
            return address;
        } else {
            return nonHttpUri;
        }
    }


    public static void copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        while (true) {
            int count = input.read(buffer);
            if (count == -1) {
                break;
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

    private static boolean copyQuietly(File inFile, File outFile) {
        InputStream input = null;
        OutputStream output = null;
        try {
            input = new FileInputStream(inFile);
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

    @NonNull
    public static Uri getLocalRepoUri(Repository repo) {
        if (TextUtils.isEmpty(repo.getAddress())) {
            return Uri.parse("http://wifi-not-enabled");
        }
        Uri uri = Uri.parse(repo.getAddress());
        Uri.Builder b = uri.buildUpon();
        if (!TextUtils.isEmpty(repo.getCertificate())) {
            String fingerprint = DigestUtils.sha256Hex(repo.getCertificate());
            b.appendQueryParameter("fingerprint", fingerprint);
        }
        String scheme = Preferences.get().isLocalRepoHttpsEnabled() ? "https" : "http";
        b.scheme(scheme);
        return b.build();
    }

    public static Uri getSharingUri(Repository repo) {
        if (repo == null || TextUtils.isEmpty(repo.getAddress())) {
            return Uri.parse("http://wifi-not-enabled");
        }
        Uri localRepoUri = getLocalRepoUri(repo);
        Uri.Builder b = localRepoUri.buildUpon();
        b.scheme(localRepoUri.getScheme().replaceFirst("http", "fdroidrepo"));
        b.appendQueryParameter("swap", "1");
        if (!TextUtils.isEmpty(FDroidApp.bssid)) {
            b.appendQueryParameter("bssid", FDroidApp.bssid);
            if (!TextUtils.isEmpty(FDroidApp.ssid)) {
                b.appendQueryParameter("ssid", FDroidApp.ssid);
            }
        }
        return b.build();
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

    public static String getApplicationLabel(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        ApplicationInfo appInfo;
        try {
            appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            return appInfo.loadLabel(pm).toString();
        } catch (PackageManager.NameNotFoundException | Resources.NotFoundException e) {
            Utils.debugLog(TAG, "Could not get application label: " + e.getMessage());
        }
        return packageName; // all else fails, return packageName
    }

    @SuppressWarnings("unused")
    public static class Profiler {
        public final long startTime = System.currentTimeMillis();
        public final String logTag;

        public Profiler(String logTag) {
            this.logTag = logTag;
        }

        public void log(String message) {
            long duration = System.currentTimeMillis() - startTime;
            Utils.debugLog(logTag, "[" + duration + "ms] " + message);
        }
    }

    /**
     * In order to send a {@link Toast} from a {@link android.app.Service}, we
     * have to do these tricks.
     */
    public static void showToastFromService(final Context context, final String msg, final int length) {
        if (toastHandler == null) {
            toastHandler = new Handler(Looper.getMainLooper());
        }
        toastHandler.post(() -> Toast.makeText(context.getApplicationContext(), msg, length).show());
    }

    public static Bitmap generateQrBitmap(@NonNull final AppCompatActivity activity,
                                          @NonNull final String qrData) {
        final DisplayCompat.ModeCompat displayMode = DisplayCompat.getMode(activity,
                activity.getWindowManager().getDefaultDisplay());
        final int qrCodeDimension = Math.min(displayMode.getPhysicalWidth(),
                displayMode.getPhysicalHeight());
        debugLog(TAG, "generating QRCode Bitmap of " + qrCodeDimension + "x" + qrCodeDimension);
        try {
            return new QRCodeEncoder(qrData, null, Contents.Type.TEXT,
                    BarcodeFormat.QR_CODE.toString(), qrCodeDimension).encodeAsBitmap();
        } catch (WriterException e) {
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        }
    }

    public static ArrayList<String> toString(@Nullable List<FileV2> files) {
        if (files == null) return new ArrayList<>(0);
        ArrayList<String> list = new ArrayList<>(files.size());
        for (FileV2 file : files) {
            list.add(file.serialize());
        }
        return list;
    }

    public static List<FileV2> fileV2FromStrings(List<String> list) {
        ArrayList<FileV2> files = new ArrayList<>(list.size());
        for (String s : list) {
            files.add(FileV2.deserialize(s));
        }
        return files;
    }

    /**
     * Keep an instance of this class as an field in an AppCompatActivity for figuring out whether the on
     * screen keyboard is currently visible or not.
     */
    public static class KeyboardStateMonitor {

        private boolean visible = false;

        /**
         * @param contentView this must be the top most Container of the layout used by the AppCompatActivity
         */
        public KeyboardStateMonitor(final View contentView) {
            contentView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                        int screenHeight = contentView.getRootView().getHeight();
                        Rect rect = new Rect();
                        contentView.getWindowVisibleDisplayFrame(rect);
                        int keypadHeight = screenHeight - rect.bottom;
                        visible = keypadHeight >= screenHeight * 0.15;
                    }
            );
        }

        public boolean isKeyboardVisible() {
            return visible;
        }
    }

    public static boolean isPortInUse(String host, int port) {
        boolean result = false;

        try {
            (new Socket(host, port)).close();
            result = true;
        } catch (IOException e) {
            // Could not connect.
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Copy text to the clipboard and show a toast informing the user that something has been copied.
     *
     * @param context the context to use
     * @param label   the label used in the clipboard
     * @param text    the text to copy
     */
    public static void copyToClipboard(@NonNull Context context, @Nullable String label,
                                       @NonNull String text) {
        copyToClipboard(context, label, text, R.string.copied_to_clipboard);
    }

    /**
     * Copy text to the clipboard and show a toast informing the user that the text has been copied.
     *
     * @param context the context to use
     * @param label   the label used in the clipboard
     * @param text    the text to copy
     * @param message the message to show in the toast
     */
    public static void copyToClipboard(@NonNull Context context, @Nullable String label,
                                       @NonNull String text, @StringRes int message) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) {
            // permission denied
            return;
        }
        try {
            clipboard.setPrimaryClip(ClipData.newPlainText(label, text));
            if (Build.VERSION.SDK_INT < 33) {
                // Starting with Android 13 (SDK 33) there is a system dialog with more clipboard actions
                // shown automatically so there is no need to inform the user about the copy action.
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            // should not happen, something went wrong internally
            debugLog(TAG, "Could not copy to clipboard: " + e.getMessage());
        }

    }

    public static String toJsonStringArray(List<String> list) {
        JSONArray jsonArray = new JSONArray();
        for (String str : list) {
            jsonArray.put(str);
        }
        return jsonArray.toString();
    }

    public static List<String> parseJsonStringArray(String json) {
        try {
            JSONArray jsonArray = new JSONArray(json);
            List<String> l = new ArrayList<>(jsonArray.length());
            for (int i = 0; i < jsonArray.length(); i++) {
                l.add(jsonArray.getString(i));
            }
            return l;
        } catch (JSONException e) {
            return Collections.emptyList();
        }
    }
}

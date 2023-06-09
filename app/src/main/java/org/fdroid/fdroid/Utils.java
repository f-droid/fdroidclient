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

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.CharacterStyle;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Consumer;
import androidx.core.util.Supplier;
import androidx.core.view.DisplayCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.encode.Contents;
import com.google.zxing.encode.QRCodeEncoder;

import org.fdroid.IndexFile;
import org.fdroid.database.AppOverviewItem;
import org.fdroid.database.Repository;
import org.fdroid.download.DownloadRequest;
import org.fdroid.download.Mirror;
import org.fdroid.fdroid.compat.FileCompat;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.SanitizedFile;
import org.fdroid.fdroid.net.TreeUriDownloader;
import org.fdroid.index.v2.FileV2;
import org.xml.sax.XMLReader;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import info.guardianproject.netcipher.NetCipher;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import vendored.org.apache.commons.codec.binary.Hex;
import vendored.org.apache.commons.codec.digest.DigestUtils;

public final class Utils {

    private static final String TAG = "Utils";

    private static final int BUFFER_SIZE = 4096;

    private static final String[] FRIENDLY_SIZE_FORMAT = {
            "%.0f B", "%.0f KiB", "%.1f MiB", "%.2f GiB",
    };

    private static RequestOptions iconRequestOptions;
    private static RequestOptions alwaysShowIconRequestOptions;

    private static Pattern safePackageNamePattern;

    private static Handler toastHandler;

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
     * but in case of a content:// repo, we need to take its local Uri instead.
     */
    public static String getRepoAddress(Repository repository) {
        List<Mirror> mirrors = repository.getAllMirrors();
        if (mirrors.size() == 2 && mirrors.get(1).getBaseUrl().startsWith("content://")) {
            return mirrors.get(1).getBaseUrl();
        } else {
            String address = repository.getAddress();
            if (address.endsWith("/")) return address.substring(0, address.length() - 1);
            return address;
        }
    }

    /**
     * @return the directory where cached icons/feature graphics/screenshots are stored
     */
    public static File getImageCacheDir(Context context) {
        File cacheDir = Glide.getPhotoCacheDir(context.getApplicationContext());
        return new File(cacheDir, "icons");
    }

    public static long getImageCacheDirAvailableMemory(Context context) {
        File statDir = getImageCacheDir(context);
        while (statDir != null && !statDir.exists()) {
            statDir = statDir.getParentFile();
        }
        if (statDir == null) {
            return 50 * 1024 * 1024; // just return a minimal amount
        }
        StatFs stat = new StatFs(statDir.getPath());
        return stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
    }

    public static long getImageCacheDirTotalMemory(Context context) {
        File statDir = getImageCacheDir(context);
        while (statDir != null && !statDir.exists()) {
            statDir = statDir.getParentFile();
        }
        if (statDir == null) {
            return 100 * 1024 * 1024; // just return a minimal amount
        }
        StatFs stat = new StatFs(statDir.getPath());
        return stat.getBlockCountLong() * stat.getBlockSizeLong();
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

    public static String getFriendlySize(long size) {
        double s = size;
        int i = 0;
        while (i < FRIENDLY_SIZE_FORMAT.length - 1 && s >= 1024) {
            s = (100 * s / 1024) / 100.0;
            i++;
        }
        return String.format(FRIENDLY_SIZE_FORMAT[i], s);
    }

    private static final String[] ANDROID_VERSION_NAMES = {
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
            "6.0",   // 23
            "7.0",   // 24
            "7.1",   // 25
            "8.0",   // 26
            "8.1",   // 27
            "9.0",   // 28
            "10.0",  // 29
            "11.0",  // 30
    };

    public static String getAndroidVersionName(int sdkLevel) {
        if (sdkLevel < 0) {
            return ANDROID_VERSION_NAMES[0];
        }
        if (sdkLevel >= ANDROID_VERSION_NAMES.length) {
            return String.format(Locale.ENGLISH, "v%d", sdkLevel);
        }
        return ANDROID_VERSION_NAMES[sdkLevel];
    }

    // return a fingerprint formatted for display
    public static String formatFingerprint(Context context, String fingerprint) {
        if (TextUtils.isEmpty(fingerprint)
                || fingerprint.length() != 64 // SHA-256 is 64 hex chars
                || fingerprint.matches(".*[^0-9a-fA-F].*")) { // its a hex string
            return context.getString(R.string.bad_fingerprint);
        }
        StringBuilder displayFP = new StringBuilder(fingerprint.substring(0, 2));
        for (int i = 2; i < fingerprint.length(); i = i + 2) {
            displayFP.append(" ").append(fingerprint.substring(i, i + 2));
        }
        return displayFP.toString().toUpperCase(Locale.US);
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

    public static String calcFingerprint(Certificate cert) {
        if (cert == null) {
            return null;
        }
        try {
            return calcFingerprint(cert.getEncoded());
        } catch (CertificateEncodingException e) {
            return null;
        }
    }

    private static String calcFingerprint(byte[] key) {
        if (key == null) {
            return null;
        }
        if (key.length < 256) {
            Log.e(TAG, "key was shorter than 256 bytes (" + key.length + "), cannot be valid!");
            return null;
        }
        String ret = null;
        try {
            // keytool -list -v gives you the SHA-256 fingerprint
            MessageDigest digest = MessageDigest.getInstance("sha256");
            digest.update(key);
            byte[] fingerprint = digest.digest();
            Formatter formatter = new Formatter(new StringBuilder());
            for (byte aFingerprint : fingerprint) {
                formatter.format("%02X", aFingerprint);
            }
            ret = formatter.toString();
            formatter.close();
        } catch (Throwable e) { // NOPMD
            Log.w(TAG, "Unable to get certificate fingerprint", e);
        }
        return ret;
    }

    /**
     * Checks the file against the provided hash, returning whether it is a match.
     */
    public static boolean isFileMatchingHash(File file, String hash, String hashType) {
        if (file == null || !file.exists() || TextUtils.isEmpty(hash)) {
            return false;
        }
        return hash.equals(getFileHexDigest(file, hashType));
    }

    /**
     * Get the standard, lowercase SHA-256 fingerprint used to represent an
     * APK or JAR signing key. <b>NOTE</b>: this does not handle signers that
     * have multiple X.509 signing certificates.
     * <p>
     * Calling the X.509 signing certificate the "signature" is incorrect, e.g.
     * {@link PackageInfo#signatures} or {@link android.content.pm.Signature}.
     * The Android docs about APK signatures call this the "signer".
     *
     * @see org.fdroid.fdroid.data.Apk#signer
     * @see PackageInfo#signatures
     * @see <a href="https://source.android.com/docs/security/features/apksigning/v2">APK Signature Scheme v2</a>
     */
    @Nullable
    public static String getPackageSigner(PackageInfo info) {
        if (info == null || info.signatures == null || info.signatures.length < 1) {
            return null;
        }
        return DigestUtils.sha256Hex(info.signatures[0].toByteArray());
    }

    /**
     * Gets the {@link RequestOptions} instance used to configure
     * {@link Glide} instances used to display app icons that should always be
     * downloaded.  It lazy loads a reusable static instance.
     */
    public static RequestOptions getAlwaysShowIconRequestOptions() {
        if (alwaysShowIconRequestOptions == null) {
            alwaysShowIconRequestOptions = new RequestOptions()
                    .onlyRetrieveFromCache(false)
                    .error(R.drawable.ic_repo_app_default)
                    .fallback(R.drawable.ic_repo_app_default);
        }
        return alwaysShowIconRequestOptions;
    }

    /**
     * Write app icon into the view, downloading it as necessary and if the
     * settings allow it.  Fall back to the placeholder icon otherwise.
     *
     * @see Preferences#isBackgroundDownloadAllowed()
     */
    public static void setIconFromRepoOrPM(@NonNull App app, ImageView iv, Context context) {
        loadWithGlide(context, app.repoId, app.iconFile, iv);
    }

    @Deprecated
    public static void setIconFromRepoOrPM(@NonNull AppOverviewItem app, ImageView iv, Context context) {
        long repoId = app.getRepoId();
        IndexFile iconFile = app.getIcon(App.getLocales());
        loadWithGlide(context, repoId, iconFile, iv);
    }

    public static void loadWithGlide(Context context, long repoId, @Nullable IndexFile file, ImageView iv) {
        if (file == null) {
            Glide.with(context).clear(iv);
            iv.setImageResource(R.drawable.ic_repo_app_default);
            return;
        }
        if (iconRequestOptions == null) {
            iconRequestOptions = new RequestOptions()
                    .error(R.drawable.ic_repo_app_default)
                    .fallback(R.drawable.ic_repo_app_default);
        }
        RequestOptions options = iconRequestOptions.onlyRetrieveFromCache(
                !Preferences.get().isBackgroundDownloadAllowed());

        Repository repo = FDroidApp.getRepoManager(context).getRepository(repoId);
        if (repo == null) {
            Glide.with(context).clear(iv);
            return;
        }
        String address = getRepoAddress(repo);
        if (address.startsWith("content://")) {
            String uri = getUri(address, file.getName().split("/")).toString();
            Glide.with(context).load(uri).apply(options).into(iv);
        } else {
            DownloadRequest request = getDownloadRequest(repo, file);
            Glide.with(context).load(request).apply(options).into(iv);
        }
    }

    @Nullable
    public static DownloadRequest getDownloadRequest(@NonNull Repository repo, @Nullable IndexFile file) {
        if (file == null) return null;
        List<Mirror> mirrors = repo.getMirrors();
        Proxy proxy = NetCipher.getProxy();
        return new DownloadRequest(file, mirrors, proxy, repo.getUsername(), repo.getPassword());
    }

    /**
     * Get the checksum hash of the file {@code file} using the algorithm in {@code hashAlgo}.
     * {@code file} must exist on the filesystem and {@code hashAlgo} must be supported
     * by this device, otherwise an {@link IllegalArgumentException} is thrown.  This
     * method must be very defensive about checking whether the file exists, since APKs
     * can be uninstalled/deleted in background at any time, even if this is in the
     * middle of running.
     * <p>
     * This also will run into filesystem corruption if the device is having trouble.
     * So hide those so F-Droid does not pop up crash reports about that. As such this
     * exception-message-parsing-and-throwing-a-new-ignorable-exception-hackery is
     * probably warranted. See https://www.gitlab.com/fdroid/fdroidclient/issues/855
     * for more detail.
     *
     * @see
     * <a href="https://gitlab.com/fdroid/fdroidclient/-/merge_requests/1089#note_822501322">forced to vendor Apache Commons Codec</a>
     */
    @Nullable
    static String getFileHexDigest(File file, String hashAlgo) {
        try {
            return Hex.encodeHexString(DigestUtils.digest(DigestUtils.getDigest(hashAlgo), file));
        } catch (IOException e) {
            String message = e.getMessage();
            if (message.contains("read failed: EIO (I/O error)")) {
                Utils.debugLog(TAG, "potential filesystem corruption while accessing " + file + ": " + message);
            } else if (message.contains(" ENOENT ")) {
                Utils.debugLog(TAG, file + " vanished: " + message);
            }
        }
        return null;
    }

    /**
     * Formats the app name using "sans-serif" and then appends the summary after a space with
     * "sans-serif-light". Doesn't mandate any font sizes or any other styles, that is up to the
     * {@link android.widget.TextView} which it ends up being displayed in.
     */
    public static CharSequence formatAppNameAndSummary(String appName, @Nullable String summary) {
        String toFormat = appName;
        if (summary != null) toFormat += ' ' + summary;
        CharacterStyle normal = new TypefaceSpan("sans-serif");
        CharacterStyle light = new TypefaceSpan("sans-serif-light");

        SpannableStringBuilder sb = new SpannableStringBuilder(toFormat);
        sb.setSpan(normal, 0, appName.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(light, appName.length(), toFormat.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        return sb;
    }

    /**
     * This is not strict validation of the package name, this is just to make
     * sure that the package name is not used as an attack vector, e.g. SQL
     * Injection.
     */
    public static boolean isSafePackageName(@Nullable String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        if (safePackageNamePattern == null) {
            safePackageNamePattern = Pattern.compile("[a-zA-Z0-9._]+");
        }
        return safePackageNamePattern.matcher(packageName).matches();
    }

    /**
     * Calculate the number of days since the given date.
     */
    public static int daysSince(long ms) {
        long msDiff = Calendar.getInstance().getTimeInMillis() - ms;
        return (int) TimeUnit.MILLISECONDS.toDays(msDiff);
    }

    public static String formatLastUpdated(@NonNull Resources res, @NonNull Date date) {
        return formatLastUpdated(res, date.getTime());
    }

    public static String formatLastUpdated(@NonNull Resources res, long date) {
        double msDiff = System.currentTimeMillis() - date;
        long days = Math.round(msDiff / DateUtils.DAY_IN_MILLIS);
        long weeks = Math.round(msDiff / (DateUtils.WEEK_IN_MILLIS));
        long months = Math.round(msDiff / (DateUtils.DAY_IN_MILLIS * 30));
        long years = Math.round(msDiff / (DateUtils.YEAR_IN_MILLIS));

        if (days < 1) {
            return res.getString(R.string.details_last_updated_today);
        } else if (weeks < 3) {
            return res.getQuantityString(R.plurals.details_last_update_days, (int) days, days);
        } else if (months < 2) {
            return res.getQuantityString(R.plurals.details_last_update_weeks, (int) weeks, weeks);
        } else if (years < 2) {
            return res.getQuantityString(R.plurals.details_last_update_months, (int) months, months);
        } else {
            return res.getQuantityString(R.plurals.details_last_update_years, (int) years, years);
        }
    }

    /**
     * Need this to add the unimplemented support for ordered and unordered
     * lists to Html.fromHtml().
     */
    public static class HtmlTagHandler implements Html.TagHandler {
        int listNum;

        @Override
        public void handleTag(boolean opening, String tag, Editable output,
                              XMLReader reader) {
            switch (tag) {
                case "ul":
                    if (opening) {
                        listNum = -1;
                    } else {
                        output.append('\n');
                    }
                    break;
                case "ol":
                    if (opening) {
                        listNum = 1;
                    } else {
                        output.append('\n');
                    }
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

    /**
     * Try to get the {@link PackageInfo#versionName} of the
     * client.
     *
     * @return null on failure
     */
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

    public static String getUserAgent() {
        return "F-Droid " + BuildConfig.VERSION_NAME;
    }

    /**
     * Try to get the {@link PackageInfo} for the {@code packageName} provided.
     *
     * @return null on failure
     */
    public static PackageInfo getPackageInfo(Context context, String packageName) {
        try {
            return context.getPackageManager().getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            debugLog(TAG, "Could not get PackageInfo: ", e);
        }
        return null;
    }

    /**
     * Converts a {@code long} bytes value, like from {@link File#length()}, to
     * an {@code int} value that is kilobytes, suitable for things like
     * {@link android.widget.ProgressBar#setMax(int)} or
     * {@link androidx.core.app.NotificationCompat.Builder#setProgress(int, int, boolean)}
     */
    public static int bytesToKb(long bytes) {
        return (int) (bytes / 1024);
    }

    /**
     * Converts two {@code long} bytes values, like from {@link File#length()}, to
     * an {@code int} value that is a percentage, suitable for things like
     * {@link android.widget.ProgressBar#setMax(int)} or
     * {@link androidx.core.app.NotificationCompat.Builder#setProgress(int, int, boolean)}.
     * {@code total} must never be zero!
     */
    public static int getPercent(long current, long total) {
        return (int) ((100L * current + total / 2) / total);
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

    public static void applySwipeLayoutColors(SwipeRefreshLayout swipeLayout) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = swipeLayout.getContext().getTheme();
        theme.resolveAttribute(R.attr.colorPrimary, typedValue, true);
        swipeLayout.setColorSchemeColors(typedValue.data);
    }

    public static boolean canConnectToSocket(String host, int port) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 5);
            socket.close();
            return true;
        } catch (IOException e) {
            // Could not connect.
            return false;
        }
    }

    public static boolean isServerSocketInUse(int port) {
        try {
            (new ServerSocket(port)).close();
            return false;
        } catch (IOException e) {
            // Could not connect.
            return true;
        }
    }

    @NonNull
    public static Single<Bitmap> generateQrBitmap(@NonNull final AppCompatActivity activity,
                                                  @NonNull final String qrData) {
        return Single.fromCallable(() -> {
            // TODO: Use DisplayCompat.getMode() once it becomes available in Core 1.6.0.
            final DisplayCompat.ModeCompat displayMode = DisplayCompat.getSupportedModes(activity,
                    activity.getWindowManager().getDefaultDisplay())[0];
            final int qrCodeDimension = Math.min(displayMode.getPhysicalWidth(),
                    displayMode.getPhysicalHeight());
            debugLog(TAG, "generating QRCode Bitmap of " + qrCodeDimension + "x" + qrCodeDimension);

            return new QRCodeEncoder(qrData, null, Contents.Type.TEXT,
                    BarcodeFormat.QR_CODE.toString(), qrCodeDimension).encodeAsBitmap();
        })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorReturnItem(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
                .doOnError(throwable -> Log.e(TAG, "Could not encode QR as bitmap", throwable));
    }

    public static <T> Disposable runOffUiThread(Supplier<T> supplier, Consumer<T> consumer) {
        return Single.fromCallable(supplier::get)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(throwable -> Log.e(TAG, "Error running off UiThread", throwable))
                .subscribe(consumer::accept, e -> {
                    Log.e(TAG, "Could not run off UI thread: ", e);
                    consumer.accept(null);
                });
    }

    public static Disposable runOffUiThread(Runnable runnable) {
        return Single.fromCallable(() -> {
            runnable.run();
            return true;
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(throwable -> Log.e(TAG, "Error running off UiThread", throwable))
                .subscribe();
    }

    public static <T> void observeOnce(LiveData<T> liveData, LifecycleOwner lifecycleOwner, Consumer<T> consumer) {
        liveData.observe(lifecycleOwner, new Observer<T>() {
            @Override
            public void onChanged(T t) {
                consumer.accept(t);
                liveData.removeObserver(this);
            }
        });
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
}

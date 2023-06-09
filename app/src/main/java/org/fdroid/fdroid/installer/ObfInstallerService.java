package org.fdroid.fdroid.installer;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.core.content.FileProvider;

import org.apache.commons.io.FileUtils;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.net.DownloaderService;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * An {@link IntentService} subclass for installing {@code .obf} and {@code .obf.zip}
 * map files into OsmAnd.  This will unzip the {@code .obf}
 */
public class ObfInstallerService extends IntentService {
    private static final String TAG = "ObfInstallerService";

    private static final String ACTION_INSTALL_OBF = "org.fdroid.fdroid.installer.action.INSTALL_OBF";

    private static final String EXTRA_OBF_PATH = "org.fdroid.fdroid.installer.extra.OBF_PATH";

    public ObfInstallerService() {
        super("ObfInstallerService");
    }

    public static void install(Context context, Uri canonicalUri, App app, Apk apk, File path) {
        Intent intent = new Intent(context, ObfInstallerService.class);
        intent.setAction(ACTION_INSTALL_OBF);
        intent.putExtra(DownloaderService.EXTRA_CANONICAL_URL, canonicalUri.toString());
        intent.putExtra(Installer.EXTRA_APP, app);
        intent.putExtra(Installer.EXTRA_APK, apk);
        intent.putExtra(EXTRA_OBF_PATH, path.getAbsolutePath());
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null || !ACTION_INSTALL_OBF.equals(intent.getAction())) {
            Log.e(TAG, "received invalid intent: " + intent);
            return;
        }
        Uri canonicalUri = Uri.parse(intent.getStringExtra(DownloaderService.EXTRA_CANONICAL_URL));
        final App app = intent.getParcelableExtra(Installer.EXTRA_APP);
        final Apk apk = intent.getParcelableExtra(Installer.EXTRA_APK);
        final String path = intent.getStringExtra(EXTRA_OBF_PATH);
        final String extension = MimeTypeMap.getFileExtensionFromUrl(path);
        if ("obf".equals(extension)) {
            sendPostInstallAndCompleteIntents(canonicalUri, app, apk, new File(path));
            return;
        }
        if (!"zip".equals(extension)) {
            sendBroadcastInstall(Installer.ACTION_INSTALL_INTERRUPTED, canonicalUri, app, apk,
                    "Only .obf and .zip files are supported: " + path);
            return;
        }
        try {
            File zip = new File(path);
            ZipFile zipFile = new ZipFile(zip);
            if (zipFile.size() < 1) {
                sendBroadcastInstall(Installer.ACTION_INSTALL_INTERRUPTED, canonicalUri, app, apk,
                        "Corrupt or empty ZIP file!");
            }
            ZipEntry zipEntry = zipFile.entries().nextElement();
            File extracted = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    zipEntry.getName());
            FileUtils.copyInputStreamToFile(zipFile.getInputStream(zipEntry), extracted);
            // Since we delete the file here, it won't show as installed anymore
            zip.delete();
            sendPostInstallAndCompleteIntents(canonicalUri, app, apk, extracted);
        } catch (IOException e) {
            e.printStackTrace();
            sendBroadcastInstall(Installer.ACTION_INSTALL_INTERRUPTED, canonicalUri, app, apk, e.getMessage());
        }
    }

    private void sendBroadcastInstall(String action, Uri canonicalUri, App app, Apk apk, String msg) {
        Installer.sendBroadcastInstall(this, canonicalUri, action, app, apk, null, msg);
    }

    /**
     * Once the file is downloaded and installed, send an {@link Intent} to
     * let map apps know that the file is available for install.
     * <p>
     * When this was written, OsmAnd only supported importing OBF files via a
     * {@code file:///} URL, so this disables {@link android.os.FileUriExposedException}.
     */
    void sendPostInstallAndCompleteIntents(Uri canonicalUri, App app, Apk apk, File file) {
        if (Build.VERSION.SDK_INT >= 24) {
            try {
                Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
                m.invoke(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension("obf");
        if (TextUtils.isEmpty(mimeType)) {
            mimeType = "application/octet-stream";
        }
        if (Build.VERSION.SDK_INT < 24) {
            intent.setDataAndType(Uri.fromFile(file), mimeType);
        } else {
            intent.setDataAndType(FileProvider.getUriForFile(this, Installer.AUTHORITY, file), mimeType);
        }
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
            sendBroadcastInstall(Installer.ACTION_INSTALL_COMPLETE, canonicalUri, app, apk, null);
        } else {
            Log.i(TAG, "No AppCompatActivity available to handle " + intent);
            sendBroadcastInstall(Installer.ACTION_INSTALL_INTERRUPTED, canonicalUri, app, apk, null);
        }
    }
}
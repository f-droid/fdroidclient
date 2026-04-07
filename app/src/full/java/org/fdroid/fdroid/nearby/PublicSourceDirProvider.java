package org.fdroid.fdroid.nearby;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;

/**
 * A collection of tricks to make it possible to share the APKs of apps that
 * are installed on the device.  {@code file:///} are no longer a viable option.
 * Also, the official MIME Type for APK files is
 * {@code application/vnd.android.package-archive}.  It is often blocked by
 * Android, so this provides a MIME Type that is more likely to get around some
 * of those blocks.
 */
public class PublicSourceDirProvider extends ContentProvider {

    public static final String TAG = "PublicSourceDirProvider";

    public static final String SHARE_APK_MIME_TYPE = "application/zip"; // TODO maybe use intent.setType("*/*");

    private static PackageManager pm;

    @Override
    public boolean onCreate() {
        return true;
    }

    public static Uri getUri(Context context, String packageName) {
        return Uri.parse(String.format(Locale.ENGLISH, "content://%s.nearby.%s/%s.apk",
                context.getPackageName(), TAG, packageName));
    }

    public static Intent getApkShareIntent(Context context, String packageName) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        Uri apkUri = getUri(context, packageName);
        intent.setType(SHARE_APK_MIME_TYPE);
        intent.putExtra(Intent.EXTRA_STREAM, apkUri);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return intent;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection,
                        @Nullable String selection, @Nullable String[] selectionArgs,
                        @Nullable String sortOrder) {
        MatrixCursor metadataCursor = new MatrixCursor(new String[]{
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.DATA,
                MediaStore.MediaColumns.SIZE,
        });
        try {
            ApplicationInfo applicationInfo = getApplicationInfo(uri);
            File f = new File(applicationInfo.publicSourceDir);
            metadataCursor.addRow(new Object[]{
                    pm.getApplicationLabel(applicationInfo).toString().replace(" ", "") + ".apk",
                    SHARE_APK_MIME_TYPE,
                    Uri.parse("file://" + f.getCanonicalPath()),
                    f.length(),
            });
        } catch (PackageManager.NameNotFoundException | IOException e) {
            e.printStackTrace();
        }
        return metadataCursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return SHARE_APK_MIME_TYPE;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        throw new IllegalStateException("unimplemented");
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values,
                      @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, @NonNull String mode) throws FileNotFoundException {
        try {
            ApplicationInfo applicationInfo = getApplicationInfo(uri);
            File apkFile = new File(applicationInfo.publicSourceDir);
            return ParcelFileDescriptor.open(apkFile, ParcelFileDescriptor.MODE_READ_ONLY);
        } catch (IOException | PackageManager.NameNotFoundException e) {
            throw new FileNotFoundException(e.getLocalizedMessage());
        }
    }

    private ApplicationInfo getApplicationInfo(Uri uri) throws PackageManager.NameNotFoundException {
        if (pm == null) {
            pm = getContext().getPackageManager();
        }
        String apkName = uri.getLastPathSegment();
        String packageName = apkName.substring(0, apkName.length() - 4);
        return pm.getApplicationInfo(packageName, 0);
    }
}

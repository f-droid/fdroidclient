package org.fdroid.fdroid.nearby;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class PublicSourceDirProviderTest {
    public static final String TAG = "DataApkProviderTest";

    Context context;
    List<PackageInfo> packageInfoList;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        packageInfoList = null;
    }

    /**
     * Test whether reading installed APKs via our custom {@link android.content.ContentProvider}
     * works.  This skips system apps just to make the test easier to manage.  It also only
     * copies max 3 apps so it doesn't take a long time to run.
     */
    @Test
    public void testCopyFromGetUri() throws IOException {
        int copyTotal = 3;
        PackageManager pm = context.getPackageManager();
        List<PackageInfo> packageInfoList = pm.getInstalledPackages(0);
        for (PackageInfo packageInfo : packageInfoList) {
            File apk = new File(packageInfo.applicationInfo.publicSourceDir);
            if (apk.getCanonicalPath().startsWith("/system")) {
                continue;
            }
            Uri uri = PublicSourceDirProvider.getUri(context, packageInfo.packageName);
            InputStream is = null;
            File f = null;
            is = context.getContentResolver().openInputStream(uri);
            f = File.createTempFile("received", ".apk");
            FileUtils.copyInputStreamToFile(is, f);
            assertTrue("dest file " + f + " should exist", f.exists());
            assertEquals(f + " should equal " + apk, apk.length(), f.length());
            f.delete();

            copyTotal--;
            if (copyTotal < 0) {
                break;
            }
        }
    }

    /**
     * Test whether querying the custom {@link android.content.ContentProvider}
     * for installed APKs returns the right kind of data.
     */
    @Test
    public void testQuery() throws IOException {
        PackageManager pm = context.getPackageManager();
        List<PackageInfo> packageInfoList = pm.getInstalledPackages(0);
        for (PackageInfo packageInfo : packageInfoList) {
            File apk = new File(packageInfo.applicationInfo.publicSourceDir);
            if (apk.getCanonicalPath().startsWith("/system")) {
                continue;
            }
            Uri uri = PublicSourceDirProvider.getUri(context, packageInfo.packageName);
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            assertNotNull(cursor);

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                assertNotNull(cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)));
                cursor.moveToNext();
            }
            cursor.close();
        }
    }
}

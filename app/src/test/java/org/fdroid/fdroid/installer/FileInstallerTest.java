package org.fdroid.fdroid.installer;

import android.content.ContextWrapper;

import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.data.Apk;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

import androidx.test.core.app.ApplicationProvider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(RobolectricTestRunner.class)
public class FileInstallerTest {
    public static final String TAG = "FileInstallerTest";

    private ContextWrapper context;

    @Before
    public final void setUp() {
        context = ApplicationProvider.getApplicationContext();
        Preferences.setupForTests(context);
        ShadowLog.stream = System.out;
    }

    @Test
    public void testInstallOtaZip() {
        Apk apk = new Apk();
        apk.apkName = "org.fdroid.fdroid.privileged.ota_2010.zip";
        apk.packageName = "org.fdroid.fdroid.privileged.ota";
        apk.versionCode = 2010;
        assertFalse(apk.isApk());
        Installer installer = InstallerFactory.create(context, apk);
        assertEquals("should be a FileInstaller",
                FileInstaller.class,
                installer.getClass());
    }
}

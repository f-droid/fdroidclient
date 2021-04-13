package org.fdroid.fdroid.nearby;

import android.content.Context;
import android.text.TextUtils;

import org.apache.commons.io.IOUtils;
import org.fdroid.fdroid.IndexUpdater;
import org.fdroid.fdroid.Utils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import androidx.test.core.app.ApplicationProvider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public class LocalRepoKeyStoreTest {

    @Test
    public void testSignZip() throws IOException, LocalRepoKeyStore.InitException, IndexUpdater.SigningException {
        Context context = ApplicationProvider.getApplicationContext();

        File xmlIndexJarUnsigned = File.createTempFile(getClass().getName(), "unsigned.jar");
        BufferedOutputStream bo = new BufferedOutputStream(new FileOutputStream(xmlIndexJarUnsigned));
        JarOutputStream jo = new JarOutputStream(bo);
        JarEntry je = new JarEntry(IndexUpdater.DATA_FILE_NAME);
        jo.putNextEntry(je);
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("smallRepo.xml");
        IOUtils.copy(inputStream, jo);
        jo.close();
        bo.close();

        LocalRepoKeyStore localRepoKeyStore = LocalRepoKeyStore.get(context);
        Certificate localCert = localRepoKeyStore.getCertificate();
        assertFalse(TextUtils.isEmpty(Utils.calcFingerprint(localCert)));

        File xmlIndexJar = File.createTempFile(getClass().getName(), IndexUpdater.SIGNED_FILE_NAME);
        localRepoKeyStore.signZip(xmlIndexJarUnsigned, xmlIndexJar);

        JarFile jarFile = new JarFile(xmlIndexJar, true);
        JarEntry indexEntry = (JarEntry) jarFile.getEntry(IndexUpdater.DATA_FILE_NAME);
        byte[] data = IOUtils.toByteArray(jarFile.getInputStream(indexEntry));
        assertEquals(17187, data.length);
        assertNotNull(IndexUpdater.getSigningCertFromJar(indexEntry));
    }
}

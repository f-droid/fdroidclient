package kellinwood.security.zipsigner;

import kellinwood.security.zipsigner.ZipSigner;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.operator.OperatorCreationException;
import org.fdroid.fdroid.nearby.LocalRepoKeyStore;
import org.fdroid.index.v1.IndexV1VerifierKt;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * This test the JAR signing functions of {@link ZipSigner}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk={23, 25, 32}) // minSdkVersion, targetSdkVersion, max SDK supported by Robolectric
public class ZipSignerTest {
    public static final String TAG = "ZipSignerTest";

    private File unsigned;
    private File signed;
    
    @Before
    public void setUp() {
        ShadowLog.stream = System.out;
        try {
            unsigned = File.createTempFile(getClass().getName(), "unsigned.jar");
            BufferedOutputStream bo = new BufferedOutputStream(new FileOutputStream(unsigned));
            JarOutputStream jo = new JarOutputStream(bo);
            JarEntry je = new JarEntry(IndexV1VerifierKt.DATA_FILE_NAME);
            jo.putNextEntry(je);
            IOUtils.write("{\"fake\": \"json\"}", jo);
            jo.flush();
            jo.close();
            bo.close();
        } catch (IOException e) {
            fail();
        }
    }
    
    @After
    public void tearDown() {
        if (unsigned != null) {
            unsigned.delete();
        }
        if (signed != null) {
            signed.delete();
        }
    }

    @Test
    public void testSignApk()
        throws CertificateException, ClassNotFoundException, GeneralSecurityException, IllegalAccessException, InstantiationException, IOException, NoSuchAlgorithmException, OperatorCreationException {
 
        System.out.println("wrote " + unsigned);
        assertTrue(unsigned.exists());
        assertTrue(unsigned.length() > 0);

        ZipSigner zipSigner = new ZipSigner();
        KeyPair keys = LocalRepoKeyStore.generateRandomKeypair();
        PrivateKey privateKey = keys.getPrivate();
        X500Name subject = new X500Name("O=test,OU=suite");
        X509Certificate cert = (X509Certificate) LocalRepoKeyStore.generateSelfSignedCertChain(keys, subject);
        File signed = File.createTempFile(getClass().getName(), "signed.jar");

        zipSigner.setKeys("test", cert, privateKey, LocalRepoKeyStore.DEFAULT_SIG_ALG, null);
        zipSigner.signZip(unsigned.getAbsolutePath(), signed.getAbsolutePath());
        System.out.println("signed " + unsigned + " into " + signed);

        assertTrue(signed.exists());
        assertTrue(signed.length() > unsigned.length());
   }
}

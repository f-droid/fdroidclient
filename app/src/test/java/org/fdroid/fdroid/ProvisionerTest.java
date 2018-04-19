package org.fdroid.fdroid;

import org.fdroid.fdroid.shadows.ShadowLog;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author Michael Poehn (michael.poehn@fsfe.org)
 */
@Config(constants = BuildConfig.class)
@RunWith(RobolectricTestRunner.class)
@SuppressWarnings("LineLength")
public class ProvisionerTest {

    @Before
    public void setUp() {
        ShadowLog.stream = System.out;
    }

    @Test
    public void provisionLookup() throws IOException {
        // wired hack for getting resource dir path ...
        String resourceDir = getResourceFile(
                "demo_credentials_user1.fdrp").getParent();

        Provisioner p = new Provisioner();
        List<File> files = p.findProvisionFilesInDir(new File(resourceDir));

        List<String> expectedFilenames = Arrays.asList(
                "demo_credentials_user1.fdrp",
                "demo_credentials_user2.fdrp");

        Assert.assertEquals(2, files.size());
        for (File f : files) {
            Assert.assertTrue("unexpected file name " + f.getName(), expectedFilenames.contains(f.getName()));
        }
    }

    @Test
    public void rot13() {
        Provisioner p = new Provisioner();
        String result = p.rot13("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890{}\"':=");
        Assert.assertEquals("nopqrstuvwxyzabcdefghijklmNOPQRSTUVWXYZABCDEFGHIJKLM1234567890{}\"':=", result);
    }

    @Test
    public void deobfuscate() {
        Provisioner p = new Provisioner();
        String result = p.deobfuscate("rlWVMKWuL2kcqUImVwbaGz90nTyhMlOyozE1pzImVTW1qPOwnTShM2HhWljXVPNtVPq3nTIhWmbtJlWuLz91qPN1ZQNtDv5QYvWqsD==");
        Assert.assertEquals("{\"Heraclitus\":'Nothing endures but change.',\n    'when': [\"about 500 B.C.\"]}", result);
    }

    @Test
    public void extractProvisionsPlaintextUnobfuscated() throws IOException {
        Provisioner p = new Provisioner();
        List<File> files = Arrays.asList(getResourceFile("demo_credentials_user2.fdrp"));
        List<Provisioner.ProvisionPlaintext> result = p.extractProvisionsPlaintext(files);

        Assert.assertEquals(result.size(), 1);
        Assert.assertEquals("{\"username\": \"user2\", \"password\": \"other secret\", \"name\": \"Example Repo\", \"url\": \"https://example.com/fdroid/repo\", \"sigfp\": \"1111222233334444555566667777888899990000aaaabbbbccccddddeeeeffff\"}", result.get(0).getRepositoryProvision());
        Assert.assertTrue(String.valueOf(result.get(0).getProvisionPath()).endsWith("demo_credentials_user2.fdrp"));
    }

    @Test
    public void extractProvisionsPlaintextObfuscated() throws IOException {
        Provisioner p = new Provisioner();
        List<File> files = Arrays.asList(getResourceFile("demo_credentials_user1.fdrp"));
        List<Provisioner.ProvisionPlaintext> result = p.extractProvisionsPlaintext(files);

        Assert.assertEquals(result.size(), 1);
        Assert.assertEquals("{\"sigfp\": \"1111222233334444555566667777888899990000aaaabbbbccccddddeeeeffff\", \"name\": \"Example Repo\", \"password\": \"secret1\", \"url\": \"https://example.com/fdroid/repo\", \"username\": \"user1\"}", result.get(0).getRepositoryProvision());
        Assert.assertTrue(String.valueOf(result.get(0).getProvisionPath()).endsWith("demo_credentials_user1.fdrp"));
    }

    @Test
    public void parseProvisions() {

        List<Provisioner.ProvisionPlaintext> plaintexts = Arrays.asList(new Provisioner.ProvisionPlaintext(), new Provisioner.ProvisionPlaintext());
        plaintexts.get(0).setProvisionPath("/some/dir/abc.fdrp");
        plaintexts.get(0).setRepositoryProvision("{\"username\": \"user1\", \"password\": \"secret1\", \"name\": \"test repo a\", \"url\": \"https://example.com/fdroid/repo\", \"sigfp\": \"1111222233334444555566667777888899990000aaaabbbbccccddddeeeeffff\"}");
        plaintexts.get(1).setProvisionPath("/some/dir/def.fdrp");
        plaintexts.get(1).setRepositoryProvision("{\"username\": \"user2\", \"name\": \"test repo a\", \"password\": \"other secret\", \"url\": \"https://example.com/fdroid/repo\", \"sigfp\": \"1111222233334444555566667777888899990000aaaabbbbccccddddeeeeffff\"}");

        Provisioner p = new Provisioner();
        List<Provisioner.Provision> result = p.parseProvisions(plaintexts);

        Assert.assertEquals("/some/dir/abc.fdrp", result.get(0).getProvisonPath());
        Assert.assertEquals("test repo a", result.get(0).getRepositoryProvision().getName());
        Assert.assertEquals("https://example.com/fdroid/repo", result.get(0).getRepositoryProvision().getUrl());
        Assert.assertEquals("1111222233334444555566667777888899990000aaaabbbbccccddddeeeeffff", result.get(0).getRepositoryProvision().getSigfp());
        Assert.assertEquals("user1", result.get(0).getRepositoryProvision().getUsername());
        Assert.assertEquals("secret1", result.get(0).getRepositoryProvision().getPassword());

        Assert.assertEquals("/some/dir/def.fdrp", result.get(1).getProvisonPath());
        Assert.assertEquals("test repo a", result.get(1).getRepositoryProvision().getName());
        Assert.assertEquals("https://example.com/fdroid/repo", result.get(1).getRepositoryProvision().getUrl());
        Assert.assertEquals("1111222233334444555566667777888899990000aaaabbbbccccddddeeeeffff", result.get(1).getRepositoryProvision().getSigfp());
        Assert.assertEquals("user2", result.get(1).getRepositoryProvision().getUsername());
        Assert.assertEquals("other secret", result.get(1).getRepositoryProvision().getPassword());
    }

    private File getResourceFile(String resourceFileName) {
        return new File(getClass().getClassLoader().getResource(resourceFileName).getPath());
    }
}

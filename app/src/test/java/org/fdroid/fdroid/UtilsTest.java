package org.fdroid.fdroid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static vendored.org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA_256;

import android.content.Context;
import android.content.res.Resources;
import android.text.format.DateUtils;

import androidx.test.core.app.ApplicationProvider;

import org.fdroid.fdroid.views.AppDetailsRecyclerViewAdapter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.io.IOException;

/**
 * @see <a href="https://gitlab.com/fdroid/fdroidclient/-/merge_requests/1089#note_822501322">forced to vendor Apache Commons Codec</a>
 */
@RunWith(RobolectricTestRunner.class)
@SuppressWarnings("LineLength")
public class UtilsTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    @Before
    public void setUp() {
        Preferences.setupForTests(context);
    }

    @Test
    public void trailingNewLines() {
        CharSequence threeParagraphs = AppDetailsRecyclerViewAdapter.trimTrailingNewlines("Paragraph One\n\nParagraph Two\n\nParagraph Three\n\n");
        assertEquals("Paragraph One\n\nParagraph Two\n\nParagraph Three", threeParagraphs);

        CharSequence leadingAndExtraTrailing = AppDetailsRecyclerViewAdapter.trimTrailingNewlines("\n\n\nA\n\n\n");
        assertEquals("\n\n\nA", leadingAndExtraTrailing);
    }

    @Test
    public void testFormatFingerprint() {
        Context context = ApplicationProvider.getApplicationContext();
        String badResult = Utils.formatFingerprint(context, "");
        // real fingerprints
        String formatted;
        String fdroidFingerprint =
                "43238D512C1E5EB2D6569F4A3AFBF5523418B82E0A3ED1552770ABB9A9C9CCAB";
        formatted = Utils.formatFingerprint(context, fdroidFingerprint);
        assertNotEquals(formatted, badResult);
        assertTrue(formatted.matches("[A-Z0-9][A-Z0-9] [A-Z0-9 ]+"));
        String gpRepoFingerprint =
                "59050C8155DCA377F23D5A15B77D3713400CDBD8B42FBFBE0E3F38096E68CECE";
        formatted = Utils.formatFingerprint(context, gpRepoFingerprint);
        assertNotEquals(formatted, badResult);
        assertTrue(formatted.matches("[A-Z0-9][A-Z0-9] [A-Z0-9 ]+"));
        String gpTest1Fingerprint =
                "C63AED1AC79D37C7B0474472AC6EFA6C3AB2B11A767A4F42CF360FA5496E3C50";
        formatted = Utils.formatFingerprint(context, gpTest1Fingerprint);
        assertNotEquals(formatted, badResult);
        assertTrue(formatted.matches("[A-Z0-9][A-Z0-9] [A-Z0-9 ]+"));
        // random garbage
        assertEquals(
                badResult,
                Utils.formatFingerprint(context, "234k2lk3jljwlk4j2lk3jlkmqwekljrlkj34lk2jlk2j34lkjl2k3j4lk2j34lja"));
        assertEquals(
                badResult,
                Utils.formatFingerprint(context, "g000000000000000000000000000000000000000000000000000000000000000"));
        assertEquals(
                badResult,
                Utils.formatFingerprint(context, "98273498723948728934789237489273p1928731982731982739182739817238"));
        // too short
        assertEquals(
                badResult,
                Utils.formatFingerprint(context, "C63AED1AC79D37C7B0474472AC6EFA6C3AB2B11A767A4F42CF360FA5496E3C5"));
        assertEquals(
                badResult,
                Utils.formatFingerprint(context, "C63AED1"));
        assertEquals(
                badResult,
                Utils.formatFingerprint(context, "f"));
        assertEquals(
                badResult,
                Utils.formatFingerprint(context, ""));
        assertEquals(
                badResult,
                Utils.formatFingerprint(context, null));
        // real digits but too long
        assertEquals(
                badResult,
                Utils.formatFingerprint(context, "43238D512C1E5EB2D6569F4A3AFBF5523418B82E0A3ED1552770ABB9A9C9CCAB43238D512C1E5EB2D6569F4A3AFBF5523418B82E0A3ED1552770ABB9A9C9CCAB"));
        assertEquals(
                badResult,
                Utils.formatFingerprint(context, "C63AED1AC79D37C7B0474472AC6EFA6C3AB2B11A767A4F42CF360FA5496E3C50F"));
        assertEquals(
                badResult,
                Utils.formatFingerprint(context, "3082035e30820246a00302010202044c49cd00300d06092a864886f70d01010505003071310b300906035504061302554b3110300e06035504081307556e6b6e6f776e3111300f0603550407130857657468657262793110300e060355040a1307556e6b6e6f776e3110300e060355040b1307556e6b6e6f776e311930170603550403131043696172616e2047756c746e69656b73301e170d3130303732333137313032345a170d3337313230383137313032345a3071310b300906035504061302554b3110300e06035504081307556e6b6e6f776e3111300f0603550407130857657468657262793110300e060355040a1307556e6b6e6f776e3110300e060355040b1307556e6b6e6f776e311930170603550403131043696172616e2047756c746e69656b7330820122300d06092a864886f70d01010105000382010f003082010a028201010096d075e47c014e7822c89fd67f795d23203e2a8843f53ba4e6b1bf5f2fd0e225938267cfcae7fbf4fe596346afbaf4070fdb91f66fbcdf2348a3d92430502824f80517b156fab00809bdc8e631bfa9afd42d9045ab5fd6d28d9e140afc1300917b19b7c6c4df4a494cf1f7cb4a63c80d734265d735af9e4f09455f427aa65a53563f87b336ca2c19d244fcbba617ba0b19e56ed34afe0b253ab91e2fdb1271f1b9e3c3232027ed8862a112f0706e234cf236914b939bcf959821ecb2a6c18057e070de3428046d94b175e1d89bd795e535499a091f5bc65a79d539a8d43891ec504058acb28c08393b5718b57600a211e803f4a634e5c57f25b9b8c4422c6fd90203010001300d06092a864886f70d0101050500038201010008e4ef699e9807677ff56753da73efb2390d5ae2c17e4db691d5df7a7b60fc071ae509c5414be7d5da74df2811e83d3668c4a0b1abc84b9fa7d96b4cdf30bba68517ad2a93e233b042972ac0553a4801c9ebe07bf57ebe9a3b3d6d663965260e50f3b8f46db0531761e60340a2bddc3426098397fda54044a17e5244549f9869b460ca5e6e216b6f6a2db0580b480ca2afe6ec6b46eedacfa4aa45038809ece0c5978653d6c85f678e7f5a2156d1bedd8117751e64a4b0dcd140f3040b021821a8d93aed8d01ba36db6c82372211fed714d9a32607038cdfd565bd529ffc637212aaa2c224ef22b603eccefb5bf1e085c191d4b24fe742b17ab3f55d4e6f05ef"));
    }

    @Test
    public void testIsFileMatchingHash() {
        Utils.isFileMatchingHash(null, null, null);
        Utils.isFileMatchingHash(new File("/"), "", null);

        assertFalse(Utils.isFileMatchingHash(null, null, ""));
        assertFalse(Utils.isFileMatchingHash(null, null, SHA_256));
        assertFalse(Utils.isFileMatchingHash(new File("/"), null, SHA_256));
        assertFalse(Utils.isFileMatchingHash(new File("/"), "", SHA_256));

        assertTrue(Utils.isFileMatchingHash(TestUtils.copyResourceToTempFile("Norway_bouvet_europe_2.obf.zip"),
                "6e8a584e004c6cd26d3822a04b0591e355dc5d07b5a3d0f8e309443f47ad1208", SHA_256));
        assertTrue(Utils.isFileMatchingHash(TestUtils.copyResourceToTempFile("install_history_all"),
                "4ad118d4a600dcc104834635d248a89e337fc91b173163d646996b9c54d77372", SHA_256));

        File f = TestUtils.copyResourceToTempFile("additional_repos.xml");
        assertTrue(Utils.isFileMatchingHash(f,
                "47ad2284d3042373e6280012cc10e9b82f75352db6d6d9bab1e06934b7b1dab7", SHA_256));
        assertFalse("uppercase fails",
                Utils.isFileMatchingHash(f,
                        "47AD2284D3042373E6280012CC10E9B82F75352DB6D6D9BAB1E06934B7B1DAB7", SHA_256));
        assertFalse("one uppercase digit fails",
                Utils.isFileMatchingHash(f,
                        "47Ad2284d3042373e6280012cc10e9b82f75352db6d6d9bab1e06934b7b1dab7", SHA_256));
        assertFalse("missing digit fails",
                Utils.isFileMatchingHash(f,
                        "47ad2284d3042373e6280012cc10e9b82f75352db6d6d9bab1e06934b7b1dab", SHA_256));
        assertFalse("extra digit fails",
                Utils.isFileMatchingHash(f,
                        "47ad2284d3042373e6280012cc10e9b82f75352db6d6d9bab1e06934b7b1dab71", SHA_256));
        assertFalse("all zeros fails",
                Utils.isFileMatchingHash(f,
                        "0000000000000000000000000000000000000000000000000000000000000000", SHA_256));
        assertFalse("null fails",
                Utils.isFileMatchingHash(f, null, SHA_256));
        assertFalse("empty string fails",
                Utils.isFileMatchingHash(f, "", SHA_256));
    }

    @Test
    public void testGetFileHexDigest() throws IOException {
        // zero size file should have a stable hex digest file
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                Utils.getFileHexDigest(File.createTempFile("asdf", "asdf"), SHA_256));

        assertNull(Utils.getFileHexDigest(new File("/kasdfkjasdhflkjasd"), SHA_256));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetFileHexDigestBadAlgo() {
        File f = TestUtils.copyResourceToTempFile("additional_repos.xml");
        assertNull(Utils.getFileHexDigest(f, "FAKE"));
    }

    @Test(expected = NullPointerException.class)
    public void testGetFileHexDigestNullFile() {
        assertNull(Utils.getFileHexDigest(null, SHA_256));
    }

    // TODO write tests that work with a Certificate

    @Test
    public void testFormatLastUpdated() {
        Resources res = context.getResources();
        long now = System.currentTimeMillis();
        long offset = DateUtils.MINUTE_IN_MILLIS * 10;
        assertEquals(
                "Updated today",
                Utils.formatLastUpdated(res, now - offset)
        );
        assertEquals(
                "Updated today",
                Utils.formatLastUpdated(res, now - DateUtils.DAY_IN_MILLIS / 2 + offset)
        );
        assertEquals(
                "Updated 1 day ago",
                Utils.formatLastUpdated(res, now - DateUtils.DAY_IN_MILLIS / 2 - offset)
        );
        assertEquals(
                "Updated 1 day ago",
                Utils.formatLastUpdated(res, now - DateUtils.DAY_IN_MILLIS - offset)
        );
        assertEquals(
                "Updated 3 days ago",
                Utils.formatLastUpdated(res, now - 234834870L)
        );
        assertEquals(
                "Updated 13 days ago",
                Utils.formatLastUpdated(res, now - DateUtils.DAY_IN_MILLIS * 13 - offset)
        );
        assertEquals(
                "Updated 7 months ago",
                Utils.formatLastUpdated(res, now - DateUtils.DAY_IN_MILLIS * 30 * 7 + offset)
        );
    }
}

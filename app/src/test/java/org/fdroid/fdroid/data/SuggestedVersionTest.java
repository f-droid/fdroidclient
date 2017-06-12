package org.fdroid.fdroid.data;

import android.app.Application;
import android.content.ContentValues;
import android.content.Context;

import org.fdroid.fdroid.Assert;
import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.Hasher;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.TestUtils;
import org.fdroid.fdroid.data.Schema.AppMetadataTable.Cols;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@Config(constants = BuildConfig.class, application = Application.class, sdk = 24)
@RunWith(RobolectricTestRunner.class)
public class SuggestedVersionTest extends FDroidProviderTest {

    private static final String FDROID_CERT = "308202ed308201d5a003020102020426ffa009300d06092a864886f70d01010b05003027310b300906035504061302444531183016060355040a130f4e4f47415050532050726f6a656374301e170d3132313030363132303533325a170d3337303933303132303533325a3027310b300906035504061302444531183016060355040a130f4e4f47415050532050726f6a65637430820122300d06092a864886f70d01010105000382010f003082010a02820101009a8d2a5336b0eaaad89ce447828c7753b157459b79e3215dc962ca48f58c2cd7650df67d2dd7bda0880c682791f32b35c504e43e77b43c3e4e541f86e35a8293a54fb46e6b16af54d3a4eda458f1a7c8bc1b7479861ca7043337180e40079d9cdccb7e051ada9b6c88c9ec635541e2ebf0842521c3024c826f6fd6db6fd117c74e859d5af4db04448965ab5469b71ce719939a06ef30580f50febf96c474a7d265bb63f86a822ff7b643de6b76e966a18553c2858416cf3309dd24278374bdd82b4404ef6f7f122cec93859351fc6e5ea947e3ceb9d67374fe970e593e5cd05c905e1d24f5a5484f4aadef766e498adf64f7cf04bddd602ae8137b6eea40722d0203010001a321301f301d0603551d0e04160414110b7aa9ebc840b20399f69a431f4dba6ac42a64300d06092a864886f70d01010b0500038201010007c32ad893349cf86952fb5a49cfdc9b13f5e3c800aece77b2e7e0e9c83e34052f140f357ec7e6f4b432dc1ed542218a14835acd2df2deea7efd3fd5e8f1c34e1fb39ec6a427c6e6f4178b609b369040ac1f8844b789f3694dc640de06e44b247afed11637173f36f5886170fafd74954049858c6096308fc93c1bc4dd5685fa7a1f982a422f2a3b36baa8c9500474cf2af91c39cbec1bc898d10194d368aa5e91f1137ec115087c31962d8f76cd120d28c249cf76f4c70f5baa08c70a7234ce4123be080cee789477401965cfe537b924ef36747e8caca62dfefdd1a6288dcb1c4fd2aaa6131a7ad254e9742022cfd597d2ca5c660ce9e41ff537e5a4041e37"; // NOCHECKSTYLE LineLength
    private static final String UPSTREAM_CERT = "308204e1308202c9a0030201020204483450fa300d06092a864886f70d01010b050030213110300e060355040b1307462d44726f6964310d300b06035504031304736f7661301e170d3136303832333133333131365a170d3434303130393133333131365a30213110300e060355040b1307462d44726f6964310d300b06035504031304736f766130820222300d06092a864886f70d01010105000382020f003082020a0282020100dfdcd120f3ab224999dddf4ea33ea588d295e4d7130bef48c143e9d76e5c0e0e9e5d45e64208e35feebc79a83f08939dd6a343b7d1e2179930a105a1249ccd36d88ff3feffc6e4dc53dae0163a7876dd45ecc1ddb0adf5099aa56c1a84b52affcd45d0711ffa4de864f35ac0333ebe61ea8673eeda35a88f6af678cc4d0f80b089338ac8f2a8279a64195c611d19445cab3fd1a020afed9bd739bb95142fb2c00a8f847db5ef3325c814f8eb741bacf86ed3907bfe6e4564d2de5895df0c263824e0b75407589bae2d3a4666c13b92102d8781a8ee9bb4a5a1a78c4a9c21efdaf5584da42e84418b28f5a81d0456a3dc5b420991801e6b21e38c99bbe018a5b2d690894a114bc860d35601416aa4dc52216aff8a288d4775cddf8b72d45fd2f87303a8e9c0d67e442530be28eaf139894337266e0b33d57f949256ab32083bcc545bc18a83c9ab8247c12aea037e2b68dee31c734cb1f04f241d3b94caa3a2b258ffaf8e6eae9fbbe029a934dc0a0859c5f120334812693a1c09352340a39f2a678dbc1afa2a978bfee43afefcb7e224a58af2f3d647e5745db59061236b8af6fcfd93b3602f9e456978534f3a7851e800071bf56da80401c81d91c45f82568373af0576b1cc5eef9b85654124b6319770be3cdba3fbebe3715e8918fb6c8966624f3d0e815effac3d2ee06dd34ab9c693218b2c7c06ba99d6b74d4f17b8c3cb0203010001a321301f301d0603551d0e04160414d62bee9f3798509546acc62eb1de14b08b954d4f300d06092a864886f70d01010b05000382020100743f7c5692085895f9d1fffad390fb4202c15f123ed094df259185960fd6dadf66cb19851070f180297bba4e6996a4434616573b375cfee94fee73a4505a7ec29136b7e6c22e6436290e3686fe4379d4e3140ec6a08e70cfd3ed5b634a5eb5136efaaabf5f38e0432d3d79568a556970b8cfba2972f5d23a3856d8a981b9e9bbbbb88f35e708bde9cbc5f681cbd974085b9da28911296fe2579fa64bbe9fa0b93475a7a8db051080b0c5fade0d1c018e7858cd4cbe95145b0620e2f632cbe0f8af9cbf22e2fdaa72245ae31b0877b07181cc69dd2df74454251d8de58d25e76354abe7eb690f22e59b08795a8f2c98c578e0599503d9085927634072c82c9f82abd50fd12b8fd1a9d1954eb5cc0b4cfb5796b5aaec0356643b4a65a368442d92ef94edd3ac6a2b7fe3571b8cf9f462729228aab023ef9183f73792f5379633ccac51079177d604c6bc1873ada6f07d8da6d68c897e88a5fa5d63fdb8df820f46090e0716e7562dd3c140ba279a65b996f60addb0abe29d4bf2f5abe89480771d492307b926d91f02f341b2148502903c43d40f3c6c86a811d060711f0698b384acdcc0add44eb54e42962d3d041accc715afd49407715adc09350cb55e8d9281a3b0b6b5fcd91726eede9b7c8b13afdebb2c2b377629595f1096ba62fb14946dbac5f3c5f0b4e5b712e7acc7dcf6c46cdc5e6d6dfdeee55a0c92c2d70f080ac6"; // NOCHECKSTYLE LineLength
    private static final String THIRD_PARTY_CERT = "308204e1308202c9a0030201020204483450fa300d06092a864886f70d01010b050030213110300e060355040b130abcdeabcde012340123400b06035504031304736f7661301e170d3136303832333133333131365a170d3434303130393133333131365a30213110300e060355040b1307462d44726f6964310d300b06035504031304736f766130820222300d06092a864886f70d01010105000382020f003082020a0282020100dfdcd120f3ab224999dddf4ea33ea588d295e4d7130bef48c143e9d76e5c0e0e9e5d45e64208e35feebc79a83f08939dd6a343b7d1e2179930a105a1249ccd36d88ff3feffc6e4dc53dae0163a7876dd45ecc1ddb0adf5099aa56c1a84b52affcd45d0711ffa4de864f35ac0333ebe61ea8673eeda35a88f6af678cc4d0f80b089338ac8f2a8279a64195c611d19445cab3fd1a020afed9bd739bb95142fb2c00a8f847db5ef3325c814f8eb741bacf86ed3907bfe6e4564d2de5895df0c263824e0b75407589bae2d3a4666c13b92102d8781a8ee9bb4a5a1a78c4a9c21efdaf5584da42e84418b28f5a81d0456a3dc5b420991801e6b21e38c99bbe018a5b2d690894a114bc860d35601416aa4dc52216aff8a288d4775cddf8b72d45fd2f87303a8e9c0d67e442530be28eaf139894337266e0b33d57f949256ab32083bcc545bc18a83c9ab8247c12aea037e2b68dee31c734cb1f04f241d3b94caa3a2b258ffaf8e6eae9fbbe029a934dc0a0859c5f120334812693a1c09352340a39f2a678dbc1afa2a978bfee43afefcb7e224a58af2f3d647e5745db59061236b8af6fcfd93b3602f9e456978534f3a7851e800071bf56da80401c81d91c45f82568373af0576b1cc5eef9b85654124b6319770be3cdba3fbebe3715e8918fb6c8966624f3d0e815effac3d2ee06dd34ab9c693218b2c7c06ba99d6b74d4f17b8c3cb0203010001a321301f301d0603551d0e04160414d62bee9f3798509546acc62eb1de14b08b954d4f300d06092a864886f70d01010b05000382020100743f7c5692085895f9d1fffad390fb4202c15f123ed094df259185960fd6dadf66cb19851070f180297bba4e6996a4434616573b375cfee94fee73a4505a7ec29136b7e6c22e6436290e3686fe4379d4e3140ec6a08e70cfd3ed5b634a5eb5136efaaabf5f38e0432d3d79568a556970b8cfba2972f5d23a3856d8a981b9e9bbbbb88f35e708bde9cbc5f681cbd974085b9da28911296fe2579fa64bbe9fa0b93475a7a8db051080b0c5fade0d1c018e7858cd4cbe95145b0620e2f632cbe0f8af9cbf22e2fdaa72245ae31b0877b07181cc69dd2df74454251d8de58d25e76354abe7eb690f22e59b08795a8f2c98c578e0599503d9085927634072c82c9f82abd50fd12b8fd1a9d1954eb5cc0b4cfb5796b5aaec0356643b4a65a368442d92ef94edd3ac6a2b7fe3571b8cf9f462729228aab023ef9183f73792f5379633ccac51079177d604c6bc1873ada6f07d8da6d68c897e88a5fa5d63fdb8df820f46090e0716e7562dd3c140ba279a65b996f60addb0abe29d4bf2f5abe89480771d492307b926d91f02f341b2148502903c43d40f3c6c86a811d060711f0698b384acdcc0add44eb54e42962d3d041accc715afd49407715adc09350cb55e8d9281a3b0b6b5fcd91726eede9b7c8b13afdebb2c2b377629595f1096ba62fb14946dbac5f3c5f0b4e5b712e7acc7dcf6c46cdc5e6d6dfdeee55a0c92c2d70f080ac6"; // NOCHECKSTYLE LineLength

    private static final String FDROID_SIG;
    private static final String UPSTREAM_SIG;
    private static final String THIRD_PARTY_SIG;

    static {
        // Some code requires the full certificate (e.g. when we mock PackageInfo to give to the
        // installed app provider), while others requires the hashed certificate (e.g. inserting
        // into the apk provider directly, without the need to mock anything).
        try {
            FDROID_SIG = new Hasher("MD5", FDROID_CERT.getBytes()).getHash();
            UPSTREAM_SIG = new Hasher("MD5", UPSTREAM_CERT.getBytes()).getHash();
            THIRD_PARTY_SIG = new Hasher("MD5", THIRD_PARTY_CERT.getBytes()).getHash();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Before
    public void setup() {
        TestUtils.registerContentProvider(AppProvider.getAuthority(), AppProvider.class);
        Preferences.setup(context);
    }

    @After
    public void tearDown() {
        Preferences.clearSingletonForTesting();
    }

    @Test
    public void singleRepoSingleSig() {
        App singleApp = insertApp(context, "single.app", "Single App (with beta)", 2, "https://beta.simple.repo");
        insertApk(context, singleApp, 1, FDROID_SIG);
        insertApk(context, singleApp, 2, FDROID_SIG);
        insertApk(context, singleApp, 3, FDROID_SIG);
        AppProvider.Helper.calcSuggestedApks(context);

        App found2 = findApp(singleApp);
        assertEquals(2, found2.suggestedVersionCode);

        Apk found2Apk = findApk(found2);
        assertEquals(2, found2Apk.versionCode);
        assertEquals(FDROID_SIG, found2Apk.sig);

        // By enabling unstable updates, the "upstreamVersionCode" should get ignored, and we should
        // suggest the latest version (3).
        Preferences.get().setUnstableUpdates(true);
        AppProvider.Helper.calcSuggestedApks(context);
        App found3 = findApp(singleApp);
        assertEquals(3, found3.suggestedVersionCode);

        Apk found3Apk = findApk(found3);
        assertEquals(3, found3Apk.versionCode);
        assertEquals(FDROID_SIG, found3Apk.sig);
    }

    private App findApp(App app) {
        return AppProvider.Helper.findSpecificApp(context.getContentResolver(), app.packageName, app.repoId);
    }

    private Apk findApk(App app) {
        return ApkProvider.Helper.findSuggestedApk(context, app);
    }

    @Test
    public void suggestedApkQuery() {
        App singleApp = insertApp(context, "single.app", "Single App", 0, "https://simple.repo");
        insertApk(context, singleApp, 1, FDROID_SIG);
        insertApk(context, singleApp, 1, UPSTREAM_SIG);
        insertApk(context, singleApp, 2, FDROID_SIG);
        insertApk(context, singleApp, 2, UPSTREAM_SIG);
        insertApk(context, singleApp, 3, FDROID_SIG);
        insertApk(context, singleApp, 3, UPSTREAM_SIG);
        insertApk(context, singleApp, 3, THIRD_PARTY_SIG);
        insertApk(context, singleApp, 4, FDROID_SIG);
        insertApk(context, singleApp, 4, UPSTREAM_SIG);
        insertApk(context, singleApp, 4, THIRD_PARTY_SIG);
        insertApk(context, singleApp, 5, FDROID_SIG);
        insertApk(context, singleApp, 5, UPSTREAM_SIG);
        AppProvider.Helper.calcSuggestedApks(context);

        App notInstalled = findApp(singleApp);
        Apk suggestedApkForNotInstalled = findApk(notInstalled);
        assertNull(notInstalled.installedSig);

        // It could be either of these, I think it is actually non-deterministic as to which is chosen.
        // TODO: Make it deterministic based on repo priority.
        assertTrue(FDROID_SIG.equals(suggestedApkForNotInstalled.sig) ||
                UPSTREAM_SIG.equals(suggestedApkForNotInstalled.sig));
        assertEquals(5, suggestedApkForNotInstalled.versionCode);

        InstalledAppTestUtils.install(context, "single.app", 3, "v1", THIRD_PARTY_CERT);
        AppProvider.Helper.calcSuggestedApks(context);

        App installed = findApp(singleApp);
        Apk suggestedApkForInstalled = findApk(installed);
        assertEquals(THIRD_PARTY_SIG, installed.installedSig);
        assertEquals(4, installed.suggestedVersionCode);
        assertEquals(THIRD_PARTY_SIG, suggestedApkForInstalled.sig);
        assertEquals(4, suggestedApkForInstalled.versionCode);
    }

    @Test
    public void singleRepoMultiSig() {
        App unrelatedApp = insertApp(context, "noisy.app", "Noisy App", 3, "https://simple.repo");
        insertApk(context, unrelatedApp, 3, FDROID_SIG);

        App singleApp = insertApp(context, "single.app", "Single App", 4, "https://simple.repo");
        insertApk(context, singleApp, 1, FDROID_SIG);
        insertApk(context, singleApp, 2, FDROID_SIG);
        insertApk(context, singleApp, 3, FDROID_SIG);
        insertApk(context, singleApp, 4, UPSTREAM_SIG);
        insertApk(context, singleApp, 5, UPSTREAM_SIG);
        AppProvider.Helper.calcSuggestedApks(context);

        // Given we aren't installed yet, we don't care which signature.
        // Just get as close to upstreamVersionCode as possible.
        App suggestUpstream4 = findApp(singleApp);
        assertEquals(4, suggestUpstream4.suggestedVersionCode);

        Apk suggestedUpstream4Apk = findApk(suggestUpstream4);
        assertEquals(4, suggestedUpstream4Apk.versionCode);
        assertEquals(UPSTREAM_SIG, suggestedUpstream4Apk.sig);

        // Now install v1 with the f-droid signature. In response, we should only suggest
        // apps with that sig in the future. That is, version 4 from upstream is not considered.
        InstalledAppTestUtils.install(context, "single.app", 1, "v1", FDROID_CERT);
        AppProvider.Helper.calcSuggestedApks(context);
        App suggestFDroid3 = findApp(singleApp);
        assertEquals(3, suggestFDroid3.suggestedVersionCode);

        Apk suggestedFDroid3Apk = findApk(suggestFDroid3);
        assertEquals(3, suggestedFDroid3Apk.versionCode);
        assertEquals(FDROID_SIG, suggestedFDroid3Apk.sig);

        // This adds the "upstreamVersionCode" version of the app, but signed by f-droid.
        insertApk(context, singleApp, 4, FDROID_SIG);
        insertApk(context, singleApp, 5, FDROID_SIG);
        AppProvider.Helper.calcSuggestedApks(context);
        App suggestFDroid4 = findApp(singleApp);
        assertEquals(4, suggestFDroid4.suggestedVersionCode);

        Apk suggestedFDroid4Apk = findApk(suggestFDroid4);
        assertEquals(4, suggestedFDroid4Apk.versionCode);
        assertEquals(FDROID_SIG, suggestedFDroid4Apk.sig);

        // Version 5 from F-Droid is not the "upstreamVersionCode", but with beta updates it should
        // still become the suggested version now.
        Preferences.get().setUnstableUpdates(true);
        AppProvider.Helper.calcSuggestedApks(context);
        App suggestFDroid5 = findApp(singleApp);
        assertEquals(5, suggestFDroid5.suggestedVersionCode);

        Apk suggestedFDroid5Apk = findApk(suggestFDroid5);
        assertEquals(5, suggestedFDroid5Apk.versionCode);
        assertEquals(FDROID_SIG, suggestedFDroid5Apk.sig);
    }

    private void recalculateMetadata() {
        AppProvider.Helper.calcSuggestedApks(context);
        AppProvider.Helper.recalculatePreferredMetadata(context);
    }

    private App highestPriorityApp(String packageName) {
        return AppProvider.Helper.findHighestPriorityMetadata(context.getContentResolver(), packageName);
    }

    @Test
    public void multiRepoMultiSig() {
        App unrelatedApp = insertApp(context, "noisy.app", "Noisy App", 3, "https://simple.repo");
        insertApk(context, unrelatedApp, 3, FDROID_SIG);

        App mainApp = insertApp(context, "single.app", "Single App (Main repo)", 4, "https://main.repo");
        App thirdPartyApp = insertApp(context, "single.app", "Single App (3rd party)", 4, "https://3rd-party.repo");

        insertApk(context, mainApp, 1, FDROID_SIG);
        insertApk(context, mainApp, 2, FDROID_SIG);
        insertApk(context, mainApp, 3, FDROID_SIG);
        insertApk(context, mainApp, 4, UPSTREAM_SIG);
        insertApk(context, mainApp, 5, UPSTREAM_SIG);

        insertApk(context, thirdPartyApp, 3, THIRD_PARTY_SIG);
        insertApk(context, thirdPartyApp, 4, THIRD_PARTY_SIG);
        insertApk(context, thirdPartyApp, 5, THIRD_PARTY_SIG);
        insertApk(context, thirdPartyApp, 6, THIRD_PARTY_SIG);

        recalculateMetadata();

        // Given we aren't installed yet, we don't care which signature or even which repo.
        // Just get as close to upstreamVersionCode as possible.
        App suggestAnyVersion4 = highestPriorityApp("single.app");
        assertEquals(4, suggestAnyVersion4.suggestedVersionCode);

        // Now install v1 with the f-droid signature. In response, we should only suggest
        // apps with that sig in the future. That is, version 4 from upstream is not considered.
        InstalledAppTestUtils.install(context, "single.app", 1, "v1", FDROID_CERT);
        recalculateMetadata();
        App suggestFDroid3 = highestPriorityApp("single.app");
        assertEquals(3, suggestFDroid3.suggestedVersionCode);

        // This adds the "upstreamVersionCode" version of the app, but signed by f-droid.
        insertApk(context, mainApp, 4, FDROID_SIG);
        insertApk(context, mainApp, 5, FDROID_SIG);
        recalculateMetadata();
        App suggestFDroid4 = highestPriorityApp("single.app");
        assertEquals(4, suggestFDroid4.suggestedVersionCode);

        // Uninstalling the F-Droid build and installing v3 of the third party means we can now go
        // back to suggesting version 4.
        InstalledAppProviderService.deleteAppFromDb(context, "single.app");
        InstalledAppTestUtils.install(context, "single.app", 3, "v3", THIRD_PARTY_CERT);
        recalculateMetadata();
        suggestAnyVersion4 = highestPriorityApp("single.app");
        assertEquals(4, suggestAnyVersion4.suggestedVersionCode);

        // Version 6 from the 3rd party repo is not the "upstreamVersionCode", but with beta updates
        // it should still become the suggested version now.
        Preferences.get().setUnstableUpdates(true);
        recalculateMetadata();
        App suggest3rdParty6 = highestPriorityApp("single.app");
        assertEquals(6, suggest3rdParty6.suggestedVersionCode);
    }

    private void insertApk(Context context, App app, int versionCode, String signature) {
        ContentValues values = new ContentValues();
        values.put(Schema.ApkTable.Cols.SIGNATURE, signature);
        Assert.insertApk(context, app, versionCode, values);
    }

    private App insertApp(Context context, String packageName, String appName, int upstreamVersionCode,
                          String repoUrl) {
        Repo repo = ensureRepo(context, repoUrl);
        ContentValues values = new ContentValues();
        values.put(Cols.REPO_ID, repo.getId());
        values.put(Cols.UPSTREAM_VERSION_CODE, upstreamVersionCode);
        return Assert.insertApp(context, packageName, appName, values);
    }

    private Repo ensureRepo(Context context, String repoUrl) {
        Repo existing = RepoProvider.Helper.findByAddress(context, repoUrl);
        if (existing != null) {
            return existing;
        }

        return RepoProviderTest.insertRepo(context, repoUrl, "", "", "");
    }

}

package org.fdroid.fdroid;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ProviderInfo;
import android.net.Uri;

import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.data.RepoProviderTest;
import org.fdroid.fdroid.data.Schema;
import org.mockito.AdditionalAnswers;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowContentResolver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class TestUtils {

    @SuppressWarnings("unused")
    private static final String TAG = "TestUtils"; // NOPMD

    /**
     * This is the F-Droid signature used to sign the AdAway binaries used for the multiRepo.*.jar
     * repos used by some tests.
     */
    public static final String FDROID_CERT = "3082033c30820224a00302010202044e9c4ba6300d06092a864886f70d01010505003060310b300906035504061302554b310c300a060355040813034f5247310c300a060355040713034f524731133011060355040a130a6664726f69642e6f7267310f300d060355040b13064644726f6964310f300d060355040313064644726f6964301e170d3131313031373135333731305a170d3339303330343135333731305a3060310b300906035504061302554b310c300a060355040813034f5247310c300a060355040713034f524731133011060355040a130a6664726f69642e6f7267310f300d060355040b13064644726f6964310f300d060355040313064644726f696430820122300d06092a864886f70d01010105000382010f003082010a0282010100981b0aac96f1c66be3c21e773327ee8c4d3b18c75c548243f4cfedbe8ef0d3c6cc1b3b7b094ddd39cdf71d034ef2cd2d1e7bdca458801b04a531cbe7106a3575151375cb32177b017f81cc508f981a1809d0a417c6f3d59ddfa876c3d91874b1d59e08eaf757da13fb82f7e6f7340abc56f0ab672f02e957d446585931388b1affb6f43a16efc7f060df9c8da17c86899b19495114cc5939decd521e172b48e68c6ec03bc58776acd6a52fd61fd839d2a404df25ae79c2ccec2d9a07c9a1751c341e5e9b706b8e713bec2149e16f5ca15a1d6fe67d52ebb210995ee03d9416118fa9434f65ffe6d43dddfe3e2b0c54b94ea8e5a1031ed41856cd369da41dc6790203010001300d06092a864886f70d0101050500038201010080951aa68b5a2c7ac464b66078afd4826df96e2c10b612a441036e43aa923bfa55f26c61b5d94c2132877a3801c2394328f70b322f6308dbea6ed4f0f4897d73d13af9498277f60685239acd8922275544334d295b07245ef0ec924e1c35e8004d8d268d97c957078149cc5635f8977ce432a56278a03664a45a6be51319b0b5f3e27b2372ae859215e3f3d0f5c8b86d1a42f742abe4d224870d419600966e46d83ce41df04e315353f334378f0f994732a6c05d351b1bea66efc62471762d0f752d379966e8293fc5fe4150665427b0f3fb3a1b64c3b75128abadc02c3efa44c06e2d22ba8f1c3f4b782ac2da0d56307173093fde31215d26ab05714a12d696"; // NOCHECKSTYLE LineLength
    public static final String UPSTREAM_CERT = "308204e1308202c9a0030201020204483450fa300d06092a864886f70d01010b050030213110300e060355040b1307462d44726f6964310d300b06035504031304736f7661301e170d3136303832333133333131365a170d3434303130393133333131365a30213110300e060355040b1307462d44726f6964310d300b06035504031304736f766130820222300d06092a864886f70d01010105000382020f003082020a0282020100dfdcd120f3ab224999dddf4ea33ea588d295e4d7130bef48c143e9d76e5c0e0e9e5d45e64208e35feebc79a83f08939dd6a343b7d1e2179930a105a1249ccd36d88ff3feffc6e4dc53dae0163a7876dd45ecc1ddb0adf5099aa56c1a84b52affcd45d0711ffa4de864f35ac0333ebe61ea8673eeda35a88f6af678cc4d0f80b089338ac8f2a8279a64195c611d19445cab3fd1a020afed9bd739bb95142fb2c00a8f847db5ef3325c814f8eb741bacf86ed3907bfe6e4564d2de5895df0c263824e0b75407589bae2d3a4666c13b92102d8781a8ee9bb4a5a1a78c4a9c21efdaf5584da42e84418b28f5a81d0456a3dc5b420991801e6b21e38c99bbe018a5b2d690894a114bc860d35601416aa4dc52216aff8a288d4775cddf8b72d45fd2f87303a8e9c0d67e442530be28eaf139894337266e0b33d57f949256ab32083bcc545bc18a83c9ab8247c12aea037e2b68dee31c734cb1f04f241d3b94caa3a2b258ffaf8e6eae9fbbe029a934dc0a0859c5f120334812693a1c09352340a39f2a678dbc1afa2a978bfee43afefcb7e224a58af2f3d647e5745db59061236b8af6fcfd93b3602f9e456978534f3a7851e800071bf56da80401c81d91c45f82568373af0576b1cc5eef9b85654124b6319770be3cdba3fbebe3715e8918fb6c8966624f3d0e815effac3d2ee06dd34ab9c693218b2c7c06ba99d6b74d4f17b8c3cb0203010001a321301f301d0603551d0e04160414d62bee9f3798509546acc62eb1de14b08b954d4f300d06092a864886f70d01010b05000382020100743f7c5692085895f9d1fffad390fb4202c15f123ed094df259185960fd6dadf66cb19851070f180297bba4e6996a4434616573b375cfee94fee73a4505a7ec29136b7e6c22e6436290e3686fe4379d4e3140ec6a08e70cfd3ed5b634a5eb5136efaaabf5f38e0432d3d79568a556970b8cfba2972f5d23a3856d8a981b9e9bbbbb88f35e708bde9cbc5f681cbd974085b9da28911296fe2579fa64bbe9fa0b93475a7a8db051080b0c5fade0d1c018e7858cd4cbe95145b0620e2f632cbe0f8af9cbf22e2fdaa72245ae31b0877b07181cc69dd2df74454251d8de58d25e76354abe7eb690f22e59b08795a8f2c98c578e0599503d9085927634072c82c9f82abd50fd12b8fd1a9d1954eb5cc0b4cfb5796b5aaec0356643b4a65a368442d92ef94edd3ac6a2b7fe3571b8cf9f462729228aab023ef9183f73792f5379633ccac51079177d604c6bc1873ada6f07d8da6d68c897e88a5fa5d63fdb8df820f46090e0716e7562dd3c140ba279a65b996f60addb0abe29d4bf2f5abe89480771d492307b926d91f02f341b2148502903c43d40f3c6c86a811d060711f0698b384acdcc0add44eb54e42962d3d041accc715afd49407715adc09350cb55e8d9281a3b0b6b5fcd91726eede9b7c8b13afdebb2c2b377629595f1096ba62fb14946dbac5f3c5f0b4e5b712e7acc7dcf6c46cdc5e6d6dfdeee55a0c92c2d70f080ac6"; // NOCHECKSTYLE LineLength
    public static final String THIRD_PARTY_CERT = "308204e1308202c9a0030201020204483450fa300d06092a864886f70d01010b050030213110300e060355040b130abcdeabcde012340123400b06035504031304736f7661301e170d3136303832333133333131365a170d3434303130393133333131365a30213110300e060355040b1307462d44726f6964310d300b06035504031304736f766130820222300d06092a864886f70d01010105000382020f003082020a0282020100dfdcd120f3ab224999dddf4ea33ea588d295e4d7130bef48c143e9d76e5c0e0e9e5d45e64208e35feebc79a83f08939dd6a343b7d1e2179930a105a1249ccd36d88ff3feffc6e4dc53dae0163a7876dd45ecc1ddb0adf5099aa56c1a84b52affcd45d0711ffa4de864f35ac0333ebe61ea8673eeda35a88f6af678cc4d0f80b089338ac8f2a8279a64195c611d19445cab3fd1a020afed9bd739bb95142fb2c00a8f847db5ef3325c814f8eb741bacf86ed3907bfe6e4564d2de5895df0c263824e0b75407589bae2d3a4666c13b92102d8781a8ee9bb4a5a1a78c4a9c21efdaf5584da42e84418b28f5a81d0456a3dc5b420991801e6b21e38c99bbe018a5b2d690894a114bc860d35601416aa4dc52216aff8a288d4775cddf8b72d45fd2f87303a8e9c0d67e442530be28eaf139894337266e0b33d57f949256ab32083bcc545bc18a83c9ab8247c12aea037e2b68dee31c734cb1f04f241d3b94caa3a2b258ffaf8e6eae9fbbe029a934dc0a0859c5f120334812693a1c09352340a39f2a678dbc1afa2a978bfee43afefcb7e224a58af2f3d647e5745db59061236b8af6fcfd93b3602f9e456978534f3a7851e800071bf56da80401c81d91c45f82568373af0576b1cc5eef9b85654124b6319770be3cdba3fbebe3715e8918fb6c8966624f3d0e815effac3d2ee06dd34ab9c693218b2c7c06ba99d6b74d4f17b8c3cb0203010001a321301f301d0603551d0e04160414d62bee9f3798509546acc62eb1de14b08b954d4f300d06092a864886f70d01010b05000382020100743f7c5692085895f9d1fffad390fb4202c15f123ed094df259185960fd6dadf66cb19851070f180297bba4e6996a4434616573b375cfee94fee73a4505a7ec29136b7e6c22e6436290e3686fe4379d4e3140ec6a08e70cfd3ed5b634a5eb5136efaaabf5f38e0432d3d79568a556970b8cfba2972f5d23a3856d8a981b9e9bbbbb88f35e708bde9cbc5f681cbd974085b9da28911296fe2579fa64bbe9fa0b93475a7a8db051080b0c5fade0d1c018e7858cd4cbe95145b0620e2f632cbe0f8af9cbf22e2fdaa72245ae31b0877b07181cc69dd2df74454251d8de58d25e76354abe7eb690f22e59b08795a8f2c98c578e0599503d9085927634072c82c9f82abd50fd12b8fd1a9d1954eb5cc0b4cfb5796b5aaec0356643b4a65a368442d92ef94edd3ac6a2b7fe3571b8cf9f462729228aab023ef9183f73792f5379633ccac51079177d604c6bc1873ada6f07d8da6d68c897e88a5fa5d63fdb8df820f46090e0716e7562dd3c140ba279a65b996f60addb0abe29d4bf2f5abe89480771d492307b926d91f02f341b2148502903c43d40f3c6c86a811d060711f0698b384acdcc0add44eb54e42962d3d041accc715afd49407715adc09350cb55e8d9281a3b0b6b5fcd91726eede9b7c8b13afdebb2c2b377629595f1096ba62fb14946dbac5f3c5f0b4e5b712e7acc7dcf6c46cdc5e6d6dfdeee55a0c92c2d70f080ac6"; // NOCHECKSTYLE LineLength

    public static final String FDROID_SIG;
    public static final String UPSTREAM_SIG;
    public static final String THIRD_PARTY_SIG;

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

    private static String formatSigForDebugging(String sig) {
        String suffix;

        // Can't use a switch statement here because *_SIG is not a constant, despite beign static final.
        if (sig.equals(FDROID_SIG)) {
            suffix = "F-Droid";
        } else if (sig.equals(UPSTREAM_SIG)) {
            suffix = "Upstream";
        } else if (sig.equals(THIRD_PARTY_SIG)) {
            suffix = "3rd Party";
        } else {
            suffix = "Unknown";
        }

        return sig + " [" + suffix + "]";
    }

    public static void assertSignaturesMatch(String message, String expected, String actual) {
        assertEquals(message, formatSigForDebugging(expected), formatSigForDebugging(actual));
    }

    public static Apk insertApk(Context context, App app, int versionCode, String signature) {
        ContentValues values = new ContentValues();
        values.put(Schema.ApkTable.Cols.SIGNATURE, signature);

        long repoId = app.repoId > 0 ? app.repoId : 1;
        values.put(Schema.ApkTable.Cols.REPO_ID, repoId);
        Uri uri = Assert.insertApk(context, app, versionCode, values);
        return ApkProvider.Helper.findByUri(context, uri, Schema.ApkTable.Cols.ALL);
    }

    public static App insertApp(Context context, String packageName, String appName, int upstreamVersionCode,
                          String repoUrl, String preferredSigner) {
        Repo repo = ensureRepo(context, repoUrl);
        return insertApp(context, packageName, appName, upstreamVersionCode, repo, preferredSigner);
    }

    public static App insertApp(Context context, String packageName, String appName, int upstreamVersionCode,
                          Repo repo, String preferredSigner) {
        ContentValues values = new ContentValues();
        values.put(Schema.AppMetadataTable.Cols.REPO_ID, repo.getId());
        values.put(Schema.AppMetadataTable.Cols.UPSTREAM_VERSION_CODE, upstreamVersionCode);
        values.put(Schema.AppMetadataTable.Cols.PREFERRED_SIGNER, preferredSigner);
        return Assert.insertApp(context, packageName, appName, values);
    }

    public static Repo ensureRepo(Context context, String repoUrl) {
        Repo existing = RepoProvider.Helper.findByAddress(context, repoUrl);
        if (existing != null) {
            return existing;
        }

        return RepoProviderTest.insertRepo(context, repoUrl, "", "", "");
    }

    public static <T extends ContentProvider> void registerContentProvider(String authority, Class<T> providerClass) {
        ProviderInfo info = new ProviderInfo();
        info.authority = authority;
        Robolectric.buildContentProvider(providerClass).create(info);
    }

    public static File copyResourceToTempFile(String resourceName) {
        File tempFile = null;
        InputStream input = null;
        OutputStream output = null;
        try {
            tempFile = File.createTempFile(resourceName + "-", ".testasset");
            input = TestUtils.class.getClassLoader().getResourceAsStream(resourceName);
            output = new FileOutputStream(tempFile);
            Utils.copy(input, output);
        } catch (IOException e) {
            e.printStackTrace();
            if (tempFile != null && tempFile.exists()) {
                assertTrue(tempFile.delete());
            }
            fail();
            return null;
        } finally {
            Utils.closeQuietly(output);
            Utils.closeQuietly(input);
        }
        return tempFile;
    }

    /**
     * The way that Robolectric has to implement shadows for Android classes
     * such as {@link android.content.ContentProvider} is by using a special
     * annotation that means the classes will implement the correct methods at
     * runtime.  However this means that the shadow of a content provider does
     * not actually extend {@link android.content.ContentProvider}. As such,
     * we need to do some special mocking using Mockito in order to provide a
     * {@link ContextWrapper} which is able to return a proper content
     * resolver that delegates to the Robolectric shadow object.
     */
    public static ContextWrapper createContextWithContentResolver(ShadowContentResolver contentResolver) {
        final ContentResolver resolver = mock(ContentResolver.class, AdditionalAnswers.delegatesTo(contentResolver));
        return new ContextWrapper(RuntimeEnvironment.application.getApplicationContext()) {
            @Override
            public ContentResolver getContentResolver() {
                return resolver;
            }
        };
    }

    /**
     * Normally apps/apks are only added to the database in response to a repo update.
     * At the end of a repo update, the {@link AppProvider} updates the suggested apks and
     * recalculates the preferred metadata for each app. Because we are adding apps/apks
     * directly to the database, we need to simulate this update after inserting stuff.
     */
    public static void updateDbAfterInserting(Context context) {
        AppProvider.Helper.calcSuggestedApks(context);
        AppProvider.Helper.recalculatePreferredMetadata(context);
    }
}

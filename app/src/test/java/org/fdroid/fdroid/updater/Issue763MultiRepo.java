package org.fdroid.fdroid.updater;

import android.content.ContentValues;
import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.RepoUpdater;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.data.Schema;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import static org.junit.Assert.assertEquals;

@Config(constants = BuildConfig.class)
@RunWith(RobolectricTestRunner.class)
@SuppressWarnings("LineLength")
public class Issue763MultiRepo extends MultiRepoUpdaterTest {

    private Repo microGRepo;
    private Repo antoxRepo;

    @Before
    public void setup() {
        String microGCert = "308202ed308201d5a003020102020426ffa009300d06092a864886f70d01010b05003027310b300906035504061302444531183016060355040a130f4e4f47415050532050726f6a656374301e170d3132313030363132303533325a170d3337303933303132303533325a3027310b300906035504061302444531183016060355040a130f4e4f47415050532050726f6a65637430820122300d06092a864886f70d01010105000382010f003082010a02820101009a8d2a5336b0eaaad89ce447828c7753b157459b79e3215dc962ca48f58c2cd7650df67d2dd7bda0880c682791f32b35c504e43e77b43c3e4e541f86e35a8293a54fb46e6b16af54d3a4eda458f1a7c8bc1b7479861ca7043337180e40079d9cdccb7e051ada9b6c88c9ec635541e2ebf0842521c3024c826f6fd6db6fd117c74e859d5af4db04448965ab5469b71ce719939a06ef30580f50febf96c474a7d265bb63f86a822ff7b643de6b76e966a18553c2858416cf3309dd24278374bdd82b4404ef6f7f122cec93859351fc6e5ea947e3ceb9d67374fe970e593e5cd05c905e1d24f5a5484f4aadef766e498adf64f7cf04bddd602ae8137b6eea40722d0203010001a321301f301d0603551d0e04160414110b7aa9ebc840b20399f69a431f4dba6ac42a64300d06092a864886f70d01010b0500038201010007c32ad893349cf86952fb5a49cfdc9b13f5e3c800aece77b2e7e0e9c83e34052f140f357ec7e6f4b432dc1ed542218a14835acd2df2deea7efd3fd5e8f1c34e1fb39ec6a427c6e6f4178b609b369040ac1f8844b789f3694dc640de06e44b247afed11637173f36f5886170fafd74954049858c6096308fc93c1bc4dd5685fa7a1f982a422f2a3b36baa8c9500474cf2af91c39cbec1bc898d10194d368aa5e91f1137ec115087c31962d8f76cd120d28c249cf76f4c70f5baa08c70a7234ce4123be080cee789477401965cfe537b924ef36747e8caca62dfefdd1a6288dcb1c4fd2aaa6131a7ad254e9742022cfd597d2ca5c660ce9e41ff537e5a4041e37";
        microGRepo = createRepo("MicroG", "https://microg.org/fdroid/repo", context, microGCert);

        String antoxCert = "308204e1308202c9a0030201020204483450fa300d06092a864886f70d01010b050030213110300e060355040b1307462d44726f6964310d300b06035504031304736f7661301e170d3136303832333133333131365a170d3434303130393133333131365a30213110300e060355040b1307462d44726f6964310d300b06035504031304736f766130820222300d06092a864886f70d01010105000382020f003082020a0282020100dfdcd120f3ab224999dddf4ea33ea588d295e4d7130bef48c143e9d76e5c0e0e9e5d45e64208e35feebc79a83f08939dd6a343b7d1e2179930a105a1249ccd36d88ff3feffc6e4dc53dae0163a7876dd45ecc1ddb0adf5099aa56c1a84b52affcd45d0711ffa4de864f35ac0333ebe61ea8673eeda35a88f6af678cc4d0f80b089338ac8f2a8279a64195c611d19445cab3fd1a020afed9bd739bb95142fb2c00a8f847db5ef3325c814f8eb741bacf86ed3907bfe6e4564d2de5895df0c263824e0b75407589bae2d3a4666c13b92102d8781a8ee9bb4a5a1a78c4a9c21efdaf5584da42e84418b28f5a81d0456a3dc5b420991801e6b21e38c99bbe018a5b2d690894a114bc860d35601416aa4dc52216aff8a288d4775cddf8b72d45fd2f87303a8e9c0d67e442530be28eaf139894337266e0b33d57f949256ab32083bcc545bc18a83c9ab8247c12aea037e2b68dee31c734cb1f04f241d3b94caa3a2b258ffaf8e6eae9fbbe029a934dc0a0859c5f120334812693a1c09352340a39f2a678dbc1afa2a978bfee43afefcb7e224a58af2f3d647e5745db59061236b8af6fcfd93b3602f9e456978534f3a7851e800071bf56da80401c81d91c45f82568373af0576b1cc5eef9b85654124b6319770be3cdba3fbebe3715e8918fb6c8966624f3d0e815effac3d2ee06dd34ab9c693218b2c7c06ba99d6b74d4f17b8c3cb0203010001a321301f301d0603551d0e04160414d62bee9f3798509546acc62eb1de14b08b954d4f300d06092a864886f70d01010b05000382020100743f7c5692085895f9d1fffad390fb4202c15f123ed094df259185960fd6dadf66cb19851070f180297bba4e6996a4434616573b375cfee94fee73a4505a7ec29136b7e6c22e6436290e3686fe4379d4e3140ec6a08e70cfd3ed5b634a5eb5136efaaabf5f38e0432d3d79568a556970b8cfba2972f5d23a3856d8a981b9e9bbbbb88f35e708bde9cbc5f681cbd974085b9da28911296fe2579fa64bbe9fa0b93475a7a8db051080b0c5fade0d1c018e7858cd4cbe95145b0620e2f632cbe0f8af9cbf22e2fdaa72245ae31b0877b07181cc69dd2df74454251d8de58d25e76354abe7eb690f22e59b08795a8f2c98c578e0599503d9085927634072c82c9f82abd50fd12b8fd1a9d1954eb5cc0b4cfb5796b5aaec0356643b4a65a368442d92ef94edd3ac6a2b7fe3571b8cf9f462729228aab023ef9183f73792f5379633ccac51079177d604c6bc1873ada6f07d8da6d68c897e88a5fa5d63fdb8df820f46090e0716e7562dd3c140ba279a65b996f60addb0abe29d4bf2f5abe89480771d492307b926d91f02f341b2148502903c43d40f3c6c86a811d060711f0698b384acdcc0add44eb54e42962d3d041accc715afd49407715adc09350cb55e8d9281a3b0b6b5fcd91726eede9b7c8b13afdebb2c2b377629595f1096ba62fb14946dbac5f3c5f0b4e5b712e7acc7dcf6c46cdc5e6d6dfdeee55a0c92c2d70f080ac6";
        antoxRepo = createRepo("Tox", "https://pkg.tox.chat/fdroid/repo", context, antoxCert);
    }

    public void setEnabled(Repo repo, boolean enabled) {
        ContentValues values = new ContentValues(1);
        values.put(Schema.RepoTable.Cols.IN_USE, enabled ? 1 : 0);
        RepoProvider.Helper.update(context, repo, values);
    }

    @Test
    public void antoxRepo() throws RepoUpdater.UpdateException {
        assertAntoxEmpty();
        setEnabled(microGRepo, true);
        updateAntox();
        assertAntoxExists();
    }

    private void updateAntox() throws RepoUpdater.UpdateException {
        updateRepo(new RepoUpdater(context, antoxRepo), "index.antox.jar");
    }

    @Test
    public void microGRepo() throws RepoUpdater.UpdateException {
        assertMicroGEmpty();
        setEnabled(microGRepo, true);
        updateMicroG();
        assertMicroGExists();
    }

    private void updateMicroG() throws RepoUpdater.UpdateException {
        updateRepo(new RepoUpdater(context, microGRepo), "index.microg.jar");
    }

    @Test
    public void antoxAndMicroG() throws RepoUpdater.UpdateException {
        assertMicroGEmpty();
        assertAntoxEmpty();

        setEnabled(microGRepo, true);
        setEnabled(antoxRepo, true);
        updateMicroG();
        updateAntox();
        assertMicroGExists();
        assertAntoxExists();

        setEnabled(microGRepo, false);
        RepoProvider.Helper.purgeApps(context, microGRepo);
        assertMicroGEmpty();
        assertAntoxExists();

        setEnabled(microGRepo, true);
        updateMicroG();
        assertMicroGExists();
        assertAntoxExists();

        setEnabled(antoxRepo, false);
        RepoProvider.Helper.purgeApps(context, antoxRepo);
        assertMicroGExists();
        assertAntoxEmpty();

        setEnabled(antoxRepo, true);
        updateAntox();
        assertMicroGExists();
        assertAntoxExists();
    }

    private void assertAntoxEmpty() {
        List<Apk> actualApksBeforeUpdate = ApkProvider.Helper.findByRepo(context, antoxRepo, Schema.ApkTable.Cols.ALL);
        assertEquals(0, actualApksBeforeUpdate.size());
    }

    private void assertMicroGEmpty() {
        List<Apk> actualApksBeforeUpdate = ApkProvider.Helper.findByRepo(context, microGRepo, Schema.ApkTable.Cols.ALL);
        assertEquals(0, actualApksBeforeUpdate.size());
    }

    private void assertAntoxExists() {
        String packageName = "chat.tox.antox";
        List<Apk> actualApksAfterUpdate = ApkProvider.Helper.findByRepo(context, antoxRepo, Schema.ApkTable.Cols.ALL);
        int[] expectedVersions = new int[] {15421};

        assertApp(packageName, expectedVersions);
        assertApksExist(actualApksAfterUpdate, packageName, expectedVersions);
    }

    private void assertMicroGExists() {
        List<Apk> actualApksAfterUpdate = ApkProvider.Helper.findByRepo(context, microGRepo, Schema.ApkTable.Cols.ALL);

        String vendingPackage = "com.android.vending";
        int[] expectedVendingVersions = new int[] {1};
        assertApp(vendingPackage, expectedVendingVersions);
        assertApksExist(actualApksAfterUpdate, vendingPackage, expectedVendingVersions);

        String gmsPackage = "com.google.android.gms";
        int[] expectedGmsVersions = new int[] {11059462, 10545451, 10545440, 10087438, 10087435, 9258259, 8492252, };
        assertApp(gmsPackage, expectedGmsVersions);
        assertApksExist(actualApksAfterUpdate, gmsPackage, expectedGmsVersions);

        String gsfPackage = "com.google.android.gsf";
        int[] expectedGsfVersions = new int[] {8};
        assertApp(gsfPackage, expectedGsfVersions);
        assertApksExist(actualApksAfterUpdate, gsfPackage, expectedGsfVersions);
    }

}

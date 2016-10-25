package org.belmarket.shop.updater;

import android.content.ContentValues;

import org.belmarket.shop.BuildConfig;
import org.belmarket.shop.RepoUpdater;
import org.belmarket.shop.data.Apk;
import org.belmarket.shop.data.ApkProvider;
import org.belmarket.shop.data.Repo;
import org.belmarket.shop.data.RepoProvider;
import org.belmarket.shop.data.Schema;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import static org.junit.Assert.assertEquals;

// TODO: Use sdk=24 when Robolectric supports this
@Config(constants = BuildConfig.class, sdk = 23)
@RunWith(RobolectricGradleTestRunner.class)
public class Issue763MultiRepo extends MultiRepoUpdaterTest {

    private Repo microGRepo;
    private Repo antoxRepo;

    @Before
    public void setup() {
        String microGCert = "308202ed308201d5a003020102020426ffa009300d06092a864886f70d01010b05003027310b300906035504061302444531183016060355040a130f4e4f47415050532050726f6a656374301e170d3132313030363132303533325a170d3337303933303132303533325a3027310b300906035504061302444531183016060355040a130f4e4f47415050532050726f6a65637430820122300d06092a864886f70d01010105000382010f003082010a02820101009a8d2a5336b0eaaad89ce447828c7753b157459b79e3215dc962ca48f58c2cd7650df67d2dd7bda0880c682791f32b35c504e43e77b43c3e4e541f86e35a8293a54fb46e6b16af54d3a4eda458f1a7c8bc1b7479861ca7043337180e40079d9cdccb7e051ada9b6c88c9ec635541e2ebf0842521c3024c826f6fd6db6fd117c74e859d5af4db04448965ab5469b71ce719939a06ef30580f50febf96c474a7d265bb63f86a822ff7b643de6b76e966a18553c2858416cf3309dd24278374bdd82b4404ef6f7f122cec93859351fc6e5ea947e3ceb9d67374fe970e593e5cd05c905e1d24f5a5484f4aadef766e498adf64f7cf04bddd602ae8137b6eea40722d0203010001a321301f301d0603551d0e04160414110b7aa9ebc840b20399f69a431f4dba6ac42a64300d06092a864886f70d01010b0500038201010007c32ad893349cf86952fb5a49cfdc9b13f5e3c800aece77b2e7e0e9c83e34052f140f357ec7e6f4b432dc1ed542218a14835acd2df2deea7efd3fd5e8f1c34e1fb39ec6a427c6e6f4178b609b369040ac1f8844b789f3694dc640de06e44b247afed11637173f36f5886170fafd74954049858c6096308fc93c1bc4dd5685fa7a1f982a422f2a3b36baa8c9500474cf2af91c39cbec1bc898d10194d368aa5e91f1137ec115087c31962d8f76cd120d28c249cf76f4c70f5baa08c70a7234ce4123be080cee789477401965cfe537b924ef36747e8caca62dfefdd1a6288dcb1c4fd2aaa6131a7ad254e9742022cfd597d2ca5c660ce9e41ff537e5a4041e37";
        microGRepo = createRepo("MicroG", "https://microg.org/fdroid/repo", context, microGCert);

        String antoxCert = "308204f1308202d9a0030201020204565444c6300d06092a864886f70d01010b050030293110300e060355040b1307462d44726f6964311530130603550403130c706b672e746f782e63686174301e170d3136303131323037353333395a170d3433303533303037353333395a30293110300e060355040b1307462d44726f6964311530130603550403130c706b672e746f782e6368617430820222300d06092a864886f70d01010105000382020f003082020a0282020100a16c61417d25545d681d7c01acea881f268bb4d708099aa12143f12d24a18afe120f532901efc3a26137915bba5ffd4e8f0d21783965b2c207593b44002e6ed7ca6cbb124829c134950c9c76388d70cdb5c1ac37581687f1a4a51ce7d5c0a21ab000bae2c14572d26693de8b33726852e262ec9c85bed5d6a1e236977862d8e796a3722bc69346ae27951527963165469ab4ec9c8a38ccabd4c3de718eeaa8ff054bcd04374ac46af7cf011d97bd2625c4c7f6c2851b0cab8446eaf2f94a2b1506aafdfb192d0a31ede495fd4cc4e122f92daa500806d29aa3f63e51dbf6f00fb37979b4b70543f019d55e95173165378983517f2d2811fe79d491f09dc2568794c3a09f539986d4489b939d65c26a83f6b6165976c00cd648d081afc3a5eeabd1c4e3c0ae42d92197f4086caca8bcfe939036d02f84e815b95842da27daea297bd098507f806012fe0cae2d4dd38bd876a7efb0c173bfae260e820d56e7026afe4f8806d12ffa2e75da33d178472625414d2ed3e0fe9ef1f2ba0b26b877960bea54c8e32f36540758712b40775b56ef1149db3df17202e5163733df12a48011f9077e34aebeba2fe11dc578ce12bd26a30a1d458d0172378b530de118dc46823e17f6bbb7d0163483531e12d416f6f9d90d63b6c248a30058fb1c0b1c78a50601fdad0ca22dbfa470059251f37ce558a7bacf2ebad2d810049717aee16c8d530203010001a321301f301d0603551d0e0416041419f45074c35bffc3bdc4ebee34966db52cfcb54e300d06092a864886f70d01010b05000382020100426ce3f1a5944dbd41e6abc5348e32f220f46b58a8794091e5de1f6248af18e42c0b1ed9c5196b27c9fbd0aa59170653f3044b0f8cd60f027f888be91fdb52fb7e6f3c125bbbf968ca1d43fede1a47a82ebd89ef37e2abc5931f2475ed7c3b95707c75f1e90e0f08460288ee090e5136a4dd682ddf8755b6d2c8e8ff58037865d69f198599371cca60e6ab8cac7c35de1edfaff2730a8c91489e30c7d770fe7b2299b41229d27989bf20260d43c8c077e53ec11e1a17b8879ec8c995fb9fa178d6ffdd6629c3104601368cc76e0f10f7ad3a2a729f92d219700dd44a8621a8102ec61e28d534b518633b4edb125966e80bf006f0e1258f7bd36357ab2ebdb8fb40fb1616f75bf5db2689b3910dff266084832159afc1571454a8070fe2a02389254e5f3cbab933a57117cb76bb615c4180c88f3bd04c6f23ea75ac05ab81dd4ddd2c9f2b3eb94d54682f12c7838612f434b00a6da678e9e82b5b4a18c037929a622773c6bc4bcf1eb45872c998248d98812d6be1a77d0d70182b9b2296b8802fbd0fb2f04b78bfafbaae756940f777ae43f63e8f47e97618063fc743ba1dd3d37bb434581fb3487420dd893fa474d87b94923102c12a680ede3107c680be0d8d5e7f89c0033a740f0ba3e1563baddb540f0f9154653b003d9dd3cbdd649e808ecfec9dd8d9949ccabb7b8b7ad287023bc8ca12dad9892158da300a25de07aaca";
        antoxRepo = createRepo("Tox", "https://pkg.tox.chat/fdroid/repo", context, antoxCert);
    }

    public void setEnabled(Repo repo, boolean enabled) {
        ContentValues values = new ContentValues(1);
        values.put(Schema.RepoTable.Cols.IN_USE, enabled);
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
        int[] expectedGmsVersions = new int[] {9452267, 9452266, 9452265, 9258262, 9258259, 9258258, 8492252, };
        assertApp(gmsPackage, expectedGmsVersions);
        assertApksExist(actualApksAfterUpdate, gmsPackage, expectedGmsVersions);

        String gsfPackage = "com.google.android.gsf";
        int[] expectedGsfVersions = new int[] {8};
        assertApp(gsfPackage, expectedGsfVersions);
        assertApksExist(actualApksAfterUpdate, gsfPackage, expectedGsfVersions);
    }

}

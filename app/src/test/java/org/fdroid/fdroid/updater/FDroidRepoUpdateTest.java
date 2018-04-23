
package org.fdroid.fdroid.updater;

import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.RepoUpdater;
import org.fdroid.fdroid.Utils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Tests two versions of the official main F-Droid metadata, from 10 days apart. This is here
 * because there is so much metadata to parse in the main repo, covering many different aspects
 * of the available metadata. Some apps will be added, others updated, and it should all just work.
 */
@Config(constants = BuildConfig.class)
@RunWith(RobolectricTestRunner.class)
public class FDroidRepoUpdateTest extends MultiRepoUpdaterTest {

    private static final String TAG = "FDroidRepoUpdateTest";

    private static final String REPO_FDROID = "F-Droid";
    private static final String REPO_FDROID_URI = "https://f-droid.org/repo";
    private static final String REPO_FDROID_PUB_KEY = "3082035e30820246a00302010202044c49cd00300d06092a864886f70d01010505003071310b300906035504061302554b3110300e06035504081307556e6b6e6f776e3111300f0603550407130857657468657262793110300e060355040a1307556e6b6e6f776e3110300e060355040b1307556e6b6e6f776e311930170603550403131043696172616e2047756c746e69656b73301e170d3130303732333137313032345a170d3337313230383137313032345a3071310b300906035504061302554b3110300e06035504081307556e6b6e6f776e3111300f0603550407130857657468657262793110300e060355040a1307556e6b6e6f776e3110300e060355040b1307556e6b6e6f776e311930170603550403131043696172616e2047756c746e69656b7330820122300d06092a864886f70d01010105000382010f003082010a028201010096d075e47c014e7822c89fd67f795d23203e2a8843f53ba4e6b1bf5f2fd0e225938267cfcae7fbf4fe596346afbaf4070fdb91f66fbcdf2348a3d92430502824f80517b156fab00809bdc8e631bfa9afd42d9045ab5fd6d28d9e140afc1300917b19b7c6c4df4a494cf1f7cb4a63c80d734265d735af9e4f09455f427aa65a53563f87b336ca2c19d244fcbba617ba0b19e56ed34afe0b253ab91e2fdb1271f1b9e3c3232027ed8862a112f0706e234cf236914b939bcf959821ecb2a6c18057e070de3428046d94b175e1d89bd795e535499a091f5bc65a79d539a8d43891ec504058acb28c08393b5718b57600a211e803f4a634e5c57f25b9b8c4422c6fd90203010001300d06092a864886f70d0101050500038201010008e4ef699e9807677ff56753da73efb2390d5ae2c17e4db691d5df7a7b60fc071ae509c5414be7d5da74df2811e83d3668c4a0b1abc84b9fa7d96b4cdf30bba68517ad2a93e233b042972ac0553a4801c9ebe07bf57ebe9a3b3d6d663965260e50f3b8f46db0531761e60340a2bddc3426098397fda54044a17e5244549f9869b460ca5e6e216b6f6a2db0580b480ca2afe6ec6b46eedacfa4aa45038809ece0c5978653d6c85f678e7f5a2156d1bedd8117751e64a4b0dcd140f3040b021821a8d93aed8d01ba36db6c82372211fed714d9a32607038cdfd565bd529ffc637212aaa2c224ef22b603eccefb5bf1e085c191d4b24fe742b17ab3f55d4e6f05ef"; // NOCHECKSTYLE LineLength

    @Test
    public void doesntCrash() throws RepoUpdater.UpdateException {
        assertEmpty();
        updateEarlier();
        updateLater();
        updateV1Later();
    }

    protected void updateEarlier() throws RepoUpdater.UpdateException {
        Utils.debugLog(TAG, "Updating earlier version of F-Droid repo");
        updateRepo(createRepoUpdater(REPO_FDROID, REPO_FDROID_URI, context, REPO_FDROID_PUB_KEY),
                "index.fdroid.2016-10-30.jar");
    }

    protected void updateLater() throws RepoUpdater.UpdateException {
        Utils.debugLog(TAG, "Updating later version of F-Droid repo");
        updateRepo(createRepoUpdater(REPO_FDROID, REPO_FDROID_URI, context, REPO_FDROID_PUB_KEY),
                "index.fdroid.2016-11-10.jar");
    }

    protected void updateV1Later() throws RepoUpdater.UpdateException {
        Utils.debugLog(TAG, "Updating later version of F-Droid index-v1");
        updateRepo(createIndexV1Updater(REPO_FDROID, REPO_FDROID_URI, context, REPO_FDROID_PUB_KEY),
                "index-v1.fdroid.2017-07-07.jar");
    }
}

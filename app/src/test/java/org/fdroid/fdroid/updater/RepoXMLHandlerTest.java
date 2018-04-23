/*
 * Copyright (C) 2016 Blue Jay Wireless
 * Copyright (C) 2015 Daniel Martí <mvdan@mvdan.cc>
 * Copyright (C) 2014-2016 Hans-Christoph Steiner <hans@eds.org>
 * Copyright (C) 2014-2016 Peter Serwylo <peter@serwylo.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */

package org.fdroid.fdroid.updater;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import org.apache.commons.io.FileUtils;
import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoPushRequest;
import org.fdroid.fdroid.mock.RepoDetails;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@Config(constants = BuildConfig.class)
@RunWith(RobolectricTestRunner.class)
public class RepoXMLHandlerTest {
    private static final String TAG = "RepoXMLHandlerTest";

    private static final String FAKE_SIGNING_CERT = "012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345"; // NOCHECKSTYLE LineLength

    @Test
    public void testExtendedPerms() throws IOException {
        Repo expectedRepo = new Repo();
        expectedRepo.name = "F-Droid";
        expectedRepo.signingCertificate = "3082035e30820246a00302010202044c49cd00300d06092a864886f70d01010505003071310b300906035504061302554b3110300e06035504081307556e6b6e6f776e3111300f0603550407130857657468657262793110300e060355040a1307556e6b6e6f776e3110300e060355040b1307556e6b6e6f776e311930170603550403131043696172616e2047756c746e69656b73301e170d3130303732333137313032345a170d3337313230383137313032345a3071310b300906035504061302554b3110300e06035504081307556e6b6e6f776e3111300f0603550407130857657468657262793110300e060355040a1307556e6b6e6f776e3110300e060355040b1307556e6b6e6f776e311930170603550403131043696172616e2047756c746e69656b7330820122300d06092a864886f70d01010105000382010f003082010a028201010096d075e47c014e7822c89fd67f795d23203e2a8843f53ba4e6b1bf5f2fd0e225938267cfcae7fbf4fe596346afbaf4070fdb91f66fbcdf2348a3d92430502824f80517b156fab00809bdc8e631bfa9afd42d9045ab5fd6d28d9e140afc1300917b19b7c6c4df4a494cf1f7cb4a63c80d734265d735af9e4f09455f427aa65a53563f87b336ca2c19d244fcbba617ba0b19e56ed34afe0b253ab91e2fdb1271f1b9e3c3232027ed8862a112f0706e234cf236914b939bcf959821ecb2a6c18057e070de3428046d94b175e1d89bd795e535499a091f5bc65a79d539a8d43891ec504058acb28c08393b5718b57600a211e803f4a634e5c57f25b9b8c4422c6fd90203010001300d06092a864886f70d0101050500038201010008e4ef699e9807677ff56753da73efb2390d5ae2c17e4db691d5df7a7b60fc071ae509c5414be7d5da74df2811e83d3668c4a0b1abc84b9fa7d96b4cdf30bba68517ad2a93e233b042972ac0553a4801c9ebe07bf57ebe9a3b3d6d663965260e50f3b8f46db0531761e60340a2bddc3426098397fda54044a17e5244549f9869b460ca5e6e216b6f6a2db0580b480ca2afe6ec6b46eedacfa4aa45038809ece0c5978653d6c85f678e7f5a2156d1bedd8117751e64a4b0dcd140f3040b021821a8d93aed8d01ba36db6c82372211fed714d9a32607038cdfd565bd529ffc637212aaa2c224ef22b603eccefb5bf1e085c191d4b24fe742b17ab3f55d4e6f05ef"; // NOCHECKSTYLE LineLength
        expectedRepo.description = "This is just a test of the extended permissions attributes.";
        expectedRepo.timestamp = 1467169032;
        RepoDetails actualDetails = getFromFile("extendedPerms.xml");
        handlerTestSuite(expectedRepo, actualDetails, 2, 6, 14, 16);
    }

    @Test
    public void testObbIndex() throws IOException {
        writeResourceToObbDir("main.1101613.obb.main.twoversions.obb");
        writeResourceToObbDir("main.1101615.obb.main.twoversions.obb");
        writeResourceToObbDir("main.1434483388.obb.main.oldversion.obb");
        writeResourceToObbDir("main.1619.obb.mainpatch.current.obb");
        writeResourceToObbDir("patch.1619.obb.mainpatch.current.obb");
        RepoDetails actualDetails = getFromFile("obbIndex.xml");
        for (Apk indexApk : actualDetails.apks) {
            Apk localApk = new Apk();
            localApk.packageName = indexApk.packageName;
            localApk.versionCode = indexApk.versionCode;
            localApk.hashType = indexApk.hashType;
            App.initInstalledObbFiles(localApk);
            assertEquals(indexApk.obbMainFile, localApk.obbMainFile);
            assertEquals(indexApk.obbMainFileSha256, localApk.obbMainFileSha256);
            assertEquals(indexApk.obbPatchFile, localApk.obbPatchFile);
            assertEquals(indexApk.obbPatchFileSha256, localApk.obbPatchFileSha256);
        }
    }

    @Test
    public void testSimpleIndex() {
        Repo expectedRepo = new Repo();
        expectedRepo.name = "F-Droid";
        expectedRepo.signingCertificate = "308201ee30820157a0030201020204300d845b300d06092a864886f70d01010b0500302a3110300e060355040b1307462d44726f6964311630140603550403130d70616c6174736368696e6b656e301e170d3134303432373030303633315a170d3431303931323030303633315a302a3110300e060355040b1307462d44726f6964311630140603550403130d70616c6174736368696e6b656e30819f300d06092a864886f70d010101050003818d0030818902818100a439472e4b6d01141bfc94ecfe131c7c728fdda670bb14c57ca60bd1c38a8b8bc0879d22a0a2d0bc0d6fdd4cb98d1d607c2caefbe250a0bd0322aedeb365caf9b236992fac13e6675d3184a6c7c6f07f73410209e399a9da8d5d7512bbd870508eebacff8b57c3852457419434d34701ccbf692267cbc3f42f1c5d1e23762d790203010001a321301f301d0603551d0e041604140b1840691dab909746fde4bfe28207d1cae15786300d06092a864886f70d01010b05000381810062424c928ffd1b6fd419b44daafef01ca982e09341f7077fb865905087aeac882534b3bd679b51fdfb98892cef38b63131c567ed26c9d5d9163afc775ac98ad88c405d211d6187bde0b0d236381cc574ba06ef9080721a92ae5a103a7301b2c397eecc141cc850dd3e123813ebc41c59d31ddbcb6e984168280c53272f6a442b"; // NOCHECKSTYLE LineLength
        expectedRepo.description = "The official repository of the F-Droid client. Applications in this repository are either official binaries built by the original application developers, or are binaries built from source by the admin of f-droid.org using the tools on https://gitorious.org/f-droid.";  // NOCHECKSTYLE LineLength
        expectedRepo.timestamp = 1398733213;
        RepoDetails actualDetails = getFromFile("simpleIndex.xml");
        handlerTestSuite(expectedRepo, actualDetails, 0, 0, -1, 12);
    }

    @Test
    public void testSmallRepo() {
        Repo expectedRepo = new Repo();
        expectedRepo.name = "Android-Nexus-7-20139453 on UNSET";
        expectedRepo.signingCertificate = "308202da308201c2a00302010202080eb08c796fec91aa300d06092a864886f70d0101050500302d3111300f060355040a0c084b6572706c61707031183016060355040b0c0f477561726469616e50726f6a656374301e170d3134313030333135303631325a170d3135313030333135303631325a302d3111300f060355040a0c084b6572706c61707031183016060355040b0c0f477561726469616e50726f6a65637430820122300d06092a864886f70d01010105000382010f003082010a0282010100c7ab44b130be5c00eedcc3625462f6f6ac26e502641cd641f3e30cbb0ff1ba325158611e7fc2448a35b6a6df30dc6e23602cf6909448befcf11e2fe486b580f1e76fe5887d159050d00afd2c4079f6538896bb200627f4b3e874f011ce5df0fef5d150fcb0b377b531254e436eaf4083ea72fe3b8c3ef450789fa858f2be8f6c5335bb326aff3dda689fbc7b5ba98dea53651dbea7452c38d294985ac5dd8a9e491a695de92c706d682d6911411fcaef3b0a08a030fe8a84e47acaab0b7edcda9d190ce39e810b79b1d8732eca22b15f0d048c8d6f00503a7ee81ab6e08919ff465883432304d95238b95e95c5f74e0a421809e2a6a85825aed680e0d6939e8f0203010001300d06092a864886f70d010105050003820101006d17aad3271b8b2c299dbdb7b1182849b0d5ddb9f1016dcb3487ae0db02b6be503344c7d066e2050bcd01d411b5ee78c7ed450f0ff9da5ce228f774cbf41240361df53d9c6078159d16f4d34379ab7dedf6186489397c83b44b964251a2ebb42b7c4689a521271b1056d3b5a5fa8f28ba64fb8ce5e2226c33c45d27ba3f632dc266c12abf582b8438c2abcf3eae9de9f31152b4158ace0ef33435c20eb809f1b3988131db6e5a1442f2617c3491d9565fedb3e320e8df4236200d3bd265e47934aa578f84d0d1a5efeb49b39907e876452c46996d0feff9404b41aa5631b4482175d843d5512ded45e12a514690646492191e7add434afce63dbff8f0b03ec0c"; // NOCHECKSTYLE LineLength
        expectedRepo.description = "A local FDroid repo generated from apps installed on Android-Nexus-7-20139453";
        expectedRepo.timestamp = 1412696461;
        RepoDetails actualDetails = getFromFile("smallRepo.xml");
        handlerTestSuite(expectedRepo, actualDetails, 12, 12, 14, -1);
        checkIncludedApps(actualDetails.apps, new String[]{
                "org.mozilla.firefox",
                "com.koushikdutta.superuser",
                "info.guardianproject.courier",
                "org.adaway",
                "info.guardianproject.gilga",
                "com.google.zxing.client.android",
                "info.guardianproject.lildebi",
                "de.danoeh.antennapod",
                "info.guardianproject.otr.app.im",
                "org.torproject.android",
                "org.gege.caldavsyncadapter",
                "info.guardianproject.checkey",
        });
    }

    @Test
    public void testPushRequestsRepoIgnore() {
        Repo expectedRepo = new Repo();
        expectedRepo.name = "non-public test repo";
        expectedRepo.signingCertificate = "308204e1308202c9a0030201020204483450fa300d06092a864886f70d01010b050030213110300e060355040b1307462d44726f6964310d300b06035504031304736f7661301e170d3136303832333133333131365a170d3434303130393133333131365a30213110300e060355040b1307462d44726f6964310d300b06035504031304736f766130820222300d06092a864886f70d01010105000382020f003082020a0282020100dfdcd120f3ab224999dddf4ea33ea588d295e4d7130bef48c143e9d76e5c0e0e9e5d45e64208e35feebc79a83f08939dd6a343b7d1e2179930a105a1249ccd36d88ff3feffc6e4dc53dae0163a7876dd45ecc1ddb0adf5099aa56c1a84b52affcd45d0711ffa4de864f35ac0333ebe61ea8673eeda35a88f6af678cc4d0f80b089338ac8f2a8279a64195c611d19445cab3fd1a020afed9bd739bb95142fb2c00a8f847db5ef3325c814f8eb741bacf86ed3907bfe6e4564d2de5895df0c263824e0b75407589bae2d3a4666c13b92102d8781a8ee9bb4a5a1a78c4a9c21efdaf5584da42e84418b28f5a81d0456a3dc5b420991801e6b21e38c99bbe018a5b2d690894a114bc860d35601416aa4dc52216aff8a288d4775cddf8b72d45fd2f87303a8e9c0d67e442530be28eaf139894337266e0b33d57f949256ab32083bcc545bc18a83c9ab8247c12aea037e2b68dee31c734cb1f04f241d3b94caa3a2b258ffaf8e6eae9fbbe029a934dc0a0859c5f120334812693a1c09352340a39f2a678dbc1afa2a978bfee43afefcb7e224a58af2f3d647e5745db59061236b8af6fcfd93b3602f9e456978534f3a7851e800071bf56da80401c81d91c45f82568373af0576b1cc5eef9b85654124b6319770be3cdba3fbebe3715e8918fb6c8966624f3d0e815effac3d2ee06dd34ab9c693218b2c7c06ba99d6b74d4f17b8c3cb0203010001a321301f301d0603551d0e04160414d62bee9f3798509546acc62eb1de14b08b954d4f300d06092a864886f70d01010b05000382020100743f7c5692085895f9d1fffad390fb4202c15f123ed094df259185960fd6dadf66cb19851070f180297bba4e6996a4434616573b375cfee94fee73a4505a7ec29136b7e6c22e6436290e3686fe4379d4e3140ec6a08e70cfd3ed5b634a5eb5136efaaabf5f38e0432d3d79568a556970b8cfba2972f5d23a3856d8a981b9e9bbbbb88f35e708bde9cbc5f681cbd974085b9da28911296fe2579fa64bbe9fa0b93475a7a8db051080b0c5fade0d1c018e7858cd4cbe95145b0620e2f632cbe0f8af9cbf22e2fdaa72245ae31b0877b07181cc69dd2df74454251d8de58d25e76354abe7eb690f22e59b08795a8f2c98c578e0599503d9085927634072c82c9f82abd50fd12b8fd1a9d1954eb5cc0b4cfb5796b5aaec0356643b4a65a368442d92ef94edd3ac6a2b7fe3571b8cf9f462729228aab023ef9183f73792f5379633ccac51079177d604c6bc1873ada6f07d8da6d68c897e88a5fa5d63fdb8df820f46090e0716e7562dd3c140ba279a65b996f60addb0abe29d4bf2f5abe89480771d492307b926d91f02f341b2148502903c43d40f3c6c86a811d060711f0698b384acdcc0add44eb54e42962d3d041accc715afd49407715adc09350cb55e8d9281a3b0b6b5fcd91726eede9b7c8b13afdebb2c2b377629595f1096ba62fb14946dbac5f3c5f0b4e5b712e7acc7dcf6c46cdc5e6d6dfdeee55a0c92c2d70f080ac6"; // NOCHECKSTYLE LineLength
        expectedRepo.description = "This is a repository of apps to be used with F-Droid. Applications in this repository are either official binaries built by the original application developers, or are binaries built from source by the admin of f-droid.org using the tools on https://gitlab.com/u/fdroid."; // NOCHECKSTYLE LineLength
        expectedRepo.timestamp = 1472071347;
        RepoDetails actualDetails = getFromFile("pushRequestsIndex.xml", Repo.PUSH_REQUEST_IGNORE);
        handlerTestSuite(expectedRepo, actualDetails, 2, 14, -1, 17);
        checkPushRequests(actualDetails);

        List<RepoPushRequest> repoPushRequests = actualDetails.repoPushRequestList;
        assertNotNull(repoPushRequests);
        assertEquals(0, repoPushRequests.size());
    }

    @Test
    public void testPushRequestsRepoAlways() {
        Repo expectedRepo = new Repo();
        expectedRepo.name = "non-public test repo";
        expectedRepo.signingCertificate = "308204e1308202c9a0030201020204483450fa300d06092a864886f70d01010b050030213110300e060355040b1307462d44726f6964310d300b06035504031304736f7661301e170d3136303832333133333131365a170d3434303130393133333131365a30213110300e060355040b1307462d44726f6964310d300b06035504031304736f766130820222300d06092a864886f70d01010105000382020f003082020a0282020100dfdcd120f3ab224999dddf4ea33ea588d295e4d7130bef48c143e9d76e5c0e0e9e5d45e64208e35feebc79a83f08939dd6a343b7d1e2179930a105a1249ccd36d88ff3feffc6e4dc53dae0163a7876dd45ecc1ddb0adf5099aa56c1a84b52affcd45d0711ffa4de864f35ac0333ebe61ea8673eeda35a88f6af678cc4d0f80b089338ac8f2a8279a64195c611d19445cab3fd1a020afed9bd739bb95142fb2c00a8f847db5ef3325c814f8eb741bacf86ed3907bfe6e4564d2de5895df0c263824e0b75407589bae2d3a4666c13b92102d8781a8ee9bb4a5a1a78c4a9c21efdaf5584da42e84418b28f5a81d0456a3dc5b420991801e6b21e38c99bbe018a5b2d690894a114bc860d35601416aa4dc52216aff8a288d4775cddf8b72d45fd2f87303a8e9c0d67e442530be28eaf139894337266e0b33d57f949256ab32083bcc545bc18a83c9ab8247c12aea037e2b68dee31c734cb1f04f241d3b94caa3a2b258ffaf8e6eae9fbbe029a934dc0a0859c5f120334812693a1c09352340a39f2a678dbc1afa2a978bfee43afefcb7e224a58af2f3d647e5745db59061236b8af6fcfd93b3602f9e456978534f3a7851e800071bf56da80401c81d91c45f82568373af0576b1cc5eef9b85654124b6319770be3cdba3fbebe3715e8918fb6c8966624f3d0e815effac3d2ee06dd34ab9c693218b2c7c06ba99d6b74d4f17b8c3cb0203010001a321301f301d0603551d0e04160414d62bee9f3798509546acc62eb1de14b08b954d4f300d06092a864886f70d01010b05000382020100743f7c5692085895f9d1fffad390fb4202c15f123ed094df259185960fd6dadf66cb19851070f180297bba4e6996a4434616573b375cfee94fee73a4505a7ec29136b7e6c22e6436290e3686fe4379d4e3140ec6a08e70cfd3ed5b634a5eb5136efaaabf5f38e0432d3d79568a556970b8cfba2972f5d23a3856d8a981b9e9bbbbb88f35e708bde9cbc5f681cbd974085b9da28911296fe2579fa64bbe9fa0b93475a7a8db051080b0c5fade0d1c018e7858cd4cbe95145b0620e2f632cbe0f8af9cbf22e2fdaa72245ae31b0877b07181cc69dd2df74454251d8de58d25e76354abe7eb690f22e59b08795a8f2c98c578e0599503d9085927634072c82c9f82abd50fd12b8fd1a9d1954eb5cc0b4cfb5796b5aaec0356643b4a65a368442d92ef94edd3ac6a2b7fe3571b8cf9f462729228aab023ef9183f73792f5379633ccac51079177d604c6bc1873ada6f07d8da6d68c897e88a5fa5d63fdb8df820f46090e0716e7562dd3c140ba279a65b996f60addb0abe29d4bf2f5abe89480771d492307b926d91f02f341b2148502903c43d40f3c6c86a811d060711f0698b384acdcc0add44eb54e42962d3d041accc715afd49407715adc09350cb55e8d9281a3b0b6b5fcd91726eede9b7c8b13afdebb2c2b377629595f1096ba62fb14946dbac5f3c5f0b4e5b712e7acc7dcf6c46cdc5e6d6dfdeee55a0c92c2d70f080ac6"; // NOCHECKSTYLE LineLength
        expectedRepo.description = "This is a repository of apps to be used with F-Droid. Applications in this repository are either official binaries built by the original application developers, or are binaries built from source by the admin of f-droid.org using the tools on https://gitlab.com/u/fdroid."; // NOCHECKSTYLE LineLength
        expectedRepo.timestamp = 1472071347;
        RepoDetails actualDetails = getFromFile("pushRequestsIndex.xml", Repo.PUSH_REQUEST_ACCEPT_ALWAYS);
        handlerTestSuite(expectedRepo, actualDetails, 2, 14, -1, 17);
        checkPushRequests(actualDetails);

        List<RepoPushRequest> repoPushRequests = actualDetails.repoPushRequestList;
        assertNotNull(repoPushRequests);
        assertEquals(6, repoPushRequests.size());
    }

    @Test
    public void testMediumRepo() {
        Repo expectedRepo = new Repo();
        expectedRepo.name = "Guardian Project Official Releases";
        expectedRepo.signingCertificate = "308205d8308203c0020900a397b4da7ecda034300d06092a864886f70d01010505003081ad310b30090603550406130255533111300f06035504080c084e657720596f726b3111300f06035504070c084e657720596f726b31143012060355040b0c0b4644726f6964205265706f31193017060355040a0c10477561726469616e2050726f6a656374311d301b06035504030c14677561726469616e70726f6a6563742e696e666f3128302606092a864886f70d0109011619726f6f7440677561726469616e70726f6a6563742e696e666f301e170d3134303632363139333931385a170d3431313131303139333931385a3081ad310b30090603550406130255533111300f06035504080c084e657720596f726b3111300f06035504070c084e657720596f726b31143012060355040b0c0b4644726f6964205265706f31193017060355040a0c10477561726469616e2050726f6a656374311d301b06035504030c14677561726469616e70726f6a6563742e696e666f3128302606092a864886f70d0109011619726f6f7440677561726469616e70726f6a6563742e696e666f30820222300d06092a864886f70d01010105000382020f003082020a0282020100b3cd79121b9b883843be3c4482e320809106b0a23755f1dd3c7f46f7d315d7bb2e943486d61fc7c811b9294dcc6b5baac4340f8db2b0d5e14749e7f35e1fc211fdbc1071b38b4753db201c314811bef885bd8921ad86facd6cc3b8f74d30a0b6e2e6e576f906e9581ef23d9c03e926e06d1f033f28bd1e21cfa6a0e3ff5c9d8246cf108d82b488b9fdd55d7de7ebb6a7f64b19e0d6b2ab1380a6f9d42361770d1956701a7f80e2de568acd0bb4527324b1e0973e89595d91c8cc102d9248525ae092e2c9b69f7414f724195b81427f28b1d3d09a51acfe354387915fd9521e8c890c125fc41a12bf34d2a1b304067ab7251e0e9ef41833ce109e76963b0b256395b16b886bca21b831f1408f836146019e7908829e716e72b81006610a2af08301de5d067c9e114a1e5759db8a6be6a3cc2806bcfe6fafd41b5bc9ddddb3dc33d6f605b1ca7d8a9e0ecdd6390d38906649e68a90a717bea80fa220170eea0c86fc78a7e10dac7b74b8e62045a3ecca54e035281fdc9fe5920a855fde3c0be522e3aef0c087524f13d973dff3768158b01a5800a060c06b451ec98d627dd052eda804d0556f60dbc490d94e6e9dea62ffcafb5beffbd9fc38fb2f0d7050004fe56b4dda0a27bc47554e1e0a7d764e17622e71f83a475db286bc7862deee1327e2028955d978272ea76bf0b88e70a18621aba59ff0c5993ef5f0e5d6b6b98e68b70203010001300d06092a864886f70d0101050500038202010079c79c8ef408a20d243d8bd8249fb9a48350dc19663b5e0fce67a8dbcb7de296c5ae7bbf72e98a2020fb78f2db29b54b0e24b181aa1c1d333cc0303685d6120b03216a913f96b96eb838f9bff125306ae3120af838c9fc07ebb5100125436bd24ec6d994d0bff5d065221871f8410daf536766757239bf594e61c5432c9817281b985263bada8381292e543a49814061ae11c92a316e7dc100327b59e3da90302c5ada68c6a50201bda1fcce800b53f381059665dbabeeb0b50eb22b2d7d2d9b0aa7488ca70e67ac6c518adb8e78454a466501e89d81a45bf1ebc350896f2c3ae4b6679ecfbf9d32960d4f5b493125c7876ef36158562371193f600bc511000a67bdb7c664d018f99d9e589868d103d7e0994f166b2ba18ff7e67d8c4da749e44dfae1d930ae5397083a51675c409049dfb626a96246c0015ca696e94ebb767a20147834bf78b07fece3f0872b057c1c519ff882501995237d8206b0b3832f78753ebd8dcbd1d3d9f5ba733538113af6b407d960ec4353c50eb38ab29888238da843cd404ed8f4952f59e4bbc0035fc77a54846a9d419179c46af1b4a3b7fc98e4d312aaa29b9b7d79e739703dc0fa41c7280d5587709277ffa11c3620f5fba985b82c238ba19b17ebd027af9424be0941719919f620dd3bb3c3f11638363708aa11f858e153cf3a69bce69978b90e4a273836100aa1e617ba455cd00426847f"; // NOCHECKSTYLE LineLength
        expectedRepo.description = "The official app repository of The Guardian Project. Applications in this repository are official binaries build by the original application developers and signed by the same key as the APKs that are released in the Google Play store."; // NOCHECKSTYLE LineLength
        expectedRepo.timestamp = 1411427879;
        RepoDetails actualDetails = getFromFile("mediumRepo.xml");
        handlerTestSuite(expectedRepo, actualDetails, 15, 36, 60, 12);
        checkIncludedApps(actualDetails.apps, new String[]{
                "info.guardianproject.cacert",
                "info.guardianproject.otr.app.im",
                "info.guardianproject.soundrecorder",
                "info.guardianproject.checkey",
                "info.guardianproject.courier",
                "org.fdroid.fdroid",
                "info.guardianproject.gpg",
                "info.guardianproject.lildebi",
                "info.guardianproject.notepadbot",
                "org.witness.sscphase1",
                "org.torproject.android",
                "info.guardianproject.browser",
                "info.guardianproject.pixelknot",
                "info.guardianproject.chatsecure.emoji.core",
                "info.guardianproject.mrapp",
        });
    }

    @Test
    public void testLargeRepo() {
        Repo expectedRepo = new Repo();
        expectedRepo.name = "F-Droid";
        expectedRepo.signingCertificate = "3082035e30820246a00302010202044c49cd00300d06092a864886f70d01010505003071310b300906035504061302554b3110300e06035504081307556e6b6e6f776e3111300f0603550407130857657468657262793110300e060355040a1307556e6b6e6f776e3110300e060355040b1307556e6b6e6f776e311930170603550403131043696172616e2047756c746e69656b73301e170d3130303732333137313032345a170d3337313230383137313032345a3071310b300906035504061302554b3110300e06035504081307556e6b6e6f776e3111300f0603550407130857657468657262793110300e060355040a1307556e6b6e6f776e3110300e060355040b1307556e6b6e6f776e311930170603550403131043696172616e2047756c746e69656b7330820122300d06092a864886f70d01010105000382010f003082010a028201010096d075e47c014e7822c89fd67f795d23203e2a8843f53ba4e6b1bf5f2fd0e225938267cfcae7fbf4fe596346afbaf4070fdb91f66fbcdf2348a3d92430502824f80517b156fab00809bdc8e631bfa9afd42d9045ab5fd6d28d9e140afc1300917b19b7c6c4df4a494cf1f7cb4a63c80d734265d735af9e4f09455f427aa65a53563f87b336ca2c19d244fcbba617ba0b19e56ed34afe0b253ab91e2fdb1271f1b9e3c3232027ed8862a112f0706e234cf236914b939bcf959821ecb2a6c18057e070de3428046d94b175e1d89bd795e535499a091f5bc65a79d539a8d43891ec504058acb28c08393b5718b57600a211e803f4a634e5c57f25b9b8c4422c6fd90203010001300d06092a864886f70d0101050500038201010008e4ef699e9807677ff56753da73efb2390d5ae2c17e4db691d5df7a7b60fc071ae509c5414be7d5da74df2811e83d3668c4a0b1abc84b9fa7d96b4cdf30bba68517ad2a93e233b042972ac0553a4801c9ebe07bf57ebe9a3b3d6d663965260e50f3b8f46db0531761e60340a2bddc3426098397fda54044a17e5244549f9869b460ca5e6e216b6f6a2db0580b480ca2afe6ec6b46eedacfa4aa45038809ece0c5978653d6c85f678e7f5a2156d1bedd8117751e64a4b0dcd140f3040b021821a8d93aed8d01ba36db6c82372211fed714d9a32607038cdfd565bd529ffc637212aaa2c224ef22b603eccefb5bf1e085c191d4b24fe742b17ab3f55d4e6f05ef"; // NOCHECKSTYLE LineLength
        expectedRepo.description = "The official FDroid repository. Applications in this repository are mostly built directory from the source code. Some are official binaries built by the original application developers - these will be replaced by source-built versions over time."; // NOCHECKSTYLE LineLength
        expectedRepo.timestamp = 1412746769;
        RepoDetails actualDetails = getFromFile("largeRepo.xml");
        handlerTestSuite(expectedRepo, actualDetails, 1211, 2381, 14, 12);

        // Generated using something like the following:
        // sed 's,<application,\n<application,g' largeRepo.xml | grep "antifeatures" | sed 's,.*id="\(.*\)".*<antifeatures>\(.*\)</antifeatures>.*,\1 \2,p' | sort | uniq // NOCHECKSTYLE LineLength
        Map<String, List<String>> expectedAntiFeatures = new HashMap<>();
        expectedAntiFeatures.put("org.fdroid.fdroid", new ArrayList<String>());
        expectedAntiFeatures.put("org.adblockplus.android", Arrays.asList("Tracking", "Ads"));
        expectedAntiFeatures.put("org.microg.nlp.backend.apple", Arrays.asList("Tracking", "NonFreeNet"));
        expectedAntiFeatures.put("com.ds.avare", Collections.singletonList("NonFreeDep"));
        expectedAntiFeatures.put("com.miracleas.bitcoin_spinner", Collections.singletonList("NonFreeAdd"));
        expectedAntiFeatures.put("de.Cherubin7th.blackscreenpresentationremote", Collections.singletonList("Ads"));
        expectedAntiFeatures.put("budo.budoist", Collections.singletonList("NonFreeNet"));
        expectedAntiFeatures.put("no.rkkc.bysykkel", Collections.singletonList("NonFreeDep"));
        expectedAntiFeatures.put("com.jadn.cc", Collections.singletonList("Tracking"));
        expectedAntiFeatures.put("org.atai.TessUI", Collections.singletonList("NonFreeNet"));
        expectedAntiFeatures.put("org.zephyrsoft.checknetwork", Collections.singletonList("Tracking"));
        expectedAntiFeatures.put("de.bashtian.dashclocksunrise", Collections.singletonList("NonFreeDep"));
        expectedAntiFeatures.put("org.geometerplus.zlibrary.ui.android", Collections.singletonList("NonFreeAdd"));
        expectedAntiFeatures.put("org.mozilla.firefox", Arrays.asList("NonFreeAdd", "Tracking"));
        expectedAntiFeatures.put("com.gmail.charleszq", Collections.singletonList("NonFreeDep"));
        expectedAntiFeatures.put("it.andreascarpino.forvodroid", Arrays.asList("NonFreeNet", "NonFreeDep"));
        expectedAntiFeatures.put("de.b0nk.fp1_epo_autoupdate", Collections.singletonList("NonFreeNet"));
        expectedAntiFeatures.put("com.blogspot.tonyatkins.freespeech", Collections.singletonList("Tracking"));
        expectedAntiFeatures.put("com.frostwire.android", Collections.singletonList("Tracking"));
        expectedAntiFeatures.put("com.namsor.api.samples.gendre", Collections.singletonList("NonFreeNet"));
        expectedAntiFeatures.put("com.github.mobile", Collections.singletonList("NonFreeNet"));
        expectedAntiFeatures.put("com.cradle.iitc_mobile", Collections.singletonList("NonFreeNet"));
        expectedAntiFeatures.put("com.matteopacini.katana", Collections.singletonList("Tracking"));
        expectedAntiFeatures.put("de.enaikoon.android.keypadmapper3", Collections.singletonList("Tracking"));
        expectedAntiFeatures.put("org.linphone", Collections.singletonList("NonFreeDep"));
        expectedAntiFeatures.put("ch.rrelmy.android.locationcachemap", Collections.singletonList("NonFreeDep"));
        expectedAntiFeatures.put("com.powerpoint45.lucidbrowser", Arrays.asList("Ads", "NonFreeDep"));
        expectedAntiFeatures.put("org.mixare", Collections.singletonList("NonFreeDep"));
        expectedAntiFeatures.put("apps.droidnotify", Collections.singletonList("NonFreeAdd"));
        expectedAntiFeatures.put("com.numix.calculator", Collections.singletonList("NonFreeAdd"));
        expectedAntiFeatures.put("com.numix.icons_circle", Collections.singletonList("NonFreeAdd"));
        expectedAntiFeatures.put("com.gh4a", Collections.singletonList("NonFreeNet"));
        expectedAntiFeatures.put("at.tomtasche.reader", Collections.singletonList("Tracking"));
        expectedAntiFeatures.put("de.uni_potsdam.hpi.openmensa", Collections.singletonList("NonFreeNet"));
        expectedAntiFeatures.put("net.osmand.plus", Collections.singletonList("Tracking"));
        expectedAntiFeatures.put("byrne.utilities.pasteedroid", Collections.singletonList("NonFreeNet"));
        expectedAntiFeatures.put("com.bwx.bequick", Collections.singletonList("NonFreeAdd"));
        expectedAntiFeatures.put("be.geecko.QuickLyric", Collections.singletonList("Tracking"));
        expectedAntiFeatures.put("com.wanghaus.remembeer", Collections.singletonList("NonFreeNet"));
        expectedAntiFeatures.put("cri.sanity", Collections.singletonList("Ads"));
        expectedAntiFeatures.put("com.showmehills", Collections.singletonList("Tracking"));
        expectedAntiFeatures.put("com.akop.bach", Collections.singletonList("NonFreeNet"));
        expectedAntiFeatures.put("org.dmfs.tasks", Collections.singletonList("NonFreeAdd"));
        expectedAntiFeatures.put("org.telegram.messenger", Collections.singletonList("NonFreeNet"));
        expectedAntiFeatures.put("com.danvelazco.fbwrapper", Collections.singletonList("Tracking"));
        expectedAntiFeatures.put("org.zephyrsoft.trackworktime", Collections.singletonList("Tracking"));
        expectedAntiFeatures.put("org.transdroid", Collections.singletonList("Tracking"));
        expectedAntiFeatures.put("com.lonepulse.travisjr", Collections.singletonList("NonFreeNet"));
        expectedAntiFeatures.put("com.twsitedapps.homemanager", Collections.singletonList("NonFreeAdd"));
        expectedAntiFeatures.put("org.zeitgeist.movement", Collections.singletonList("NonFreeDep"));
        expectedAntiFeatures.put("net.wigle.wigleandroid", Collections.singletonList("NonFreeNet"));
        expectedAntiFeatures.put("org.nick.wwwjdic", Collections.singletonList("Tracking"));

        checkAntiFeatures(actualDetails.apps, expectedAntiFeatures);

        /*
         * generated using: sed 's,<application,\n<application,g' largeRepo.xml
         * | sed -n 's,.*id="\(.[^"]*\)".*,"\1"\,,p'
         */
        checkIncludedApps(actualDetails.apps, new String[]{
                "org.zeroxlab.zeroxbenchmark", "com.uberspot.a2048", "com.traffar.a24game",
                "info.staticfree.android.twentyfourhour", "nerd.tuxmobil.fahrplan.congress",
                "com.jecelyin.editor", "com.markuspage.android.atimetracker", "a2dp.Vol",
                "com.zoffcc.applications.aagtl", "aarddict.android", "com.kai1973i",
                "net.georgewhiteside.android.abstractart", "com.morphoss.acal",
                "org.billthefarmer.accordion", "com.achep.acdisplay", "anupam.acrylic",
                "net.androidcomics.acv", "org.adaway", "com.matoski.adbm",
                "org.adblockplus.android", "siir.es.adbWireless", "org.dgtale.icsimport",
                "com.addi", "org.hystudio.android.dosbox", "hu.vsza.adsdroid", "org.adw.launcher",
                "dev.ukanth.ufirewall", "com.madgag.agit", "jp.sblo.pandora.aGrep",
                "net.gorry.aicia", "com.brosmike.airpushdetector", "org.ligi.ajsha",
                "org.akvo.rsr.up", "com.angrydoughnuts.android.alarmclock", "org.jtb.alogcat",
                "rs.pedjaapps.alogcatroot.app", "org.ametro", "com.orphan.amplayer",
                "eu.domob.anacam", "com.as.anagramsolver", "com.nephoapp.anarxiv",
                "net.bible.android.activity", "li.klass.fhem", "org.xapek.andiodine", "net.avs234",
                "com.github.andlyticsproject", "org.quovadit.apps.andof",
                "com.gpl.rpg.AndorsTrail", "net.progval.android.andquote", "net.rocrail.androc",
                "de.hechler.andfish", "com.android.inputmethod.latin", "aws.apps.androidDrawables",
                "org.yuttadhammo.tipitaka", "uk.co.bitethebullet.android.token",
                "jp.ksksue.app.terminal", "com.templaro.opsiz.aka", "fr.asterope",
                "android.androidVNC", "com.tritop.androsense2", "net.tedstein.AndroSS",
                "com.androzic", "org.andstatus.app", "net.sourceforge.andsys",
                "com.miqote.angelplayerwp", "eu.domob.angulo", "com.ichi2.anki",
                "net.haltcondition.anode", "An.stop", "de.danoeh.antennapod",
                "com.fivasim.antikythera", "de.antonwolf.agendawidget", "com.example.anycut",
                "org.liberty.android.fantastischmemo", "com.menny.android.anysoftkeyboard",
                "com.anysoftkeyboard.languagepack.catalan", "com.anysoftkeyboard.theme.classic_pc",
                "com.anysoftkeyboard.languagepack.danish",
                "com.anysoftkeyboard.languagepack.esperanto",
                "com.anysoftkeyboard.languagepack.french_xlarge",
                "com.anysoftkeyboard.languagepack.georgian.fdroid",
                "com.anysoftkeyboard.languagepack.greek",
                "com.anysoftkeyboard.languagepack.hebrew_large",
                "org.herrlado.ask.languagepack.lithuanian",
                "com.anysoftkeyboard.languagepack.hungarian",
                "com.anysoftkeyboard.languagepack.malayalam",
                "com.anysoftkeyboard.languagepack.pali",
                "com.anysoftkeyboard.languagepack.persian",
                "com.anysoftkeyboard.languagepack.spain", "com.anysoftkeyboard.languagepack.SSH",
                "com.anysoftkeyboard.languagepack.ukrainian", "com.scar45.aokp.co.webviewer",
                "org.thialfihar.android.apg", "ch.blinkenlights.android.apnswitch",
                "com.andrew.apollo", "com.nolanlawson.apptracker",
                "com.episode6.android.appalarm.pro", "org.moparisthebest.appbak",
                "org.microg.nlp.backend.apple", "com.gueei.applocker",
                "com.google.code.appsorganizer", "com.google.code.apps2org",
                "cx.hell.android.pdfview", "org.androidfromfrankfurt.archnews", "org.ardour",
                "com.primavera.arduino.listener", "arity.calculator",
                "com.commonsware.android.arXiv", "com.dozingcatsoftware.asciicam",
                "com.alfray.asqare", "dk.andsen.asqlitemanager", "net.somethingdreadful.MAL",
                "org.tamanegi.atmosphere", "indrora.atomic",
                "com.google.android.apps.authenticator2", "com.teamdc.stephendiniz.autoaway",
                "com.everysoft.autoanswer", "com.elsdoerfer.android.autostarts", "com.ds.avare",
                "apps.babycaretimer", "com.tkjelectronics.balanduino", "com.liato.bankdroid",
                "uk.ac.cam.cl.dtg.android.barcodebox", "com.google.zxing.client.android",
                "net.szym.barnacle", "com.dougkeen.bart", "ch.blinkenlights.battery",
                "net.sf.andbatdog.batterydog", "ch.rrelmy.android.batterymanager",
                "org.droidparts.battery_widget", "org.androidappdev.batterywidget",
                "com.darshancomputing.BatteryIndicatorPro", "com.tobykurien.batteryfu",
                "com.mohammadag.beamfile", "com.corner23.android.beautyclocklivewallpaper",
                "com.knirirr.beecount", "com.beem.project.beem", "com.glanznig.beepme",
                "headrevision.BehatReporter", "com.asksven.betterwifionoff",
                "com.asksven.betterbatterystats", "net.imatruck.betterweather",
                "org.segin.bfinterpreter", "com.ihunda.android.binauralbeat",
                "org.birthdayadapter", "com.rigid.birthdroid", "com.saibotd.bitbeaker",
                "de.schildbach.wallet", "com.veken0m.bitcoinium", "com.miracleas.bitcoin_spinner",
                "caldwell.ben.bites", "eu.domob.bjtrainer",
                "de.Cherubin7th.blackscreenpresentationremote", "com.miqote.brswp",
                "com.blippex.app", "de.grobox.blitzmail", "org.blockinger.game",
                "org.scoutant.blokish", "org.broeuschmeul.android.gps.bluetooth.provider",
                "com.hermit.btreprap", "ru.sash0k.bluetooth_terminal", "net.bluetoothviewer",
                "com.hexad.bluezime", "com.hexad.bluezime.hidenabler", "cxa.lineswallpaper",
                "com.zola.bmi", "com.boardgamegeek", "byrne.utilities.converter",
                "org.yuttadhammo.BodhiTimer", "mobi.boilr.boilr", "org.beide.bomber",
                "org.bombusmod", "com.eleybourn.bookcatalogue", "com.totsp.bookworm",
                "com.botbrew.basil", "com.github.grimpy.botifier",
                "priv.twoerner.brightnesswidget", "nl.frankkie.bronylivewallpaper",
                "com.intrications.android.sharebrowser", "fr.strasweb.browserquest",
                "net.androgames.level", "com.notriddle.budget", "budo.budoist",
                "org.nathan.jf.build.prop.editor", "com.sandeel.bushidoblocks", "no.rkkc.bysykkel",
                "info.guardianproject.cacert", "com.frozendevs.cache.cleaner",
                "at.bitfire.cadroid", "org.iilab.pb", "home.jmstudios.calc",
                "com.android2.calculator3", "org.gege.caldavsyncadapter",
                "de.k3b.android.calendar.ics.adapter", "com.plusonelabs.calendar",
                "de.ub0r.android.callmeter", "com.call.recorder",
                "com.wordpress.sarfraznawaz.callerdetails", "com.integralblue.callerid",
                "com.android.camera2", "campyre.android", "com.dozingcatsoftware.cameratimer",
                "com.jadn.cc", "me.kuehle.carreport", "org.systemcall.scores",
                "com.ridgelineapps.resdicegame", "com.nolanlawson.logcat", "com.github.cetoolbox",
                "fr.strasweb.asso", "com.linkomnia.android.Changjie", "org.atai.TessUI",
                "com.kkinder.charmap", "com.googlecode.chartdroid",
                "info.guardianproject.otr.app.im", "org.zephyrsoft.checknetwork",
                "jwtc.android.chess", "cz.hejl.chesswalk", "org.hekmatof.chesswatch",
                "org.scoutant.cc", "com.nilhcem.frcndict", "com.nolanlawson.chordreader",
                "net.pmarks.chromadoze", "me.bpear.chromeapkpackager",
                "us.lindanrandy.cidrcalculator", "name.starnberger.guenther.android.cbw",
                "com.sapos_aplastados.game.clash_of_balls", "de.qspool.clementineremote",
                "de.ub0r.android.clipboardbeam", "com.ssaurel.clocklw", "org.floens.chan",
                "dk.mide.fas.cmnightlies", "de.fmaul.android.cmis", "com.banasiak.coinflip",
                "com.banasiak.coinflipext.example", "com.coinbase.android",
                "com.brianco.colorclock", "com.color.colornamer", "com.nauj27.android.colorpicker",
                "org.androidsoft.coloring", "org.gringene.colourclock", "net.kervala.comicsreader",
                "com.sgr_b2.compass", "net.micode.compass", "org.dyndns.fules.ck",
                "org.gringene.concentricclock", "org.connectbot", "de.measite.contactmerger",
                "com.appengine.paranoid_android.lost", "com.boombuler.widgets.contacts",
                "com.github.nutomic.controldlna", "eu.siacs.conversations", "org.coolreader",
                "com.dconstructing.cooper", "se.johanhil.clipboard", "ru.o2genum.coregame",
                "net.vivekiyer.GAL", "com.example.CosyDVR", "de.mreiter.countit",
                "com.cr5315.cfdc", "ch.fixme.cowsay", "com.qubling.sidekick",
                "com.bvalosek.cpuspy", "com.github.alijc.cricketsalarm", "groomiac.crocodilenote",
                "org.eehouse.android.xw4", "org.nick.cryptfs.passwdmanager", "com.csipsimple",
                "com.hykwok.CurrencyConverter", "com.elsewhat.android.currentwallpaper",
                "com.manor.currentwidget", "net.cyclestreets", "org.mult.daap",
                "com.bottleworks.dailymoney", "org.jessies.dalvikexplorer", "com.darknessmap",
                "net.nurik.roman.dashclock",
                "it.gmariotti.android.apps.dashclock.extensions.battery", "com.mridang.cellinfo",
                "net.logomancy.dashquotes.civ5", "me.malladi.dashcricket", "com.dwak.lastcall",
                "de.bashtian.dashclocksunrise", "com.mridang.wifiinfo", "dasher.android",
                "com.umang.dashnotifier", "at.bitfire.davdroid", "net.czlee.debatekeeper",
                "org.dyndns.sven_ola.debian_kit", "net.debian.debiandroid", "com.wentam.defcol",
                "com.serone.desktoplabel", "com.f2prateek.dfg", "org.dnaq.dialer2",
                "jpf.android.diary", "com.voidcode.diasporawebclient",
                "com.edwardoyarzun.diccionario", "de.kugihan.dictionaryformids.hmi_android",
                "si.modrajagoda.didi", "net.logomancy.diedroid",
                "kaljurand_at_gmail_dot_com.diktofon", "in.shick.diode",
                "com.google.android.diskusage", "to.networld.android.divedroid",
                "org.diygenomics.pg", "org.sufficientlysecure.viewer",
                "org.sufficientlysecure.viewer.fontpack", "com.dozingcatsoftware.dodge",
                "org.katsarov.dofcalc", "org.dolphinemu.dolphinemu", "us.bravender.android.dongsa",
                "net.iowaline.dotdash", "de.stefan_oltmann.kaesekaestchen", "steele.gerry.dotty",
                "fr.xtof54.dragonGoApp", "com.drismo", "com.xatik.app.droiddraw.client",
                "jackpal.droidexaminer", "edu.rit.poe.atomix", "org.petero.droidfish",
                "org.beide.droidgain", "org.jtb.droidlife", "com.mkf.droidsat", "org.droidseries",
                "org.droidupnp", "com.googlecode.droidwall", "de.delusions.measure",
                "com.shurik.droidzebra", "de.onyxbits.drudgery", "ch.dissem.android.drupal",
                "github.daneren2005.dsub", "se.johanhil.duckduckgo",
                "com.duckduckgo.mobile.android", "it.ecosw.dudo", "org.dynalogin.android",
                "org.uaraven.e", "com.seb.SLWP", "com.seb.SLWPmaps", "net.pejici.easydice",
                "de.audioattack.openlink", "app.easytoken", "com.f0x.eddymalou",
                "org.congresointeractivo.elegilegi", "com.ultramegatech.ey",
                "com.blntsoft.emailpopup", "com.kibab.android.EncPassChanger",
                "org.epstudios.epmobile", "org.jamienicol.episodes",
                "com.mirasmithy.epochlauncher", "it.angrydroids.epub3reader", "it.iiizio.epubator",
                "com.googlecode.awsms", "com.mehmetakiftutuncu.eshotroid",
                "com.googlecode.eyesfree.espeak", "com.sweetiepiggy.everylocale",
                "de.pinyto.exalteddicer", "org.kost.externalip", "ch.hsr.eyecam",
                "com.google.marvin.shell", "org.fdroid.fdroid",
                "com.easwareapps.f2lflap2lock_adfree", "faenza.adw.theme", "org.balau.fakedawn",
                "de.stefan_oltmann.falling_blocks", "com.codebutler.farebot", "org.ligi.fast",
                "com.mod.android.widget.fbcw", "org.fastergps",
                "org.geometerplus.zlibrary.ui.android",
                "org.geometerplus.fbreader.plugin.local_opds_scanner",
                "org.geometerplus.fbreader.plugin.tts", "com.hyperionics.fbreader.plugin.tts_plus",
                "net.micode.fileexplorer", "com.cyanogenmod.filemanager.ics",
                "com.michaldabski.filemanager", "com.github.wdkapps.fillup",
                "se.erikofsweden.findmyphone", "org.mozilla.firefox", "com.ten15.diyfish",
                "org.mysociety.FixMyStreet", "uk.co.danieljarvis.android.flashback",
                "com.nightshadelabs.anotherbrowser", "com.studio332.flickit",
                "com.gmail.charleszq", "fi.harism.wallpaper.flier", "org.aja.flightmode",
                "dk.nindroid.rss", "genius.mohammad.floating.stickies", "net.fred.feedex",
                "fr.xplod.focal", "com.oakley.fon", "com.sputnik.wispr", "ro.ieval.fonbot",
                "net.phunehehe.foocam", "com.iazasoft.footguy", "org.jsharkey.sky",
                "it.andreascarpino.forvodroid", "be.digitalia.fosdem",
                "de.b0nk.fp1_epo_autoupdate", "pt.isec.tp.am",
                "com.blogspot.tonyatkins.freespeech", "org.fedorahosted.freeotp",
                "de.cwde.freeshisen", "nf.frex.android", "de.wikilab.android.friendica01",
                "de.serverfrog.pw.android", "org.froscon.schedule", "com.frostwire.android",
                "org.jfedor.frozenbubble", "be.ppareit.swiftp_free", "com.easwareapps.g2l",
                "com.nesswit.galbijjimsearcher", "com.traffar.game_of_life", "com.androidemu.gba",
                "com.tobykurien.google_news", "com.androidemu.gbc", "com.gcstar.scanner",
                "com.gcstar.viewer", "com.jeffboody.GearsES2eclair",
                "com.namsor.api.samples.gendre", "de.onyxbits.geobookmark", "pl.nkg.geokrety",
                "se.danielj.geometridestroyer", "eu.hydrologis.geopaparazzi",
                "org.herrlado.geofonts", "com.github.ruleant.getback_gps", "at.bitfire.gfxtablet",
                "com.ghostsq.commander", "com.ghostsq.commander.samba",
                "com.ghostsq.commander.sftp", "net.gaast.giggity", "info.guardianproject.gilga",
                "com.github.mobile", "com.timvdalen.gizmooi", "com.glTron",
                "zame.GloomyDungeons.opensource.game", "com.kaeruct.glxy", "de.duenndns.gmdice",
                "org.gmote.client.android", "org.gnucash.android", "info.guardianproject.gpg",
                "org.ligi.gobandroid_hd", "com.googlecode.gogodroid",
                "org.wroot.android.goldeneye", "com.traffar.gomoku", "net.sf.crypt.gort",
                "com.mendhak.gpslogger", "com.gpstether", "com.Bisha.TI89EmuDonation",
                "org.cyanogenmod.great.freedom", "it.greenaddress.cordova", "org.gfd.gsmlocation",
                "de.srlabs.gsmmap", "com.googlecode.gtalksms", "ru.zxalexis.ugaday",
                "com.gulshansingh.hackerlivewallpaper", "org.pocketworkstation.pckeyboard",
                "net.sf.times", "org.durka.hallmonitor", "com.smerty.ham", "net.tapi.handynotes",
                "ca.mimic.apphangar", "com.hobbyone.HashDroid", "com.ginkel.hashit",
                "byrne.utilities.hashpass", "com.zaren", "com.jakebasile.android.hearingsaver",
                "ca.ddaly.android.heart", "com.vanderbie.heart_rate_monitor",
                "com.jwetherell.heart_rate_monitor", "fi.testbed2", "com.borneq.heregpslocation",
                "net.damsy.soupeaucaillou.heriswap", "com.sam.hex",
                "org.gitorious.jamesjrh.isokeys", "com.manuelmaly.hn", "com.gluegadget.hndroid",
                "com.omegavesko.holocounter", "com.tortuca.holoken",
                "com.dynamicg.homebuttonlauncher", "com.naholyr.android.horairessncf",
                "it.andreascarpino.hostisdown", "com.nilhcem.hostseditor",
                "com.smorgasbork.hotdeath", "net.sf.andhsli.hotspotlogin",
                "com.bobbyrne01.howfardoyouswim", "hsware.HSTempo", "org.jtb.httpmon",
                "eu.woju.android.packages.hud", "de.nico.ha_manager", "com.roguetemple.hydroid",
                "com.frankcalise.h2droid", "de.boesling.hydromemo", "com.roguetemple.hyperroid",
                "com.ancantus.HYPNOTOAD", "com.kostmo.wallpaper.spiral", "net.i2p.android.router",
                "com.germainz.identiconizer", "com.dozuki.ifixit", "com.cradle.iitc_mobile",
                "eu.e43.impeller", "am.ed.importcontacts", "org.libreoffice.impressremote",
                "com.shahul3d.indiasatelliteweather", "org.smc.inputmethod.indic",
                "net.luniks.android.inetify", "com.bri1.soundbored", "com.silentlexx.instead",
                "uk.co.ashtonbrsc.android.intentintercept", "org.smblott.intentradio",
                "org.safermobile.intheclear", "to.doc.android.ipv6config",
                "org.woltage.irssiconnectbot", "org.valos.isolmoa", "com.github.egonw.isotopes",
                "de.tui.itlogger", "com.teleca.jamendo", "com.nolanlawson.jnameconverter",
                "julianwi.javainstaller", "com.achep.widget.jellyclock", "com.jlyr",
                "com.jonglen7.jugglinglab", "jupiter.broadcasting.live.tv",
                "uk.co.jarofgreen.JustADamnCompass", "jp.co.kayo.android.localplayer",
                "jp.co.kayo.android.localplayer.ds.ampache",
                "jp.co.kayo.android.localplayer.ds.podcast", "com.brocktice.JustSit",
                "com.fsck.k9", "de.cketti.dashclock.k9", "vnd.blueararat.kaleidoscope6",
                "com.leafdigital.kanji.android", "com.matteopacini.katana",
                "org.kde.kdeconnect_tp", "net.lardcave.keepassnfc", "com.android.keepass",
                "com.seawolfsanctuary.keepingtracks", "com.nolanlawson.keepscore",
                "de.enaikoon.android.keypadmapper3", "com.concentricsky.android.khan",
                "com.leinardi.kitchentimer", "org.nerdcircus.android.klaxon", "at.dasz.KolabDroid",
                "org.kontalk", "org.kwaak3", "pro.oneredpixel.l9droid", "eu.prismsw.lampshade",
                "com.adstrosoftware.launchappops", "com.android.launcher3",
                "com.example.android.maxpapers", "de.danielweisser.android.ldapsync",
                "net.fercanet.LNM", "org.pulpdust.lesserpad", "net.healeys.lexic",
                "com.mykola.lexinproject", "com.martinborjesson.o2xtouchlednotifications",
                "de.grobox.liberario", "fm.libre.droid", "acr.browser.barebones",
                "acr.browser.lightning", "info.guardianproject.lildebi",
                "com.willhauck.linconnectclient", "org.peterbaldwin.client.android.tinyurl",
                "org.linphone", "it.mn.salvi.linuxDayOSM", "com.xenris.liquidwarsos",
                "de.onyxbits.listmyapps", "name.juodumas.ext_kbd_lithuanian", "com.lligainterm",
                "ch.rrelmy.android.locationcachemap", "in.shick.lockpatterngenerator",
                "it.reyboz.screenlock", "sk.madzik.android.logcatudp",
                "in.shubhamchaudhary.logmein", "com.powerpoint45.lucidbrowser",
                "org.lumicall.android", "jpf.android.magiadni", "com.anoshenko.android.mahjongg",
                "info.kesavan.malartoon", "uk.ac.ed.inf.mandelbrotmaps", "com.zapta.apps.maniana",
                "be.quentinloos.manille", "com.chmod0.manpages", "net.pierrox.mcompass",
                "org.dsandler.apps.markers", "org.evilsoft.pathfinder.reference",
                "de.ph1b.audiobook", "net.cactii.mathdoku", "org.jessies.mathdroid",
                "org.aminb.mathtools.app", "jp.yhonda", "org.projectmaxs.main",
                "org.projectmaxs.module.alarmset", "org.projectmaxs.module.bluetooth",
                "org.projectmaxs.module.bluetoothadmin", "org.projectmaxs.module.clipboard",
                "org.projectmaxs.module.contactsread", "org.projectmaxs.module.fileread",
                "org.projectmaxs.module.filewrite", "org.projectmaxs.module.locationfine",
                "org.projectmaxs.module.misc", "org.projectmaxs.module.nfc",
                "org.projectmaxs.module.notification", "org.projectmaxs.module.phonestateread",
                "org.projectmaxs.module.ringermode", "org.projectmaxs.module.shell",
                "org.projectmaxs.module.smsnotify", "org.projectmaxs.module.smsread",
                "org.projectmaxs.module.smssend", "org.projectmaxs.module.smswrite",
                "org.projectmaxs.module.wifiaccess", "org.projectmaxs.module.wifichange",
                "org.projectmaxs.transport.xmpp", "com.harleensahni.android.mbr",
                "eu.johncasson.meerkatchallenge", "org.billthefarmer.melodeon",
                "org.zakky.memopad", "org.androidsoft.games.memory.kids", "net.asceai.meritous",
                "com.intervigil.micdroid", "com.midisheetmusic", "de.syss.MifareClassicTool",
                "com.rhiannonweb.android.migrainetracker", "com.evancharlton.mileage",
                "com.xlythe.minecraftclock", "it.reyboz.minesweeper", "com.example.sshtry",
                "jp.gr.java_conf.hatalab.mnv", "nitezh.ministock", "org.kde.necessitas.ministro",
                "com.jaygoel.virginminuteschecker", "de.azapps.mirakelandroid",
                "de.azapps.mirakel.dashclock", "org.bitbucket.tickytacky.mirrormirror",
                "de.homac.Mirrored", "org.mixare", "edu.harvard.android.mmskeeper",
                "org.tbrk.mnemododo", "com.matburt.mobileorg", "com.gs.mobileprint",
                "org.n52.sosmobileclient", "com.dngames.mobilewebcam", "com.mobiperf",
                "dev.drsoran.moloko", "ivl.android.moneybalance",
                "com.tobiaskuban.android.monthcalendarwidgetfoss", "org.montrealtransit.android",
                "org.montrealtransit.android.schedule.stmbus", "akk.astro.droid.moonphase",
                "org.epstudios.morbidmeter", "org.mosspaper", "org.cry.otp", "com.morlunk.mountie",
                "com.spazedog.mounts2sd", "com.nutomic.zertman",
                "org.logicallycreative.movingpolygons", "org.mozilla.mozstumbler",
                "com.uraroji.garage.android.mp3recvoice", "com.namelessdev.mpdroid",
                "org.bc_bd.mrwhite", "com.fgrim.msnake", "com.gelakinetic.mtgfam",
                "com.hectorone.multismssender", "org.tamanegi.wallpaper.multipicture.dnt",
                "kr.softgear.multiping", "com.artifex.mupdfdemo",
                "paulscode.android.mupen64plusae", "com.android.music",
                "com.danielme.muspyforandroid", "org.mustard.android", "org.mumod.android",
                "net.nurik.roman.muzei", "net.ebt.muzei.miyazaki",
                "com.projectsexception.myapplist.open", "net.dahanne.banq.notifications",
                "org.totschnig.myexpenses", "com.futonredemption.mylocation", "ch.fixme.status",
                "i4nc4mp.myLock", "org.aykit.MyOwnNotes", "org.mythdroid",
                "tkj.android.homecontrol.mythmote", "org.coolfrood.mytronome",
                "name.livitski.games.puzzle.android", "de.laxu.apps.nachtlagerdownloader",
                "com.nanoconverter.zlab", "fr.miximum.napply", "org.vono.narau",
                "com.example.muzei.muzeiapod", "de.msal.muzei.nationalgeographic",
                "org.navitproject.navit", "org.ndeftools.boilerplate", "jp.sfjp.webglmol.NDKmol",
                "com.opendoorstudios.ds4droid", "com.kvance.Nectroid", "jp.sawada.np2android",
                "com.androidemu.nes", "net.jaqpot.netcounter", "free.yhc.netmbuddy",
                "org.ncrmnt.nettts", "de.mangelow.network", "info.lamatricexiste.network",
                "com.googlecode.networklog", "org.gc.networktester", "com.newsblur",
                "jp.softstudio.DriversLicenseReader", "pl.net.szafraniec.NFCKey",
                "se.anyro.nfc_reader", "pl.net.szafraniec.NFCTagmaker", "com.sinpo.xnfc",
                "com.digitallizard.nicecompass", "net.gorry.android.input.nicownng",
                "ru.glesik.nostrangersms", "it.sineo.android.noFrillsCPUClassic",
                "com.netthreads.android.noiz2", "pe.moe.nori", "info.guardianproject.notepadbot",
                "bander.notepad", "ru.ttyh.neko259.notey", "org.jmoyer.NotificationPlus",
                "apps.droidnotify", "net.thauvin.erik.android.noussd", "org.npr.android.news",
                "mobi.cyann.nstools", "org.ntpsync", "com.notriddle.null_launcer",
                "com.numix.calculator", "com.numix.icons_circle", "org.jfedor.nxtremotecontrol",
                "com.powerje.nyan", "com.palliser.nztides", "dk.jens.backup",
                "com.valleytg.oasvn.android", "eu.lighthouselabs.obd.reader",
                "nz.gen.geek_central.ObjViewer", "trikita.obsqr", "edu.sfsu.cs.orange.ocr",
                "com.gh4a", "org.odk.collect.android", "org.sufficientlysecure.localcalendar",
                "com.sli.ohmcalc", "org.openintents.about", "org.openintents.filemanager",
                "org.openintents.safe", "edu.nyu.cs.omnidroid.app", "org.hanenoshino.onscripter",
                "com.euedge.openaviationmap.android", "pro.dbro.bart",
                "net.sourceforge.opencamera", "org.brandroid.openmanager",
                "io.github.sanbeg.flashlight", "com.nexes.manager", "de.skubware.opentraining",
                "com.dje.openwifinetworkremover", "be.brunoparmentier.openbikesharing.app",
                "app.openconnect", "at.tomtasche.reader", "org.opengpx",
                "org.sufficientlysecure.keychain", "de.jdsoft.law", "org.openlp.android",
                "de.uni_potsdam.hpi.openmensa", "jp.redmine.redmineclient",
                "cz.romario.opensudoku", "edu.killerud.kitchentimer", "de.blinkt.openvpn",
                "de.schaeuffelhut.android.openvpn", "org.ale.openwatch", "com.vwp.owmap",
                "com.vwp.owmini", "jp.co.omronsoft.openwnn", "com.googlecode.openwnn.legacy",
                "orbitlivewallpaperfree.puzzleduck.com", "org.torproject.android",
                "org.ethack.orwall", "info.guardianproject.browser", "com.eolwral.osmonitor",
                "net.oschina.app", "org.billthefarmer.scope",
                "ch.nexuscomputing.android.osciprimeics", "net.osmand.srtmPlugin.paid",
                "net.osmand.parkingPlugin", "net.osmand.plus", "net.anzix.osm.upload",
                "me.guillaumin.android.osmtracker", "de.ub0r.android.otpdroid",
                "com.traffar.oware", "com.owncloud.android", "de.luhmer.owncloudnewsreader",
                "nu.firetech.android.pactrack", "it.rgp.nyagua.pafcalc",
                "org.moparisthebest.pageplus", "net.nightwhistler.pageturner",
                "com.cybrosys.palmcalc", "com.niparasc.papanikolis", "ru.valle.btc",
                "com.paranoid.ParanoidWallpapers", "org.ligi.passandroid", "com.passcard",
                "gg.mw.passera", "com.jefftharris.passwdsafe", "com.uploadedlobster.PwdHash",
                "com.zeapo.pwdstore", "org.passwordmaker.android", "byrne.utilities.pasteedroid",
                "com.th.XenonWallpapers", "io.github.droidapps.pdfreader",
                "name.bagi.levente.pedometer", "org.jf.Penroser", "fi.harism.wallpaper.flowers",
                "mobi.omegacentauri.PerApp", "com.brewcrewfoo.performance",
                "com.frozendevs.periodictable", "de.arnowelzel.android.periodical",
                "org.androidsoft.app.permission", "com.FireFart.Permissions2",
                "com.byagowi.persiancalendar", "org.dyndns.ipignoli.petronius",
                "org.lf_net.pgpunlocker", "de.onyxbits.photobookmark",
                "unisiegen.photographers.activity", "com.ruesga.android.wallpapers.photophase",
                "org.esteban.piano", "org.musicbrainz.picard.barcodescanner", "com.pindroid",
                "com.boombuler.piraten.map", "com.rj.pixelesque", "info.guardianproject.pixelknot",
                "com.morlunk.mumbleclient", "eu.lavarde.pmtd", "de.onyxbits.pocketbandit",
                "com.zachrattner.pockettalk", "edu.cmu.pocketsphinx.demo", "com.axelby.podax",
                "com.polipoid", "com.politedroid", "com.hlidskialf.android.pomodoro",
                "com.kpz.pomodorotasks.activity", "com.tinkerlog.android.pongtime",
                "org.sixgun.ponyexpress", "com.xargsgrep.portknocker", "net.tevp.postcode",
                "org.ppsspp.ppsspp", "com.proch.practicehub", "android.game.prboom",
                "fr.simon.marquis.preferencesmanager", "damo.three.ie",
                "com.gracecode.android.presentation", "com.falconware.prestissimo", "org.primftpd",
                "org.us.andriod", "org.okfn.pod", "ro.ui.pttdroid", "org.macno.puma",
                "com.boztalay.puppyframeuid", "com.purplefoto.pfdock", "org.example.pushupbuddy",
                "name.boyle.chris.sgtpuzzles", "com.littlebytesofpi.pylauncher",
                "org.pyload.android.client", "com.zagayevskiy.pacman",
                "com.lgallardo.qbittorrentclient", "com.android.quake", "com.jeyries.quake2",
                "com.iskrembilen.quasseldroid", "com.qsp.player", "com.bwx.bequick",
                "com.hughes.android.dictionary", "vu.de.urpool.quickdroid", "be.geecko.QuickLyric",
                "net.vreeken.quickmsg", "com.lightbox.android.camera", "com.write.Quill",
                "es.cesar.quitesleep", "net.xenotropic.quizznworldcap", "com.radioreddit.android",
                "org.openbmap", "com.tmarki.comicmaker", "cc.rainwave.android",
                "be.norio.randomapp", "org.recentwidget", "au.com.wallaceit.reddinator",
                "com.btmura.android.reddit", "org.quantumbadger.redreader",
                "net.dahanne.android.regalandroid", "urbanstew.RehearsalAssistant",
                "com.reicast.emulator", "com.harasoft.relaunch", "com.wanghaus.remembeer",
                "net.noio.Reminder", "com.lostrealm.lembretes",
                "org.peterbaldwin.client.android.vlcremote", "de.onyxbits.remotekeyboard",
                "org.damazio.notifier", "com.vsmartcard.remotesmartcardreader.app",
                "remuco.client.android", "com.replica.replicaisland", "br.usp.ime.retrobreaker",
                "org.retroarch", "buet.rafi.dictionary", "fr.keuse.rightsalert",
                "org.hoi_polloi.android.ringcode", "com.ringdroid", "com.dririan.RingyDingyDingy",
                "fr.hnit.riverferry", "se.norenh.rkfread", "com.robert.maps", "org.rmll",
                "info.staticfree.android.robotfindskitten", "com.abcdjdj.rootverifier",
                "de.zieren.rot13", "org.penghuang.tools.rotationlock",
                "com.spydiko.rotationmanager_foss", "mohammad.adib.roundr", "com.ath0.rpn",
                "ru0xdc.rtkgps", "de.steinpfeffer.rdt", "net.aangle.rvclock", "org.sagemath.droid",
                "com.cepmuvakkit.times", "monakhv.android.samlib",
                "com.maxfierke.sandwichroulette", "cri.sanity", "it.sasabz.android.sasabus",
                "org.ligi.satoshiproof", "com.vonglasow.michael.satstat",
                "org.chorem.android.saymytexts", "org.crocodile.sbautologin",
                "org.ale.scanner.zotero", "org.scid.android", "com.lukekorth.screennotifications",
                "com.jotabout.screeninfo", "com.gmail.altakey.effy",
                "net.jjc1138.android.scrobbler", "org.scummvm.scummvm", "gr.ndre.scuttloid",
                "com.gmail.jerickson314.sdscanner", "com.seafile.seadroid",
                "com.seafile.seadroid2", "com.ideasfrombrain.search_based_launcher_v2",
                "com.scottmain.android.searchlight", "com.shadcat.secdroid",
                "com.doplgangr.secrecy", "fr.simon.marquis.secretcodes", "fr.seeks",
                "com.ariwilson.seismowallpaper", "mobi.omegacentauri.SendReduced",
                "com.ivanvolosyuk.sharetobrowser", "ru.gelin.android.sendtosd",
                "org.totschnig.sendwithftp", "de.onyxbits.sensorreadout", "at.univie.sensorium",
                "com.monead.games.android.sequence", "org.servalproject", "org.servDroid.web",
                "net.sourceforge.servestream", "me.sheimi.sgit", "org.emergent.android.weave",
                "net.sylvek.sharemyposition", "com.MarcosDiez.shareviahttp", "com.android.shellms",
                "com.boombuler.games.shift", "name.soulayrol.rhaa.sholi",
                "com.github.nicolassmith.urlevaluator", "com.totsp.crossword.shortyz",
                "com.showmehills", "be.ppareit.shutdown", "org.sickstache", "kr.hybdms.sidepanel",
                "org.billthefarmer.siggen", "ru.neverdark.silentnight", "com.better.alarm",
                "com.brentpanther.bitcoinwidget", "nl.ttys0.simplec25k", "com.chessclock.android",
                "com.casimirlab.simpleDeadlines", "com.mareksebera.simpledilbert",
                "com.dnielfe.manager", "com.adam.aslfms", "ee.smkv.calc.loan",
                "com.poloure.simplerss", "nl.mpcjanssen.simpletask", "kdk.android.simplydo",
                "eu.siebeck.sipswitch", "org.sipdroid.sipua", "com.sismics.reader",
                "com.google.android.stardroid", "eu.flatworld.android.slider",
                "de.shandschuh.slightbackup", "org.androidsoft.games.slowit",
                "tritop.androidSLWCpuWidget", "tritop.android.SLWTrafficMeterWidget",
                "wb.receiptspro", "com.unleashyouradventure.swaccess", "com.java.SmokeReducer",
                "com.zegoggles.smssync", "bughunter2.smsfilter", "net.everythingandroid.smspopup",
                "de.ub0r.android.smsdroid", "org.addhen.smssync", "com.mobilepearls.sokoban",
                "com.kmagic.solitaire", "org.andglkmod.hunkypunk", "sonoroxadc.garethmurfin.co.uk",
                "com.htruong.inputmethod.latin", "com.roozen.SoundManagerv2",
                "net.micode.soundrecorder", "com.akop.bach", "org.sparkleshare.android",
                "de.shandschuh.sparserss", "mixedbit.speechtrainer", "net.codechunk.speedofsound",
                "fly.speedmeter.grub", "isn.fly.speedmeter", "SpeedoMeterApp.main",
                "net.majorkernelpanic.spydroid", "csci567.squeez", "uk.org.ngo.squeezer",
                "org.sufficientlysecure.standalonecalendar", "fr.bellev.stdatmosphere",
                "com.piwi.stickeroid", "net.stkaddons.viewer", "com.nma.util.sdcardtrac",
                "com.gokhanmoral.stweaks.app", "net.sourceforge.subsonic.androidapp",
                "org.subsurface", "in.ac.dtu.subtlenews", "com.app2go.sudokufree", "de.sudoq",
                "org.sudowars", "net.pejici.summation", "info.staticfree.SuperGenPass",
                "com.koushikdutta.superuser", "com.omegavesko.sutransplus",
                "biz.codefuture.svgviewer", "com.nutomic.syncthingandroid",
                "org.ciasaboark.tacere", "com.kyakujin.android.tagnotepad",
                "com.weicheng.taipeiyoubikeoffline", "com.google.android.marvin.talkback",
                "com.ciarang.tallyphant", "org.tof", "com.acvarium.tasclock", "org.dmfs.tasks",
                "org.tasks", "goo.TeaTimer", "net.chilon.matt.teacup", "fr.xgouchet.texteditor",
                "org.telegram.messenger", "com.jmartin.temaki", "jackpal.androidterm",
                "de.schildbach.wallet_test", "org.paulmach.textedit", "de.onyxbits.textfiction",
                "com.myopicmobile.textwarrior.android", "pl.narfsoftware.thermometer",
                "com.threedlite.livePolys", "org.ironrabbit.bhoboard", "org.ironrabbit",
                "de.smasi.tickmate", "com.gladis.tictactoe", "org.tigase.messenger.phone.pro",
                "com.lecz.android.tiltmazes", "org.dpadgett.timer", "com.alfray.timeriffic",
                "com.tastycactus.timesheet", "org.poirsouille.tinc_gui",
                "com.danvelazco.fbwrapper", "org.tint", "org.tint.adblock",
                "com.xmission.trevin.android.todo", "org.chrisbailey.todo",
                "com.earthblood.tictactoe", "com.dwalkes.android.toggleheadset2", "org.tomdroid",
                "ch.hgdev.toposuite", "com.colinmcdonough.android.torch", "com.piratebayfree",
                "com.amphoras.tpthelper", "org.traccar.client", "org.zephyrsoft.trackworktime",
                "org.softcatala.traductor", "com.aselalee.trainschedule",
                "com.andybotting.tramhunter", "org.transdroid.full", "org.transdroid",
                "org.transdroid.search", "fr.ybo.transportsbordeaux", "fr.ybo.transportsrennes",
                "com.lonepulse.travisjr", "de.koelle.christian.trickytripper",
                "org.hermit.tricorder", "caldwell.ben.trolly", "fr.strasweb.campus",
                "com.unwrappedapps.android.wallpaper.creative", "org.tryton.client",
                "org.segin.ttleditor", "org.ttrssreader", "com.seavenois.tetris",
                "com.dalthed.tucan", "tuioDroid.impl", "de.tum.in.tumcampus",
                "org.billthefarmer.tuner", "org.tunesremote", "com.tunes.viewer",
                "com.maskyn.fileeditorpro", "com.drodin.tuxrider", "org.me.tvhguide",
                "org.mariotaku.twidere", "org.mariotaku.twidere.extension.twitlonger",
                "com.reddyetwo.hashmypass.app", "com.twsitedapps.homemanager",
                "szelok.app.twister", "com.googamaphone.typeandspeak", "org.zeitgeist.movement",
                "ro.weednet.contactssync", "org.madore.android.unicodeMap",
                "info.staticfree.android.units", "net.ralphbroenink.muzei.unsplash",
                "com.u17od.upm", "cc.co.eurdev.urecorder", "com.threedlite.urforms",
                "com.bretternst.URLazy", "me.hda.urlhda", "aws.apps.usbDeviceEnumerator",
                "com.threedlite.userhash.location", "ch.blinkenlights.android.vanilla",
                "org.kreed.vanilla", "com.dozingcatsoftware.bouncy", "de.blau.android",
                "net.momodalo.app.vimtouch", "com.code.android.vibevault",
                "com.pheelicks.visualizer", "org.videolan.vlc", "com.vlille.checker",
                "com.pilot51.voicenotify", "de.jurihock.voicesmith",
                "mancioboxblog.altervista.it.volumecontrol",
                "org.projectvoodoo.simplecarrieriqdetector", "org.projectvoodoo.otarootkeeper",
                "org.projectvoodoo.screentestpatterns", "com.poinsart.votar", "org.vudroid",
                "sk.vx.connectbot", "net.mafro.android.wakeonlan", "fr.gaulupeau.apps.InThePoche",
                "de.markusfisch.android.wavelines", "ru.gelin.android.weather.notification",
                "ru.gelin.android.weather.notification.skin.blacktext",
                "ru.gelin.android.weather.notification.skin.whitetext",
                "de.geeksfactory.opacclient", "net.solutinno.websearch", "de.ub0r.android.websms",
                "de.ub0r.android.websms.connector.gmx",
                "de.ub0r.android.websms.connector.smspilotru", "com.ubergeek42.WeechatAndroid",
                "jp.co.qsdn.android.jinbei3d", "fr.ludo1520.whatexp",
                "org.wheelmap.android.online", "org.edunivers.whereami", "seanfoy.wherering",
                "de.freewarepoint.whohasmystuff", "org.cprados.wificellmanager",
                "ru.glesik.wifireminders", "org.androidappdev.wifiwidget",
                "org.marcus905.wifi.ace", "de.j4velin.wifiAutoOff", "teaonly.droideye",
                "org.wahtod.wififixer", "net.sourceforge.wifiremoteplay", "com.volosyukivan",
                "net.wigle.wigleandroid", "org.wikilovesmonuments",
                "de.mbutscher.wikiandpad.alphabeta", "org.wikimedia.commons",
                "org.wikimedia.commons.muzei", "org.wikipedia", "fr.renzo.wikipoff",
                "org.github.OxygenGuide", "org.wiktionary", "uk.org.cardboardbox.wonderdroid",
                "com.developfreedom.wordpowermadeeasy", "org.wordpress.android",
                "eu.vranckaert.worktime", "com.irahul.worldclock", "com.sigseg.android.worldmap",
                "mazechazer.android.wottankquiz", "org.nick.wwwjdic", "au.com.darkside.XServer",
                "com.xabber.androiddev", "org.xbmc.android.remote", "org.xcsoar",
                "net.bytten.xkcdviewer", "org.helllabs.android.xmp", "biz.gyrus.yaab", "de.yaacc",
                "org.yaaic", "org.yabause.android", "uobikiemukot.yaft", "com.tum.yahtzee",
                "org.yaxim.androidclient", "fi.harism.wallpaper.yinyang",
                "dentex.youtube.downloader", "com.yubico.yubiclip", "com.yubico.yubioath",
                "com.yubico.yubitotp", "de.antonfluegge.android.yubnubwidgetadfree",
                "com.gimranov.zandy.app", "org.zirco", "org.smerty.zooborns", "org.qii.weiciyuan",
                "com.googlecode.tcime",
        });
    }

    private void checkAntiFeatures(List<App> apps, Map<String, List<String>> expectedAntiFeatures) {
        for (App app : apps) {
            if (expectedAntiFeatures.containsKey(app.packageName)) {
                List<String> antiFeatures = expectedAntiFeatures.get(app.packageName);
                if (antiFeatures.size() == 0) {
                    assertNull(app.antiFeatures);
                } else {
                    List<String> actualAntiFeatures = new ArrayList<>();
                    Collections.addAll(actualAntiFeatures, app.antiFeatures);
                    assertTrue(actualAntiFeatures.containsAll(antiFeatures));
                    assertTrue(antiFeatures.containsAll(actualAntiFeatures));
                }
            }
        }
    }

    private void checkIncludedApps(List<App> actualApps, String[] expctedAppIds) {
        assertNotNull(actualApps);
        assertNotNull(expctedAppIds);
        assertEquals(actualApps.size(), expctedAppIds.length);
        for (String id : expctedAppIds) {
            boolean thisAppMissing = true;
            for (App app : actualApps) {
                if (TextUtils.equals(app.packageName, id)) {
                    thisAppMissing = false;
                    break;
                }
            }
            assertFalse(thisAppMissing);
        }
    }

    private void checkPushRequests(RepoDetails actualDetails) {
        final Object[] expectedPushRequestsIndex = new Object[]{
                "install", "org.fdroid.fdroid", 101002,
                "install", "org.fdroid.fdroid.privileged", null,
                "uninstall", "com.android.vending", null,
                "uninstall", "com.facebook.orca", -12345,
                "uninstall", null, null,  // request with no data
                "install", "asdfasdfasdf", null, // non-existent app
        };

        checkIncludedApps(actualDetails.apps, new String[]{
                "org.fdroid.fdroid", "org.fdroid.fdroid.privileged",
        });

        List<RepoPushRequest> repoPushRequestList = actualDetails.repoPushRequestList;
        int i = 0;
        for (RepoPushRequest repoPushRequest : repoPushRequestList) {
            assertEquals(repoPushRequest.request, expectedPushRequestsIndex[i]);
            assertEquals(repoPushRequest.packageName, expectedPushRequestsIndex[i + 1]);
            assertEquals(repoPushRequest.versionCode, expectedPushRequestsIndex[i + 2]);
            i += 3;
        }
    }

    private void handlerTestSuite(Repo expectedRepo, RepoDetails actualDetails,
                                  int appCount, int apkCount, int maxAge, int version) {
        assertNotNull(actualDetails);
        assertFalse(TextUtils.isEmpty(actualDetails.signingCert));
        assertEquals(expectedRepo.signingCertificate.length(), actualDetails.signingCert.length());
        assertEquals(expectedRepo.signingCertificate, actualDetails.signingCert);
        assertFalse(FAKE_SIGNING_CERT.equals(actualDetails.signingCert));

        assertFalse(TextUtils.isEmpty(actualDetails.name));
        assertEquals(expectedRepo.name.length(), actualDetails.name.length());
        assertEquals(expectedRepo.name, actualDetails.name);

        assertFalse(TextUtils.isEmpty(actualDetails.description));
        assertEquals(expectedRepo.description.length(), actualDetails.description.length());
        assertEquals(expectedRepo.description, actualDetails.description);

        assertEquals(actualDetails.maxAge, maxAge);
        assertEquals(actualDetails.version, version);
        assertEquals(expectedRepo.timestamp, actualDetails.timestamp);

        List<App> apps = actualDetails.apps;
        assertNotNull(apps);
        assertEquals(apps.size(), appCount);

        List<Apk> apks = actualDetails.apks;
        assertNotNull(apks);
        assertEquals(apks.size(), apkCount);
    }

    @NonNull
    private RepoDetails getFromFile(String indexFilename) {
        return getFromFile(indexFilename, Repo.PUSH_REQUEST_IGNORE);
    }

    @NonNull
    private RepoDetails getFromFile(String indexFilename, int pushRequests) {
        return getFromFile(getClass().getClassLoader(), indexFilename, pushRequests);
    }

    @NonNull
    static RepoDetails getFromFile(ClassLoader classLoader, String indexFilename, int pushRequests) {
        Log.i(TAG, "test file: " + classLoader.getResource(indexFilename));
        InputStream inputStream = classLoader.getResourceAsStream(indexFilename);
        return RepoDetails.getFromFile(inputStream, pushRequests);
    }

    private void writeResourceToObbDir(String assetName) throws IOException {
        InputStream input = getClass().getClassLoader().getResourceAsStream(assetName);
        String packageName = assetName.substring(assetName.indexOf("obb"),
                assetName.lastIndexOf('.'));
        File f = new File(App.getObbDir(packageName), assetName);
        FileUtils.copyToFile(input, f);
        input.close();
    }
}

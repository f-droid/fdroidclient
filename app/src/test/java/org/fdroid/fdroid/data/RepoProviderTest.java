/*
 * Copyright (C) 2016 Blue Jay Wireless
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

package org.fdroid.fdroid.data;

import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.Nullable;
import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Schema.RepoTable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@Config(constants = BuildConfig.class, application = Application.class)
@RunWith(RobolectricTestRunner.class)
public class RepoProviderTest extends FDroidProviderTest {

    private static final String[] COLS = RepoTable.Cols.ALL;

    @Test
    public void countEnabledRepos() {

        // By default, f-droid is enabled.
        assertEquals(1, RepoProvider.Helper.countEnabledRepos(context));

        Repo gpRepo = RepoProvider.Helper.findByAddress(context, "https://guardianproject.info/fdroid/repo");
        gpRepo = setEnabled(gpRepo, true);
        assertEquals(2, RepoProvider.Helper.countEnabledRepos(context));

        Repo fdroidRepo = RepoProvider.Helper.findByAddress(context, "https://f-droid.org/repo");
        setEnabled(fdroidRepo, false);
        setEnabled(gpRepo, false);

        assertEquals(0, RepoProvider.Helper.countEnabledRepos(context));
    }

    private Repo setEnabled(Repo repo, boolean enabled) {
        ContentValues enable = new ContentValues(1);
        enable.put(RepoTable.Cols.IN_USE, enabled);
        RepoProvider.Helper.update(context, repo, enable);
        return RepoProvider.Helper.findByAddress(context, repo.address);
    }

    @Test
    public void lastUpdated() {
        assertNull(RepoProvider.Helper.lastUpdate(context));

        Repo gpRepo = RepoProvider.Helper.findByAddress(context, "https://guardianproject.info/fdroid/repo");

        // Set date to 2017-04-05 11:56:38
        setLastUpdate(gpRepo, new Date(1491357408643L));

        // GP is not yet enabled, so it is not counted.
        assertNull(RepoProvider.Helper.lastUpdate(context));

        // Set date to 2017-04-04 11:56:38
        Repo fdroidRepo = RepoProvider.Helper.findByAddress(context, "https://f-droid.org/repo");
        setLastUpdate(fdroidRepo, new Date(1491357408643L - (1000 * 60 * 60 * 24)));
        assertEquals("2017-04-04", Utils.formatDate(RepoProvider.Helper.lastUpdate(context), null));

        setEnabled(gpRepo, true);
        assertEquals("2017-04-05", Utils.formatDate(RepoProvider.Helper.lastUpdate(context), null));
    }

    private Repo setLastUpdate(Repo repo, Date date) {
        ContentValues values = new ContentValues(1);
        values.put(RepoTable.Cols.LAST_UPDATED, Utils.formatDate(date, null));
        RepoProvider.Helper.update(context, repo, values);
        return RepoProvider.Helper.findByAddress(context, repo.address);
    }

    @Test
    public void findByUrl() {

        Repo fdroidRepo = RepoProvider.Helper.findByAddress(context, "https://f-droid.org/repo");
        Repo fdroidArchiveRepo = RepoProvider.Helper.findByAddress(context, "https://f-droid.org/archive");

        String[] noRepos = {
                "https://not-a-repo.example.com",
                "https://f-droid.org",
                "https://f-droid.org/",
        };

        for (String url : noRepos) {
            assertNull(RepoProvider.Helper.findByUrl(context, Uri.parse(url), COLS));
        }

        String[] fdroidRepoUrls = {
                "https://f-droid.org/repo/index.jar",
                "https://f-droid.org/repo/index.jar?random-junk-in-query=yes",
                "https://f-droid.org/repo/index.jar?random-junk-in-query=yes&more-junk",
                "https://f-droid.org/repo/icons/org.fdroid.fdroid.100.png",
                "https://f-droid.org/repo/icons-640/org.fdroid.fdroid.100.png",
        };

        assertUrlsBelongToRepo(fdroidRepoUrls, fdroidRepo);

        String[] fdroidArchiveUrls = {
                "https://f-droid.org/archive/index.jar",
                "https://f-droid.org/archive/index.jar?random-junk-in-query=yes",
                "https://f-droid.org/archive/index.jar?random-junk-in-query=yes&more-junk",
                "https://f-droid.org/archive/icons/org.fdroid.fdroid.100.png",
                "https://f-droid.org/archive/icons-640/org.fdroid.fdroid.100.png",
        };

        assertUrlsBelongToRepo(fdroidArchiveUrls, fdroidArchiveRepo);
    }

    private void assertUrlsBelongToRepo(String[] urls, Repo expectedRepo) {
        for (String url : urls) {
            Repo actualRepo = RepoProvider.Helper.findByUrl(context, Uri.parse(url), COLS);
            assertNotNull("No repo matching URL " + url, actualRepo);
            assertEquals("Invalid repo for URL [" + url + "]. Expected [" + expectedRepo.address + "] but got ["
                    + actualRepo.address + "]", expectedRepo.id, actualRepo.id);
        }

    }

    /**
     * The {@link DBHelper} class populates the default repos when it first creates a database.
     * The names/URLs/signing certificates for these repos are all hard coded in the source/res.
     */
    @Test
    public void defaultRepos() {
        List<Repo> defaultRepos = RepoProvider.Helper.all(context);
        assertEquals(defaultRepos.size(), 4); // based on app/src/main/res/default_repo.xml

        String[] reposFromXml = context.getResources().getStringArray(R.array.default_repos);
        if (reposFromXml.length % DBHelper.REPO_XML_ARG_COUNT != 0) {
            throw new IllegalArgumentException(
                    "default_repo.xml array does not have the right number of elements");
        }
        for (int i = 0; i < reposFromXml.length / DBHelper.REPO_XML_ARG_COUNT; i++) {
            int offset = i * DBHelper.REPO_XML_ARG_COUNT;
            assertRepo(
                    defaultRepos.get(i),
                    reposFromXml[offset + 1], // address
                    reposFromXml[offset + 2], // description
                    Utils.calcFingerprint(reposFromXml[offset + 7]), // pubkey
                    reposFromXml[offset]      // name
            );
        }
    }

    @Test
    public void canAddAdditionalRepos() {
        /*
        Repo is structured as follows:
            0) name
            1) address
            2) description
            3) version
            4) enabled
            5) priority (actually ignored in this use case)
            6) pushRequests
            7) pubkey
        */
        String packageName = context.getPackageName();
        FileOutputStream outputStream = new FileOutputStream("/oem/etc/" + packageName + "/additional_repos.xml");
        outputStream.write(("<?xml version=\"1.0\" encoding=\"utf-8\"?>
                    <resources>
                        <string-array name=\"default_repos\">

                            <!-- name -->
                            <item>oem0Name</item>
                            <!-- address -->
                            <item>https://www.oem0.com/yeah/repo</item>
                            <!-- description -->
                            <item>I'm the first oem repo.</item>
                            <!-- version -->
                            <item>22</item>
                            <!-- enabled -->
                            <item>1</item>
                            <!-- push requests -->
                            <item>ignore</item>
                            <!-- pubkey -->
                            <item>fffff2313aaaaabcccc111</item>

                            <!-- name -->
                            <item>oem1MyNameIs</item>
                            <!-- address -->
                            <item>https://www.mynameis.com/rapper/repo</item>
                            <!-- description -->
                            <item>Who is the first repo?</item>
                            <!-- version -->
                            <item>22</item>
                            <!-- enabled -->
                            <item>0</item>
                            <!-- push requests -->
                            <item>ignore</item>
                            <!-- pubkey -->
                            <item>ddddddd2313aaaaabcccc111</item>

                        </string-array>
                    </resources>"
                    ).getBytes());
        outputStream.close();

        // Load the actual repos
        List<String> defaultRepos = DBHelper.loadDefaultRepos(context);

        // Construct the repos that we should have loaded
        List<String> oem0 = Arrays.asList("oem0Name", "https://www.oem0.com/yeah/repo", "I'm the first oem repo.",
                                            "22", "1", "0", "ignore", "fffff2313aaaaabcccc111");
        List<String> oem1 = Arrays.asList("oem1MyNameIs", "https://www.mynameis.com/rapper/repo", "Who is the first repo?",
                                            "22", "0", "0", "ignore", "ddddddd2313aaaaabcccc111");
        List<String> fdroid0 = Arrays.asList("F-Droid", "https://f-droid.org/repo", "The official F-Droid repository. Applications in this repository are mostly built directory from the source code. Some are official binaries built by the original application developers - these will be replaced by source-built versions over time.",
                                            "13", "1", "1", "ignore", "3082035e30820246a00302010202044c49cd00300d06092a864886f70d01010505003071310b300906035504061302554b3110300e06035504081307556e6b6e6f776e3111300f0603550407130857657468657262793110300e060355040a1307556e6b6e6f776e3110300e060355040b1307556e6b6e6f776e311930170603550403131043696172616e2047756c746e69656b73301e170d3130303732333137313032345a170d3337313230383137313032345a3071310b300906035504061302554b3110300e06035504081307556e6b6e6f776e3111300f0603550407130857657468657262793110300e060355040a1307556e6b6e6f776e3110300e060355040b1307556e6b6e6f776e311930170603550403131043696172616e2047756c746e69656b7330820122300d06092a864886f70d01010105000382010f003082010a028201010096d075e47c014e7822c89fd67f795d23203e2a8843f53ba4e6b1bf5f2fd0e225938267cfcae7fbf4fe596346afbaf4070fdb91f66fbcdf2348a3d92430502824f80517b156fab00809bdc8e631bfa9afd42d9045ab5fd6d28d9e140afc1300917b19b7c6c4df4a494cf1f7cb4a63c80d734265d735af9e4f09455f427aa65a53563f87b336ca2c19d244fcbba617ba0b19e56ed34afe0b253ab91e2fdb1271f1b9e3c3232027ed8862a112f0706e234cf236914b939bcf959821ecb2a6c18057e070de3428046d94b175e1d89bd795e535499a091f5bc65a79d539a8d43891ec504058acb28c08393b5718b57600a211e803f4a634e5c57f25b9b8c4422c6fd90203010001300d06092a864886f70d0101050500038201010008e4ef699e9807677ff56753da73efb2390d5ae2c17e4db691d5df7a7b60fc071ae509c5414be7d5da74df2811e83d3668c4a0b1abc84b9fa7d96b4cdf30bba68517ad2a93e233b042972ac0553a4801c9ebe07bf57ebe9a3b3d6d663965260e50f3b8f46db0531761e60340a2bddc3426098397fda54044a17e5244549f9869b460ca5e6e216b6f6a2db0580b480ca2afe6ec6b46eedacfa4aa45038809ece0c5978653d6c85f678e7f5a2156d1bedd8117751e64a4b0dcd140f3040b021821a8d93aed8d01ba36db6c82372211fed714d9a32607038cdfd565bd529ffc637212aaa2c224ef22b603eccefb5bf1e085c191d4b24fe742b17ab3f55d4e6f05ef");
        List<String> fdroid1 = Arrays.asList("F-Droid Archive", "https://f-droid.org/archive", "The archive repository of the F-Droid client. This contains older versions of applications from the main repository.",
                                            "13", "0", "2", "ignore", "3082035e30820246a00302010202044c49cd00300d06092a864886f70d01010505003071310b300906035504061302554b3110300e06035504081307556e6b6e6f776e3111300f0603550407130857657468657262793110300e060355040a1307556e6b6e6f776e3110300e060355040b1307556e6b6e6f776e311930170603550403131043696172616e2047756c746e69656b73301e170d3130303732333137313032345a170d3337313230383137313032345a3071310b300906035504061302554b3110300e06035504081307556e6b6e6f776e3111300f0603550407130857657468657262793110300e060355040a1307556e6b6e6f776e3110300e060355040b1307556e6b6e6f776e311930170603550403131043696172616e2047756c746e69656b7330820122300d06092a864886f70d01010105000382010f003082010a028201010096d075e47c014e7822c89fd67f795d23203e2a8843f53ba4e6b1bf5f2fd0e225938267cfcae7fbf4fe596346afbaf4070fdb91f66fbcdf2348a3d92430502824f80517b156fab00809bdc8e631bfa9afd42d9045ab5fd6d28d9e140afc1300917b19b7c6c4df4a494cf1f7cb4a63c80d734265d735af9e4f09455f427aa65a53563f87b336ca2c19d244fcbba617ba0b19e56ed34afe0b253ab91e2fdb1271f1b9e3c3232027ed8862a112f0706e234cf236914b939bcf959821ecb2a6c18057e070de3428046d94b175e1d89bd795e535499a091f5bc65a79d539a8d43891ec504058acb28c08393b5718b57600a211e803f4a634e5c57f25b9b8c4422c6fd90203010001300d06092a864886f70d0101050500038201010008e4ef699e9807677ff56753da73efb2390d5ae2c17e4db691d5df7a7b60fc071ae509c5414be7d5da74df2811e83d3668c4a0b1abc84b9fa7d96b4cdf30bba68517ad2a93e233b042972ac0553a4801c9ebe07bf57ebe9a3b3d6d663965260e50f3b8f46db0531761e60340a2bddc3426098397fda54044a17e5244549f9869b460ca5e6e216b6f6a2db0580b480ca2afe6ec6b46eedacfa4aa45038809ece0c5978653d6c85f678e7f5a2156d1bedd8117751e64a4b0dcd140f3040b021821a8d93aed8d01ba36db6c82372211fed714d9a32607038cdfd565bd529ffc637212aaa2c224ef22b603eccefb5bf1e085c191d4b24fe742b17ab3f55d4e6f05ef");
        List<String> fdroid2 = Arrays.asList("Guardian Project", "https://guardianproject.info/fdroid/repo", "The official app repository of The Guardian Project. Applications in this repository are official binaries build by the original application developers and signed by the same key as the APKs that are released in the Google Play store.",
                                            "13", "0", "3", "ignore", "308205d8308203c0020900a397b4da7ecda034300d06092a864886f70d01010505003081ad310b30090603550406130255533111300f06035504080c084e657720596f726b3111300f06035504070c084e657720596f726b31143012060355040b0c0b4644726f6964205265706f31193017060355040a0c10477561726469616e2050726f6a656374311d301b06035504030c14677561726469616e70726f6a6563742e696e666f3128302606092a864886f70d0109011619726f6f7440677561726469616e70726f6a6563742e696e666f301e170d3134303632363139333931385a170d3431313131303139333931385a3081ad310b30090603550406130255533111300f06035504080c084e657720596f726b3111300f06035504070c084e657720596f726b31143012060355040b0c0b4644726f6964205265706f31193017060355040a0c10477561726469616e2050726f6a656374311d301b06035504030c14677561726469616e70726f6a6563742e696e666f3128302606092a864886f70d0109011619726f6f7440677561726469616e70726f6a6563742e696e666f30820222300d06092a864886f70d01010105000382020f003082020a0282020100b3cd79121b9b883843be3c4482e320809106b0a23755f1dd3c7f46f7d315d7bb2e943486d61fc7c811b9294dcc6b5baac4340f8db2b0d5e14749e7f35e1fc211fdbc1071b38b4753db201c314811bef885bd8921ad86facd6cc3b8f74d30a0b6e2e6e576f906e9581ef23d9c03e926e06d1f033f28bd1e21cfa6a0e3ff5c9d8246cf108d82b488b9fdd55d7de7ebb6a7f64b19e0d6b2ab1380a6f9d42361770d1956701a7f80e2de568acd0bb4527324b1e0973e89595d91c8cc102d9248525ae092e2c9b69f7414f724195b81427f28b1d3d09a51acfe354387915fd9521e8c890c125fc41a12bf34d2a1b304067ab7251e0e9ef41833ce109e76963b0b256395b16b886bca21b831f1408f836146019e7908829e716e72b81006610a2af08301de5d067c9e114a1e5759db8a6be6a3cc2806bcfe6fafd41b5bc9ddddb3dc33d6f605b1ca7d8a9e0ecdd6390d38906649e68a90a717bea80fa220170eea0c86fc78a7e10dac7b74b8e62045a3ecca54e035281fdc9fe5920a855fde3c0be522e3aef0c087524f13d973dff3768158b01a5800a060c06b451ec98d627dd052eda804d0556f60dbc490d94e6e9dea62ffcafb5beffbd9fc38fb2f0d7050004fe56b4dda0a27bc47554e1e0a7d764e17622e71f83a475db286bc7862deee1327e2028955d978272ea76bf0b88e70a18621aba59ff0c5993ef5f0e5d6b6b98e68b70203010001300d06092a864886f70d0101050500038202010079c79c8ef408a20d243d8bd8249fb9a48350dc19663b5e0fce67a8dbcb7de296c5ae7bbf72e98a2020fb78f2db29b54b0e24b181aa1c1d333cc0303685d6120b03216a913f96b96eb838f9bff125306ae3120af838c9fc07ebb5100125436bd24ec6d994d0bff5d065221871f8410daf536766757239bf594e61c5432c9817281b985263bada8381292e543a49814061ae11c92a316e7dc100327b59e3da90302c5ada68c6a50201bda1fcce800b53f381059665dbabeeb0b50eb22b2d7d2d9b0aa7488ca70e67ac6c518adb8e78454a466501e89d81a45bf1ebc350896f2c3ae4b6679ecfbf9d32960d4f5b493125c7876ef36158562371193f600bc511000a67bdb7c664d018f99d9e589868d103d7e0994f166b2ba18ff7e67d8c4da749e44dfae1d930ae5397083a51675c409049dfb626a96246c0015ca696e94ebb767a20147834bf78b07fece3f0872b057c1c519ff882501995237d8206b0b3832f78753ebd8dcbd1d3d9f5ba733538113af6b407d960ec4353c50eb38ab29888238da843cd404ed8f4952f59e4bbc0035fc77a54846a9d419179c46af1b4a3b7fc98e4d312aaa29b9b7d79e739703dc0fa41c7280d5587709277ffa11c3620f5fba985b82c238ba19b17ebd027af9424be0941719919f620dd3bb3c3f11638363708aa11f858e153cf3a69bce69978b90e4a273836100aa1e617ba455cd00426847f");
        List<String> fdroid3 = Arrays.asList("Guardian Project Archive", "https://guardianproject.info/fdroid/archive", "The official repository of The Guardian Project apps for use with F-Droid client. This contains older versions of applications from the main repository.",
                                            "13", "0", "4", "ignore", "308205d8308203c0020900a397b4da7ecda034300d06092a864886f70d01010505003081ad310b30090603550406130255533111300f06035504080c084e657720596f726b3111300f06035504070c084e657720596f726b31143012060355040b0c0b4644726f6964205265706f31193017060355040a0c10477561726469616e2050726f6a656374311d301b06035504030c14677561726469616e70726f6a6563742e696e666f3128302606092a864886f70d0109011619726f6f7440677561726469616e70726f6a6563742e696e666f301e170d3134303632363139333931385a170d3431313131303139333931385a3081ad310b30090603550406130255533111300f06035504080c084e657720596f726b3111300f06035504070c084e657720596f726b31143012060355040b0c0b4644726f6964205265706f31193017060355040a0c10477561726469616e2050726f6a656374311d301b06035504030c14677561726469616e70726f6a6563742e696e666f3128302606092a864886f70d0109011619726f6f7440677561726469616e70726f6a6563742e696e666f30820222300d06092a864886f70d01010105000382020f003082020a0282020100b3cd79121b9b883843be3c4482e320809106b0a23755f1dd3c7f46f7d315d7bb2e943486d61fc7c811b9294dcc6b5baac4340f8db2b0d5e14749e7f35e1fc211fdbc1071b38b4753db201c314811bef885bd8921ad86facd6cc3b8f74d30a0b6e2e6e576f906e9581ef23d9c03e926e06d1f033f28bd1e21cfa6a0e3ff5c9d8246cf108d82b488b9fdd55d7de7ebb6a7f64b19e0d6b2ab1380a6f9d42361770d1956701a7f80e2de568acd0bb4527324b1e0973e89595d91c8cc102d9248525ae092e2c9b69f7414f724195b81427f28b1d3d09a51acfe354387915fd9521e8c890c125fc41a12bf34d2a1b304067ab7251e0e9ef41833ce109e76963b0b256395b16b886bca21b831f1408f836146019e7908829e716e72b81006610a2af08301de5d067c9e114a1e5759db8a6be6a3cc2806bcfe6fafd41b5bc9ddddb3dc33d6f605b1ca7d8a9e0ecdd6390d38906649e68a90a717bea80fa220170eea0c86fc78a7e10dac7b74b8e62045a3ecca54e035281fdc9fe5920a855fde3c0be522e3aef0c087524f13d973dff3768158b01a5800a060c06b451ec98d627dd052eda804d0556f60dbc490d94e6e9dea62ffcafb5beffbd9fc38fb2f0d7050004fe56b4dda0a27bc47554e1e0a7d764e17622e71f83a475db286bc7862deee1327e2028955d978272ea76bf0b88e70a18621aba59ff0c5993ef5f0e5d6b6b98e68b70203010001300d06092a864886f70d0101050500038202010079c79c8ef408a20d243d8bd8249fb9a48350dc19663b5e0fce67a8dbcb7de296c5ae7bbf72e98a2020fb78f2db29b54b0e24b181aa1c1d333cc0303685d6120b03216a913f96b96eb838f9bff125306ae3120af838c9fc07ebb5100125436bd24ec6d994d0bff5d065221871f8410daf536766757239bf594e61c5432c9817281b985263bada8381292e543a49814061ae11c92a316e7dc100327b59e3da90302c5ada68c6a50201bda1fcce800b53f381059665dbabeeb0b50eb22b2d7d2d9b0aa7488ca70e67ac6c518adb8e78454a466501e89d81a45bf1ebc350896f2c3ae4b6679ecfbf9d32960d4f5b493125c7876ef36158562371193f600bc511000a67bdb7c664d018f99d9e589868d103d7e0994f166b2ba18ff7e67d8c4da749e44dfae1d930ae5397083a51675c409049dfb626a96246c0015ca696e94ebb767a20147834bf78b07fece3f0872b057c1c519ff882501995237d8206b0b3832f78753ebd8dcbd1d3d9f5ba733538113af6b407d960ec4353c50eb38ab29888238da843cd404ed8f4952f59e4bbc0035fc77a54846a9d419179c46af1b4a3b7fc98e4d312aaa29b9b7d79e739703dc0fa41c7280d5587709277ffa11c3620f5fba985b82c238ba19b17ebd027af9424be0941719919f620dd3bb3c3f11638363708aa11f858e153cf3a69bce69978b90e4a273836100aa1e617ba455cd00426847f");

        List<String> shouldBeRepos = new LinkedList<>();
        shouldBeRepos.addAll(oem0);
        shouldBeRepos.addAll(oem1);
        shouldBeRepos.addAll(fdroid0);
        shouldBeRepos.addAll(fdroid1);
        shouldBeRepos.addAll(fdroid2);
        shouldBeRepos.addAll(fdroid3);

        for (int i = 0; i < defaultRepos.size(); i++) {
            assertEquals(shouldBeRepos.get(i), defaultRepos.get(i));
        }
    }

    @Test
    public void canAddRepo() {

        assertEquals(4, RepoProvider.Helper.all(context).size());

        Repo mock1 = insertRepo(
                context,
                "https://mock-repo-1.example.com/fdroid/repo",
                "Just a made up repo",
                "ABCDEF1234567890",
                "Mock Repo 1"
        );

        Repo mock2 = insertRepo(
                context,
                "http://mock-repo-2.example.com/fdroid/repo",
                "Mock repo without a name",
                "0123456789ABCDEF"
        );

        assertEquals(6, RepoProvider.Helper.all(context).size());

        assertRepo(
                mock1,
                "https://mock-repo-1.example.com/fdroid/repo",
                "Just a made up repo",
                "ABCDEF1234567890",
                "Mock Repo 1"
        );

        assertRepo(
                mock2,
                "http://mock-repo-2.example.com/fdroid/repo",
                "Mock repo without a name",
                "0123456789ABCDEF",
                "mock-repo-2.example.com/fdroid/repo"
        );
    }

    private static void assertRepo(Repo actualRepo, String expectedAddress, String expectedDescription,
                                   String expectedFingerprint, String expectedName) {
        assertEquals(expectedAddress, actualRepo.address);
        assertEquals(expectedDescription, actualRepo.description);
        assertEquals(expectedFingerprint, actualRepo.fingerprint);
        assertEquals(expectedName, actualRepo.name);
    }

    @Test
    public void canDeleteRepo() {
        Repo mock1 = insertRepo(
                context,
                "https://mock-repo-1.example.com/fdroid/repo",
                "Just a made up repo",
                "ABCDEF1234567890",
                "Mock Repo 1"
        );

        Repo mock2 = insertRepo(
                context,
                "http://mock-repo-2.example.com/fdroid/repo",
                "Mock repo without a name",
                "0123456789ABCDEF"
        );

        List<Repo> beforeDelete = RepoProvider.Helper.all(context);
        assertEquals(6, beforeDelete.size()); // Expect six repos, because of the four default ones.
        assertEquals(mock1.id, beforeDelete.get(4).id);
        assertEquals(mock2.id, beforeDelete.get(5).id);

        RepoProvider.Helper.remove(context, mock1.getId());

        List<Repo> afterDelete = RepoProvider.Helper.all(context);
        assertEquals(5, afterDelete.size());
        assertEquals(mock2.id, afterDelete.get(4).id);
    }

    public Repo insertRepo(Context context, String address, String description, String fingerprint) {
        return insertRepo(context, address, description, fingerprint, null);
    }

    public static Repo insertRepo(Context context, String address, String description,
                                  String fingerprint, @Nullable String name) {
        return insertRepo(context, address, description, fingerprint, name, false);
    }

    public static Repo insertRepo(Context context, String address, String description,
                                  String fingerprint, @Nullable String name, boolean isSwap) {
        ContentValues values = new ContentValues();
        values.put(RepoTable.Cols.ADDRESS, address);
        values.put(RepoTable.Cols.DESCRIPTION, description);
        values.put(RepoTable.Cols.FINGERPRINT, fingerprint);
        values.put(RepoTable.Cols.NAME, name);
        values.put(RepoTable.Cols.IS_SWAP, isSwap);

        RepoProvider.Helper.insert(context, values);
        return RepoProvider.Helper.findByAddress(context, address);
    }
}

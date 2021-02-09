/*
 * Copyright (C) 2021 Angus Gratton
 * Copyright (C) 2018 Senecto Limited
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.fdroid.fdroid;

import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.FDroidProviderTest;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.mock.MockApk;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class RepoUrlsTest extends FDroidProviderTest {
    public static final String TAG = "RepoUrlsTest";

    /**
     * Private class describing a repository URL we're going to test, and
     * the file pattern for any files within that URL.
     */
    private static class TestRepo {
        // Repo URL for the test case
        public String repoUrl;
        // String format pattern for generating file URLs, should contain a single %s for the filename
        public String fileUrlPattern;

        TestRepo(String repoUrl, String fileUrlPattern) {
            this.repoUrl = repoUrl;
            this.fileUrlPattern = fileUrlPattern;
        }
    }

    private static final String APK_NAME = "test-v1.apk";

    private static final TestRepo[] REPOS = {
            new TestRepo(
                    "https://microg.org/fdroid/repo",
                    "https://microg.org/fdroid/repo/%s"),
            new TestRepo(
                    "http://bdf2wcxujkg6qqff.onion/fdroid/repo",
                    "http://bdf2wcxujkg6qqff.onion/fdroid/repo/%s"),
            new TestRepo(
                    "http://lysator7eknrfl47rlyxvgeamrv7ucefgrrlhk7rouv3sna25asetwid.onion/pub/fdroid/repo",
                    "http://lysator7eknrfl47rlyxvgeamrv7ucefgrrlhk7rouv3sna25asetwid.onion/pub/fdroid/repo/%s"),
            new TestRepo(
                    "https://mirrors.nju.edu.cn/fdroid/repo?fingerprint=43238D512C1E5EB2D6569F4A3AFBF5523418B82E0A3ED1552770ABB9A9C9CCAB",
                    "https://mirrors.nju.edu.cn/fdroid/repo/%s?fingerprint=43238D512C1E5EB2D6569F4A3AFBF5523418B82E0A3ED1552770ABB9A9C9CCAB"),
            new TestRepo(
                    "https://raw.githubusercontent.com/guardianproject/fdroid-repo/master/fdroid/repo",
                    "https://raw.githubusercontent.com/guardianproject/fdroid-repo/master/fdroid/repo/%s"),
            new TestRepo(
                    "content://com.android.externalstorage.documents/tree/1AFB-2402%3A/document/1AFB-2402%3Atesty.at.or.at%2Ffdroid%2Frepo",
                    // note: to have a URL-escaped path in a format string pattern, we need to
                    // %-escape all URL %
                    "content://com.android.externalstorage.documents/tree/1AFB-2402%%3A/document/1AFB-2402%%3Atesty.at.or.at%%2Ffdroid%%2Frepo%%2F%s"),
            new TestRepo(
                    "content://authority/tree/313E-1F1C%3A/document/313E-1F1C%3Aguardianproject.info%2Ffdroid%2Frepo",
                    // note: to have a URL-escaped path in a format string pattern, we need to
                    // %-escape all URL %
                    "content://authority/tree/313E-1F1C%%3A/document/313E-1F1C%%3Aguardianproject.info%%2Ffdroid%%2Frepo%%2F%s"),
            new TestRepo(
                    "http://10.20.31.244:8888/fdroid/repo?FINGERPRINT=35521D88285A9D06FBE33D35FB8B4BB872D753666CF981728E2249FEE6D2D0F2&SWAP=1&BSSID=FE:EE:DA:45:2D:4E",
                    "http://10.20.31.244:8888/fdroid/repo/%s?FINGERPRINT=35521D88285A9D06FBE33D35FB8B4BB872D753666CF981728E2249FEE6D2D0F2&SWAP=1&BSSID=FE:EE:DA:45:2D:4E"),
            new TestRepo(
                    "fdroidrepos://briarproject.org/fdroid/repo?fingerprint=1FB874BEE7276D28ECB2C9B06E8A122EC4BCB4008161436CE474C257CBF49BD6",
                    "fdroidrepos://briarproject.org/fdroid/repo/%s?fingerprint=1FB874BEE7276D28ECB2C9B06E8A122EC4BCB4008161436CE474C257CBF49BD6"),
    };

    @Before
    public void setup() {
        Preferences.setupForTests(context);
    }

    interface GetFileFromRepo {
        String get(TestRepo tr);
    }

    /**
     * Utility test function - go through the list of test repos,
     * using the useOfRepo interface to instantiate a repo from the URL
     * and return a file of some kind (Apk, index, etc.) and check that
     * it matches the test repo's expected URL format.
     *
     * @param fileName  File that 'useOfRepo' will return in the repo, when called
     * @param useOfRepo Instance of the function that uses the repo to build a file URL
     */
    private void testReposWithFile(String fileName, GetFileFromRepo useOfRepo) {
        for (TestRepo tr : REPOS) {
            String expectedUrl = String.format(tr.fileUrlPattern, fileName);
            System.out.println("Testing URL " + expectedUrl);
            String actualUrl = useOfRepo.get(tr);
            assertEquals(expectedUrl, actualUrl);
        }
    }

    @Test
    public void testIndexUrls() {
        testReposWithFile(IndexUpdater.SIGNED_FILE_NAME, new GetFileFromRepo() {
            @Override
            public String get(TestRepo tr) {
                Repo repo = new Repo();
                repo.address = tr.repoUrl;
                IndexUpdater updater = new IndexUpdater(context, repo);
                return updater.getIndexUrl(repo);
            }
        });
    }

    @Test
    public void testIndexV1Urls() {
        testReposWithFile(IndexV1Updater.SIGNED_FILE_NAME, new GetFileFromRepo() {
            @Override
            public String get(TestRepo tr) {
                Repo repo = new Repo();
                repo.address = tr.repoUrl;
                IndexV1Updater updater = new IndexV1Updater(context, repo);
                return updater.getIndexUrl(repo);
            }
        });
    }

    @Test
    public void testApkUrls() {
        testReposWithFile(APK_NAME, new GetFileFromRepo() {
            @Override
            public String get(TestRepo tr) {
                Apk apk = new MockApk(APK_NAME, 1, tr.repoUrl, APK_NAME);
                return apk.getCanonicalUrl();
            }
        });
    }
}

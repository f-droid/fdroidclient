/*
 * Copyright (C) 2018 Hans-Christoph Steiner <hans@eds.org>
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

package org.fdroid.fdroid.localrepo;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Process;
import android.support.v4.provider.DocumentFile;
import android.util.Log;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.fdroid.fdroid.AddRepoIntentService;
import org.fdroid.fdroid.IndexUpdater;
import org.fdroid.fdroid.IndexV1Updater;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

/**
 * An {@link IntentService} subclass for handling asynchronous scanning of a
 * removable storage device like an SD Card or USB OTG thumb drive using the
 * Storage Access Framework.  Permission must first be granted by the user
 * {@link android.content.Intent#ACTION_OPEN_DOCUMENT_TREE} or
 * {@link android.os.storage.StorageVolume#createAccessIntent(String)}request,
 * then F-Droid will have permanent access to that{@link Uri}.
 * <p>
 * Even though the Storage Access Framework was introduced in
 * {@link android.os.Build.VERSION_CODES#KITKAT android-19}, this approach is only
 * workable if {@link android.content.Intent#ACTION_OPEN_DOCUMENT_TREE} is available.
 * It was added in {@link android.os.Build.VERSION_CODES#LOLLIPOP android-21}.
 *
 * @see <a href="https://commonsware.com/blog/2017/11/15/storage-situation-removable-storage.html"> The Storage Situation: Removable Storage </a>
 * @see <a href="https://developer.android.com/training/articles/scoped-directory-access.html">Using Scoped Directory Access</a>
 * @see <a href="https://developer.android.com/guide/topics/providers/document-provider.html">Open Files using Storage Access Framework</a>
 */
@TargetApi(21)
public class TreeUriScannerIntentService extends IntentService {
    public static final String TAG = "TreeUriScannerIntentSer";

    private static final String ACTION_SCAN_TREE_URI = "org.fdroid.fdroid.localrepo.action.SCAN_TREE_URI";

    public TreeUriScannerIntentService() {
        super("TreeUriScannerIntentService");
    }

    public static void scan(Context context, Uri data) {
        if (Preferences.get().isScanRemovableStorageEnabled()) {
            Intent intent = new Intent(context, TreeUriScannerIntentService.class);
            intent.setAction(ACTION_SCAN_TREE_URI);
            intent.setData(data);
            context.startService(intent);
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null || !ACTION_SCAN_TREE_URI.equals(intent.getAction())) {
            return;
        }
        Uri treeUri = intent.getData();
        if (treeUri == null) {
            return;
        }
        Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
        DocumentFile treeFile = DocumentFile.fromTreeUri(this, treeUri);
        searchDirectory(treeFile);
    }

    private void searchDirectory(DocumentFile documentFileDir) {
        DocumentFile[] documentFiles = documentFileDir.listFiles();
        if (documentFiles == null) {
            return;
        }
        for (DocumentFile documentFile : documentFiles) {
            if (documentFile.isDirectory()) {
                searchDirectory(documentFile);
            } else {
                if (IndexV1Updater.SIGNED_FILE_NAME.equals(documentFile.getName())) {
                    registerRepo(documentFile);
                }
            }
        }
    }

    /**
     * For all files called {@link IndexV1Updater#SIGNED_FILE_NAME} found, check
     * the JAR signature and read the fingerprint of the signing certificate.
     * The fingerprint is then used to find whether this local repo is a mirror
     * of an existing repo, or a totally new repo.  In order to verify the
     * signatures in the JAR, the whole file needs to be read in first.
     *
     * @see JarInputStream#JarInputStream(InputStream, boolean)
     */
    private void registerRepo(DocumentFile index) {
        InputStream inputStream = null;
        try {
            Log.i(TAG, "FOUND: " + index.getUri());
            inputStream = getContentResolver().openInputStream(index.getUri());
            Log.i(TAG, "repo URL: " + index.getParentFile().getUri());
            registerRepo(this, inputStream, index.getParentFile().getUri());
        } catch (IOException | IndexUpdater.SigningException e) {
            e.printStackTrace();
        } finally {
            Utils.closeQuietly(inputStream);
        }
    }

    public static void registerRepo(Context context, InputStream inputStream, Uri repoUri)
            throws IOException, IndexUpdater.SigningException {
        if (inputStream == null) {
            return;
        }
        File destFile = File.createTempFile("dl-", IndexV1Updater.SIGNED_FILE_NAME, context.getCacheDir());
        FileUtils.copyInputStreamToFile(inputStream, destFile);
        JarFile jarFile = new JarFile(destFile, true);
        JarEntry indexEntry = (JarEntry) jarFile.getEntry(IndexV1Updater.DATA_FILE_NAME);
        IOUtils.readLines(jarFile.getInputStream(indexEntry));
        Certificate certificate = IndexUpdater.getSigningCertFromJar(indexEntry);
        Log.i(TAG, "Got certificate: " + certificate);
        String fingerprint = Utils.calcFingerprint(certificate);
        Log.i(TAG, "Got fingerprint: " + fingerprint);
        destFile.delete();

        Log.i(TAG, "Found a valid, signed index-v1.json");
        for (Repo repo : RepoProvider.Helper.all(context)) {
            if (fingerprint.equals(repo.fingerprint)) {
                Log.i(TAG, repo.address + " has the SAME fingerprint: " + fingerprint);
            } else {
                Log.i(TAG, repo.address + " different fingerprint");
            }
        }

        AddRepoIntentService.addRepo(context, repoUri, fingerprint);
        // TODO rework IndexUpdater.getSigningCertFromJar to work for here
    }
}

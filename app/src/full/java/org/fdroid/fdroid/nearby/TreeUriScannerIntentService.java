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

package org.fdroid.fdroid.nearby;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.fdroid.database.Repository;
import org.fdroid.fdroid.AddRepoIntentService;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.index.SigningException;
import org.fdroid.index.v1.IndexV1UpdaterKt;
import org.fdroid.index.v1.IndexV1VerifierKt;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.CodeSigner;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
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
 * {@link android.os.storage.StorageVolume#createAccessIntent(String)} is also
 * necessary to do this with any kind of rational UX.
 *
 * @see <a href="https://commonsware.com/blog/2017/11/15/storage-situation-removable-storage.html">The Storage Situation: Removable Storage </a>
 * @see <a href="https://commonsware.com/blog/2016/11/18/be-careful-scoped-directory-access.html">Be Careful with Scoped Directory Access</a>
 * @see <a href="https://developer.android.com/training/articles/scoped-directory-access.html">Using Scoped Directory Access</a>
 * @see <a href="https://developer.android.com/guide/topics/providers/document-provider.html">Open Files using Storage Access Framework</a>
 */
public class TreeUriScannerIntentService extends IntentService {
    public static final String TAG = "TreeUriScannerIntentSer";

    private static final String ACTION_SCAN_TREE_URI = "org.fdroid.fdroid.nearby.action.SCAN_TREE_URI";
    /**
     * @see <a href="https://android.googlesource.com/platform/frameworks/base/+/android-10.0.0_r38/core/java/android/provider/DocumentsContract.java#238">DocumentsContract.EXTERNAL_STORAGE_PROVIDER_AUTHORITY</a>
     * @see <a href="https://android.googlesource.com/platform/frameworks/base/+/android-10.0.0_r38/packages/ExternalStorageProvider/src/com/android/externalstorage/ExternalStorageProvider.java#70">ExternalStorageProvider.AUTHORITY</a>
     */
    public static final String EXTERNAL_STORAGE_PROVIDER_AUTHORITY = "com.android.externalstorage.documents";

    public TreeUriScannerIntentService() {
        super("TreeUriScannerIntentService");
    }

    public static void scan(Context context, Uri data) {
        Intent intent = new Intent(context, TreeUriScannerIntentService.class);
        intent.setAction(ACTION_SCAN_TREE_URI);
        intent.setData(data);
        context.startService(intent);
    }

    /**
     * Now determine if it is External Storage that must be handled by the
     * {@link TreeUriScannerIntentService} or whether it is External Storage
     * like an SD Card that can be directly accessed via the file system.
     */
    public static void onActivityResult(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        Uri uri = intent.getData();
        if (uri != null) {
            ContentResolver contentResolver = context.getContentResolver();
            int perms = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
            contentResolver.takePersistableUriPermission(uri, perms);
            String msg = String.format(context.getString(R.string.swap_toast_using_path), uri.toString());
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            scan(context, uri);
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

    /**
     * Recursively search for {@link IndexV1UpdaterKt#SIGNED_FILE_NAME} starting
     * from the given directory, looking at files first before recursing into
     * directories.  This is "depth last" since the index file is much more
     * likely to be shallow than deep, and there can be a lot of files to
     * search through starting at 4 or more levels deep, like the fdroid
     * icons dirs and the per-app "external storage" dirs.
     */
    private void searchDirectory(DocumentFile documentFileDir) {
        DocumentFile[] documentFiles = documentFileDir.listFiles();
        if (documentFiles == null) {
            return;
        }
        boolean foundIndex = false;
        ArrayList<DocumentFile> dirs = new ArrayList<>();
        for (DocumentFile documentFile : documentFiles) {
            if (documentFile.isDirectory()) {
                dirs.add(documentFile);
            } else if (!foundIndex) {
                if (IndexV1UpdaterKt.SIGNED_FILE_NAME.equals(documentFile.getName())) {
                    registerRepo(documentFile);
                    foundIndex = true;
                }
            }
        }
        for (DocumentFile dir : dirs) {
            searchDirectory(dir);
        }
    }

    /**
     * For all files called {@link IndexV1UpdaterKt#SIGNED_FILE_NAME} found, check
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
            inputStream = getContentResolver().openInputStream(index.getUri());
            registerRepo(this, inputStream, index.getParentFile().getUri());
        } catch (IOException | SigningException e) {
            e.printStackTrace();
        } finally {
            Utils.closeQuietly(inputStream);
        }
    }

    public static void registerRepo(Context context, InputStream inputStream, Uri repoUri)
            throws IOException, SigningException {
        if (inputStream == null) {
            return;
        }
        File destFile = File.createTempFile("dl-", IndexV1UpdaterKt.SIGNED_FILE_NAME, context.getCacheDir());
        FileUtils.copyInputStreamToFile(inputStream, destFile);
        JarFile jarFile = new JarFile(destFile, true);
        JarEntry indexEntry = (JarEntry) jarFile.getEntry(IndexV1VerifierKt.DATA_FILE_NAME);
        IOUtils.readLines(jarFile.getInputStream(indexEntry));
        Certificate certificate = getSigningCertFromJar(indexEntry);
        String fingerprint = Utils.calcFingerprint(certificate);
        Log.i(TAG, "Got fingerprint: " + fingerprint);
        destFile.delete();

        Log.i(TAG, "Found a valid, signed index-v1.json");
        for (Repository repo : FDroidApp.getRepoManager(context).getRepositories()) {
            if (fingerprint.equals(repo.getFingerprint())) {
                Log.i(TAG, repo.getAddress() + " has the SAME fingerprint: " + fingerprint);
            } else {
                Log.i(TAG, repo.getAddress() + " different fingerprint");
            }
        }

        AddRepoIntentService.addRepo(context, repoUri, fingerprint);
        // TODO rework IndexUpdater.getSigningCertFromJar to work for here
    }

    /**
     * FDroid's index.jar is signed using a particular format and does not allow lots of
     * signing setups that would be valid for a regular jar.  This validates those
     * restrictions.
     */
    static X509Certificate getSigningCertFromJar(JarEntry jarEntry) throws SigningException {
        final CodeSigner[] codeSigners = jarEntry.getCodeSigners();
        if (codeSigners == null || codeSigners.length == 0) {
            throw new SigningException("No signature found in index");
        }
        /* we could in theory support more than 1, but as of now we do not */
        if (codeSigners.length > 1) {
            throw new SigningException("index.jar must be signed by a single code signer!");
        }
        List<? extends Certificate> certs = codeSigners[0].getSignerCertPath().getCertificates();
        if (certs.size() != 1) {
            throw new SigningException("index.jar code signers must only have a single certificate!");
        }
        return (X509Certificate) certs.get(0);
    }
}

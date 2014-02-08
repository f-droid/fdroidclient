package org.fdroid.fdroid.updater;

import android.content.Context;
import android.util.Log;
import org.fdroid.fdroid.Hasher;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Repo;

import java.io.*;
import java.security.cert.Certificate;
import java.util.Date;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class SignedRepoUpdater extends RepoUpdater {

    public SignedRepoUpdater(Context ctx, Repo repo) {
        super(ctx, repo);
    }

    private boolean verifyCerts(JarEntry item) throws UpdateException {
        Certificate[] certs = item.getCertificates();
        if (certs == null || certs.length == 0) {
            throw new UpdateException(repo, "No signature found in index");
        }

        Log.d("FDroid", "Index has " + certs.length + " signature(s)");
        boolean match = false;
        for (Certificate cert : certs) {
            String certdata = Hasher.hex(cert);
            if (repo.pubkey == null && repo.fingerprint.equals(Utils.calcFingerprint(cert))) {
                repo.pubkey = certdata;
            }
            if (repo.pubkey != null && repo.pubkey.equals(certdata)) {
                match = true;
                break;
            }
        }
        return match;
    }

    protected File extractIndexFromJar(File indexJar) throws UpdateException {
        File indexFile  = null;
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(indexJar, true);
            JarEntry indexEntry = (JarEntry)jarFile.getEntry("index.xml");

            indexFile  = File.createTempFile("index-", ".xml", context.getFilesDir());
            InputStream input = null;
            OutputStream output = null;
            try {
                input = jarFile.getInputStream(indexEntry);
                output = new FileOutputStream(indexFile);
                Utils.copy(input, output);
            } finally {
                Utils.closeQuietly(output);
                Utils.closeQuietly(input);
            }

            // Can only read certificates from jar after it has been read
            // completely, so we put it after the copy above...
            if (!verifyCerts(indexEntry)) {
                indexFile.delete();
                throw new UpdateException(repo, "Index signature mismatch");
            }
        } catch (IOException e) {
            if (indexFile != null) {
                indexFile.delete();
            }
            throw new UpdateException(
                    repo, "Error opening signed index", e);
        } finally {
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (IOException ioe) {
                    // ignore
                }
            }
        }

        return indexFile;
    }

    protected String getIndexAddress() {
        return repo.address + "/index.jar?client_version=" + context.getString(R.string.version_name);
    }

    /**
     * As this is a signed repo - we download the jar file,
     * check the signature, and extract the index file
     */
    @Override
    protected File getIndexFromFile(File downloadedFile) throws
            UpdateException {
        Date updateTime = new Date(System.currentTimeMillis());
        Log.d("FDroid", "Getting signed index from " + repo.address + " at " +
                Utils.LOG_DATE_FORMAT.format(updateTime));

        File indexJar  = downloadedFile;
        File indexXml  = null;

        // Don't worry about checking the status code for 200. If it was a
        // successful download, then we will have a file ready to use:
        if (indexJar != null && indexJar.exists()) {
            indexXml = extractIndexFromJar(indexJar);
        }
        return indexXml;
    }
}

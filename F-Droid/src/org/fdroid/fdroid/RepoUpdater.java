package org.fdroid.fdroid;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.net.Downloader;
import org.fdroid.fdroid.net.DownloaderFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.CodeSigner;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class RepoUpdater {

    private static final String TAG = "RepoUpdater";

    public static final String PROGRESS_TYPE_PROCESS_XML = "processingXml";
    public static final String PROGRESS_DATA_REPO_ADDRESS = "repoAddress";

    @NonNull protected final Context context;
    @NonNull protected final Repo repo;
    private List<App> apps = new ArrayList<>();
    private List<Apk> apks = new ArrayList<>();
    private RepoUpdateRememberer rememberer = null;
    protected boolean hasChanged = false;
    @Nullable protected ProgressListener progressListener;

    /**
     * Updates an app repo as read out of the database into a {@link Repo} instance.
     *
     * @param context
     * @param repo    a {@link Repo} read out of the local database
     */
    public RepoUpdater(@NonNull Context context, @NonNull Repo repo) {
        this.context = context;
        this.repo = repo;
    }

    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    public boolean hasChanged() { return hasChanged; }

    public List<App> getApps() { return apps; }

    public List<Apk> getApks() { return apks; }

    protected String getIndexAddress() {
        try {
            String versionName = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
            return repo.address + "/index.jar?client_version=" + versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return repo.address + "/index.jar";
    }

    Downloader downloadIndex() throws UpdateException {
        Downloader downloader = null;
        try {
            downloader = DownloaderFactory.create(
                getIndexAddress(), File.createTempFile("index-", "-downloaded", context.getCacheDir()));
            downloader.setCacheTag(repo.lastetag);

            if (progressListener != null) { // interactive session, show progress
                Bundle data = new Bundle(1);
                data.putString(PROGRESS_DATA_REPO_ADDRESS, repo.address);
                downloader.setProgressListener(progressListener, data);
            }

            downloader.downloadUninterrupted();

            if (downloader.isCached()) {
                // The index is unchanged since we last read it. We just mark
                // everything that came from this repo as being updated.
                Log.d(TAG, "Repo index for " + getIndexAddress() + " is up to date (by etag)");
            }

        } catch (IOException e) {
            if (downloader != null && downloader.getFile() != null) {
                downloader.getFile().delete();
            }
            throw new UpdateException(repo, "Error getting index file from " + repo.address, e);
        }
        return downloader;
    }

    /**
     * All repos are represented by a signed jar file, {@code index.jar}, which contains
     * a single file, {@code index.xml}.  This takes the {@code index.jar}, verifies the
     * signature, then returns the unzipped {@code index.xml}.
     *
     * @throws UpdateException All error states will come from here.
     */
    public void update() throws UpdateException {

        final Downloader downloader = downloadIndex();
        hasChanged = downloader.hasChanged();

        if (hasChanged) {
            // Don't worry about checking the status code for 200. If it was a
            // successful download, then we will have a file ready to use:
            processDownloadedFile(downloader.getFile(), downloader.getCacheTag());
        }
    }

    void processDownloadedFile(File downloadedFile, String cacheTag) throws UpdateException {
        InputStream indexInputStream = null;
        try {
            boolean storePubKey = false;
            if (repo.pubkey == null) // new repo, no signing certificate stored
                storePubKey = true;

            if (downloadedFile == null || !downloadedFile.exists())
                throw new UpdateException(repo, downloadedFile + " does not exist!");

            JarFile jarFile = new JarFile(downloadedFile, true);
            JarEntry indexEntry = (JarEntry) jarFile.getEntry("index.xml");
            indexInputStream = new ProgressBufferedInputStream(jarFile.getInputStream(indexEntry),
                    progressListener, repo, (int)indexEntry.getSize());

            // Process the index...
            final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            final XMLReader reader = parser.getXMLReader();
            final RepoXMLHandler repoXMLHandler = new RepoXMLHandler(repo);
            reader.setContentHandler(repoXMLHandler);
            reader.parse(new InputSource(indexInputStream));

            /* JarEntry can only read certificates after the file represented by that JarEntry
             * has been read completely, so verification can run until now... */
            verifyCerts(indexEntry, storePubKey);

            apps = repoXMLHandler.getApps();
            apks = repoXMLHandler.getApks();

            rememberer = new RepoUpdateRememberer();
            rememberer.context = context;
            rememberer.repo = repo;
            rememberer.values = prepareRepoDetailsForSaving(repoXMLHandler, cacheTag, storePubKey);
        } catch (SAXException | ParserConfigurationException | IOException e) {
            throw new UpdateException(repo, "Error parsing index for repo " + repo.address, e);
        } finally {
            if (indexInputStream != null) {
                try {
                    indexInputStream.close();
                } catch (IOException e) {
                    // ignored
                }
            }
            if (downloadedFile != null) {
                downloadedFile.delete();
            }
        }
    }

    private ContentValues prepareRepoDetailsForSaving(RepoXMLHandler handler, String etag, boolean storePubKey) {
        ContentValues values = new ContentValues();

        values.put(RepoProvider.DataColumns.LAST_UPDATED, Utils.formatDate(new Date(), ""));

        if (repo.lastetag == null || !repo.lastetag.equals(etag)) {
            values.put(RepoProvider.DataColumns.LAST_ETAG, etag);
        }

        /*
         * We received a repo config that included the fingerprint, so we need to save
         * the pubkey now.
         */
        if (storePubKey && repo.pubkey != null && repo.pubkey.equals(handler.getPubKey())) {
            Log.d(TAG, "Public key found - saving in the database.");
            values.put(RepoProvider.DataColumns.PUBLIC_KEY, handler.getPubKey());
        }

        if (handler.getVersion() != -1 && handler.getVersion() != repo.version) {
            Log.d(TAG, "Repo specified a new version: from "
                    + repo.version + " to " + handler.getVersion());
            values.put(RepoProvider.DataColumns.VERSION, handler.getVersion());
        }

        if (handler.getMaxAge() != -1 && handler.getMaxAge() != repo.maxage) {
            Log.d(TAG,
                    "Repo specified a new maximum age - updated");
            values.put(RepoProvider.DataColumns.MAX_AGE, handler.getMaxAge());
        }

        if (handler.getDescription() != null && !handler.getDescription().equals(repo.description)) {
            values.put(RepoProvider.DataColumns.DESCRIPTION, handler.getDescription());
        }

        if (handler.getName() != null && !handler.getName().equals(repo.name)) {
            values.put(RepoProvider.DataColumns.NAME, handler.getName());
        }

        return values;
    }

    public RepoUpdateRememberer getRememberer() { return rememberer; }

    public static class RepoUpdateRememberer {

        private Context context;
        private Repo repo;
        private ContentValues values;

        public void rememberUpdate() {
             RepoProvider.Helper.update(context, repo, values);
        }

    }

    public static class UpdateException extends Exception {

        private static final long serialVersionUID = -4492452418826132803L;
        public final Repo repo;

        public UpdateException(Repo repo, String message) {
            super(message);
            this.repo = repo;
        }

        public UpdateException(Repo repo, String message, Exception cause) {
            super(message, cause);
            this.repo = repo;
        }
    }

    private void verifyCerts(JarEntry jarEntry, boolean storePubKey) throws UpdateException {
        final CodeSigner[] codeSigners = jarEntry.getCodeSigners();
        if (codeSigners == null || codeSigners.length == 0) {
            throw new UpdateException(repo, "No signature found in index");
        }
        /* we could in theory support more than 1, but as of now we do not */
        if (codeSigners.length > 1) {
            throw new UpdateException(repo, "index.jar must be signed by a single code signer!");
        }
        List<? extends Certificate> certs = codeSigners[0].getSignerCertPath().getCertificates();
        if (certs.size() != 1) {
            throw new UpdateException(repo, "index.jar code signers must only have a single certificate!");
        }
        Certificate cert = certs.get(0);

        // though its called repo.pubkey, its actually a X509 certificate
        String pubkey = Hasher.hex(cert);

        /* The first time a repo is added, it can be added with the signing key's
           fingerprint.  In that case, check that fingerprint against what is
           actually in the index.jar itself */
        if (repo.pubkey == null && repo.fingerprint != null) {
            String certFingerprint = Utils.calcFingerprint(cert);
            if (repo.fingerprint.equalsIgnoreCase(certFingerprint)) {
                storePubKey = true;
            } else {
                throw new UpdateException(repo, "Supplied certificate fingerprint does not match!");
            }
        }
        /* This storePubKey business makes me uncomfortable, but there seems no way around it
         * since writing the pubkey to the database happens far from here in RepoUpdateRememberer */
        if (storePubKey) {
            repo.pubkey = pubkey;
            return;
        } else if (repo.pubkey != null && repo.pubkey.equals(pubkey)) {
            return;  // we have a match!
        }
        throw new UpdateException(repo, "Signing certificate does not match!");
    }

}

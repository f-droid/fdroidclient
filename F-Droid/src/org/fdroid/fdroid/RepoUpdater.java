package org.fdroid.fdroid;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
    protected boolean usePubkeyInJar = false;
    protected boolean hasChanged = false;
    @Nullable protected ProgressListener progressListener;

    public RepoUpdater(@NonNull Context ctx, @NonNull Repo repo) {
        this.context = ctx;
        this.repo    = repo;
    }

    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    public boolean hasChanged() { return hasChanged; }

    public List<App> getApps() { return apps; }

    public List<Apk> getApks() { return apks; }

    /**
     * All repos are represented by a signed jar file, {@code index.jar}, which contains
     * a single file, {@code index.xml}.  This takes the {@code index.jar}, verifies the
     * signature, then returns the unzipped {@code index.xml}.
     *
     * @throws UpdateException All error states will come from here.
     */
    protected File getIndexFromFile(File downloadedFile) throws UpdateException {
        final Date updateTime = new Date(System.currentTimeMillis());
        Log.d(TAG, "Getting signed index from " + repo.address + " at " +
                Utils.formatLogDate(updateTime));

        final File indexJar = downloadedFile;
        File indexXml = null;

        // Don't worry about checking the status code for 200. If it was a
        // successful download, then we will have a file ready to use:
        if (indexJar != null && indexJar.exists()) {

            // Due to a bug in android 5.0 lollipop, the inclusion of BouncyCastle causes
            // breakage when verifying the signature of the downloaded .jar. For more
            // details, check out https://gitlab.com/fdroid/fdroidclient/issues/111.
            try {
                FDroidApp.disableSpongyCastleOnLollipop();
                indexXml = extractIndexFromJar(indexJar);
            } finally {
                FDroidApp.enableSpongyCastleOnLollipop();
            }

        }
        return indexXml;
    }

    protected String getIndexAddress() {
        try {
            String versionName = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
            return repo.address + "/index.jar?client_version=" + versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return repo.address + "/index.jar";
    }

    protected Downloader downloadIndex() throws UpdateException {
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

    private int estimateAppCount(File indexFile) {
        int count = -1;
        try {
            // A bit of a hack, this might return false positives if an apps description
            // or some other part of the XML file contains this, but it is a pretty good
            // estimate and makes the progress counter more informative.
            // As with asking the server about the size of the index before downloading,
            // this also has a time tradeoff. It takes about three seconds to iterate
            // through the file and count 600 apps on a slow emulator (v17), but if it is
            // taking two minutes to update, the three second wait may be worth it.
            final String APPLICATION = "<application";
            count = Utils.countSubstringOccurrence(indexFile, APPLICATION);
        } catch (IOException e) {
            // Do nothing. Leave count at default -1 value.
        }
        return count;
    }

    public void update() throws UpdateException {

        File downloadedFile = null;
        File indexFile = null;
        try {

            final Downloader downloader = downloadIndex();
            hasChanged = downloader.hasChanged();

            if (hasChanged) {

                downloadedFile = downloader.getFile();
                indexFile = getIndexFromFile(downloadedFile);

                // Process the index...
                final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
                final XMLReader reader = parser.getXMLReader();
                final RepoXMLHandler handler = new RepoXMLHandler(repo, progressListener);

                if (progressListener != null) {
                    // Only bother spending the time to count the expected apps
                    // if we can show that to the user...
                    handler.setTotalAppCount(estimateAppCount(indexFile));
                }

                reader.setContentHandler(handler);
                InputSource is = new InputSource(
                        new BufferedReader(new FileReader(indexFile)));

                reader.parse(is);
                apps = handler.getApps();
                apks = handler.getApks();

                rememberer = new RepoUpdateRememberer();
                rememberer.context = context;
                rememberer.repo = repo;
                rememberer.values = prepareRepoDetailsForSaving(handler, downloader.getCacheTag());
            }
        } catch (SAXException | ParserConfigurationException | IOException e) {
            throw new UpdateException(repo, "Error parsing index for repo " + repo.address, e);
        } finally {
            if (downloadedFile != null && downloadedFile != indexFile && downloadedFile.exists()) {
                downloadedFile.delete();
            }
            if (indexFile != null && indexFile.exists()) {
                indexFile.delete();
            }

        }
    }

    private ContentValues prepareRepoDetailsForSaving(RepoXMLHandler handler, String etag) {

        ContentValues values = new ContentValues();

        values.put(RepoProvider.DataColumns.LAST_UPDATED, Utils.formatDate(new Date(), ""));

        if (repo.lastetag == null || !repo.lastetag.equals(etag)) {
            values.put(RepoProvider.DataColumns.LAST_ETAG, etag);
        }

        /*
         * We received a repo config that included the fingerprint, so we need to save
         * the pubkey now.
         */
        if (handler.getPubKey() != null && (repo.pubkey == null || usePubkeyInJar)) {
            Log.d(TAG, "Public key found - saving in the database.");
            values.put(RepoProvider.DataColumns.PUBLIC_KEY, handler.getPubKey());
            usePubkeyInJar = false;
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

    private boolean verifyCerts(JarEntry item) throws UpdateException {
        final Certificate[] certs = item.getCertificates();
        if (certs == null || certs.length == 0) {
            throw new UpdateException(repo, "No signature found in index");
        }

        Log.d(TAG, "Index has " + certs.length + " signature(s)");
        boolean match = false;
        for (final Certificate cert : certs) {
            String certdata = Hasher.hex(cert);
            if (repo.pubkey == null && repo.fingerprint != null) {
                String certFingerprint = Utils.calcFingerprint(cert);
                Log.d(TAG, "No public key for repo " + repo.address + " yet, but it does have a fingerprint, so comparing them.");
                Log.d(TAG, "Repo fingerprint: " + repo.fingerprint);
                Log.d(TAG, "Cert fingerprint: " + certFingerprint);
                if (repo.fingerprint.equalsIgnoreCase(certFingerprint)) {
                    repo.pubkey = certdata;
                    usePubkeyInJar = true;
                }
            }
            if (repo.pubkey != null && repo.pubkey.equals(certdata)) {
                Log.d(TAG, "Checking repo public key against cert found in jar.");
                match = true;
                break;
            }
        }
        return match;
    }

    protected File extractIndexFromJar(File indexJar) throws UpdateException {
        File indexFile = null;
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(indexJar, true);
            JarEntry indexEntry = (JarEntry) jarFile.getEntry("index.xml");

            indexFile = File.createTempFile("index-", "-extracted.xml", context.getCacheDir());
            InputStream input = null;
            OutputStream output = null;
            try {
                /*
                 * JarFile.getInputStream() provides the signature check, even
                 * though the Android docs do not mention this, the Java docs do
                 * and Android seems to implement it the same:
                 * http://docs.oracle.com/javase/6/docs/api/java/util/jar/JarFile.html#getInputStream(java.util.zip.ZipEntry)
                 * https://developer.android.com/reference/java/util/jar/JarFile.html#getInputStream(java.util.zip.ZipEntry)
                 */
                input = jarFile.getInputStream(indexEntry);
                output = new FileOutputStream(indexFile);
                Utils.copy(input, output);
            } finally {
                Utils.closeQuietly(output);
                Utils.closeQuietly(input);
            }

            // Can only read certificates from jar after it has been read
            // completely, so we put it after the copy above...
            if (isTofuRequest()) {
                Log.i(TAG, "Implicitly trusting the signature of index.jar, because this is a TOFU request");
                // Note that later on in the process we will save the pubkey against they repo, so
                // that future requests verify against the signature we got this time.
            } else if (!verifyCerts(indexEntry)) {
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

    /**
     * If the repo doesn't have a fingerprint, then this is a "Trust On First Use" (TOFU)
     * request. In that case, we will not verify the certificate, but rather implicitly trust
     * the file we downloaded. We'll extract the certificate from the jar, and then use that
     * to verify future requests to the same repository.
     */
    private boolean isTofuRequest() {
        return TextUtils.isEmpty(repo.fingerprint);
    }

}

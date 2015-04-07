package org.fdroid.fdroid.updater;

import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import org.fdroid.fdroid.ProgressListener;
import org.fdroid.fdroid.RepoXMLHandler;
import org.fdroid.fdroid.Utils;
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
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

abstract public class RepoUpdater {

    private static final String TAG = "fdroid.RepoUpdater";

    public static final String PROGRESS_TYPE_PROCESS_XML = "processingXml";

    public static final String PROGRESS_DATA_REPO_ADDRESS = "repoAddress";

    public static RepoUpdater createUpdaterFor(Context ctx, Repo repo) {
        if (repo.fingerprint == null && repo.pubkey == null) {
            return new UnsignedRepoUpdater(ctx, repo);
        } else {
            return new SignedRepoUpdater(ctx, repo);
        }
    }

    protected final Context context;
    protected final Repo repo;
    private List<App> apps = new ArrayList<>();
    private List<Apk> apks = new ArrayList<>();
    private RepoUpdateRememberer rememberer = null;
    protected boolean usePubkeyInJar = false;
    protected boolean hasChanged = false;
    protected ProgressListener progressListener;

    public RepoUpdater(Context ctx, Repo repo) {
        this.context = ctx;
        this.repo    = repo;
    }

    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    public boolean hasChanged() {
        return hasChanged;
    }

    public List<App> getApps() {
        return apps;
    }

    public List<Apk> getApks() {
        return apks;
    }

    /**
     * For example, you may want to unzip a jar file to get the index inside,
     * or if the file is not compressed, you can just return a reference to
     * the downloaded file.
     *
     * @throws UpdateException All error states will come from here.
     */
    protected abstract File getIndexFromFile(File downloadedFile) throws UpdateException;

    protected abstract String getIndexAddress();

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

            Downloader downloader = downloadIndex();
            hasChanged = downloader.hasChanged();

            if (hasChanged) {

                downloadedFile = downloader.getFile();
                indexFile = getIndexFromFile(downloadedFile);

                // Process the index...
                SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
                XMLReader reader = parser.getXMLReader();
                RepoXMLHandler handler = new RepoXMLHandler(repo, progressListener);

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

    private ContentValues prepareRepoDetailsForSaving (RepoXMLHandler handler, String etag) {

        ContentValues values = new ContentValues();

        values.put(RepoProvider.DataColumns.LAST_UPDATED, Utils.formatDate(new Date(), ""));

        if (repo.lastetag == null || !repo.lastetag.equals(etag)) {
            values.put(RepoProvider.DataColumns.LAST_ETAG, etag);
        }

        /*
         * We read an unsigned index that indicates that a signed version
         * is available. Or we received a repo config that included the
         * fingerprint, so we need to save the pubkey now.
         */
        if (handler.getPubKey() != null &&
                (repo.pubkey == null || usePubkeyInJar)) {
            // TODO: Spend the time *now* going to get the etag of the signed
            // repo, so that we can prevent downloading it next time. Otherwise
            // next time we update, we have to download the signed index
            // in its entirety, regardless of if it contains the same
            // information as the unsigned one does not...
            Log.d(TAG,
                    "Public key found - switching to signed repo for future updates");
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

    public RepoUpdateRememberer getRememberer() {
        return rememberer;
    }

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

}

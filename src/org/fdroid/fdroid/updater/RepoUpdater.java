package org.fdroid.fdroid.updater;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import org.fdroid.fdroid.DB;
import org.fdroid.fdroid.ProgressListener;
import org.fdroid.fdroid.RepoXMLHandler;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.net.Downloader;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.net.ssl.SSLHandshakeException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

abstract public class RepoUpdater {

    public static final int PROGRESS_TYPE_DOWNLOAD     = 1;
    public static final int PROGRESS_TYPE_PROCESS_XML  = 2;
    public static final String PROGRESS_DATA_REPO      = "repo";

    public static RepoUpdater createUpdaterFor(Context ctx, DB.Repo repo) {
        if (repo.pubkey != null) {
            return new SignedRepoUpdater(ctx, repo);
        } else {
            return new UnsignedRepoUpdater(ctx, repo);
        }
    }

    protected final Context context;
    protected final DB.Repo repo;
    protected final List<DB.App> apps = new ArrayList<DB.App>();
    protected boolean hasChanged = false;
    protected ProgressListener progressListener;

    public RepoUpdater(Context ctx, DB.Repo repo) {
        this.context = ctx;
        this.repo    = repo;
    }

    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    public boolean hasChanged() {
        return hasChanged;
    }

    public List<DB.App> getApps() {
        return apps;
    }

    public boolean isInteractive() {
        return progressListener != null;
    }

    /**
     * For example, you may want to unzip a jar file to get the index inside,
     * or if the file is not compressed, you can just return a reference to
     * the downloaded file.
     *
     * @throws UpdateException All error states will come from here.
     */
    protected abstract File getIndexFromFile(File downloadedFile) throws
            UpdateException;

    protected abstract String getIndexAddress();

    protected Downloader downloadIndex() throws UpdateException {
        Bundle progressData = createProgressData(repo.address);
        Downloader downloader = null;
        try {
            downloader = new Downloader(getIndexAddress(), context);
            downloader.setETag(repo.lastetag);

            if (isInteractive()) {
                ProgressListener.Event event =
                    new ProgressListener.Event(
                        RepoUpdater.PROGRESS_TYPE_DOWNLOAD, progressData);
                downloader.setProgressListener(progressListener, event);
            }

            int status = downloader.download();

            if (status == 304) {
                // The index is unchanged since we last read it. We just mark
                // everything that came from this repo as being updated.
                Log.d("FDroid", "Repo index for " + repo.address
                        + " is up to date (by etag)");
            } else if (status == 200) {
                // Nothing needed to be done here...
            } else {
                // Is there any code other than 200 which still returns
                // content? Just in case, lets try to clean up.
                if (downloader.getFile() != null) {
                    downloader.getFile().delete();
                }
                throw new UpdateException(
                        repo,
                        "Failed to update repo " + repo.address +
                        " - HTTP response " + status);
            }
        } catch (SSLHandshakeException e) {
            throw new UpdateException(
                    repo,
                    "A problem occurred while establishing an SSL " +
                    "connection. If this problem persists, AND you have a " +
                    "very old device, you could try using http instead of " +
                    "https for the repo URL.",
                    e );
        } catch (IOException e) {
            if (downloader != null && downloader.getFile() != null) {
                downloader.getFile().delete();
            }
            throw new UpdateException(
                    repo,
                    "Error getting index file from " + repo.address,
                    e);
        }
        return downloader;
    }

    public static Bundle createProgressData(String repoAddress) {
        Bundle data = new Bundle();
        data.putString(PROGRESS_DATA_REPO, repoAddress);
        return data;
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
                repo.lastetag = downloader.getETag();

                indexFile = getIndexFromFile(downloadedFile);

                // Process the index...
                SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
                XMLReader reader = parser.getXMLReader();
                RepoXMLHandler handler = new RepoXMLHandler(repo, apps, progressListener);

                if (isInteractive()) {
                    // Only bother spending the time to count the expected apps
                    // if we can show that to the user...
                    handler.setTotalAppCount(estimateAppCount(indexFile));
                }

                reader.setContentHandler(handler);
                InputSource is = new InputSource(
                        new BufferedReader(new FileReader(indexFile)));

                reader.parse(is);
                updateRepo(handler.getPubKey(), handler.getMaxAge());
            }
        } catch (SAXException e) {
            throw new UpdateException(
                    repo, "Error parsing index for repo " + repo.address, e);
        } catch (FileNotFoundException e) {
            throw new UpdateException(
                    repo, "Error parsing index for repo " + repo.address, e);
        } catch (ParserConfigurationException e) {
            throw new UpdateException(
                    repo, "Error parsing index for repo " + repo.address, e);
        } catch (IOException e) {
            throw new UpdateException(
                    repo, "Error parsing index for repo " + repo.address, e);
        } finally {
            if (downloadedFile != null &&
                    downloadedFile != indexFile && downloadedFile.exists()) {
                downloadedFile.delete();
            }
            if (indexFile != null && indexFile.exists()) {
                indexFile.delete();
            }

        }
    }

    private void updateRepo(String publicKey, int maxAge) {
        boolean changed = false;

        // We read an unsigned index, but that indicates that
        // a signed version is now available...
        if (publicKey != null && repo.pubkey == null) {
            changed = true;
            // TODO: Spend the time *now* going to get the etag of the signed
            // repo, so that we can prevent downloading it next time. Otherwise
            // next time we update, we have to download the signed index
            // in its entirety, regardless of if it contains the same
            // information as the unsigned one does not...
            Log.d("FDroid", "Public key found - switching to signed repo " +
                    "for future updates");
            repo.pubkey = publicKey;
        }

        if (repo.maxage != maxAge) {
            changed = true;
            repo.maxage = maxAge;
        }

        if (changed) {
            try {
                DB db = DB.getDB();
                db.updateRepoByAddress(repo);
            } finally {
                DB.releaseDB();
            }
        }
    }

    public static class UpdateException extends Exception {

        public final DB.Repo repo;

        public UpdateException(DB.Repo repo, String message) {
            super(message);
            this.repo = repo;
        }

        public UpdateException(DB.Repo repo, String message, Exception cause) {
            super(message, cause);
            this.repo = repo;
        }
    }

}

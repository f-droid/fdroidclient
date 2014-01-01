package org.fdroid.fdroid.updater;

import android.content.Context;
import android.util.Log;
import org.fdroid.fdroid.DB;
import org.fdroid.fdroid.net.Downloader;

import java.io.File;

public class UnsignedRepoUpdater extends RepoUpdater {

    public UnsignedRepoUpdater(Context ctx, DB.Repo repo) {
        super(ctx, repo);
    }

    @Override
    protected File getIndexFile() throws UpdateException {
        Log.d("FDroid", "Getting unsigned index from " + getIndexAddress());
        Downloader downloader = downloadIndex();
        return downloader.getFile();
    }

    @Override
    protected String getIndexAddress() {
        return repo.address + "/index.xml";
    }
}

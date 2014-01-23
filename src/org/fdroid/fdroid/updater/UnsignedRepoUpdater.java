package org.fdroid.fdroid.updater;

import android.content.Context;
import android.util.Log;
import org.fdroid.fdroid.DB;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.net.Downloader;

import java.io.File;

public class UnsignedRepoUpdater extends RepoUpdater {

    public UnsignedRepoUpdater(Context ctx, Repo repo) {
        super(ctx, repo);
    }

    @Override
    protected File getIndexFromFile(File file) throws UpdateException {
        Log.d("FDroid", "Getting unsigned index from " + getIndexAddress());
        return file;
    }

    @Override
    protected String getIndexAddress() {
        return repo.address + "/index.xml";
    }
}

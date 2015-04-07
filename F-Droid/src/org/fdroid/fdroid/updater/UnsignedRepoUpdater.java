package org.fdroid.fdroid.updater;

import android.content.Context;
import android.util.Log;

import org.fdroid.fdroid.data.Repo;

import java.io.File;

public class UnsignedRepoUpdater extends RepoUpdater {

    private static final String TAG = "fdroid.UnsignedRepoUpdater";

    public UnsignedRepoUpdater(Context ctx, Repo repo) {
        super(ctx, repo);
    }

    @Override
    protected File getIndexFromFile(File file) throws UpdateException {
        Log.d(TAG, "Getting unsigned index from " + getIndexAddress());
        return file;
    }

    @Override
    protected String getIndexAddress() {
        return repo.address + "/index.xml";
    }
}

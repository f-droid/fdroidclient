package org.fdroid.fdroid.nearby;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

/**
 * Handles setting up and generating the local repo used to swap apps, including
 * the {@code index.jar}, the symlinks to the shared APKs, etc.
 * <p/>
 * The work is done in a {@link Thread} so that new incoming {@code Intents}
 * are not blocked by processing. A new {@code Intent} immediately nullifies
 * the current state because it means the user has chosen a different set of
 * apps.  That is also enforced here since new {@code Intent}s with the same
 * {@link Set} of apps as the current one are ignored.  Having the
 * {@code Thread} also makes it easy to kill work that is in progress.
 */
public class LocalRepoService extends IntentService {
    public static final String TAG = "LocalRepoService";

    public static final String ACTION_CREATE = "org.fdroid.fdroid.nearby.action.CREATE";
    public static final String EXTRA_PACKAGE_NAMES = "org.fdroid.fdroid.nearby.extra.PACKAGE_NAMES";

    public static final String ACTION_STATUS = "localRepoStatusAction";
    public static final String EXTRA_STATUS = "localRepoStatusExtra";
    public static final int STATUS_STARTED = 0;
    public static final int STATUS_PROGRESS = 1;
    public static final int STATUS_ERROR = 2;

    private String[] currentlyProcessedApps = new String[0];

    private GenerateLocalRepoThread thread;

    public LocalRepoService() {
        super("LocalRepoService");
    }

    /**
     * Creates a skeleton swap repo with only F-Droid itself in it
     */
    public static void create(Context context) {
        create(context, Collections.singleton(context.getPackageName()));
    }

    /**
     * Sets up the local repo with the included {@code packageNames}
     */
    public static void create(Context context, Set<String> packageNames) {
        Intent intent = new Intent(context, LocalRepoService.class);
        intent.setAction(ACTION_CREATE);
        intent.putExtra(EXTRA_PACKAGE_NAMES, packageNames.toArray(new String[0]));
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
        String[] packageNames = intent.getStringArrayExtra(EXTRA_PACKAGE_NAMES);
        if (packageNames == null || packageNames.length == 0) {
            Utils.debugLog(TAG, "no packageNames found, quitting");
            return;
        }
        Arrays.sort(packageNames);

        if (Arrays.equals(currentlyProcessedApps, packageNames)) {
            Utils.debugLog(TAG, "packageNames list unchanged, quitting");
            return;
        }
        currentlyProcessedApps = packageNames;

        if (thread != null) {
            thread.interrupt();
        }
        thread = new GenerateLocalRepoThread();
        thread.start();
    }

    private class GenerateLocalRepoThread extends Thread {
        private static final String TAG = "GenerateLocalRepoThread";

        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_LOWEST);
            runProcess(LocalRepoService.this, currentlyProcessedApps);
        }
    }

    public static void runProcess(Context context, String[] selectedApps) {
        try {
            final LocalRepoManager lrm = LocalRepoManager.get(context);
            broadcast(context, STATUS_PROGRESS, R.string.deleting_repo);
            lrm.deleteRepo();
            broadcast(context, STATUS_PROGRESS, R.string.linking_apks);
            String urlString = Utils.getSharingUri(FDroidApp.repo).toString();
            lrm.generateIndex(urlString, selectedApps);
            broadcast(context, STATUS_STARTED, null);
        } catch (Exception e) {
            broadcast(context, STATUS_ERROR, e.getLocalizedMessage());
            Log.e(TAG, "Error creating repo", e);
        }
    }

    /**
     * Translate Android style broadcast {@link Intent}s to {@code PrepareSwapRepo}
     */
    static void broadcast(Context context, int status, String message) {
        Intent intent = new Intent(ACTION_STATUS);
        intent.putExtra(EXTRA_STATUS, status);
        if (message != null) {
            intent.putExtra(Intent.EXTRA_TEXT, message);
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    static void broadcast(Context context, int status, int resId) {
        broadcast(context, status, context.getString(resId));
    }
}

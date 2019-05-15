package org.fdroid.fdroid.localrepo;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.support.v4.content.LocalBroadcastManager;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.views.swap.SwapWorkflowActivity;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.fdroid.fdroid.views.swap.SwapWorkflowActivity.PrepareSwapRepo.EXTRA_TYPE;
import static org.fdroid.fdroid.views.swap.SwapWorkflowActivity.PrepareSwapRepo.TYPE_COMPLETE;
import static org.fdroid.fdroid.views.swap.SwapWorkflowActivity.PrepareSwapRepo.TYPE_ERROR;
import static org.fdroid.fdroid.views.swap.SwapWorkflowActivity.PrepareSwapRepo.TYPE_STATUS;

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

    public static final String ACTION_PROGRESS = "org.fdroid.fdroid.localrepo.LocalRepoService.action.PROGRESS";
    public static final String ACTION_COMPLETE = "org.fdroid.fdroid.localrepo.LocalRepoService.action.COMPLETE";
    public static final String ACTION_ERROR = "org.fdroid.fdroid.localrepo.LocalRepoService.action.ERROR";

    public static final String EXTRA_MESSAGE = "org.fdroid.fdroid.localrepo.LocalRepoService.extra.MESSAGE";

    public static final String ACTION_CREATE = "org.fdroid.fdroid.localrepo.action.CREATE";
    public static final String EXTRA_PACKAGE_NAMES = "org.fdroid.fdroid.localrepo.extra.PACKAGE_NAMES";

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
            Utils.debugLog(TAG, "no packageNames found, quiting");
            return;
        }
        Arrays.sort(packageNames);

        if (Arrays.equals(currentlyProcessedApps, packageNames)) {
            Utils.debugLog(TAG, "packageNames list unchanged, quiting");
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
            broadcast(context, ACTION_PROGRESS, R.string.deleting_repo);
            lrm.deleteRepo();
            for (String app : selectedApps) {
                broadcast(context, ACTION_PROGRESS, context.getString(R.string.adding_apks_format, app));
                lrm.addApp(context, app);
            }
            String urlString = Utils.getSharingUri(FDroidApp.repo).toString();
            lrm.writeIndexPage(urlString);
            broadcast(context, ACTION_PROGRESS, R.string.writing_index_jar);
            lrm.writeIndexJar();
            broadcast(context, ACTION_PROGRESS, R.string.linking_apks);
            lrm.copyApksToRepo();
            broadcast(context, ACTION_PROGRESS, R.string.copying_icons);
            // run the icon copy without progress, its not a blocker
            new Thread() {
                @Override
                public void run() {
                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_LOWEST);
                    lrm.copyIconsToRepo();
                }
            }.start();

            broadcast(context, ACTION_COMPLETE, null);
        } catch (IOException | XmlPullParserException | LocalRepoKeyStore.InitException e) {
            broadcast(context, ACTION_ERROR, e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    /**
     * Translate Android style broadcast {@link Intent}s to {@code PrepareSwapRepo}
     */
    static void broadcast(Context context, String action, String message) {
        Intent intent = new Intent(context, SwapWorkflowActivity.class);
        intent.setAction(SwapWorkflowActivity.PrepareSwapRepo.ACTION);
        switch (action) {
            case ACTION_PROGRESS:
                intent.putExtra(EXTRA_TYPE, TYPE_STATUS);
                break;
            case ACTION_COMPLETE:
                intent.putExtra(EXTRA_TYPE, TYPE_COMPLETE);
                break;
            case ACTION_ERROR:
                intent.putExtra(EXTRA_TYPE, TYPE_ERROR);
                break;
            default:
                throw new IllegalArgumentException("unsupported action");
        }
        if (message != null) {
            Utils.debugLog(TAG, "Preparing swap: " + message);
            intent.putExtra(EXTRA_MESSAGE, message);
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    static void broadcast(Context context, String action, int resId) {
        broadcast(context, action, context.getString(resId));
    }
}

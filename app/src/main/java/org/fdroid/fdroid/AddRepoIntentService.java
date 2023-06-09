package org.fdroid.fdroid;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.fdroid.database.Repository;
import org.fdroid.download.Mirror;
import org.fdroid.fdroid.views.ManageReposActivity;
import org.fdroid.fdroid.views.main.MainActivity;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/**
 * Handles requests to add new repos via URLs.  This is an {@code IntentService}
 * so that requests are queued, which is necessary when either
 * {@link org.fdroid.fdroid.nearby.TreeUriScannerIntentService} or
 * {@link org.fdroid.fdroid.nearby.SDCardScannerService} finds multiple
 * repos on a disk.  This should hopefully also serve as the beginnings of
 * a new architecture for handling these requests.  This does all the
 * processing first, up front, then only launches UI as needed.
 * {@link org.fdroid.fdroid.views.ManageReposActivity} currently does the
 * opposite.
 * <p>
 * This only really properly queues {@link Intent}s that get filtered out. The
 * {@code Intent}s that go on to {@code ManageReposActivity} will not wait
 * until for that {@code AppCompatActivity} to be ready to handle the next.  So when
 * multiple mirrors are discovered at once, only one in that session will
 * likely be added.
 */
public class AddRepoIntentService extends IntentService {
    public static final String TAG = "AddRepoIntentService";

    private static final String ACTION_ADD_REPO = "org.fdroid.fdroid.action.ADD_REPO";

    public AddRepoIntentService() {
        super("AddRepoIntentService");
    }

    public static void addRepo(Context context, @NonNull Uri repoUri, @Nullable String fingerprint) {
        Intent intent = new Intent(context, AddRepoIntentService.class);
        intent.setAction(ACTION_ADD_REPO);
        if (TextUtils.isEmpty(fingerprint)) {
            intent.setData(repoUri);
        } else {
            intent.setData(repoUri.buildUpon()
                    .appendQueryParameter("fingerprint", fingerprint)
                    .build());
        }
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_LOWEST);
        if (intent == null || intent.getData() == null) {
            return;
        }

        Uri uri = intent.getData();
        String urlString;
        try {
            urlString = normalizeUrl(uri);
        } catch (URISyntaxException e) {
            Log.i(TAG, "Bad URI: " + e.getLocalizedMessage());
            return;
        }

        String fingerprint = uri.getQueryParameter("fingerprint");
        for (Repository repo : FDroidApp.getRepoManager(getApplicationContext()).getRepositories()) {
            if (repo.getEnabled() && TextUtils.equals(fingerprint, repo.getFingerprint())) {
                if (TextUtils.equals(urlString, repo.getAddress())) {
                    Utils.debugLog(TAG, urlString + " already added as a repo");
                    return;
                } else {
                    for (Mirror mirror : repo.getMirrors()) {
                        if (urlString.startsWith(mirror.getBaseUrl())) {
                            Utils.debugLog(TAG, urlString + " already added as a mirror");
                            return;
                        }
                    }
                }
            }
        }
        intent.putExtra(ManageReposActivity.EXTRA_FINISH_AFTER_ADDING_REPO, false);
        intent.setComponent(new ComponentName(this, MainActivity.class));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /**
     * Some basic sanitization of URLs, so that two URLs which have the same semantic meaning
     * are represented by the exact same string by F-Droid. This will help to make sure that,
     * e.g. "http://10.0.1.50" and "http://10.0.1.50/" are not two different repositories.
     * <p>
     * Currently it normalizes the path so that "/./" are removed and "test/../" is collapsed.
     * This is done using {@link URI#normalize()}. It also removes multiple consecutive forward
     * slashes in the path and replaces them with one. Finally, it removes trailing slashes.
     * <p>
     * {@code content://} URLs used for repos stored on removable storage get messed up by
     * {@link URI}.
     */
    public static String normalizeUrl(String urlString) throws URISyntaxException {
        if (TextUtils.isEmpty(urlString)) {
            throw new URISyntaxException("null", "Uri was empty");
        }
        return normalizeUrl(Uri.parse(urlString));
    }

    public static String normalizeUrl(Uri uri) throws URISyntaxException {
        if (!uri.isAbsolute()) {
            throw new URISyntaxException(uri.toString(), "Must provide an absolute URI for repositories");
        }
        if (!uri.isHierarchical()) {
            throw new URISyntaxException(uri.toString(), "Must provide an hierarchical URI for repositories");
        }
        if ("content".equals(uri.getScheme())) {
            return uri.toString();
        }
        String path = uri.getPath();
        if (path != null) {
            path = path.replaceAll("//*/", "/"); // Collapse multiple forward slashes into 1.
            if (path.length() > 0 && path.charAt(path.length() - 1) == '/') {
                path = path.substring(0, path.length() - 1);
            }
        }
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (TextUtils.isEmpty(scheme) || TextUtils.isEmpty(host)) {
            return uri.toString();
        }
        return new URI(scheme.toLowerCase(Locale.ENGLISH),
                uri.getUserInfo(),
                host.toLowerCase(Locale.ENGLISH),
                uri.getPort(),
                path,
                uri.getQuery(),
                uri.getFragment()).normalize().toString();
    }
}

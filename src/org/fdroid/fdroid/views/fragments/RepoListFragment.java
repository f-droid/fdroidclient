
package org.fdroid.fdroid.views.fragments;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.net.NsdHelper;
import org.fdroid.fdroid.net.NsdHelper.DiscoveredRepo;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.ProgressListener;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.net.NsdHelper.RepoScanListAdapter;
import org.fdroid.fdroid.UpdateService;
import org.fdroid.fdroid.compat.ClipboardCompat;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.views.RepoAdapter;
import org.fdroid.fdroid.views.RepoDetailsActivity;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Locale;

public class RepoListFragment extends ListFragment
        implements LoaderManager.LoaderCallbacks<Cursor>, RepoAdapter.EnabledListener {

    private static final String DEFAULT_NEW_REPO_TEXT = "https://";
    private final int ADD_REPO = 1;
    private final int UPDATE_REPOS = 2;
    private final int SCAN_FOR_REPOS = 3;

    private WifiManager wifiManager;

    public boolean hasChanged() {
        return changed;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Uri uri = RepoProvider.getContentUri();
        Log.i("FDroid", "Creating repo loader '" + uri + "'.");
        String[] projection = new String[] {
                RepoProvider.DataColumns._ID,
                RepoProvider.DataColumns.NAME,
                RepoProvider.DataColumns.PUBLIC_KEY,
                RepoProvider.DataColumns.FINGERPRINT,
                RepoProvider.DataColumns.IN_USE
        };
        return new CursorLoader(getActivity(), uri, projection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Log.i("FDroid", "Repo cursor loaded.");
        repoAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        Log.i("FDroid", "Repo cursor reset.");
        repoAdapter.swapCursor(null);
    }

    /**
     * NOTE: If somebody toggles a repo off then on again, it will have removed
     * all apps from the index when it was toggled off, so when it is toggled on
     * again, then it will require a refresh. Previously, I toyed with the idea
     * of remembering whether they had toggled on or off, and then only actually
     * performing the function when the activity stopped, but I think that will
     * be problematic. What about when they press the home button, or edit a
     * repos details? It will start to become somewhat-random as to when the
     * actual enabling, disabling is performed. So now, it just does the disable
     * as soon as the user clicks "Off" and then removes the apps. To compensate
     * for the removal of apps from index, it notifies the user via a toast that
     * the apps have been removed. Also, as before, it will still prompt the
     * user to update the repos if you toggled on on.
     */
    @Override
    public void onSetEnabled(Repo repo, boolean isEnabled) {
        if (repo.inuse != isEnabled) {
            ContentValues values = new ContentValues(1);
            values.put(RepoProvider.DataColumns.IN_USE, isEnabled ? 1 : 0);
            RepoProvider.Helper.update(getActivity(), repo, values);

            if (isEnabled) {
                changed = true;
            } else {
                FDroidApp app = (FDroidApp) getActivity().getApplication();
                RepoProvider.Helper.purgeApps(getActivity(), repo, app);
                String notification = getString(R.string.repo_disabled_notification, repo.name);
                Toast.makeText(getActivity(), notification, Toast.LENGTH_LONG).show();
            }
        }
    }

    private enum PositiveAction {
        ADD_NEW, ENABLE, IGNORE
    }

    private PositiveAction positiveAction;

    private boolean changed = false;

    private RepoAdapter repoAdapter;

    /**
     * True if activity started with an intent such as from QR code. False if
     * opened from, e.g. the main menu.
     */
    private boolean isImportingRepo = false;

    private View createHeaderView() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        TextView textLastUpdate = new TextView(getActivity());
        long lastUpdate = prefs.getLong(Preferences.PREF_UPD_LAST, 0);
        String lastUpdateCheck = "";
        if (lastUpdate == 0) {
            lastUpdateCheck = getString(R.string.never);
        } else {
            Date d = new Date(lastUpdate);
            lastUpdateCheck = DateFormat.getDateFormat(getActivity()).format(d) +
                    " " + DateFormat.getTimeFormat(getActivity()).format(d);
        }
        textLastUpdate.setText(getString(R.string.last_update_check, lastUpdateCheck));

        int sidePadding = (int) getResources().getDimension(R.dimen.padding_side);
        int topPadding = (int) getResources().getDimension(R.dimen.padding_top);

        textLastUpdate.setPadding(sidePadding, topPadding, sidePadding, topPadding);
        return textLastUpdate;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Can't do this in the onCreate view, because "onCreateView" which
        // returns the list view is "called between onCreate and
        // onActivityCreated" according to the docs.
        getListView().addHeaderView(createHeaderView(), null, false);

        // This could go in onCreate (and used to) but it needs to be called
        // after addHeaderView, which can only be called after onCreate...
        repoAdapter = new RepoAdapter(getActivity(), null);
        repoAdapter.setEnabledListener(this);
        setListAdapter(repoAdapter);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        /* let's see if someone is trying to send us a new repo */
        Intent intent = getActivity().getIntent();
        /* an URL from a click, NFC, QRCode scan, etc */
        Uri uri = intent.getData();
        if (uri != null) {
            // scheme and host should only ever be pure ASCII aka Locale.ENGLISH
            String scheme = intent.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) {
                String msg = String.format(getString(R.string.malformed_repo_uri), uri);
                Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
                return;
            }
            if (scheme.equals("FDROIDREPO") || scheme.equals("FDROIDREPOS")) {
                /*
                 * QRCodes are more efficient in all upper case, so QR URIs are
                 * encoded in all upper case, then forced to lower case.
                 * Checking if the special F-Droid scheme being all is upper
                 * case means it should be downcased.
                 */
                uri = Uri.parse(uri.toString().toLowerCase(Locale.ENGLISH));
            }
            // make scheme and host lowercase so they're readable in dialogs
            scheme = scheme.toLowerCase(Locale.ENGLISH);
            host = host.toLowerCase(Locale.ENGLISH);
            String fingerprint = uri.getQueryParameter("fingerprint");
            if (scheme.equals("fdroidrepos") || scheme.equals("fdroidrepo")
                    || scheme.equals("https") || scheme.equals("http")) {

                isImportingRepo = true;

                /* sanitize and format for function and readability */
                String uriString = uri.toString()
                        .replaceAll("\\?.*$", "") // remove the whole query
                        .replaceAll("/*$", "") // remove all trailing slashes
                        .replace(uri.getHost(), host) // downcase host name
                        .replace(intent.getScheme(), scheme) // downcase scheme
                        .replace("fdroidrepo", "http"); // proper repo address
                showAddRepo(uriString, fingerprint);

                // if this is a local repo, check we're on the same wifi
                String uriBssid = uri.getQueryParameter("bssid");
                if (!TextUtils.isEmpty(uriBssid)) {
                    if (uri.getPort() != 8888
                            && !host.matches("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+")) {
                        Log.i("ManageRepo", "URI is not local repo: " + uri);
                        return;
                    }
                    Activity a = getActivity();
                    if (wifiManager == null)
                        wifiManager = (WifiManager) a.getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    String bssid = wifiInfo.getBSSID().toLowerCase(Locale.ENGLISH);
                    uriBssid = Uri.decode(uriBssid).toLowerCase(Locale.ENGLISH);
                    if (!bssid.equals(uriBssid)) {
                        String msg = String.format(getString(R.string.not_on_same_wifi),
                                uri.getQueryParameter("ssid"));
                        Toast.makeText(a, msg, Toast.LENGTH_LONG).show();
                    }
                    // TODO we should help the user to the right thing here,
                    // instead of just showing a message!
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Starts a new or restarts an existing Loader in this manager
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {

        super.onListItemClick(l, v, position, id);

        Repo repo = new Repo((Cursor) getListView().getItemAtPosition(position));
        editRepo(repo);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        MenuItem updateItem = menu.add(Menu.NONE, UPDATE_REPOS, 1,
                R.string.menu_update_repo).setIcon(R.drawable.ic_menu_refresh);
        MenuItemCompat.setShowAsAction(updateItem,
                MenuItemCompat.SHOW_AS_ACTION_ALWAYS |
                        MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT);

        MenuItem addItem = menu.add(Menu.NONE, ADD_REPO, 1, R.string.menu_add_repo).setIcon(
                android.R.drawable.ic_menu_add);
        MenuItemCompat.setShowAsAction(addItem,
                MenuItemCompat.SHOW_AS_ACTION_ALWAYS |
                        MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT);

        if (Build.VERSION.SDK_INT >= 16)
        {
            menu.add(Menu.NONE, SCAN_FOR_REPOS, 1, R.string.menu_scan_repo).setIcon(
                    android.R.drawable.ic_menu_search);
        }
    }

    public static final int SHOW_REPO_DETAILS = 1;

    public void editRepo(Repo repo) {
        Intent intent = new Intent(getActivity(), RepoDetailsActivity.class);
        intent.putExtra(RepoDetailsFragment.ARG_REPO_ID, repo.getId());
        startActivityForResult(intent, SHOW_REPO_DETAILS);
    }

    private void updateRepos() {
        UpdateService.updateNow(getActivity()).setListener(new ProgressListener() {
            @Override
            public void onProgress(Event event) {
                if (event.type == UpdateService.STATUS_COMPLETE_AND_SAME ||
                        event.type == UpdateService.STATUS_COMPLETE_WITH_CHANGES) {
                    // No need to prompt to update any more, we just did it!
                    changed = false;
                }
            }
        });
    }

    @TargetApi(16) // AKA Android 4.1 AKA Jelly Bean
    private void scanForRepos() {
        final Activity a = getActivity();

        final RepoScanListAdapter adapter = new RepoScanListAdapter(a);
        final NsdHelper nsdHelper = new NsdHelper(a.getApplicationContext(), adapter);

        final View view = getLayoutInflater(null).inflate(R.layout.repodiscoverylist, null);
        final ListView repoScanList = (ListView) view.findViewById(R.id.reposcanlist);

        final AlertDialog alrt = new AlertDialog.Builder(getActivity()).setView(view)
                                 .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        nsdHelper.stopDiscovery();
                                        dialog.dismiss();
                                    }
                                }).create();

        alrt.setTitle(R.string.local_repos_title);
        alrt.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                nsdHelper.stopDiscovery();
            }
        });

        repoScanList.setAdapter(adapter);
        repoScanList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
          @Override
          public void onItemClick(AdapterView<?> parent, final View view,
              int position, long id) {

            final DiscoveredRepo discoveredService =
                    (DiscoveredRepo) parent.getItemAtPosition(position);

            final NsdServiceInfo serviceInfo = discoveredService.getServiceInfo();

            String serviceType = serviceInfo.getServiceType();
            String protocol = serviceType.contains("fdroidrepos") ? "https://" : "http://";

            String serviceAddress = protocol + serviceInfo.getHost().getHostAddress()
                                    + ":" + serviceInfo.getPort() + "/fdroid/repo";
            showAddRepo(serviceAddress, "");
          }
        });

        alrt.show();

        Log.d("FDroid", "Starting network service discovery");
        nsdHelper.discoverServices();
    }

    private void showAddRepo() {
        showAddRepo(getNewRepoUri(), null);
    }

    private void showAddRepo(String newAddress, String newFingerprint) {
        View view = getLayoutInflater(null).inflate(R.layout.addrepo, null);
        final AlertDialog alrt = new AlertDialog.Builder(getActivity()).setView(view).create();
        final EditText uriEditText = (EditText) view.findViewById(R.id.edit_uri);
        final EditText fingerprintEditText = (EditText) view.findViewById(R.id.edit_fingerprint);

        final Repo repo = (newAddress != null && isImportingRepo)
                ? RepoProvider.Helper.findByAddress(getActivity(), newAddress)
                : null;

        alrt.setIcon(android.R.drawable.ic_menu_add);
        alrt.setTitle(getString(R.string.repo_add_title));
        alrt.setButton(DialogInterface.BUTTON_POSITIVE,
                getString(R.string.repo_add_add),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        String fp = fingerprintEditText.getText().toString();

                        // the DB uses null for no fingerprint but the above
                        // code returns "" rather than null if its blank
                        if (fp.equals(""))
                            fp = null;

                        if (positiveAction == PositiveAction.ADD_NEW)
                            createNewRepo(uriEditText.getText().toString(), fp);
                        else if (positiveAction == PositiveAction.ENABLE)
                            createNewRepo(repo);
                    }
                });

        alrt.setButton(DialogInterface.BUTTON_NEGATIVE,
                getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getActivity().setResult(Activity.RESULT_CANCELED);
                        getActivity().finish();
                        return;
                    }
                });
        alrt.show();

        final TextView overwriteMessage = (TextView) view.findViewById(R.id.overwrite_message);
        overwriteMessage.setVisibility(View.GONE);
        if (repo == null) {
            // no existing repo, add based on what we have
            positiveAction = PositiveAction.ADD_NEW;
        } else {
            // found the address in the DB of existing repos
            final Button addButton = alrt.getButton(DialogInterface.BUTTON_POSITIVE);
            alrt.setTitle(R.string.repo_exists);
            overwriteMessage.setVisibility(View.VISIBLE);
            newFingerprint = newFingerprint.toUpperCase(Locale.ENGLISH);
            if (repo.fingerprint == null && newFingerprint != null) {
                // we're upgrading from unsigned to signed repo
                overwriteMessage.setText(R.string.repo_exists_add_fingerprint);
                addButton.setText(R.string.add_key);
                positiveAction = PositiveAction.ADD_NEW;
            } else if (newFingerprint == null || newFingerprint.equals(repo.fingerprint)) {
                // this entry already exists and is not enabled, offer to enable
                // it
                if (repo.inuse) {
                    alrt.dismiss();
                    Toast.makeText(getActivity(), R.string.repo_exists_and_enabled,
                            Toast.LENGTH_LONG).show();
                    return;
                } else {
                    overwriteMessage.setText(R.string.repo_exists_enable);
                    addButton.setText(R.string.enable);
                    positiveAction = PositiveAction.ENABLE;
                }
            } else {
                // same address with different fingerprint, this could be
                // malicious, so force the user to manually delete the repo
                // before adding this one
                overwriteMessage.setTextColor(getResources().getColor(R.color.red));
                overwriteMessage.setText(R.string.repo_delete_to_overwrite);
                addButton.setText(R.string.overwrite);
                addButton.setEnabled(false);
                positiveAction = PositiveAction.IGNORE;
            }
        }

        if (newFingerprint != null)
            fingerprintEditText.setText(newFingerprint);

        if (newAddress != null) {
            // This trick of emptying text then appending,
            // rather than just setting in the first place,
            // is neccesary to move the cursor to the end of the input.
            uriEditText.setText("");
            uriEditText.append(newAddress);
        }
    }

    /**
     * Adds a new repo to the database.
     */
    private void createNewRepo(String address, String fingerprint) {
        ContentValues values = new ContentValues(2);
        values.put(RepoProvider.DataColumns.ADDRESS, address);
        if (fingerprint != null && fingerprint.length() > 0) {
            values.put(RepoProvider.DataColumns.FINGERPRINT, fingerprint.toUpperCase(Locale.ENGLISH));
        }
        RepoProvider.Helper.insert(getActivity(), values);
        finishedAddingRepo();
    }

    /**
     * Seeing as this repo already exists, we will force it to be enabled again.
     */
    private void createNewRepo(Repo repo) {
        ContentValues values = new ContentValues(1);
        values.put(RepoProvider.DataColumns.IN_USE, 1);
        RepoProvider.Helper.update(getActivity(), repo, values);
        repo.inuse = true;
        finishedAddingRepo();
    }

    /**
     * If started by an intent that expects a result (e.g. QR codes) then we
     * will set a result and finish. Otherwise, we'll refresh the list of repos
     * to reflect the newly created repo.
     */
    private void finishedAddingRepo() {
        changed = true;
        if (isImportingRepo) {
            getActivity().setResult(Activity.RESULT_OK);
            getActivity().finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == ADD_REPO) {
            showAddRepo();
            return true;
        } else if (item.getItemId() == UPDATE_REPOS) {
            updateRepos();
            return true;
        } else if (item.getItemId() == SCAN_FOR_REPOS) {
            scanForRepos();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * If there is text in the clipboard, and it looks like a URL, use that.
     * Otherwise return "https://".
     */
    private String getNewRepoUri() {
        ClipboardCompat clipboard = ClipboardCompat.create(getActivity());
        String text = clipboard.getText();
        if (text != null) {
            try {
                new URL(text);
            } catch (MalformedURLException e) {
                text = null;
            }
        }

        if (text == null) {
            text = DEFAULT_NEW_REPO_TEXT;
        }
        return text;
    }
}

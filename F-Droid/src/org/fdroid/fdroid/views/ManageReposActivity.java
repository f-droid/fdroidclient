/*
 * Copyright (C) 2010-12  Ciaran Gultnieks, ciaran@ciarang.com
 * Copyright (C) 2009  Roberto Jacinto, roberto.jacinto@caixamagica.pt
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.fdroid.fdroid.views;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NavUtils;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.DefaultHttpClient;
import org.fdroid.fdroid.FDroid;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.ProgressListener;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.UpdateService;
import org.fdroid.fdroid.compat.ClipboardCompat;
import org.fdroid.fdroid.data.NewRepoConfig;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.net.MDnsHelper;
import org.fdroid.fdroid.net.MDnsHelper.DiscoveredRepo;
import org.fdroid.fdroid.net.MDnsHelper.RepoScanListAdapter;
import org.fdroid.fdroid.views.fragments.RepoDetailsFragment;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Locale;

import javax.jmdns.ServiceInfo;

public class ManageReposActivity extends ActionBarActivity {

    /**
     * If we have a new repo added, or the address of a repo has changed, then
     * we when we're finished, we'll set this boolean to true in the intent that
     * we finish with, to signify that we want the main list of apps updated.
     */
    public static final String REQUEST_UPDATE = "update";
    private static final String TAG = "fdroid.ManageReposActivity";

    private static final String DEFAULT_NEW_REPO_TEXT = "https://";

    private enum PositiveAction {
        ADD_NEW, ENABLE, IGNORE
    }

    private PositiveAction positiveAction;

    private UpdateService.UpdateReceiver updateHandler = null;

    private static boolean changed = false;

    /**
     * True if activity started with an intent such as from QR code. False if
     * opened from, e.g. the main menu.
     */
    private boolean isImportingRepo = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        ((FDroidApp) getApplication()).applyTheme(this);
        super.onCreate(savedInstanceState);

        FragmentManager fm = getSupportFragmentManager();
        if (fm.findFragmentById(android.R.id.content) == null) {
            /*
             * Need to set a dummy view (which will get overridden by the
             * fragment manager below) so that we can call setContentView().
             * This is a work around for a (bug?) thing in 3.0, 3.1 which
             * requires setContentView to be invoked before the actionbar is
             * played with:
             * http://blog.perpetumdesign.com/2011/08/strange-case-of
             * -dr-action-and-mr-bar.html
             */
            if (Build.VERSION.SDK_INT >= 11 && Build.VERSION.SDK_INT <= 13) {
                setContentView(new LinearLayout(this));
            }

            fm.beginTransaction()
                    .add(android.R.id.content, new RepoListFragment())
                    .commit();
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // title is "Repositories" here, but "F-Droid" in VIEW Intent chooser
        getSupportActionBar().setTitle(R.string.menu_manage);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (updateHandler != null) {
            updateHandler.showDialog(this);
        }
        /* let's see if someone is trying to send us a new repo */
        addRepoFromIntent(getIntent());
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (updateHandler != null) {
            updateHandler.hideDialog();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    @Override
    public void finish() {
        Intent ret = new Intent();
        markChangedIfRequired(ret);
        setResult(RESULT_OK, ret);
        super.finish();
    }

    private boolean hasChanged() {
        return changed;
    }

    private void markChangedIfRequired(Intent intent) {
        if (hasChanged()) {
            Log.i(TAG, "Repo details have changed, prompting for update.");
            intent.putExtra(REQUEST_UPDATE, true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.manage_repos, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            Intent destIntent = new Intent(this, FDroid.class);
            markChangedIfRequired(destIntent);
            setResult(RESULT_OK, destIntent);
            NavUtils.navigateUpTo(this, destIntent);
            return true;
        case R.id.action_add_repo:
            showAddRepo();
            return true;
        case R.id.action_update_repo:
            updateRepos();
            return true;
        case R.id.action_find_local_repos:
            scanForRepos();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateRepos() {
        updateHandler = UpdateService.updateNow(this).setListener(
                new ProgressListener() {
                    @Override
                    public void onProgress(Event event) {
                        switch (event.type) {
                        case UpdateService.EVENT_COMPLETE_AND_SAME:
                        case UpdateService.EVENT_COMPLETE_WITH_CHANGES:
                            // No need to prompt to update any more, we just
                            // did it!
                            changed = false;
                            break;
                        case UpdateService.EVENT_FINISHED:
                            updateHandler = null;
                            break;
                        }
                    }
                });
    }

    private void scanForRepos() {
        final RepoScanListAdapter adapter = new RepoScanListAdapter(this);
        final MDnsHelper mDnsHelper = new MDnsHelper(this, adapter);

        final View view = getLayoutInflater().inflate(R.layout.repodiscoverylist, null);
        final ListView repoScanList = (ListView) view.findViewById(R.id.reposcanlist);

        final AlertDialog alrt = new AlertDialog.Builder(this).setView(view)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDnsHelper.stopDiscovery();
                        dialog.dismiss();
                    }
                }).create();

        alrt.setTitle(R.string.local_repos_title);
        alrt.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mDnsHelper.stopDiscovery();
            }
        });

        repoScanList.setAdapter(adapter);
        repoScanList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view,
                    int position, long id) {

                final DiscoveredRepo discoveredService =
                        (DiscoveredRepo) parent.getItemAtPosition(position);

                final ServiceInfo serviceInfo = discoveredService.getServiceInfo();
                String type = serviceInfo.getPropertyString("type");
                String protocol = type.contains("fdroidrepos") ? "https:/" : "http:/";
                String path = serviceInfo.getPropertyString("path");
                if (TextUtils.isEmpty(path))
                    path = "/fdroid/repo";
                String serviceUrl = protocol + serviceInfo.getInetAddresses()[0]
                        + ":" + serviceInfo.getPort() + path;
                showAddRepo(serviceUrl, serviceInfo.getPropertyString("fingerprint"));
            }
        });

        alrt.show();
        mDnsHelper.discoverServices();
    }

    private void showAddRepo() {
        /*
         * If there is text in the clipboard, and it looks like a URL, use that.
         * Otherwise use "https://" as default repo string.
         */
        ClipboardCompat clipboard = ClipboardCompat.create(this);
        String text = clipboard.getText();
        String fingerprint = null;
        if (!TextUtils.isEmpty(text)) {
            try {
                new URL(text);
                Uri uri = Uri.parse(text);
                fingerprint = uri.getQueryParameter("fingerprint");
                // uri might contain a QR-style, all uppercase URL:
                if (TextUtils.isEmpty(fingerprint))
                    fingerprint = uri.getQueryParameter("FINGERPRINT");
                text = NewRepoConfig.sanitizeRepoUri(uri);
            } catch (MalformedURLException e) {
                text = null;
            }
        }

        if (TextUtils.isEmpty(text)) {
            text = DEFAULT_NEW_REPO_TEXT;
        }
        showAddRepo(text, fingerprint);
    }

    private void showAddRepo(String newAddress, String newFingerprint) {
        new AddRepo(newAddress, newFingerprint);
    }

    /**
     * Utility class to encapsulate the process of adding a new repo (or an existing one,
     * depending on if the incoming address is the same as a previous repo). It is responsible
     * for managing the lifecycle of adding a repo:
     *  * Showing the add dialog
     *  * Deciding whether to add a new repo or update an existing one
     *  * Search for repos at common suffixes (/, /fdroid/repo, /repo)
     */
    private class AddRepo {

        private final Context context;
        private final AlertDialog addRepoDialog;

        public AddRepo(String newAddress, String newFingerprint) {

            context = ManageReposActivity.this;

            final View view = getLayoutInflater().inflate(R.layout.addrepo, null);
            addRepoDialog = new AlertDialog.Builder(context).setView(view).create();
            final EditText uriEditText = (EditText) view.findViewById(R.id.edit_uri);
            final EditText fingerprintEditText = (EditText) view.findViewById(R.id.edit_fingerprint);

            // If the "add new repo" dialog is launched by an action outside of
            // FDroid, i.e. a URL, then check to see if any existing repos match,
            // and change the action accordingly.
            final Repo repo = (newAddress != null && isImportingRepo)
                    ? RepoProvider.Helper.findByAddress(context, newAddress)
                    : null;

            addRepoDialog.setIcon(android.R.drawable.ic_menu_add);
            addRepoDialog.setTitle(getString(R.string.repo_add_title));

            addRepoDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

            // HACK:
            // After adding a new repo, need to show feedback to the user.
            // This could use either a fresh dialog with some status messages,
            // or modify the existing one. Either way is hard with the default API.
            // A fresh dialog is impossible until after the dialog is dismissed,
            // which happens after calling our OnClickListener. Thus we'd have to
            // remember which button was pressed, wait for the dialog to be dismissed,
            // then create a new one.
            // Editing the existing dialog is preferable, but the dialog is dismissed
            // after our onclick listener. We don't want this, we want the dialog to
            // hang around so we can show further info on it.
            //
            // Thus, the hack described at http://stackoverflow.com/a/15619098 is implemented.
            addRepoDialog.setButton(DialogInterface.BUTTON_POSITIVE,
                getString(R.string.repo_add_add),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

            addRepoDialog.show();

            // This must be *after* addRepoDialog.show() otherwise getButtion() returns null:
            // https://code.google.com/p/android/issues/detail?id=6360
            addRepoDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        String fp = fingerprintEditText.getText().toString();

                        // the DB uses null for no fingerprint but the above
                        // code returns "" rather than null if its blank
                        if (fp.equals(""))
                            fp = null;

                        if (positiveAction == PositiveAction.ADD_NEW)
                            prepareToCreateNewRepo(uriEditText.getText().toString(), fp);
                        else if (positiveAction == PositiveAction.ENABLE) {
                            enableExistingRepo(repo);
                            finishedAddingRepo();
                        }
                    }
                }
            );

            final TextView overwriteMessage = (TextView) view.findViewById(R.id.overwrite_message);
            overwriteMessage.setVisibility(View.GONE);
            if (repo == null) {
                // no existing repo, add based on what we have
                positiveAction = PositiveAction.ADD_NEW;
            } else {
                // found the address in the DB of existing repos
                final Button addButton = addRepoDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                addRepoDialog.setTitle(R.string.repo_exists);
                overwriteMessage.setVisibility(View.VISIBLE);
                if (newFingerprint != null)
                    newFingerprint = newFingerprint.toUpperCase(Locale.ENGLISH);
                if (repo.fingerprint == null && newFingerprint != null) {
                    // we're upgrading from unsigned to signed repo
                    overwriteMessage.setText(R.string.repo_exists_add_fingerprint);
                    addButton.setText(R.string.add_key);
                    positiveAction = PositiveAction.ADD_NEW;
                } else if (newFingerprint == null || newFingerprint.equals(repo.fingerprint)) {
                    // this entry already exists and is not enabled, offer to enable it
                    if (repo.inuse) {
                        addRepoDialog.dismiss();
                        Toast.makeText(context, R.string.repo_exists_and_enabled, Toast.LENGTH_LONG).show();
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
                // This trick of emptying text then appending, rather than just setting in
                // the first place, is necessary to move the cursor to the end of the input.
                uriEditText.setText("");
                uriEditText.append(newAddress);
            }
        }

        /**
         * Adds a new repo to the database.
         */
        private void prepareToCreateNewRepo(final String originalAddress, final String fingerprint) {

            addRepoDialog.findViewById(R.id.add_repo_form).setVisibility(View.GONE);
            addRepoDialog.getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.GONE);

            final TextView textSearching = (TextView) addRepoDialog.findViewById(R.id.text_searching_for_repo);
            textSearching.setText(getString(R.string.repo_searching_address, originalAddress));

            final AsyncTask<String, String, String> checker = new AsyncTask<String, String, String>() {

                @Override
                protected String doInBackground(String... params) {

                    final String originalAddress = params[0];
                    final String[] pathsToCheck = {"", "fdroid/repo", "repo"};
                    for (final String path : pathsToCheck) {

                        Log.d(TAG, "Checking for repo at " + originalAddress + " with suffix \"" + path + "\".");
                        Uri.Builder builder = Uri.parse(originalAddress).buildUpon().appendEncodedPath(path);
                        final String addressWithoutIndex = builder.build().toString();
                        publishProgress(addressWithoutIndex);

                        final Uri uri = builder.appendPath("index.jar").build();

                        try {
                            if (checkForRepository(uri)) {
                                Log.i(TAG, "Found F-Droid repo at " + addressWithoutIndex);
                                return addressWithoutIndex;
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Error while searching for repo at " + addressWithoutIndex + ": " + e.getMessage());
                            return originalAddress;
                        }

                        if (isCancelled()) {
                            Log.d(TAG, "Not checking any more repo addresses, because process was skipped.");
                            break;
                        }
                    }
                    return originalAddress;

                }

                private boolean checkForRepository(Uri indexUri) throws IOException {
                    HttpClient client = new DefaultHttpClient();
                    HttpHead head = new HttpHead(indexUri.toString());
                    return client.execute(head).getStatusLine().getStatusCode() == 200;
                }

                @Override
                protected void onProgressUpdate(String... values) {
                    String address = values[0];
                    textSearching.setText(getString(R.string.repo_searching_address, address));
                }

                @Override
                protected void onPostExecute(String newAddress) {
                    if (addRepoDialog.isShowing()) {
                        createNewRepo(newAddress, fingerprint);
                    }
                }
            };

            Button skip = addRepoDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            skip.setText(getString(R.string.skip));
            skip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Still proceed with adding the repo, just don't bother searching for
                    // a better alternative than the one provided.
                    // The reason for this is that if they are not connected to the internet,
                    // or their internet is playing up, then you'd have to wait for several
                    // connection timeouts before being able to proceed.

                    createNewRepo(originalAddress, fingerprint);
                    checker.cancel(false);
                }
            });

            checker.execute(originalAddress);
        }

        private void createNewRepo(String address, String fingerprint) {
            ContentValues values = new ContentValues(2);
            values.put(RepoProvider.DataColumns.ADDRESS, address);
            if (fingerprint != null && fingerprint.length() > 0) {
                values.put(RepoProvider.DataColumns.FINGERPRINT, fingerprint.toUpperCase(Locale.ENGLISH));
            }
            RepoProvider.Helper.insert(context, values);
            finishedAddingRepo();
            Toast.makeText(ManageReposActivity.this, getString(R.string.repo_added, address), Toast.LENGTH_SHORT).show();
        }

        /**
         * Seeing as this repo already exists, we will force it to be enabled again.
         */
        private void enableExistingRepo(Repo repo) {
            ContentValues values = new ContentValues(1);
            values.put(RepoProvider.DataColumns.IN_USE, 1);
            RepoProvider.Helper.update(context, repo, values);
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
            if (addRepoDialog.isShowing()) {
                addRepoDialog.dismiss();
            }
            if (isImportingRepo) {
                setResult(RESULT_OK);
                finish();
            }
        }

    }

    private void addRepoFromIntent(Intent intent) {
        /* an URL from a click, NFC, QRCode scan, etc */
        NewRepoConfig newRepoConfig = new NewRepoConfig(this, intent);
        if (newRepoConfig.isValidRepo()) {
            isImportingRepo = true;
            showAddRepo(newRepoConfig.getRepoUriString(), newRepoConfig.getFingerprint());
            checkIfNewRepoOnSameWifi(newRepoConfig);
        } else if (newRepoConfig.getErrorMessage() != null) {
            Toast.makeText(this, newRepoConfig.getErrorMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void checkIfNewRepoOnSameWifi(NewRepoConfig newRepo) {
        // if this is a local repo, check we're on the same wifi
        if (!TextUtils.isEmpty(newRepo.getBssid())) {
            WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            String bssid = wifiInfo.getBSSID();
            if (TextUtils.isEmpty(bssid)) /* not all devices have wifi */
                return;
            bssid = bssid.toLowerCase(Locale.ENGLISH);
            String newRepoBssid = Uri.decode(newRepo.getBssid()).toLowerCase(Locale.ENGLISH);
            if (!bssid.equals(newRepoBssid)) {
                String msg = String.format(getString(R.string.not_on_same_wifi), newRepo.getSsid());
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            }
            // TODO we should help the user to the right thing here,
            // instead of just showing a message!
        }
    }

    public static class RepoListFragment extends ListFragment
            implements LoaderManager.LoaderCallbacks<Cursor>, RepoAdapter.EnabledListener {

        @Override
        public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
            Uri uri = RepoProvider.allExceptSwapUri();
            Log.i(TAG, "Creating repo loader '" + uri + "'.");
            final String[] projection = {
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
            repoAdapter.swapCursor(cursor);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> cursorLoader) {
            repoAdapter.swapCursor(null);
        }

        /**
         * NOTE: If somebody toggles a repo off then on again, it will have
         * removed all apps from the index when it was toggled off, so when it
         * is toggled on again, then it will require a refresh. Previously, I
         * toyed with the idea of remembering whether they had toggled on or
         * off, and then only actually performing the function when the activity
         * stopped, but I think that will be problematic. What about when they
         * press the home button, or edit a repos details? It will start to
         * become somewhat-random as to when the actual enabling, disabling is
         * performed. So now, it just does the disable as soon as the user
         * clicks "Off" and then removes the apps. To compensate for the removal
         * of apps from index, it notifies the user via a toast that the apps
         * have been removed. Also, as before, it will still prompt the user to
         * update the repos if you toggled on on.
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

        private RepoAdapter repoAdapter;

        private View createHeaderView() {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            TextView textLastUpdate = new TextView(getActivity());
            long lastUpdate = prefs.getLong(Preferences.PREF_UPD_LAST, 0);
            String lastUpdateCheck;
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

            if (getListAdapter() == null) {
                /*
                 * Can't do this in the onCreate view, because "onCreateView"
                 * which returns the list view is "called between onCreate and
                 * onActivityCreated" according to the docs.
                 */
                getListView().addHeaderView(createHeaderView(), null, false);

                /*
                 * This could go in onCreate (and used to) but it needs to be
                 * called after addHeaderView, which can only be called after
                 * onCreate...
                 */
                repoAdapter = new RepoAdapter(getActivity(), null);
                repoAdapter.setEnabledListener(this);
                setListAdapter(repoAdapter);
            }
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {

            super.onCreate(savedInstanceState);
            setRetainInstance(true);
            setHasOptionsMenu(true);
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

        public static final int SHOW_REPO_DETAILS = 1;

        public void editRepo(Repo repo) {
            Intent intent = new Intent(getActivity(), RepoDetailsActivity.class);
            intent.putExtra(RepoDetailsFragment.ARG_REPO_ID, repo.getId());
            startActivityForResult(intent, SHOW_REPO_DETAILS);
        }
    }
}

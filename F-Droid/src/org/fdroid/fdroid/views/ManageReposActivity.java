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

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NavUtils;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.UpdateService;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.compat.ClipboardCompat;
import org.fdroid.fdroid.data.NewRepoConfig;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoProvider;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.Locale;

public class ManageReposActivity extends ActionBarActivity {
    private static final String TAG = "ManageReposActivity";

    private static final String DEFAULT_NEW_REPO_TEXT = "https://";

    private enum AddRepoState {
        DOESNT_EXIST, EXISTS_FINGERPRINT_MISMATCH, EXISTS_FINGERPRINT_MATCH,
        EXISTS_DISABLED, EXISTS_ENABLED, EXISTS_UPGRADABLE_TO_SIGNED, INVALID_URL,
        IS_SWAP
    }

    private RepoListFragment listFragment;

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

            listFragment = new RepoListFragment();

            fm.beginTransaction()
                    .add(android.R.id.content, listFragment)
                    .commit();
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // title is "Repositories" here, but "F-Droid" in VIEW Intent chooser
        getSupportActionBar().setTitle(R.string.menu_manage);
    }

    @Override
    protected void onResume() {
        super.onResume();

        /* let's see if someone is trying to send us a new repo */
        addRepoFromIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    @Override
    public void finish() {
        Intent ret = new Intent();
        setResult(RESULT_OK, ret);
        super.finish();
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
            setResult(RESULT_OK, destIntent);
            NavUtils.navigateUpTo(this, destIntent);
            return true;
        case R.id.action_add_repo:
            showAddRepo();
            return true;
        case R.id.action_update_repo:
            UpdateService.updateNow(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
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

        private final TextView overwriteMessage;
        private final ColorStateList defaultTextColour;
        private final Button addButton;

        private AddRepoState addRepoState;

        public AddRepo(String newAddress, String newFingerprint) {

            context = ManageReposActivity.this;

            final View view = getLayoutInflater().inflate(R.layout.addrepo, null);
            addRepoDialog = new AlertDialog.Builder(context).setView(view).create();
            final EditText uriEditText = (EditText) view.findViewById(R.id.edit_uri);
            final EditText fingerprintEditText = (EditText) view.findViewById(R.id.edit_fingerprint);

            addRepoDialog.setTitle(R.string.repo_add_title);
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
                        String url = uriEditText.getText().toString();

                        try {
                            url = normalizeUrl(url);
                        } catch (URISyntaxException e) {
                            invalidUrl();
                            return;
                        }

                        switch (addRepoState) {
                            case DOESNT_EXIST:
                                prepareToCreateNewRepo(url, fp);
                                break;

                            case IS_SWAP:
                                Utils.DebugLog(TAG, "Removing existing swap repo " + url + " before adding new repo.");
                                Repo repo = RepoProvider.Helper.findByAddress(context, url);
                                RepoProvider.Helper.remove(context, repo.getId());
                                prepareToCreateNewRepo(url, fp);
                                break;

                            case EXISTS_DISABLED:
                            case EXISTS_UPGRADABLE_TO_SIGNED:
                            case EXISTS_FINGERPRINT_MATCH:
                                updateAndEnableExistingRepo(url, fp);
                                finishedAddingRepo();
                                break;

                            default:
                                finishedAddingRepo();
                                break;
                        }
                    }
                }
            );

            addButton = addRepoDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            overwriteMessage = (TextView) view.findViewById(R.id.overwrite_message);
            overwriteMessage.setVisibility(View.GONE);
            defaultTextColour = overwriteMessage.getTextColors();

            if (newFingerprint != null) {
                fingerprintEditText.setText(newFingerprint);
            }

            if (newAddress != null) {
                // This trick of emptying text then appending, rather than just setting in
                // the first place, is necessary to move the cursor to the end of the input.
                uriEditText.setText("");
                uriEditText.append(newAddress);
            }

            final TextWatcher textChangedListener = new TextWatcher() {

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    validateRepoDetails(uriEditText.getText().toString(), fingerprintEditText.getText().toString());
                }
            };

            uriEditText.addTextChangedListener(textChangedListener);
            fingerprintEditText.addTextChangedListener(textChangedListener);
            validateRepoDetails(newAddress == null ? "" : newAddress, newFingerprint == null ? "" : newFingerprint);
        }

        /**
         * Compare the repo and the fingerprint against existing repositories, to see if this
         * repo matches and display a relevant message to the user if that is the case.
         */
        private void validateRepoDetails(@NonNull String uri, @NonNull String fingerprint) {

            try {
                uri = normalizeUrl(uri);
            } catch (URISyntaxException e) {
                // Don't bother dealing with this exception yet, as this is called every time
                // a letter is added to the repo URL text input. We don't want to display a message
                // to the user until they try to save the repo.
            }

            final Repo repo = !TextUtils.isEmpty(uri) ? RepoProvider.Helper.findByAddress(context, uri) : null;

            if (repo == null) {
                repoDoesntExist();
            } else {
                if (repo.isSwap) {
                    repoIsSwap();
                } else if (repo.fingerprint == null && fingerprint.length() > 0) {
                    upgradingToSigned();
                } else if (repo.fingerprint != null && !repo.fingerprint.equalsIgnoreCase(fingerprint)) {
                    repoFingerprintDoesntMatch();
                } else {
                    // Could be either an unsigned repo, and no fingerprint was supplied,
                    // or it could be a signed repo with a matching fingerprint.
                    if (repo.inuse) {
                        repoExistsAndEnabled();
                    } else {
                        repoExistsAndDisabled();
                    }
                }
            }
        }

        private void repoDoesntExist() {
            updateUi(AddRepoState.DOESNT_EXIST, 0, false, R.string.repo_add_add, true);
        }

        private void repoIsSwap() {
            updateUi(AddRepoState.IS_SWAP, 0, false, R.string.repo_add_add, true);
        }

        /**
         * Same address with different fingerprint, this could be malicious, so display a message
         * force the user to manually delete the repo before adding this one.
         */
        private void repoFingerprintDoesntMatch() {
            updateUi(AddRepoState.EXISTS_FINGERPRINT_MISMATCH, R.string.repo_delete_to_overwrite,
                    true, R.string.overwrite, false);
        }

        private void invalidUrl() {
            updateUi(AddRepoState.INVALID_URL, R.string.invalid_url, true,
                    R.string.repo_add_add, false);
        }

        private void repoExistsAndDisabled() {
            updateUi(AddRepoState.EXISTS_DISABLED,
                    R.string.repo_exists_enable, false, R.string.enable, true);
        }

        private void repoExistsAndEnabled() {
            updateUi(AddRepoState.EXISTS_ENABLED, R.string.repo_exists_and_enabled, false,
                    R.string.ok, true);
        }

        private void upgradingToSigned() {
            updateUi(AddRepoState.EXISTS_UPGRADABLE_TO_SIGNED, R.string.repo_exists_add_fingerprint,
                    false, R.string.add_key, true);
        }

        private void updateUi(AddRepoState state, int messageRes, boolean redMessage, int addTextRes, boolean addEnabled) {
            if (addRepoState != state) {
                addRepoState = state;

                if (messageRes > 0) {
                    overwriteMessage.setText(messageRes);
                    overwriteMessage.setVisibility(View.VISIBLE);
                    if (redMessage) {
                        overwriteMessage.setTextColor(getResources().getColor(R.color.red));
                    } else {
                        overwriteMessage.setTextColor(defaultTextColour);
                    }
                } else {
                    overwriteMessage.setVisibility(View.GONE);
                }

                addButton.setText(addTextRes);
                addButton.setEnabled(addEnabled);
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

                        Utils.DebugLog(TAG, "Checking for repo at " + originalAddress + " with suffix \"" + path + "\".");
                        Uri.Builder builder = Uri.parse(originalAddress).buildUpon().appendEncodedPath(path);
                        final String addressWithoutIndex = builder.build().toString();
                        publishProgress(addressWithoutIndex);

                        final Uri uri = builder.appendPath("index.jar").build();

                        try {
                            if (checkForRepository(uri)) {
                                Utils.DebugLog(TAG, "Found F-Droid repo at " + addressWithoutIndex);
                                return addressWithoutIndex;
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Error while searching for repo at " + addressWithoutIndex, e);
                            return originalAddress;
                        }

                        if (isCancelled()) {
                            Utils.DebugLog(TAG, "Not checking any more repo addresses, because process was skipped.");
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
            skip.setText(R.string.skip);
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

        /**
         * Some basic sanitization of URLs, so that two URLs which have the same semantic meaning
         * are represented by the exact same string by F-Droid. This will help to make sure that,
         * e.g. "http://10.0.1.50" and "http://10.0.1.50/" are not two different repositories.
         *
         * Currently it normalizes the path so that "/./" are removed and "test/../" is collapsed.
         * This is done using {@link URI#normalize()}. It also removes multiple consecutive forward
         * slashes in the path and replaces them with one. Finally, it removes trailing slashes.
         */
        private String normalizeUrl(String urlString) throws URISyntaxException {
            URI uri = new URI(urlString);
            if (!uri.isAbsolute()) {
                throw new URISyntaxException(urlString, "Must provide an absolute URI for repositories");
            }

            uri = uri.normalize();
            String path = uri.getPath().replaceAll("//*/", "/"); // Collapse multiple forward slashes into 1.
            if (path.length() > 0 && path.charAt(path.length() - 1) == '/') {
                path = path.substring(0, path.length() - 1);
            }

            return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(),
                    path, uri.getQuery(), uri.getFragment()).toString();
        }

        private void createNewRepo(String address, String fingerprint) {
            try {
                address = normalizeUrl(address);
            } catch (URISyntaxException e) {
                // Leave address as it was.
            }
            ContentValues values = new ContentValues(2);
            values.put(RepoProvider.DataColumns.ADDRESS, address);
            if (!TextUtils.isEmpty(fingerprint)) {
                values.put(RepoProvider.DataColumns.FINGERPRINT, fingerprint.toUpperCase(Locale.ENGLISH));
            }
            RepoProvider.Helper.insert(context, values);
            finishedAddingRepo();
            Toast.makeText(ManageReposActivity.this, getString(R.string.repo_added, address), Toast.LENGTH_SHORT).show();
        }

        /**
         * Seeing as this repo already exists, we will force it to be enabled again.
         */
        private void updateAndEnableExistingRepo(String url, String fingerprint) {
            if (fingerprint != null) {
                fingerprint = fingerprint.trim();
                if (TextUtils.isEmpty(fingerprint)) {
                    fingerprint = null;
                } else {
                    fingerprint = fingerprint.toUpperCase(Locale.ENGLISH);
                }
            }

            Utils.DebugLog(TAG, "Enabling existing repo: " + url);
            Repo repo = RepoProvider.Helper.findByAddress(context, url);
            ContentValues values = new ContentValues(2);
            values.put(RepoProvider.DataColumns.IN_USE, 1);
            values.put(RepoProvider.DataColumns.FINGERPRINT, fingerprint);
            RepoProvider.Helper.update(context, repo, values);
            listFragment.notifyDataSetChanged();
            finishedAddingRepo();
        }

        /**
         * If started by an intent that expects a result (e.g. QR codes) then we
         * will set a result and finish. Otherwise, we'll updateViews the list of repos
         * to reflect the newly created repo.
         */
        private void finishedAddingRepo() {
            UpdateService.updateNow(ManageReposActivity.this);
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
            Utils.DebugLog(TAG, "Creating repo loader '" + uri + "'.");
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
         * is toggled on again, then it will require a updateViews. Previously, I
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
                    UpdateService.updateNow(getActivity());
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
            intent.putExtra(RepoDetailsActivity.ARG_REPO_ID, repo.getId());
            startActivityForResult(intent, SHOW_REPO_DETAILS);
        }

        /**
         * This is necessary because even though the list will listen to content changes
         * in the RepoProvider, it doesn't update the list items if they are changed (but not
         * added or removed. The example which made this necessary was enabling an existing
         * repo, and wanting the switch to be changed to on).
         */
        private void notifyDataSetChanged() {
            getLoaderManager().restartLoader(0, null, this);
        }
    }
}

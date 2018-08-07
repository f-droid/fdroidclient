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

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.RepoUpdater;
import org.fdroid.fdroid.UpdateService;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.compat.CursorAdapterCompat;
import org.fdroid.fdroid.data.NewRepoConfig;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.data.Schema.RepoTable;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;

public class ManageReposActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor>, RepoAdapter.EnabledListener {
    private static final String TAG = "ManageReposActivity";

    private static final String DEFAULT_NEW_REPO_TEXT = "https://";

    private enum AddRepoState {
        DOESNT_EXIST, EXISTS_FINGERPRINT_MISMATCH, EXISTS_ADD_MIRROR,
        EXISTS_DISABLED, EXISTS_ENABLED, EXISTS_UPGRADABLE_TO_SIGNED, INVALID_URL,
        IS_SWAP
    }

    private Toolbar toolbar;

    /**
     * True if activity started with an intent such as from QR code. False if
     * opened from, e.g. the main menu.
     */
    private boolean isImportingRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        ((FDroidApp) getApplication()).applyTheme(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.repo_list_activity);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final ListView repoList = (ListView) findViewById(R.id.list);
        repoAdapter = RepoAdapter.create(this, null, CursorAdapterCompat.FLAG_AUTO_REQUERY);
        repoAdapter.setEnabledListener(this);
        repoList.setAdapter(repoAdapter);
        repoList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Repo repo = new Repo((Cursor) repoList.getItemAtPosition(position));
                editRepo(repo);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        FDroidApp.checkStartTor(this);

        /* let's see if someone is trying to send us a new repo */
        addRepoFromIntent(getIntent());

        // Starts a new or restarts an existing Loader in this manager
        getSupportLoaderManager().restartLoader(0, null, this);
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
        toolbar.inflateMenu(R.menu.manage_repos);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_repo:
                showAddRepo();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public String getPrimaryClipAsText() {
        CharSequence text = null;
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager.hasPrimaryClip()) {
            ClipData data = clipboardManager.getPrimaryClip();
            if (data.getItemCount() > 0) {
                text = data.getItemAt(0).getText();
            }
        }
        return text != null ? text.toString() : null;
    }

    private void showAddRepo() {
        /*
         * If there is text in the clipboard, and it looks like a URL, use that.
         * Otherwise use "https://" as default repo string.
         */
        String text = getPrimaryClipAsText();
        String fingerprint = null;
        String username = null;
        String password = null;
        if (!TextUtils.isEmpty(text)) {
            try {
                new URL(text);
                Uri uri = Uri.parse(text);
                fingerprint = uri.getQueryParameter("fingerprint");
                // uri might contain a QR-style, all uppercase URL:
                if (TextUtils.isEmpty(fingerprint)) {
                    fingerprint = uri.getQueryParameter("FINGERPRINT");
                }

                String userInfo = uri.getUserInfo();
                if (userInfo != null) {
                    String[] userInfoTokens = userInfo.split(":");
                    if (userInfoTokens.length >= 2) {
                        username = userInfoTokens[0];
                        password = userInfoTokens[1];
                        for (int i = 2; i < userInfoTokens.length; i++) {
                            password += ":" + userInfoTokens[i];
                        }
                    }
                }

                text = NewRepoConfig.sanitizeRepoUri(uri);
            } catch (MalformedURLException e) {
                text = null;
            }
        }

        if (TextUtils.isEmpty(text)) {
            text = DEFAULT_NEW_REPO_TEXT;
        }
        showAddRepo(text, fingerprint, username, password);
    }

    private void showAddRepo(String newAddress, String newFingerprint, String username, String password) {
        new AddRepo(newAddress, newFingerprint, username, password);
    }

    /**
     * Utility class to encapsulate the process of adding a new repo (or an existing one,
     * depending on if the incoming address is the same as a previous repo). It is responsible
     * for managing the lifecycle of adding a repo:
     * <li>Showing the add dialog
     * <li>Deciding whether to add a new repo or update an existing one
     * <li>Search for repos at common suffixes (/, /fdroid/repo, /repo)
     */
    private class AddRepo {

        private final Context context;
        private final HashMap<String, Repo> urlRepoMap = new HashMap<>();
        private final HashMap<String, Repo> fingerprintRepoMap = new HashMap<>();
        private final AlertDialog addRepoDialog;
        private final TextView overwriteMessage;
        private final ColorStateList defaultTextColour;
        private final Button addButton;

        private AddRepoState addRepoState;

        /**
         * Create new instance, setup GUI, and build maps for quickly looking
         * up repos based on URL or fingerprint.  These need to be in maps
         * since the user input is validated as they are typing.  This also
         * checks that the repo type matches, e.g. "repo" or "archive".
         */
        AddRepo(String newAddress, String newFingerprint, final String username, final String password) {

            context = ManageReposActivity.this;

            for (Repo repo : RepoProvider.Helper.all(context)) {
                urlRepoMap.put(repo.address, repo);
                for (String url : repo.getMirrorList()) {
                    urlRepoMap.put(url, repo);
                }
                if (!TextUtils.isEmpty(repo.fingerprint)
                        && TextUtils.equals(getRepoType(newAddress), getRepoType(repo.address))) {
                    fingerprintRepoMap.put(repo.fingerprint, repo);
                }
            }

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
                            if (isImportingRepo) {
                                ManageReposActivity.this.finish();
                            }
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

                            String url = uriEditText.getText().toString();

                            try {
                                url = normalizeUrl(url);
                            } catch (URISyntaxException e) {
                                invalidUrl();
                                return;
                            }

                            String fp = fingerprintEditText.getText().toString();
                            // remove any whitespace from fingerprint
                            fp = fp.replaceAll("\\s", "");

                            switch (addRepoState) {
                                case DOESNT_EXIST:
                                    prepareToCreateNewRepo(url, fp, username, password);
                                    break;

                                case IS_SWAP:
                                    Utils.debugLog(TAG, "Removing existing swap repo " + url
                                            + " before adding new repo.");
                                    Repo repo = RepoProvider.Helper.findByAddress(context, url);
                                    RepoProvider.Helper.remove(context, repo.getId());
                                    prepareToCreateNewRepo(url, fp, username, password);
                                    break;

                                case EXISTS_DISABLED:
                                case EXISTS_UPGRADABLE_TO_SIGNED:
                                case EXISTS_ADD_MIRROR:
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
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

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
         * Gets the repo type as represented by the final segment of the path. This is
         * a bit trickier with {@code content://} URLs, since they might have
         * encoded "/" chars in it, for example:
         * {@code content://authority/tree/313E-1F1C%3A/document/313E-1F1C%3Aguardianproject.info%2Ffdroid%2Frepo}
         */
        private String getRepoType(String url) {
            String last = Uri.parse(url).getLastPathSegment();
            if (last == null) {
                return "";
            } else {
                return new File(last).getName();
            }
        }

        /**
         * Compare the repo and the fingerprint against existing repositories, to see if this
         * repo matches and display a relevant message to the user if that is the case. There
         * are many different cases to handle:
         * <ul>
         * <li> a signed repo with a {@link Repo#address URL} and fingerprint that matches
         * <li> a signed repo with a matching fingerprint and URL that matches a mirror
         * <li> a signed repo with a matching fingerprint, but the URL doesn't match any known mirror
         * <li>an unsigned repo and no fingerprint was supplied
         * </ul>
         */
        private void validateRepoDetails(@NonNull String uri, @NonNull String fingerprint) {

            try {
                uri = normalizeUrl(uri);
            } catch (URISyntaxException e) {
                // Don't bother dealing with this exception yet, as this is called every time
                // a letter is added to the repo URL text input. We don't want to display a message
                // to the user until they try to save the repo.
            }

            Repo repo = fingerprintRepoMap.get(fingerprint);
            if (repo == null) {
                repo = urlRepoMap.get(uri);
            }

            if (repo == null) {
                repoDoesntExist();
            } else {
                if (repo.isSwap) {
                    repoIsSwap(repo);
                } else if (repo.fingerprint == null && fingerprint.length() > 0) {
                    upgradingToSigned(repo);
                } else if (repo.fingerprint != null && !repo.fingerprint.equalsIgnoreCase(fingerprint)) {
                    repoFingerprintDoesntMatch(repo);
                } else {
                    if (!TextUtils.equals(repo.address, uri)
                            && !repo.getMirrorList().contains(uri)) {
                        repoExistsAddMirror(repo);
                    } else if (repo.inuse) {
                        repoExistsAndEnabled(repo);
                    } else {
                        repoExistsAndDisabled(repo);
                    }
                }
            }
        }

        private void repoDoesntExist() {
            updateUi(null, AddRepoState.DOESNT_EXIST, 0, false, R.string.repo_add_add, true);
        }

        private void repoIsSwap(Repo repo) {
            updateUi(repo, AddRepoState.IS_SWAP, 0, false, R.string.repo_add_add, true);
        }

        /**
         * Same address with different fingerprint, this could be malicious, so display a message
         * force the user to manually delete the repo before adding this one.
         */
        private void repoFingerprintDoesntMatch(Repo repo) {
            updateUi(repo, AddRepoState.EXISTS_FINGERPRINT_MISMATCH,
                    R.string.repo_delete_to_overwrite,
                    true, R.string.overwrite, false);
        }

        private void invalidUrl() {
            updateUi(null, AddRepoState.INVALID_URL, R.string.invalid_url, true,
                    R.string.repo_add_add, false);
        }

        private void repoExistsAndDisabled(Repo repo) {
            updateUi(repo, AddRepoState.EXISTS_DISABLED,
                    R.string.repo_exists_enable, false, R.string.enable, true);
        }

        private void repoExistsAndEnabled(Repo repo) {
            updateUi(repo, AddRepoState.EXISTS_ENABLED, R.string.repo_exists_and_enabled, false,
                    R.string.ok, true);
        }

        private void repoExistsAddMirror(Repo repo) {
            updateUi(repo, AddRepoState.EXISTS_ADD_MIRROR, R.string.repo_exists_add_mirror, false,
                    R.string.repo_add_mirror, true);
        }

        private void upgradingToSigned(Repo repo) {
            updateUi(repo, AddRepoState.EXISTS_UPGRADABLE_TO_SIGNED, R.string.repo_exists_add_fingerprint,
                    false, R.string.add_key, true);
        }

        private void updateUi(Repo repo, AddRepoState state, int messageRes, boolean redMessage, int addTextRes,
                              boolean addEnabled) {
            if (addRepoState != state) {
                addRepoState = state;

                String name;
                if (repo == null) {
                    name = '"' + getString(R.string.unknown) + '"';
                } else {
                    name = repo.name;
                }

                if (messageRes > 0) {
                    overwriteMessage.setText(getString(messageRes, name));
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
        @SuppressLint("StaticFieldLeak")
        private void prepareToCreateNewRepo(final String originalAddress, final String fingerprint,
                                            final String username, final String password) {

            final View addRepoForm = addRepoDialog.findViewById(R.id.add_repo_form);
            addRepoForm.setVisibility(View.GONE);
            final View positiveButton = addRepoDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setVisibility(View.GONE);

            final TextView textSearching = (TextView) addRepoDialog.findViewById(R.id.text_searching_for_repo);
            textSearching.setText(getString(R.string.repo_searching_address, originalAddress));

            final Button skip = addRepoDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            skip.setText(R.string.skip);

            final AsyncTask<String, String, String> checker = new AsyncTask<String, String, String>() {

                private int statusCode = -1;
                private static final int REFRESH_DIALOG = Integer.MAX_VALUE;
                private static final int HTTP_UNAUTHORIZED = 401;
                private static final int HTTP_OK = 200;

                @Override
                protected String doInBackground(String... params) {
                    final String originalAddress = params[0];

                    if (fingerprintRepoMap.containsKey(fingerprint)) {
                        statusCode = REFRESH_DIALOG;
                        return originalAddress;
                    }

                    final String[] pathsToCheck = {"", "fdroid/repo", "repo"};
                    for (final String path : pathsToCheck) {

                        Utils.debugLog(TAG, "Check for repo at " + originalAddress + " with suffix '" + path + "'");
                        Uri.Builder builder = Uri.parse(originalAddress).buildUpon().appendEncodedPath(path);
                        final String addressWithoutIndex = builder.build().toString();
                        publishProgress(addressWithoutIndex);

                        if (urlRepoMap.containsKey(addressWithoutIndex)) {
                            statusCode = REFRESH_DIALOG;
                            return addressWithoutIndex;
                        }

                        final Uri uri = builder.appendPath(RepoUpdater.SIGNED_FILE_NAME).build();

                        try {
                            if (checkForRepository(uri)) {
                                Utils.debugLog(TAG, "Found F-Droid repo at " + addressWithoutIndex);
                                return addressWithoutIndex;
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Error while searching for repo at " + addressWithoutIndex, e);
                            return originalAddress;
                        }

                        if (isCancelled()) {
                            Utils.debugLog(TAG, "Not checking more repo addresses, because process was skipped.");
                            break;
                        }
                    }
                    return originalAddress;

                }

                private boolean checkForRepository(Uri indexUri) throws IOException {
                    final URL url = new URL(indexUri.toString());
                    final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("HEAD");

                    statusCode = connection.getResponseCode();

                    return statusCode == HTTP_UNAUTHORIZED || statusCode == HTTP_OK;
                }

                @Override
                protected void onProgressUpdate(String... values) {
                    String address = values[0];
                    textSearching.setText(getString(R.string.repo_searching_address, address));
                }

                @Override
                protected void onPostExecute(final String newAddress) {

                    if (addRepoDialog.isShowing()) {

                        if (statusCode == HTTP_UNAUTHORIZED) {

                            final View view = getLayoutInflater().inflate(R.layout.login, null);
                            final AlertDialog credentialsDialog = new AlertDialog.Builder(context)
                                    .setView(view).create();
                            final EditText nameInput = (EditText) view.findViewById(R.id.edit_name);
                            final EditText passwordInput = (EditText) view.findViewById(R.id.edit_password);

                            if (username != null) {
                                nameInput.setText(username);
                            }
                            if (password != null) {
                                passwordInput.setText(password);
                            }

                            credentialsDialog.setTitle(R.string.login_title);
                            credentialsDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                                    getString(R.string.cancel),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                            // cancel parent dialog, don't add repo
                                            addRepoDialog.cancel();
                                        }
                                    });

                            credentialsDialog.setButton(DialogInterface.BUTTON_POSITIVE,
                                    getString(R.string.ok),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            createNewRepo(newAddress, fingerprint,
                                                    nameInput.getText().toString(),
                                                    passwordInput.getText().toString());
                                        }
                                    });

                            credentialsDialog.show();

                        } else if (statusCode == REFRESH_DIALOG) {
                            addRepoForm.setVisibility(View.VISIBLE);
                            positiveButton.setVisibility(View.VISIBLE);
                            textSearching.setText("");
                            skip.setText(R.string.cancel);
                            skip.setOnClickListener(null);
                            validateRepoDetails(newAddress, fingerprint);
                        } else {

                            // create repo without username/password
                            createNewRepo(newAddress, fingerprint);
                        }
                    }
                }
            };

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
         * <p>
         * Currently it normalizes the path so that "/./" are removed and "test/../" is collapsed.
         * This is done using {@link URI#normalize()}. It also removes multiple consecutive forward
         * slashes in the path and replaces them with one. Finally, it removes trailing slashes.
         * <p>
         * {@code content://} URLs used for repos stored on removable storage get messed up by
         * {@link URI}.
         */
        private String normalizeUrl(String urlString) throws URISyntaxException {
            if (urlString == null) {
                return null;
            }
            Uri uri = Uri.parse(urlString);
            if (!uri.isAbsolute()) {
                throw new URISyntaxException(urlString, "Must provide an absolute URI for repositories");
            }
            if (!uri.isHierarchical()) {
                throw new URISyntaxException(urlString, "Must provide an hierarchical URI for repositories");
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
                return urlString;
            }
            return new URI(scheme.toLowerCase(Locale.ENGLISH),
                    uri.getUserInfo(),
                    host.toLowerCase(Locale.ENGLISH),
                    uri.getPort(),
                    path,
                    uri.getQuery(),
                    uri.getFragment()).normalize().toString();
        }

        /**
         * Create a repository without a username or password.
         */
        private void createNewRepo(String address, String fingerprint) {
            createNewRepo(address, fingerprint, null, null);
        }

        private void createNewRepo(String address, String fingerprint,
                                   final String username, final String password) {
            try {
                address = normalizeUrl(address);
            } catch (URISyntaxException e) {
                // Leave address as it was.
            }
            ContentValues values = new ContentValues(4);
            values.put(RepoTable.Cols.ADDRESS, address);
            if (!TextUtils.isEmpty(fingerprint)) {
                values.put(RepoTable.Cols.FINGERPRINT, fingerprint.toUpperCase(Locale.ENGLISH));
            }

            if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
                values.put(RepoTable.Cols.USERNAME, username);
                values.put(RepoTable.Cols.PASSWORD, password);
            }

            RepoProvider.Helper.insert(context, values);
            finishedAddingRepo();
            Toast.makeText(context, getString(R.string.repo_added, address), Toast.LENGTH_SHORT).show();
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

            Utils.debugLog(TAG, "Enabling existing repo: " + url);
            Repo repo = fingerprintRepoMap.get(fingerprint);
            if (repo == null) {
                repo = RepoProvider.Helper.findByAddress(context, url);
            }

            ContentValues values = new ContentValues(2);
            values.put(RepoTable.Cols.IN_USE, 1);
            values.put(RepoTable.Cols.FINGERPRINT, fingerprint);
            if (!TextUtils.equals(url, repo.address)) {
                boolean addUserMirror = true;
                for (String mirror : repo.getMirrorList()) {
                    if (TextUtils.equals(mirror, url)) {
                        addUserMirror = false;
                    }
                }
                if (addUserMirror) {
                    if (repo.userMirrors == null) {
                        repo.userMirrors = new String[]{url};
                    } else {
                        int last = repo.userMirrors.length;
                        repo.userMirrors = Arrays.copyOf(repo.userMirrors, last + 1);
                        repo.userMirrors[last] = url;
                    }
                    values.put(RepoTable.Cols.USER_MIRRORS, Utils.serializeCommaSeparatedString(repo.userMirrors));
                }
            }
            RepoProvider.Helper.update(context, repo, values);

            notifyDataSetChanged();
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
            showAddRepo(newRepoConfig.getRepoUriString(), newRepoConfig.getFingerprint(),
                    newRepoConfig.getUsername(), newRepoConfig.getPassword());
            checkIfNewRepoOnSameWifi(newRepoConfig);
        } else if (newRepoConfig.getErrorMessage() != null) {
            Toast.makeText(this, newRepoConfig.getErrorMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void checkIfNewRepoOnSameWifi(NewRepoConfig newRepo) {
        // if this is a local repo, check we're on the same wifi
        if (!TextUtils.isEmpty(newRepo.getBssid())) {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            String bssid = wifiInfo.getBSSID();
            if (TextUtils.isEmpty(bssid)) { /* not all devices have wifi */
                return;
            }
            bssid = bssid.toLowerCase(Locale.ENGLISH);
            String newRepoBssid = Uri.decode(newRepo.getBssid()).toLowerCase(Locale.ENGLISH);
            if (!bssid.equals(newRepoBssid)) {
                String msg = getString(R.string.not_on_same_wifi, newRepo.getSsid());
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            }
            // TODO we should help the user to the right thing here,
            // instead of just showing a message!
        }
    }

    private RepoAdapter repoAdapter;

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Uri uri = RepoProvider.allExceptSwapUri();
        final String[] projection = {
                RepoTable.Cols._ID,
                RepoTable.Cols.NAME,
                RepoTable.Cols.SIGNING_CERT,
                RepoTable.Cols.FINGERPRINT,
                RepoTable.Cols.IN_USE,
        };
        return new CursorLoader(this, uri, projection, null, null, null);
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
            values.put(RepoTable.Cols.IN_USE, isEnabled ? 1 : 0);
            RepoProvider.Helper.update(this, repo, values);

            if (isEnabled) {
                UpdateService.updateNow(this);
            } else {
                RepoProvider.Helper.purgeApps(this, repo);
                String notification = getString(R.string.repo_disabled_notification, repo.name);
                Toast.makeText(this, notification, Toast.LENGTH_LONG).show();
            }
        }
    }

    public static final int SHOW_REPO_DETAILS = 1;

    public void editRepo(Repo repo) {
        Intent intent = new Intent(this, RepoDetailsActivity.class);
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
        getSupportLoaderManager().restartLoader(0, null, this);
    }
}

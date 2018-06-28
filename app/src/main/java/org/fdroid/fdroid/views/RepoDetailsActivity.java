package org.fdroid.fdroid.views;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.NfcHelper;
import org.fdroid.fdroid.NfcNotEnabledActivity;
import org.fdroid.fdroid.QrGenAsyncTask;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.UpdateService;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.data.Schema.RepoTable;

import java.util.Locale;

public class RepoDetailsActivity extends AppCompatActivity {
    private static final String TAG = "RepoDetailsActivity";

    public static final String ARG_REPO_ID = "repo_id";

    /**
     * If the repo has been updated at least once, then we will show
     * all of this info, otherwise they will be hidden.
     */
    private static final int[] SHOW_IF_EXISTS = {
            R.id.label_repo_name,
            R.id.text_repo_name,
            R.id.text_description,
            R.id.label_num_apps,
            R.id.text_num_apps,
            R.id.label_last_update,
            R.id.text_last_update,
            R.id.label_username,
            R.id.text_username,
            R.id.button_edit_credentials,
            R.id.label_repo_fingerprint,
            R.id.text_repo_fingerprint,
            R.id.text_repo_fingerprint_description,
    };
    /**
     * If the repo has <em>not</em> been updated yet, then we only show
     * these, otherwise they are hidden.
     */
    private static final int[] HIDE_IF_EXISTS = {
            R.id.text_not_yet_updated,
    };
    private Repo repo;
    private long repoId;
    private View repoView;

    private String shareUrl;

    /**
     * Help function to make switching between two view states easier.
     * Perhaps there is a better way to do this. I recall that using Adobe
     * Flex, there was a thing called "ViewStates" for exactly this. Wonder if
     * that exists in  Android?
     */
    private static void setMultipleViewVisibility(View parent,
                                                  int[] viewIds,
                                                  int visibility) {
        for (int viewId : viewIds) {
            parent.findViewById(viewId).setVisibility(visibility);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        ((FDroidApp) getApplication()).applyTheme(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.repodetails);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        repoView = findViewById(R.id.repoView);

        repoId = getIntent().getLongExtra(ARG_REPO_ID, 0);
        final String[] projection = {
                RepoTable.Cols.NAME,
                RepoTable.Cols.ADDRESS,
                RepoTable.Cols.FINGERPRINT,
                RepoTable.Cols.MIRRORS,
                RepoTable.Cols.USER_MIRRORS,
        };
        repo = RepoProvider.Helper.findById(this, repoId, projection);

        TextView inputUrl = (TextView) findViewById(R.id.input_repo_url);
        inputUrl.setText(repo.address);

        if (repo.address.startsWith("content://")) {
            // no need to show a QR Code, it is not shareable
            return;
        }

        Uri uri = Uri.parse(repo.address);
        uri = uri.buildUpon().appendQueryParameter("fingerprint", repo.fingerprint).build();
        String qrUriString = uri.toString();
        new QrGenAsyncTask(this, R.id.qr_code).execute(qrUriString);
    }

    @TargetApi(14)
    private void setNfc() {
        if (NfcHelper.setPushMessage(this, Utils.getSharingUri(repo))) {
            findViewById(android.R.id.content).post(new Runnable() {
                @Override
                public void run() {
                    onNewIntent(getIntent());
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        /*
         * After, for example, a repo update, the details will have changed in the
         * database. However, or local reference to the Repo object will not
         * have been updated. The safest way to deal with this is to reload the
         * repo object directly from the database.
         */
        repo = RepoProvider.Helper.findById(this, repoId);
        updateRepoView();

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(UpdateService.LOCAL_ACTION_STATUS));

        // FDroid.java and AppDetails2 set different NFC actions, so reset here
        setNfc();
        processIntent(getIntent());
    }

    @Override
    public void onNewIntent(Intent i) {
        // onResume gets called after this to handle the intent
        setIntent(i);
    }

    private void processIntent(Intent i) {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(i.getAction())) {
            Parcelable[] rawMsgs =
                    i.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage msg = (NdefMessage) rawMsgs[0];
            String url = new String(msg.getRecords()[0].getPayload());
            Utils.debugLog(TAG, "Got this URL: " + url);
            Toast.makeText(this, "Got this URL: " + url, Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.setClass(this, ManageReposActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int statusCode = intent.getIntExtra(UpdateService.EXTRA_STATUS_CODE, -1);
            if (statusCode == UpdateService.STATUS_COMPLETE_WITH_CHANGES) {
                updateRepoView();
            }
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.repo_details_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.menu_delete:
                promptForDelete();
                return true;
            case R.id.menu_enable_nfc:
                intent = new Intent(this, NfcNotEnabledActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_share:
                intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, shareUrl);
                startActivity(Intent.createChooser(intent,
                        getResources().getString(R.string.share_repository)));
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        prepareNfcMenuItems(menu);
        prepareShareMenuItems(menu);
        return true;
    }

    @TargetApi(16)
    private void prepareNfcMenuItems(Menu menu) {
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        MenuItem menuItem = menu.findItem(R.id.menu_enable_nfc);

        if (nfcAdapter == null) {
            menuItem.setVisible(false);
            return;
        }

        boolean needsEnableNfcMenuItem;
        if (Build.VERSION.SDK_INT < 16) {
            needsEnableNfcMenuItem = !nfcAdapter.isEnabled();
        } else {
            needsEnableNfcMenuItem = !nfcAdapter.isNdefPushEnabled();
        }

        menuItem.setVisible(needsEnableNfcMenuItem);
    }

    private void prepareShareMenuItems(Menu menu) {
        if (!TextUtils.isEmpty(repo.address)) {
            if (!TextUtils.isEmpty(repo.fingerprint)) {
                shareUrl = Uri.parse(repo.address).buildUpon()
                        .appendQueryParameter("fingerprint", repo.fingerprint).toString();
            } else {
                shareUrl = repo.address;
            }
            menu.findItem(R.id.action_share).setVisible(true);
        } else {
            menu.findItem(R.id.action_share).setVisible(false);
        }
    }

    private void setupDescription(View parent, Repo repo) {

        TextView descriptionLabel = (TextView) parent.findViewById(R.id.label_description);
        TextView description = (TextView) parent.findViewById(R.id.text_description);

        if (TextUtils.isEmpty(repo.description)) {
            descriptionLabel.setVisibility(View.GONE);
            description.setVisibility(View.GONE);
            description.setText("");
        } else {
            descriptionLabel.setVisibility(View.VISIBLE);
            description.setVisibility(View.VISIBLE);
            description.setText(repo.description.replaceAll("\n", " "));
        }
    }

    private void setupRepoFingerprint(View parent, Repo repo) {
        TextView repoFingerprintView = (TextView) parent.findViewById(R.id.text_repo_fingerprint);
        TextView repoFingerprintDescView = (TextView) parent.findViewById(R.id.text_repo_fingerprint_description);

        String repoFingerprint;

        // TODO show the current state of the signature check, not just whether there is a key or not
        if (TextUtils.isEmpty(repo.fingerprint) && TextUtils.isEmpty(repo.signingCertificate)) {
            repoFingerprint = getResources().getString(R.string.unsigned);
            repoFingerprintView.setTextColor(getResources().getColor(R.color.unsigned));
            repoFingerprintDescView.setVisibility(View.VISIBLE);
            repoFingerprintDescView.setText(getResources().getString(R.string.unsigned_description));
        } else {
            // this is based on repo.fingerprint always existing, which it should
            repoFingerprint = Utils.formatFingerprint(this, repo.fingerprint);
            repoFingerprintDescView.setVisibility(View.GONE);
        }

        repoFingerprintView.setText(repoFingerprint);
    }

    private void setupCredentials(View parent, Repo repo) {

        TextView usernameLabel = (TextView) parent.findViewById(R.id.label_username);
        TextView username = (TextView) parent.findViewById(R.id.text_username);
        Button changePassword = (Button) parent.findViewById(R.id.button_edit_credentials);

        if (TextUtils.isEmpty(repo.username)) {
            usernameLabel.setVisibility(View.GONE);
            username.setVisibility(View.GONE);
            username.setText("");
            changePassword.setVisibility(View.GONE);
        } else {
            usernameLabel.setVisibility(View.VISIBLE);
            username.setVisibility(View.VISIBLE);
            username.setText(repo.username);
            changePassword.setVisibility(View.VISIBLE);
        }
    }

    private void updateRepoView() {

        if (repo.hasBeenUpdated()) {
            updateViewForExistingRepo(repoView);
        } else {
            updateViewForNewRepo(repoView);
        }

    }

    private void updateViewForNewRepo(View repoView) {
        setMultipleViewVisibility(repoView, HIDE_IF_EXISTS, View.VISIBLE);
        setMultipleViewVisibility(repoView, SHOW_IF_EXISTS, View.GONE);
    }

    private void updateViewForExistingRepo(View repoView) {
        setMultipleViewVisibility(repoView, SHOW_IF_EXISTS, View.VISIBLE);
        setMultipleViewVisibility(repoView, HIDE_IF_EXISTS, View.GONE);

        TextView name = (TextView) repoView.findViewById(R.id.text_repo_name);
        TextView numApps = (TextView) repoView.findViewById(R.id.text_num_apps);
        TextView lastUpdated = (TextView) repoView.findViewById(R.id.text_last_update);

        if (repo.mirrors != null) {
            TextView officialMirrorsLabel = (TextView) repoView.findViewById(R.id.label_official_mirrors);
            officialMirrorsLabel.setVisibility(View.VISIBLE);
            TextView officialMirrorsText = (TextView) repoView.findViewById(R.id.text_official_mirrors);
            officialMirrorsText.setVisibility(View.VISIBLE);
            StringBuilder builder = new StringBuilder();
            for (String url : repo.mirrors) {
                builder.append("◦ ");
                builder.append(url);
                builder.append('\n');
            }
            officialMirrorsText.setText(builder.toString());
        }
        if (repo.userMirrors != null) {
            TextView userMirrorsLabel = (TextView) repoView.findViewById(R.id.label_user_mirrors);
            userMirrorsLabel.setVisibility(View.VISIBLE);
            TextView userMirrorsText = (TextView) repoView.findViewById(R.id.text_user_mirrors);
            userMirrorsText.setVisibility(View.VISIBLE);
            StringBuilder builder = new StringBuilder();
            for (String url : repo.userMirrors) {
                builder.append("◦ ");
                builder.append(url);
                builder.append('\n');
            }
            userMirrorsText.setText(builder.toString());
        }

        name.setText(repo.name);

        int appCount = RepoProvider.Helper.countAppsForRepo(this, repoId);
        numApps.setText(String.format(Locale.getDefault(), "%d", appCount));

        setupDescription(repoView, repo);
        setupRepoFingerprint(repoView, repo);
        setupCredentials(repoView, repo);

        // Repos that existed before this feature was supported will have an
        // "Unknown" last update until next time they update...
        if (repo.lastUpdated == null) {
            lastUpdated.setText(R.string.unknown);
        } else {
            int format = DateUtils.isToday(repo.lastUpdated.getTime()) ?
                    DateUtils.FORMAT_SHOW_TIME :
                    DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE;
            lastUpdated.setText(DateUtils.formatDateTime(this,
                    repo.lastUpdated.getTime(), format));
        }
    }

    private void promptForDelete() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.repo_confirm_delete_title)
                .setMessage(R.string.repo_confirm_delete_body)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        RepoProvider.Helper.remove(getApplicationContext(), repoId);
                        finish();
                    }
                }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing...
                    }
                }
        ).show();
    }

    public void showChangePasswordDialog(final View parentView) {

        final View view = getLayoutInflater().inflate(R.layout.login, null);
        final AlertDialog credentialsDialog = new AlertDialog.Builder(this).setView(view).create();
        final EditText nameInput = (EditText) view.findViewById(R.id.edit_name);
        final EditText passwordInput = (EditText) view.findViewById(R.id.edit_password);

        nameInput.setText(repo.username);
        passwordInput.requestFocus();

        credentialsDialog.setTitle(R.string.repo_edit_credentials);
        credentialsDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        credentialsDialog.setButton(DialogInterface.BUTTON_POSITIVE,
                getString(R.string.ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        final String name = nameInput.getText().toString();
                        final String password = passwordInput.getText().toString();

                        if (!TextUtils.isEmpty(name)) {

                            final ContentValues values = new ContentValues(2);
                            values.put(RepoTable.Cols.USERNAME, name);
                            values.put(RepoTable.Cols.PASSWORD, password);

                            RepoProvider.Helper.update(RepoDetailsActivity.this, repo, values);

                            updateRepoView();

                            dialog.dismiss();

                        } else {

                            Toast.makeText(RepoDetailsActivity.this, R.string.repo_error_empty_username,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });

        credentialsDialog.show();
    }
}

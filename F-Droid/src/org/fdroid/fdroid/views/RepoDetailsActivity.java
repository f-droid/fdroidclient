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
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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

import java.util.Locale;

public class RepoDetailsActivity extends ActionBarActivity {
    private static final String TAG = "RepoDetailsActivity";

    public static final String MIME_TYPE = "application/vnd.org.fdroid.fdroid.repo";
    public static final String ARG_REPO_ID = "repo_id";

    /**
     * If the repo has been updated at least once, then we will show
     * all of this info, otherwise they will be hidden.
     */
    private static final int[] SHOW_IF_EXISTS = {
            R.id.label_repo_name,
            R.id.text_repo_name,
            R.id.label_description,
            R.id.text_description,
            R.id.label_num_apps,
            R.id.text_num_apps,
            R.id.label_last_update,
            R.id.text_last_update,
            R.id.label_repo_fingerprint,
            R.id.text_repo_fingerprint,
            R.id.text_repo_fingerprint_description
    };
    /**
     * If the repo has <em>not</em> been updated yet, then we only show
     * these, otherwise they are hidden.
     */
    private static final int[] HIDE_IF_EXISTS = {
            R.id.text_not_yet_updated,
            R.id.btn_update
    };
    private Repo repo;
    private long repoId;
    private View repoView;

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

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.repodetails);
        repoView = findViewById(R.id.repoView);

        repoId = getIntent().getLongExtra(ARG_REPO_ID, 0);
        final String[] projection = {
                RepoProvider.DataColumns.NAME,
                RepoProvider.DataColumns.ADDRESS,
                RepoProvider.DataColumns.FINGERPRINT
        };
        repo = RepoProvider.Helper.findById(this, repoId, projection);

        setTitle(repo.name);

        TextView inputUrl = (TextView) findViewById(R.id.input_repo_url);
        inputUrl.setText(repo.address);

        Button update = (Button) repoView.findViewById(R.id.btn_update);
        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performUpdate();
            }
        });

        Uri uri = Uri.parse(repo.address);
        uri = uri.buildUpon().appendQueryParameter("fingerprint", repo.fingerprint).build();
        String qrUriString = uri.toString().toUpperCase(Locale.ENGLISH);
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

        // FDroid.java and AppDetails set different NFC actions, so reset here
        setNfc();
        processIntent(getIntent());
    }

    @Override
    public void onNewIntent(Intent i) {
        // onResume gets called after this to handle the intent
        setIntent(i);
    }

    @TargetApi(9)
    void processIntent(Intent i) {
        if (Build.VERSION.SDK_INT < 9)
            return;
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
            if (statusCode == UpdateService.STATUS_COMPLETE_WITH_CHANGES)
                updateRepoView();
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
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.menu_update:
                performUpdate();
                return true;
            case R.id.menu_delete:
                promptForDelete();
                return true;
            case R.id.menu_enable_nfc:
                Intent intent = new Intent(this, NfcNotEnabledActivity.class);
                startActivity(intent);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (Build.VERSION.SDK_INT >= 14) {
            prepareNfcMenuItems(menu);
        }
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
        int repoFingerprintColor;

        // TODO show the current state of the signature check, not just whether there is a key or not
        if (TextUtils.isEmpty(repo.fingerprint) && TextUtils.isEmpty(repo.pubkey)) {
            repoFingerprint = getResources().getString(R.string.unsigned);
            repoFingerprintColor = getResources().getColor(R.color.unsigned);
            repoFingerprintDescView.setVisibility(View.VISIBLE);
            repoFingerprintDescView.setText(getResources().getString(R.string.unsigned_description));
        } else {
            // this is based on repo.fingerprint always existing, which it should
            repoFingerprint = Utils.formatFingerprint(this, repo.fingerprint);
            repoFingerprintColor = getResources().getColor(R.color.signed);
            repoFingerprintDescView.setVisibility(View.GONE);
        }

        repoFingerprintView.setText(repoFingerprint);
        repoFingerprintView.setTextColor(repoFingerprintColor);
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

        name.setText(repo.name);

        int appCount = RepoProvider.Helper.countAppsForRepo(this, repoId);
        numApps.setText(Integer.toString(appCount));

        setupDescription(repoView, repo);
        setupRepoFingerprint(repoView, repo);

        // Repos that existed before this feature was supported will have an
        // "Unknown" last update until next time they update...
        String lastUpdate = repo.lastUpdated != null
                ? repo.lastUpdated.toString() : getString(R.string.unknown);
        lastUpdated.setText(lastUpdate);
    }

    /**
     * When an update is performed, notify the listener so that the repo
     * list can be updated. We will perform the update ourselves though.
     */
    private void performUpdate() {
        // Ensure repo is enabled before updating...
        ContentValues values = new ContentValues(1);
        values.put(RepoProvider.DataColumns.IN_USE, 1);
        RepoProvider.Helper.update(this, repo, values);

        UpdateService.updateRepoNow(repo.address, this);
    }

    private void promptForDelete() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.repo_confirm_delete_title)
                .setMessage(R.string.repo_confirm_delete_body)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        RepoProvider.Helper.remove(getApplicationContext(), repoId);
                        finish();
                    }
                }).setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing...
                    }
                }
        ).show();
    }

}

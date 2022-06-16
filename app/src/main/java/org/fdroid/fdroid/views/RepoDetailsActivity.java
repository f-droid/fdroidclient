package org.fdroid.fdroid.views;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputLayout;

import org.fdroid.database.AppDao;
import org.fdroid.database.Repository;
import org.fdroid.database.RepositoryDao;
import org.fdroid.download.Mirror;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.NfcHelper;
import org.fdroid.fdroid.NfcNotEnabledActivity;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.UpdateService;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.DBHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

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
    private Repository repo;
    private long repoId;
    private View repoView;
    private String shareUrl;

    private MirrorAdapter adapterToNotify;

    private RepositoryDao repositoryDao;
    private AppDao appDao;
    @Nullable
    private Disposable disposable;

    /**
     * Help function to make switching between two view states easier.
     * Perhaps there is a better way to do this. I recall that using Adobe
     * Flex, there was a thing called "ViewStates" for exactly this. Wonder if
     * that exists in  Android?
     */
    private static void setMultipleViewVisibility(View parent, int[] viewIds, int visibility) {
        for (int viewId : viewIds) {
            parent.findViewById(viewId).setVisibility(visibility);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        FDroidApp fdroidApp = (FDroidApp) getApplication();
        fdroidApp.applyPureBlackBackgroundInDarkTheme(this);
        repositoryDao = DBHelper.getDb(this).getRepositoryDao();
        appDao = DBHelper.getDb(this).getAppDao();

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_repo_details);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        repoView = findViewById(R.id.repo_view);

        repoId = getIntent().getLongExtra(ARG_REPO_ID, 0);
        repo = FDroidApp.getRepo(repoId);

        TextView inputUrl = findViewById(R.id.input_repo_url);
        inputUrl.setText(repo.getAddress());

        RecyclerView officialMirrorListView = findViewById(R.id.official_mirror_list);
        officialMirrorListView.setLayoutManager(new LinearLayoutManager(this));
        adapterToNotify = new MirrorAdapter(repo, repo.getAllMirrors(false));
        officialMirrorListView.setAdapter(adapterToNotify);

        RecyclerView userMirrorListView = findViewById(R.id.user_mirror_list);
        userMirrorListView.setLayoutManager(new LinearLayoutManager(this));
        MirrorAdapter userMirrorAdapter = new MirrorAdapter(repo, repo.getUserMirrors().size());
        userMirrorAdapter.setUserMirrors(repo.getUserMirrors());
        userMirrorListView.setAdapter(userMirrorAdapter);

        if (repo.getAddress().startsWith("content://") || repo.getAddress().startsWith("file://")) {
            // no need to show a QR Code, it is not shareable
            return;
        }

        Uri uri = Uri.parse(repo.getAddress());
        if (repo.getFingerprint() != null) {
            uri = uri.buildUpon().appendQueryParameter("fingerprint", repo.getFingerprint()).build();
        }
        String qrUriString = uri.toString();
        disposable = Utils.generateQrBitmap(this, qrUriString)
                .subscribe(bitmap -> {
                    final ImageView qrCode = findViewById(R.id.qr_code);
                    if (qrCode != null) {
                        qrCode.setImageBitmap(bitmap);
                    }
                });
    }

    @Override
    protected void onDestroy() {
        if (disposable != null) disposable.dispose();
        super.onDestroy();
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
        repo = FDroidApp.getRepo(repoId);
        updateRepoView();

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(UpdateService.LOCAL_ACTION_STATUS));

        // FDroid.java and AppDetailsActivity set different NFC actions, so reset here
        setNfc();
        processIntent(getIntent());
    }

    @Override
    public void onNewIntent(Intent i) {
        super.onNewIntent(i);
        // onResume gets called after this to handle the intent
        setIntent(i);
    }

    private void processIntent(Intent i) {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(i.getAction())) {
            Parcelable[] rawMsgs = i.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
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
        needsEnableNfcMenuItem = !nfcAdapter.isNdefPushEnabled();

        menuItem.setVisible(needsEnableNfcMenuItem);
    }

    private void prepareShareMenuItems(Menu menu) {
        if (!TextUtils.isEmpty(repo.getAddress())) {
            if (!TextUtils.isEmpty(repo.getCertificate())) {
                shareUrl = Uri.parse(repo.getAddress()).buildUpon()
                        .appendQueryParameter("fingerprint", repo.getFingerprint()).toString();
            } else {
                shareUrl = repo.getAddress();
            }
            menu.findItem(R.id.action_share).setVisible(true);
        } else {
            menu.findItem(R.id.action_share).setVisible(false);
        }
    }

    private void setupDescription(View parent, Repository repo) {

        TextView descriptionLabel = (TextView) parent.findViewById(R.id.label_description);
        TextView description = (TextView) parent.findViewById(R.id.text_description);

        String desc = repo.getDescription(App.getLocales());
        if (desc == null || TextUtils.isEmpty(desc)) {
            descriptionLabel.setVisibility(View.GONE);
            description.setVisibility(View.GONE);
            description.setText("");
        } else {
            descriptionLabel.setVisibility(View.VISIBLE);
            description.setVisibility(View.VISIBLE);
            description.setText(desc.replaceAll("\n", " "));
        }
    }

    private void setupRepoFingerprint(View parent, Repository repo) {
        TextView repoFingerprintView = (TextView) parent.findViewById(R.id.text_repo_fingerprint);
        TextView repoFingerprintDescView = (TextView) parent.findViewById(R.id.text_repo_fingerprint_description);

        String repoFingerprint;

        // TODO show the current state of the signature check, not just whether there is a key or not
        if (TextUtils.isEmpty(repo.getCertificate())) {
            repoFingerprint = getResources().getString(R.string.unsigned);
            repoFingerprintView.setTextColor(ContextCompat.getColor(this, R.color.unsigned));
            repoFingerprintDescView.setVisibility(View.VISIBLE);
            repoFingerprintDescView.setText(getResources().getString(R.string.unsigned_description));
        } else {
            // this is based on repo.fingerprint always existing, which it should
            repoFingerprint = Utils.formatFingerprint(this, repo.getFingerprint());
            repoFingerprintDescView.setVisibility(View.GONE);
        }

        repoFingerprintView.setText(repoFingerprint);
    }

    private void setupCredentials(View parent, Repository repo) {

        TextView usernameLabel = parent.findViewById(R.id.label_username);
        TextView username = parent.findViewById(R.id.text_username);
        Button changePassword = parent.findViewById(R.id.button_edit_credentials);
        changePassword.setOnClickListener(this::showChangePasswordDialog);

        if (TextUtils.isEmpty(repo.getUsername())) {
            usernameLabel.setVisibility(View.GONE);
            username.setVisibility(View.GONE);
            username.setText("");
            changePassword.setVisibility(View.GONE);
        } else {
            usernameLabel.setVisibility(View.VISIBLE);
            username.setVisibility(View.VISIBLE);
            username.setText(repo.getUsername());
            changePassword.setVisibility(View.VISIBLE);
        }
    }

    private void updateRepoView() {
        TextView officialMirrorsLabel = repoView.findViewById(R.id.label_official_mirrors);
        RecyclerView officialMirrorList = repoView.findViewById(R.id.official_mirror_list);
        if (repo.getAllMirrors().size() > 1) {
            // don't show this if there is only the canonical URL available, and no other mirrors
            officialMirrorsLabel.setVisibility(View.VISIBLE);
            officialMirrorList.setVisibility(View.VISIBLE);
        } else {
            officialMirrorsLabel.setVisibility(View.GONE);
            officialMirrorList.setVisibility(View.GONE);
        }

        TextView userMirrorsLabel = repoView.findViewById(R.id.label_user_mirrors);
        RecyclerView userMirrorList = repoView.findViewById(R.id.user_mirror_list);
        if (repo.getUserMirrors().size() > 0) {
            userMirrorsLabel.setVisibility(View.VISIBLE);
            userMirrorList.setVisibility(View.VISIBLE);
        } else {
            userMirrorsLabel.setVisibility(View.GONE);
            userMirrorList.setVisibility(View.GONE);
        }

        if (repo.getLastETag() != null) {
            updateViewForExistingRepo(repoView);
        } else {
            setMultipleViewVisibility(repoView, HIDE_IF_EXISTS, View.VISIBLE);
            setMultipleViewVisibility(repoView, SHOW_IF_EXISTS, View.GONE);
        }
    }

    private void updateViewForExistingRepo(View repoView) {
        setMultipleViewVisibility(repoView, SHOW_IF_EXISTS, View.VISIBLE);
        setMultipleViewVisibility(repoView, HIDE_IF_EXISTS, View.GONE);

        TextView name = repoView.findViewById(R.id.text_repo_name);
        TextView numApps = repoView.findViewById(R.id.text_num_apps);
        TextView lastUpdated = repoView.findViewById(R.id.text_last_update);

        name.setText(repo.getName(App.getLocales()));
        disposable = Single.fromCallable(() -> appDao.getNumberOfAppsInRepository(repoId))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(appCount -> numApps.setText(String.format(Locale.getDefault(), "%d", appCount)));

        setupDescription(repoView, repo);
        setupRepoFingerprint(repoView, repo);
        setupCredentials(repoView, repo);

        // Repos that existed before this feature was supported will have an
        // "Unknown" last update until next time they update...
        if (repo.getLastUpdated() == null) {
            lastUpdated.setText(R.string.unknown);
        } else {
            int format = DateUtils.isToday(repo.getLastUpdated()) ?
                    DateUtils.FORMAT_SHOW_TIME :
                    DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE;
            lastUpdated.setText(DateUtils.formatDateTime(this, repo.getLastUpdated(), format));
        }
    }

    private void promptForDelete() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.repo_confirm_delete_title)
                .setMessage(R.string.repo_confirm_delete_body)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        runOffUiThread(() -> {
                            repositoryDao.deleteRepository(repoId);
                            return true;
                        });
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
        final TextInputLayout nameInputLayout = view.findViewById(R.id.edit_name);
        final TextInputLayout passwordInputLayout = view.findViewById(R.id.edit_password);
        final EditText nameInput = nameInputLayout.getEditText();
        final EditText passwordInput = passwordInputLayout.getEditText();

        nameInput.setText(repo.getUsername());
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
                            runOffUiThread(() -> {
                                repositoryDao.updateUsernameAndPassword(repo.getRepoId(), name, password);
                                return true;
                            });
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

    private class MirrorAdapter extends RecyclerView.Adapter<MirrorAdapter.MirrorViewHolder> {
        private final Repository repo;
        private final List<Mirror> mirrors;
        private final HashSet<String> disabledMirrors;

        class MirrorViewHolder extends RecyclerView.ViewHolder {
            View view;

            MirrorViewHolder(View view) {
                super(view);
                this.view = view;
            }
        }

        MirrorAdapter(Repository repo, List<Mirror> mirrors) {
            this.repo = repo;
            this.mirrors = mirrors;
            disabledMirrors = new HashSet<>(repo.getDisabledMirrors());
        }

        MirrorAdapter(Repository repo, int userMirrorSize) {
            this.repo = repo;
            this.mirrors = new ArrayList<>(userMirrorSize);
            disabledMirrors = new HashSet<>(repo.getDisabledMirrors());
        }

        void setUserMirrors(List<String> userMirrors) {
            for (String url : userMirrors) {
                this.mirrors.add(new Mirror(url));
            }
        }

        @NonNull
        @Override
        public MirrorAdapter.MirrorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.repo_item, parent, false);
            return new MirrorViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull MirrorViewHolder holder, final int position) {
            TextView repoNameTextView = holder.view.findViewById(R.id.repo_name);
            Mirror mirror = mirrors.get(position);
            repoNameTextView.setText(mirror.getBaseUrl());

            final String itemMirror = mirror.getBaseUrl();
            boolean enabled = true;
            for (String disabled : repo.getDisabledMirrors()) {
                if (TextUtils.equals(itemMirror, disabled)) {
                    enabled = false;
                    break;
                }
            }
            CompoundButton switchView = holder.view.findViewById(R.id.repo_switch);
            switchView.setChecked(enabled);
            switchView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        disabledMirrors.remove(itemMirror);
                    } else {
                        disabledMirrors.add(itemMirror);
                    }

                    List<Mirror> mirrors = repo.getAllMirrors(true);
                    int totalMirrors = mirrors.size();
                    if (disabledMirrors.size() == totalMirrors) {
                        // if all mirrors are disabled, re-enable canonical repo as mirror
                        disabledMirrors.remove(repo.getAddress());
                        adapterToNotify.notifyItemChanged(0);
                    }
                    runOffUiThread(() -> {
                        repositoryDao.updateDisabledMirrors(repo.getRepoId(), new ArrayList<>(disabledMirrors));
                        return true;
                    });
                }
            });

            View repoUnverified = holder.view.findViewById(R.id.repo_unverified);
            repoUnverified.setVisibility(View.GONE);

            View repoUnsigned = holder.view.findViewById(R.id.repo_unsigned);
            repoUnsigned.setVisibility(View.GONE);
        }

        @Override
        public int getItemCount() {
            if (mirrors == null) {
                return 0;
            }
            return mirrors.size();
        }
    }

    private void runOffUiThread(Callable<?> r) {
        disposable = Single.fromCallable(r)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
    }
}

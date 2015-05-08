package org.fdroid.fdroid.views;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.NfcHelper;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.views.fragments.RepoDetailsFragment;

public class RepoDetailsActivity extends ActionBarActivity {
    private static final String TAG = "RepoDetailsActivity";

    private Repo repo;

    static final String MIME_TYPE = "application/vnd.org.fdroid.fdroid.repo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        ((FDroidApp) getApplication()).applyTheme(this);
        super.onCreate(savedInstanceState);

        long repoId = getIntent().getLongExtra(RepoDetailsFragment.ARG_REPO_ID, 0);

        if (savedInstanceState == null) {

            // Need to set a dummy view (which will get overridden by the fragment manager
            // below) so that we can call setContentView(). This is a work around for
            // a (bug?) thing in 3.0, 3.1 which requires setContentView to be invoked before
            // the actionbar is played with:
            // http://blog.perpetumdesign.com/2011/08/strange-case-of-dr-action-and-mr-bar.html
            if (Build.VERSION.SDK_INT >= 11 && Build.VERSION.SDK_INT <= 13) {
                setContentView(new LinearLayout(this));
            }

            RepoDetailsFragment fragment = new RepoDetailsFragment();
            fragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(android.R.id.content, fragment)
                    .commit();
        }

        final String[] projection = {
                RepoProvider.DataColumns.NAME,
                RepoProvider.DataColumns.ADDRESS,
                RepoProvider.DataColumns.FINGERPRINT
        };
        repo = RepoProvider.Helper.findById(this, repoId, projection);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(repo.getName());
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
            Log.i(TAG, "Got this URL: " + url);
            Toast.makeText(this, "Got this URL: " + url, Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            String packageName = getPackageName();
            intent.setClassName(packageName, packageName + ".ManageRepo");
            startActivity(intent);
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}

package org.fdroid.fdroid.views.swap;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.localrepo.LocalRepoManager;

import java.util.Set;

public class SwapWorkflowActivity extends FragmentActivity {

    private ViewGroup container;

    private enum State {
        INTRO, SELECT_APPS, JOIN_WIFI
    }

    public interface InnerView {
        /** @return True if the menu should be shown. */
        boolean buildMenu(Menu menu, @NonNull MenuInflater inflater);
    }

    private State currentState = State.INTRO;
    private InnerView currentView;
    private boolean hasPreparedLocalRepo = false;
    private UpdateAsyncTask updateSwappableAppsTask = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.swap_activity);
        container = (ViewGroup) findViewById(R.id.fragment_container);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean parent = super.onPrepareOptionsMenu(menu);
        boolean inner  = currentView.buildMenu(menu, getMenuInflater());
        return parent || inner;
    }

    @Override
    protected void onResume() {
        super.onResume();
        showView();
    }

    private void showView() {
        if (currentState == State.INTRO) {
            showIntro();
        }
    }

    private void inflateInnerView(@LayoutRes int viewRes) {
        container.removeAllViews();
        View view = ((LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(viewRes, container, false);
        currentView = (InnerView)view;
        container.addView(view);
        supportInvalidateOptionsMenu();
    }

    private void showIntro() {
        inflateInnerView(R.layout.swap_blank);
    }

    public void showSelectApps() {
        inflateInnerView(R.layout.swap_select_apps);
    }

    // TODO: Pass in the selected apps, then we can figure out whether they have changed.
    public void onAppsSelected(Set<String> selectedApps) {
        if (updateSwappableAppsTask == null && !hasPreparedLocalRepo) {
            updateSwappableAppsTask = new UpdateAsyncTask(this, selectedApps);
            updateSwappableAppsTask.execute();
        } else {
            showJoinWifi();
        }
    }

    /**
     * Once the UpdateAsyncTask has finished preparing our repository index, we can
     * show the next screen to the user.
     */
    private void onLocalRepoPrepared() {
        updateSwappableAppsTask = null;
        hasPreparedLocalRepo = true;
        showJoinWifi();
    }

    private void showJoinWifi() {
        inflateInnerView(R.layout.swap_join_wifi);
    }

    class UpdateAsyncTask extends AsyncTask<Void, String, Void> {

        @SuppressWarnings("UnusedDeclaration")
        private static final String TAG = "UpdateAsyncTask";

        @NonNull
        private final ProgressDialog progressDialog;

        @NonNull
        private final Set<String> selectedApps;

        @NonNull
        private final Uri sharingUri;

        @NonNull
        private final Context context;

        public UpdateAsyncTask(Context c, @NonNull Set<String> apps) {
            context = SwapWorkflowActivity.this.getApplicationContext();
            selectedApps = apps;
            progressDialog = new ProgressDialog(c);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setTitle(R.string.updating);
            sharingUri = Utils.getSharingUri(FDroidApp.repo);
        }

        @Override
        protected void onPreExecute() {
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                final LocalRepoManager lrm = LocalRepoManager.get(context);
                publishProgress(getString(R.string.deleting_repo));
                lrm.deleteRepo();
                for (String app : selectedApps) {
                    publishProgress(String.format(getString(R.string.adding_apks_format), app));
                    lrm.addApp(context, app);
                }
                lrm.writeIndexPage(sharingUri.toString());
                publishProgress(getString(R.string.writing_index_jar));
                lrm.writeIndexJar();
                publishProgress(getString(R.string.linking_apks));
                lrm.copyApksToRepo();
                publishProgress(getString(R.string.copying_icons));
                // run the icon copy without progress, its not a blocker
                new AsyncTask<Void, Void, Void>() {

                    @Override
                    protected Void doInBackground(Void... params) {
                        lrm.copyIconsToRepo();
                        return null;
                    }
                }.execute();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            super.onProgressUpdate(progress);
            progressDialog.setMessage(progress[0]);
        }

        @Override
        protected void onPostExecute(Void result) {
            progressDialog.dismiss();
            Toast.makeText(context, R.string.updated_local_repo, Toast.LENGTH_SHORT).show();
            onLocalRepoPrepared();
        }
    }

}

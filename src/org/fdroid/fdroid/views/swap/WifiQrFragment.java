package org.fdroid.fdroid.views.swap;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.LightingColorFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.QrGenAsyncTask;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.localrepo.LocalRepoManager;
import org.fdroid.fdroid.localrepo.LocalRepoService;
import org.fdroid.fdroid.net.WifiStateChangeService;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class WifiQrFragment extends Fragment {

    private BroadcastReceiver onWifiChange = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent i) {
            setUIFromWifi();
        }
    };

    private BroadcastReceiver onLocalRepoChange = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent i) {
            setUIFromWifi();
        }
    };

    private Timer stopTimer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.swap_wifi_qr, container, false);
        ImageView qrImage = (ImageView)view.findViewById(R.id.wifi_qr_code);

        // Replace all blacks with the background blue.
        qrImage.setColorFilter(new LightingColorFilter(0xffffffff, getResources().getColor(R.color.swap_blue)));

        return view;
    }

    public void onResume() {
        super.onResume();
        setUIFromWifi();

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(onWifiChange,
                new IntentFilter(WifiStateChangeService.BROADCAST));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(onLocalRepoChange,
                new IntentFilter(LocalRepoService.STATE));

        // if no local repo exists, create one with only FDroid in it
        if (!LocalRepoManager.get(getActivity()).xmlIndex.exists())
            new UpdateAsyncTask(getActivity(), new String[] {
                    getActivity().getPackageName(),
            }).execute();

        // start repo by default
        FDroidApp.startLocalRepoService(getActivity());
        // reset the timer if viewing this Activity again
        if (stopTimer != null)
            stopTimer.cancel();
        // automatically turn off after 15 minutes
        stopTimer = new Timer();
        stopTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                FDroidApp.stopLocalRepoService(getActivity());
            }
        }, 900000); // 15 minutes
    }

    @TargetApi(14)
    private void setUIFromWifi() {

        if (TextUtils.isEmpty(FDroidApp.repo.address))
            return;

        // the fingerprint is not useful on the button label
        String buttonLabel = FDroidApp.ipAddressString + ":" + FDroidApp.port;
        TextView ipAddressView = (TextView) getView().findViewById(R.id.device_ip_address);
        ipAddressView.setText(buttonLabel);

        /*
         * Set URL to UPPER for compact QR Code, FDroid will translate it back.
         * Remove the SSID from the query string since SSIDs are case-sensitive.
         * Instead the receiver will have to rely on the BSSID to find the right
         * wifi AP to join. Lots of QR Scanners are buggy and do not respect
         * custom URI schemes, so we have to use http:// or https:// :-(
         */
        final String qrUriString = Utils.getSharingUri(getActivity(), FDroidApp.repo).toString()
                .replaceFirst("fdroidrepo", "http")
                .replaceAll("ssid=[^?]*", "")
                .toUpperCase(Locale.ENGLISH);

        Log.i("QRURI", qrUriString);

        // zxing requires >= 8
        // TODO: What about 7? I don't feel comfortable bumping the min version for this...
        // I would suggest show some alternate info, with directions for how to add a new repository manually.
        if (Build.VERSION.SDK_INT >= 8)
            new QrGenAsyncTask(getActivity(), R.id.wifi_qr_code).execute(qrUriString);

    }

    class UpdateAsyncTask extends AsyncTask<Void, String, Void> {
        private static final String TAG = "UpdateAsyncTask";
        private ProgressDialog progressDialog;
        private String[] selectedApps;
        private Uri sharingUri;

        public UpdateAsyncTask(Context c, String[] apps) {
            selectedApps = apps;
            progressDialog = new ProgressDialog(c);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setTitle(R.string.updating);
            sharingUri = Utils.getSharingUri(c, FDroidApp.repo);
        }

        @Override
        protected void onPreExecute() {
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                final LocalRepoManager lrm = LocalRepoManager.get(getActivity());
                publishProgress(getString(R.string.deleting_repo));
                lrm.deleteRepo();
                for (String app : selectedApps) {
                    publishProgress(String.format(getString(R.string.adding_apks_format), app));
                    lrm.addApp(getActivity(), app);
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
            Toast.makeText(getActivity(), R.string.updated_local_repo, Toast.LENGTH_SHORT).show();
        }
    }

}

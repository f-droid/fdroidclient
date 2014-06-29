package org.fdroid.fdroid.views.swap;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.LightingColorFilter;
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
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.QrGenAsyncTask;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.net.WifiStateChangeService;

import java.util.Locale;

public class WifiQrFragment extends Fragment {

    private BroadcastReceiver onWifiChange = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent i) {
            setUIFromWifi();
        }
    };

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

}

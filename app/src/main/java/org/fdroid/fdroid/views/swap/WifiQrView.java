package org.fdroid.fdroid.views.swap;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.LightingColorFilter;
import android.net.Uri;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.QrGenAsyncTask;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.localrepo.SwapService;
import org.fdroid.fdroid.net.WifiStateChangeService;

import java.net.URI;
import java.util.List;
import java.util.Locale;

public class WifiQrView extends ScrollView implements SwapWorkflowActivity.InnerView {

    private static final String TAG = "WifiQrView";

    public WifiQrView(Context context) {
        super(context);
    }

    public WifiQrView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WifiQrView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(21)
    public WifiQrView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private SwapWorkflowActivity getActivity() {
        return (SwapWorkflowActivity) getContext();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setUIFromWifi();

        ImageView qrImage = (ImageView) findViewById(R.id.wifi_qr_code);

        // Replace all blacks with the background blue.
        qrImage.setColorFilter(new LightingColorFilter(0xffffffff, getResources().getColor(R.color.swap_blue)));

        Button openQr = (Button) findViewById(R.id.btn_qr_scanner);
        openQr.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().initiateQrScan();
            }
        });

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                onWifiStateChanged, new IntentFilter(WifiStateChangeService.BROADCAST));
    }

    /**
     * Remove relevant listeners/receivers/etc so that they do not receive and process events
     * when this view is not in use.
     */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(onWifiStateChanged);
    }

    @Override
    public boolean buildMenu(Menu menu, @NonNull MenuInflater inflater) {
        return false;
    }

    @Override
    public int getStep() {
        return SwapService.STEP_WIFI_QR;
    }

    @Override
    public int getPreviousStep() {
        // TODO: Find a way to make this optionally go back to the NFC screen if appropriate.
        return SwapService.STEP_JOIN_WIFI;
    }

    @ColorRes
    public int getToolbarColour() {
        return R.color.swap_blue;
    }

    @Override
    public String getToolbarTitle() {
        return getResources().getString(R.string.swap_scan_qr);
    }

    private void setUIFromWifi() {

        if (TextUtils.isEmpty(FDroidApp.repo.address)) {
            return;
        }

        String scheme = Preferences.get().isLocalRepoHttpsEnabled() ? "https://" : "http://";

        // the fingerprint is not useful on the button label
        String buttonLabel = scheme + FDroidApp.ipAddressString + ":" + FDroidApp.port;
        TextView ipAddressView = (TextView) findViewById(R.id.device_ip_address);
        ipAddressView.setText(buttonLabel);

        Uri sharingUri = Utils.getSharingUri(FDroidApp.repo);
        String qrUriString = scheme + sharingUri.getHost();
        if (sharingUri.getPort() != 80) {
            qrUriString += ":" + sharingUri.getPort();
        }
        qrUriString += sharingUri.getPath();
        boolean first = true;

        // Andorid provides an API for getting the query parameters and iterating over them:
        //   Uri.getQueryParameterNames()
        // But it is only available on later Android versions. As such we use URLEncodedUtils instead.
        List<NameValuePair> parameters = URLEncodedUtils.parse(URI.create(sharingUri.toString()), "UTF-8");
        for (NameValuePair parameter : parameters) {
            if (!"ssid".equals(parameter.getName())) {
                if (first) {
                    qrUriString += "?";
                    first = false;
                } else {
                    qrUriString += "&";
                }
                qrUriString += parameter.getName().toUpperCase(Locale.ENGLISH) + "=" +
                        parameter.getValue().toUpperCase(Locale.ENGLISH);
            }
        }

        Utils.debugLog(TAG, "Encoded swap URI in QR Code: " + qrUriString);

        new QrGenAsyncTask(getActivity(), R.id.wifi_qr_code).execute(qrUriString);

    }

    private final BroadcastReceiver onWifiStateChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setUIFromWifi();
        }
    };

}

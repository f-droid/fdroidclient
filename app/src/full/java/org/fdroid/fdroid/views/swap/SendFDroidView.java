package org.fdroid.fdroid.views.swap;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.LightingColorFilter;
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
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.QrGenAsyncTask;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.localrepo.SwapService;
import org.fdroid.fdroid.net.WifiStateChangeService;
import org.fdroid.fdroid.views.swap.device.camera.CameraCharacteristicsChecker;

public class SendFDroidView extends ScrollView implements SwapWorkflowActivity.InnerView {

    private static final String TAG = "SendFDroidView";

    public SendFDroidView(Context context) {
        super(context);
    }

    public SendFDroidView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SendFDroidView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(21)
    public SendFDroidView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private SwapWorkflowActivity getActivity() {
        return (SwapWorkflowActivity) getContext();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setUIFromWifi();
        setUpWarningMessageQrScan();

        ImageView qrImage = (ImageView) findViewById(R.id.wifi_qr_code);

        // Replace all blacks with the background blue.
        qrImage.setColorFilter(new LightingColorFilter(0xffffffff, getResources().getColor(R.color.swap_blue)));

        Button useBluetooth = (Button) findViewById(R.id.btn_use_bluetooth);
        useBluetooth.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().showIntro();
                getActivity().sendFDroidBluetooth();
            }
        });

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                onWifiStateChanged, new IntentFilter(WifiStateChangeService.BROADCAST));
    }

    private void setUpWarningMessageQrScan() {
        final View qrWarningMessage = findViewById(R.id.warning_qr_scanner);
        final boolean hasAutofocus = CameraCharacteristicsChecker.getInstance(getContext()).hasAutofocus();
        final int visiblity = hasAutofocus ? GONE : VISIBLE;
        qrWarningMessage.setVisibility(visiblity);
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
        return SwapService.STEP_INTRO;
    }

    @Override
    public int getPreviousStep() {
        return SwapService.STEP_INTRO;
    }

    @ColorRes
    public int getToolbarColour() {
        return R.color.swap_blue;
    }

    @Override
    public String getToolbarTitle() {
        return getResources().getString(R.string.swap_send_fdroid);
    }

    @SuppressLint("HardwareIds")
    private void setUIFromWifi() {
        if (TextUtils.isEmpty(FDroidApp.repo.address)) {
            return;
        }

        String scheme = Preferences.get().isLocalRepoHttpsEnabled() ? "https://" : "http://";

        // the fingerprint is not useful on the button label
        String qrUriString = scheme + FDroidApp.ipAddressString + ":" + FDroidApp.port;
        TextView ipAddressView = (TextView) findViewById(R.id.device_ip_address);
        ipAddressView.setText(qrUriString);

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

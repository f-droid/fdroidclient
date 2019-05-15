package org.fdroid.fdroid.views.swap;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.localrepo.SwapView;

public class WifiQrView extends SwapView {

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

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        Button openQr = (Button) findViewById(R.id.btn_qr_scanner);
        openQr.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().initiateQrScan();
            }
        });
    }
}

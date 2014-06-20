package org.fdroid.fdroid.views.swap;

import android.annotation.TargetApi;
import android.content.Context;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;

public class NfcSwapFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.swap_nfc, container, false);
        CheckBox dontShowAgain = (CheckBox)view.findViewById(R.id.checkbox_dont_show);
        dontShowAgain.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Preferences.get().setShowNfcDuringSwap(!isChecked);
            }
        });

        setupNfc();

        return view;
    }

    public static boolean isNfcSupported(Context context) {
        return Build.VERSION.SDK_INT >= 14 && getNfcAdapter(context) != null;
    }

    @TargetApi(10)
    private static NfcAdapter getNfcAdapter(Context context) {
        return NfcAdapter.getDefaultAdapter(context.getApplicationContext());
    }

    @TargetApi(10)
    private void setupNfc() {
        // the required NFC API was added in 4.0 aka Ice Cream Sandwich
        if (Build.VERSION.SDK_INT >= 14) {
            NfcAdapter nfcAdapter = getNfcAdapter(getActivity());
            if (nfcAdapter == null)
                return;
            nfcAdapter.setNdefPushMessage(new NdefMessage(new NdefRecord[] {
                    NdefRecord.createUri(Utils.getSharingUri(getActivity(), FDroidApp.repo)),
            }), getActivity());
        }
    }

}

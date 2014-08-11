package org.fdroid.fdroid.views.swap;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;

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
        return view;
    }

}

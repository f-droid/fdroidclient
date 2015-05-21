package org.fdroid.fdroid.views.swap;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.fdroid.fdroid.R;

public class StartSwapFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LayoutInflater themedInflater = (LayoutInflater)new ContextThemeWrapper(inflater.getContext(), R.style.SwapTheme_StartSwap).getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return themedInflater.inflate(R.layout.swap_blank, container, false);
    }

}

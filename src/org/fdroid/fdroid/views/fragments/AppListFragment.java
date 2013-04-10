package org.fdroid.fdroid.views.fragments;

import android.support.v4.app.Fragment;
import org.fdroid.fdroid.FDroid;
import org.fdroid.fdroid.views.AppListViewFactory;

public class AppListFragment extends Fragment {

    protected AppListViewFactory getViewFactory() {
        FDroid parent = (FDroid)getActivity();
        return new AppListViewFactory(parent);
    }

}

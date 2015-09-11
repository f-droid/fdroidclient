package org.fdroid.fdroid.views.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import org.fdroid.fdroid.R;

public abstract class ThemeableListFragment extends ListFragment {

    protected int getThemeStyle() {
        return 0;
    }

    protected int getHeaderLayout() {
        return 0;
    }

    protected View getHeaderView() {
        return headerView;
    }

    private View headerView = null;

    private View getHeaderView(LayoutInflater inflater, ViewGroup container) {
        if (getHeaderLayout() > 0) {
            if (headerView == null) {
                headerView = inflater.inflate(getHeaderLayout(), null, false);
            }
            return headerView;
        } else {
            return null;
        }
    }

    private LayoutInflater getThemedInflater(Context context) {
        Context c = (getThemeStyle() == 0) ? context : new ContextThemeWrapper(context, getThemeStyle());
        return (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    /**
     * Normally we'd just let the baseclass ListFrament.onCreateView() from the support library do its magic.
     * However, it doesn't allow us to theme it. That is, it always passes getActivity() into the constructor
     * of widgets. We are more interested in a ContextThemeWrapper, so that the widgets get appropriately
     * themed. In order to get it working, we need to work around android bug 21742 as well
     * (https://code.google.com/p/android/issues/detail?id=21742).
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        LayoutInflater themedInflater = getThemedInflater(inflater.getContext());

        View view = themedInflater.inflate(R.layout.list_content, container, false);

        View headerView = getHeaderView(themedInflater, container);
        if (headerView != null) {
            ListView listView = (ListView) view.findViewById(android.R.id.list);
            listView.addHeaderView(headerView);
        }

        // Workaround for https://code.google.com/p/android/issues/detail?id=21742
        view.findViewById(android.R.id.empty).setId(0x00ff0001);
        view.findViewById(R.id.progressContainer).setId(0x00ff0002);
        view.findViewById(android.R.id.progress).setId(0x00ff0003);

        return view;
    }

}

package org.fdroid.fdroid.compat;

import android.content.res.Configuration;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;

import org.fdroid.fdroid.FDroid;

import java.util.ArrayList;
import java.util.List;

public class TabManager {

    public static final int INDEX_AVAILABLE  = 0;
    public static final int INDEX_INSTALLED  = 1;
    public static final int INDEX_CAN_UPDATE = 2;
    public static final int INDEX_COUNT      = 3;

    private final ViewPager pager;
    private final FDroid parent;
    private final ActionBar actionBar;
    private Spinner actionBarSpinner;

    // Used to make sure we only search for the action bar spinner once
    // in each orientation.
    private boolean dirtyConfig = true;

    public TabManager(FDroid parent, ViewPager pager) {
        actionBar = parent.getSupportActionBar();
        this.parent = parent;
        this.pager = pager;
    }

    protected CharSequence getLabel(int index) {
        return pager.getAdapter().getPageTitle(index);
    }

    public void removeNotification(int id) {
        parent.removeNotification(id);
    }

    public void createTabs() {
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        for (int i = 0; i < pager.getAdapter().getCount(); i++) {
            CharSequence label = pager.getAdapter().getPageTitle(i);
            actionBar.addTab(
                    actionBar.newTab()
                        .setText(label)
                        .setTabListener(new ActionBar.TabListener() {
                            @Override
                            public void onTabSelected(ActionBar.Tab tab,
                                                      FragmentTransaction ft) {
                                int pos = tab.getPosition();
                                pager.setCurrentItem(pos);
                                if (pos == INDEX_CAN_UPDATE)
                                    removeNotification(1);
                            }

                            @Override
                            public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
                            }

                            @Override
                            public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
                            }
                        })
            );
        }
    }

    public void selectTab(int index) {
        actionBar.setSelectedNavigationItem(index);
        Spinner actionBarSpinner = getActionBarSpinner();
        if (actionBarSpinner != null) {
            actionBarSpinner.setSelection(index);
        }
        if (index == INDEX_CAN_UPDATE)
            removeNotification(1);
    }

    public void refreshTabLabel(int index) {
        CharSequence text = getLabel(index);
        actionBar.getTabAt(index).setText(text);
    }

    public void onConfigurationChanged(Configuration newConfig) {
        dirtyConfig = true;
    }

    /**
     * Traversing the view hierarchy is a non-trivial task, and takes between 0 and 3
     * milliseconds on my SGS i9000 (Android 4.2).
     * As such, we lazily try to identify the spinner, and only search once per
     * orientation change. Once we've found it, we stop looking.
     */
    private Spinner getActionBarSpinner() {
        if (actionBarSpinner == null && dirtyConfig) {
            dirtyConfig = false;
            actionBarSpinner = findActionBarSpinner();
        }
        return actionBarSpinner;
    }

    /**
     * Dodgey hack to fix issue 231, based on the solution at
     * http://stackoverflow.com/a/13353493
     * Turns out that there is a bug in Android where the Spinner in the action
     * bar (which represents the tabs if there is not enough space) is not
     * updated when we call setSelectedNavigationItem(), and they don't expose
     * the spinner via the API. So we go on a merry hunt for all spinners in
     * our view, and find the first one with an id of -1.
     *
     * This is because the view hierarchy dictates that the action bar comes
     * before everything below it when traversing children, and also our spinner
     * on the first view (for the app categories) has an id, whereas the
     * actionbar one doesn't. If that changes in future releases of android,
     * then we will need to update the findListNavigationSpinner() method.
     */
    private Spinner findActionBarSpinner() {
        View rootView = parent.findViewById(android.R.id.content).getRootView();
        List<Spinner> spinners = traverseViewChildren((ViewGroup) rootView);
        return findListNavigationSpinner(spinners);
    }

    private Spinner findListNavigationSpinner(List<Spinner> spinners) {
        Spinner spinner = null;
        if (spinners.size() > 0) {
            Spinner first = spinners.get(0);
            if (first.getId() == -1) {
                spinner = first;
            }
        }
        return spinner;
    }

    private List<Spinner> traverseViewChildren(ViewGroup parent) {
        List<Spinner> spinners = new ArrayList<>();
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof Spinner) {
                spinners.add((Spinner) child);
            } else if (child instanceof ViewGroup) {
                spinners.addAll(traverseViewChildren((ViewGroup) child));
            }
        }
        return spinners;
    }
}

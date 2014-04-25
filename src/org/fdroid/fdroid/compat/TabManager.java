package org.fdroid.fdroid.compat;

import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.res.Configuration;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import android.support.v4.view.ViewPager;

import org.fdroid.fdroid.FDroid;
import org.fdroid.fdroid.R;

public abstract class TabManager extends Compatibility {

    public static final int INDEX_AVAILABLE  = 0;
    public static final int INDEX_INSTALLED  = 1;
    public static final int INDEX_CAN_UPDATE = 2;

    public static TabManager create(FDroid parent, ViewPager pager) {
        if (hasApi(11)) {
            return new HoneycombTabManagerImpl(parent, pager);
        } else {
            return new OldTabManagerImpl(parent, pager);
        }
    }

    protected final ViewPager pager;
    protected final FDroid parent;

    protected TabManager(FDroid parent, ViewPager pager) {
        this.parent = parent;
        this.pager = pager;
    }

    abstract public void createTabs();
    abstract public void selectTab(int index);
    abstract public void refreshTabLabel(int index);
    abstract public void onConfigurationChanged(Configuration newConfig);

    protected CharSequence getLabel(int index) {
        return pager.getAdapter().getPageTitle(index);
    }

    public void removeNotification(int id) {
        parent.removeNotification(id);
    }
}

class OldTabManagerImpl extends TabManager {

    private TabHost tabHost;

    public OldTabManagerImpl(FDroid parent, ViewPager pager) {
        super(parent, pager);
    }

    /**
     * There is a bit of boiler-plate code required to get a TabWidget showing,
     * which includes creating a TabHost, populating it with the TabWidget,
     * and giving it a FrameLayout as a child. This will make the tabs have
     * dummy empty contents and then hook them up to our ViewPager.
     */
    @Override
    public void createTabs() {
        tabHost = new TabHost(parent, null);
        tabHost.setLayoutParams(new TabHost.LayoutParams(
                TabHost.LayoutParams.MATCH_PARENT, TabHost.LayoutParams.WRAP_CONTENT));

        TabWidget tabWidget = new TabWidget(parent);
        tabWidget.setId(android.R.id.tabs);
        tabHost.setLayoutParams(new TabHost.LayoutParams(
                TabWidget.LayoutParams.MATCH_PARENT, TabWidget.LayoutParams.WRAP_CONTENT));

        FrameLayout layout = new FrameLayout(parent);
        layout.setId(android.R.id.tabcontent);
        layout.setLayoutParams(new TabWidget.LayoutParams(0, 0));

        tabHost.addView(tabWidget);
        tabHost.addView(layout);
        tabHost.setup();

        TabHost.TabContentFactory factory = new TabHost.TabContentFactory() {
            @Override
            public View createTabContent(String tag) {
                return new View(parent);
            }
        };

        TabHost.TabSpec availableTabSpec = tabHost.newTabSpec("available")
                .setIndicator(
                        parent.getString(R.string.tab_noninstalled),
                        parent.getResources().getDrawable(android.R.drawable.ic_input_add))
                .setContent(factory);

        TabHost.TabSpec installedTabSpec = tabHost.newTabSpec("installed")
                .setIndicator(
                        parent.getString(R.string.inst),
                        parent.getResources().getDrawable(android.R.drawable.star_off))
                .setContent(factory);

        TabHost.TabSpec canUpdateTabSpec = tabHost.newTabSpec("canUpdate")
                .setIndicator(
                        parent.getString(R.string.tab_updates),
                        parent.getResources().getDrawable(android.R.drawable.star_on))
                .setContent(factory);

        tabHost.addTab(availableTabSpec);
        tabHost.addTab(installedTabSpec);
        tabHost.addTab(canUpdateTabSpec);

        LinearLayout contentView = (LinearLayout)parent.findViewById(R.id.fdroid_layout);
        contentView.addView(tabHost, 0);

        tabHost.setOnTabChangedListener( new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                int pos = tabHost.getCurrentTab();
                pager.setCurrentItem(pos);
                if (pos == INDEX_CAN_UPDATE)
                    removeNotification(1);
            }
        });
    }


    @Override
    public void selectTab(int index) {
        tabHost.setCurrentTab(index);
        if (index == INDEX_CAN_UPDATE)
            removeNotification(1);
    }

    @Override
    public void refreshTabLabel(int index) {
        CharSequence text = getLabel(index);

        // Update the count on the 'Updates' tab to show the number available.
        // This is quite unpleasant, but seems to be the only way to do it.
        TextView textView = (TextView) tabHost.getTabWidget().getChildAt(index)
                .findViewById(android.R.id.title);
        textView.setText(text);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // Do nothing
    }

}

@TargetApi(11)
class HoneycombTabManagerImpl extends TabManager {

    protected final ActionBar actionBar;
    private Spinner actionBarSpinner = null;

    // Used to make sure we only search for the action bar spinner once
    // in each orientation.
    private boolean dirtyConfig = true;

    public HoneycombTabManagerImpl(FDroid parent, ViewPager pager) {
        super(parent, pager);
        actionBar = parent.getActionBar();
    }

    @Override
    public void createTabs() {
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        for (int i = 0; i < pager.getAdapter().getCount(); i ++) {
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
                    }));
        }
    }

    @Override
    public void selectTab(int index) {
        actionBar.setSelectedNavigationItem(index);
        Spinner actionBarSpinner = getActionBarSpinner();
        if (actionBarSpinner != null) {
            actionBarSpinner.setSelection(index);
        }
        if (index == INDEX_CAN_UPDATE)
            removeNotification(1);
    }

    @Override
    public void refreshTabLabel(int index) {
        CharSequence text = getLabel(index);
        actionBar.getTabAt(index).setText(text);
    }

    @Override
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
     * <p/>
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
        List<Spinner> spinners = new ArrayList<Spinner>();
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

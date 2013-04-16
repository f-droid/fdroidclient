package org.fdroid.fdroid.compat;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.*;
import org.fdroid.fdroid.FDroid;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;

public abstract class TabManager {

    public static final int INDEX_AVAILABLE  = 0;
    public static final int INDEX_INSTALLED  = 1;
    public static final int INDEX_CAN_UPDATE = 2;

    public static TabManager create(FDroid parent, ViewPager pager) {
        if (Utils.hasApi(11)) {
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

    protected CharSequence getLabel(int index) {
        return pager.getAdapter().getPageTitle(index);
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
    public void createTabs() {
        tabHost = new TabHost(parent);
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
                        parent.getString(R.string.tab_installed),
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
                pager.setCurrentItem(tabHost.getCurrentTab());
            }
        });
    }


    public void selectTab(int index) {
        tabHost.setCurrentTab(index);
    }

    public void refreshTabLabel(int index) {
        CharSequence text = getLabel(index);

        // Update the count on the 'Updates' tab to show the number available.
        // This is quite unpleasant, but seems to be the only way to do it.
        TextView textView = (TextView) tabHost.getTabWidget().getChildAt(index)
                .findViewById(android.R.id.title);
        textView.setText(text);
    }

}

class HoneycombTabManagerImpl extends TabManager {

    protected final ActionBar actionBar;

    public HoneycombTabManagerImpl(FDroid parent, ViewPager pager) {
        super(parent, pager);
        actionBar = parent.getActionBar();
    }

    public void createTabs() {
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        for (int i = 0; i < pager.getAdapter().getCount(); i ++) {
            CharSequence label = pager.getAdapter().getPageTitle(i);
            actionBar.addTab(
                actionBar.newTab()
                    .setText(label)
                    .setTabListener(new ActionBar.TabListener() {
                        public void onTabSelected(ActionBar.Tab tab,
                                                  FragmentTransaction ft) {
                            pager.setCurrentItem(tab.getPosition());
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

    public void selectTab(int index) {
        actionBar.setSelectedNavigationItem(index);
    }

    public void refreshTabLabel(int index) {
        CharSequence text = getLabel(index);
        actionBar.getTabAt(index).setText(text);
    }
}

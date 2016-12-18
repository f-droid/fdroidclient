package org.fdroid.fdroid.views.main;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.view.MenuItem;
import android.support.v7.widget.RecyclerView;

import org.fdroid.fdroid.R;

/**
 * Main view shown to users upon starting F-Droid.
 *
 * Shows a bottom navigation bar, with the following entries:
 *  + Whats new
 *  + Categories list
 *  + App swap
 *  + My apps
 *  + Settings
 *
 *  Users navigate between items by using the bottom navigation bar, or by swiping left and right.
 *  When switching from one screen to the next, we stay within this activity. The new screen will
 *  get inflated (if required)
 */
public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    public static final String EXTRA_VIEW_MY_APPS = "org.fdroid.fdroid.views.main.MainActivity.VIEW_MY_APPS";

    private RecyclerView pager;
    private MainViewAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        adapter = new MainViewAdapter(this);

        pager = (RecyclerView) findViewById(R.id.main_view_pager);
        pager.setHasFixedSize(true);
        pager.setLayoutManager(new NonScrollingHorizontalLayoutManager(this));
        pager.setAdapter(adapter);

        BottomNavigationView bottomNavigation = (BottomNavigationView) findViewById(R.id.bottom_navigation);
        bottomNavigation.setOnNavigationItemSelectedListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (getIntent().hasExtra(EXTRA_VIEW_MY_APPS)) {
            getIntent().removeExtra(EXTRA_VIEW_MY_APPS);
            pager.scrollToPosition(adapter.adapterPositionFromItemId(R.id.my_apps));
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        pager.scrollToPosition(((MainViewAdapter) pager.getAdapter()).adapterPositionFromItemId(item.getItemId()));
        return true;
    }

    private static class NonScrollingHorizontalLayoutManager extends LinearLayoutManager {
        NonScrollingHorizontalLayoutManager(Context context) {
            super(context, LinearLayoutManager.HORIZONTAL, false);
        }

        @Override
        public boolean canScrollHorizontally() {
            return false;
        }

        @Override
        public boolean canScrollVertically() {
            return false;
        }
    }

}
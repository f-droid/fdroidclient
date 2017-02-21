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

    private RecyclerView pager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        pager = (RecyclerView) findViewById(R.id.main_view_pager);
        pager.setHasFixedSize(true);
        pager.setLayoutManager(new NonScrollingHorizontalLayoutManager(this));
        pager.setAdapter(new MainViewAdapter(this));

        BottomNavigationView bottomNavigation = (BottomNavigationView) findViewById(R.id.bottom_navigation);
        bottomNavigation.setOnNavigationItemSelectedListener(this);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        pager.scrollToPosition(MainViewAdapter.ID_TO_POSITION.get(item.getItemId()));
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
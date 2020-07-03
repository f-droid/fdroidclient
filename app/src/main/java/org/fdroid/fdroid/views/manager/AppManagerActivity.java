package org.fdroid.fdroid.views.manager;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.R;

public class AppManagerActivity extends AppCompatActivity {

    private static final String TAG = "AppManagerActivity";

    private ViewPager viewPager;
    private AppManagerAdapter adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        ((FDroidApp) getApplication()).applyTheme(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_app_manager);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.installed_apps__activity_title));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        this.viewPager = findViewById(R.id.viewPager);
        TabLayout tabLayout = findViewById(R.id.tabLayout);

        adapter = new AppManagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new FragmentInstalled(), getString(R.string.installed_layout_installed));

        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.app_manager, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.menu_share:
                /*StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("packageName,versionCode,versionName\n");
                for (int i = 0; i < adapter.getItemCount(); i++) {
                    App app = adapter.getItem(i);
                    if (app != null) {
                        stringBuilder.append(app.packageName).append(',')
                                .append(app.installedVersionCode).append(',')
                                .append(app.installedVersionName).append('\n');
                    }
                }
                ShareCompat.IntentBuilder intentBuilder = ShareCompat.IntentBuilder.from(this)
                        .setSubject(getString(R.string.send_installed_apps))
                        .setChooserTitle(R.string.send_installed_apps)
                        .setText(stringBuilder.toString())
                        .setType("text/csv");
                startActivity(intentBuilder.getIntent());*/
                break;


        }

        return super.onOptionsItemSelected(item);
    }

    public void onResume() {
        super.onResume();
    }

}

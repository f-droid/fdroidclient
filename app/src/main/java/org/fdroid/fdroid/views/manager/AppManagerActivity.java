package org.fdroid.fdroid.views.manager;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.views.filebrowser.fileBrowserActivity;

import static org.fdroid.fdroid.data.CollectionProvider.getJSONUri;

public class AppManagerActivity extends AppCompatActivity {

    private static final String TAG = "AppManagerActivity";
    private static final int LAUNCH_REQUEST_CODE = 123;

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

            case R.id.menu_load:
                launchFileActivity(fileBrowserActivity.MODE_LOAD);
                break;
            case R.id.menu_save:
                launchFileActivity(fileBrowserActivity.MODE_SAVE);
                break;
            case R.id.menu_share:

                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("packageName,versionCode,versionName\n");

                Cursor cursor = getContentResolver().query(getJSONUri(), null, null, null, null);

                if(cursor != null) {
                    cursor.moveToPosition(-1);

                    while (cursor.moveToNext()) {
                        App app = new App(cursor);

                        stringBuilder.append(app.packageName).append(',')
                                .append(app.suggestedVersionCode).append(',')
                                .append(app.suggestedVersionName).append('\n');
                    }
                    cursor.close();
                }

                ShareCompat.IntentBuilder intentBuilder = ShareCompat.IntentBuilder.from(this)
                        .setSubject(getString(R.string.send_installed_apps))
                        .setChooserTitle(R.string.send_installed_apps)
                        .setText(stringBuilder.toString())
                        .setType("text/csv");
                try {
                    startActivity(intentBuilder.getIntent());
                } catch (ActivityNotFoundException e) {
                    // no Apps to send :( / nothing to do here :)
                    Toast.makeText(this, "no App to send available", Toast.LENGTH_SHORT).show();
                }

                break;
        }

        return super.onOptionsItemSelected(item);
    }


    private void launchFileActivity(int mode) {
        Intent intent = new Intent(this, fileBrowserActivity.class);
        intent.putExtra("mode", mode);
        startActivityForResult(intent, LAUNCH_REQUEST_CODE);  //start fileBrowser activity
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == LAUNCH_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                // no permission ;_;
                // do we have permission now?
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PermissionChecker.PERMISSION_GRANTED) {
                    launchFileActivity(data.getIntExtra("modeCache", 0)); //now we have permission \(^_^)/ -> try again
                }
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                if (data != null && data.getIntExtra("loadSuccessfully", 0) == 1) {
                    viewPager.setCurrentItem(1);
                }
            }
        }
    }


    public void onResume() {
        super.onResume();
    }

}

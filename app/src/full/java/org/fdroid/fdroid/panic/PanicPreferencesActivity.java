package org.fdroid.fdroid.panic;

import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.R;

public class PanicPreferencesActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle bundle) {
        ((FDroidApp) getApplication()).applyTheme(this);
        super.onCreate(bundle);
        setContentView(R.layout.activity_panic_settings);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayShowHomeEnabled(true);
            ab.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}

package org.fdroid.fdroid.panic;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.databinding.ActivityPanicSettingsBinding;

public class PanicPreferencesActivity extends AppCompatActivity {
    @Override
    public void onCreate(Bundle bundle) {
        ((FDroidApp) getApplication()).applyTheme(this);
        super.onCreate(bundle);

        ActivityPanicSettingsBinding binding = ActivityPanicSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
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

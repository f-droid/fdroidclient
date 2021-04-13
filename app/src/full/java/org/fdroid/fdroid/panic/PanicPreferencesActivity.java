package org.fdroid.fdroid.panic;

import android.os.Bundle;
import android.view.View;

import com.google.android.material.appbar.MaterialToolbar;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.R;

import androidx.appcompat.app.AppCompatActivity;

public class PanicPreferencesActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle bundle) {
        FDroidApp fdroidApp = (FDroidApp) getApplication();
        fdroidApp.applyPureBlackBackgroundInDarkTheme(this);

        super.onCreate(bundle);
        setContentView(R.layout.activity_panic_settings);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Handle navigation icon press
                onBackPressed();
            }
        });
    }
}

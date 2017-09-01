package org.fdroid.fdroid;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        FDroidApp fdroidApp = (FDroidApp) getApplication();
        fdroidApp.applyDialogTheme(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.about);

        String versionName = Utils.getVersionName(this);
        if (versionName != null) {
            ((TextView) findViewById(R.id.version)).setText(versionName);
        }

        findViewById(R.id.ok_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}

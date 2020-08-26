package org.fdroid.fdroid;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.fdroid.fdroid.databinding.AboutBinding;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        FDroidApp fdroidApp = (FDroidApp) getApplication();
        fdroidApp.applyDialogTheme(this);

        super.onCreate(savedInstanceState);

        final AboutBinding binding = AboutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String versionName = Utils.getVersionName(this);
        if (versionName != null) {
            binding.version.setText(versionName);
        }

        binding.okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}

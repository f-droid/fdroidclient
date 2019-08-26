package org.fdroid.fdroid;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class UpdateRepoNow extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UpdateService.updateNow(this);
        finish();
    }
}

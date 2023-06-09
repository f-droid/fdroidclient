package org.fdroid.fdroid.panic;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class ExitActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        finishAndRemoveTask();

        System.exit(0);
    }

    static void exitAndRemoveFromRecentApps(final AppCompatActivity activity) {
        activity.runOnUiThread(() -> {
            Intent intent = new Intent(activity, ExitActivity.class);

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK
                    | Intent.FLAG_ACTIVITY_NO_ANIMATION);

            activity.startActivity(intent);
        });

    }

}

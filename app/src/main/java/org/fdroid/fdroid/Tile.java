package org.fdroid.fdroid;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.TileService;

import org.fdroid.fdroid.FDroidApp;

@TargetApi(Build.VERSION_CODES.N)
public class Tile extends TileService {
    @Override
    public void onClick() {
        Intent collapseIntent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        sendBroadcast(collapseIntent);
        Context context = getApplicationContext();
        //FDroidApp.handleShortcuts(context, "fx");
        UpdateService.updateNow(context);
    }
}

package org.fdroid.fdroid.panic;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.views.installed.InstalledAppListAdapter;
import org.fdroid.fdroid.views.installed.InstalledAppListItemController;

import java.util.Set;

public class SelectInstalledAppListAdapter extends InstalledAppListAdapter {
    private final Set<String> selectedApps;

    SelectInstalledAppListAdapter(AppCompatActivity activity) {
        super(activity);
        Preferences prefs = Preferences.get();
        selectedApps = prefs.getPanicWipeSet();
        prefs.setPanicTmpSelectedSet(selectedApps);
    }

    @NonNull
    @Override
    public InstalledAppListItemController onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = activity.getLayoutInflater().inflate(R.layout.installed_app_list_item, parent, false);
        return new SelectInstalledAppListItemController(activity, view, selectedApps);
    }
}

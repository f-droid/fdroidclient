package org.fdroid.fdroid.views.myapps;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.UpdateService;

public class UpdatesHeaderController extends RecyclerView.ViewHolder {

    private final Activity activity;
    private final TextView updatesHeading;

    public UpdatesHeaderController(Activity activity, View itemView) {
        super(itemView);
        this.activity = activity;

        Button updateAll = (Button) itemView.findViewById(R.id.update_all_button);
        updateAll.setOnClickListener(onUpdateAll);

        updatesHeading = (TextView) itemView.findViewById(R.id.updates_heading);
        updatesHeading.setText(activity.getString(R.string.updates));
    }

    public void bindModel(int numAppsToUpdate) {
        updatesHeading.setText(activity.getResources().getQuantityString(R.plurals.my_apps_header_number_of_updateable, numAppsToUpdate, numAppsToUpdate));
    }

    private final View.OnClickListener onUpdateAll = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            UpdateService.autoDownloadUpdates(activity);
        }
    };
}

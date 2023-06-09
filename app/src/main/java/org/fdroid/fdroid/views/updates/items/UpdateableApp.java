package org.fdroid.fdroid.views.updates.items;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.hannesdorfmann.adapterdelegates4.AdapterDelegate;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;

import java.util.List;

/**
 * List of all apps which can be updated, but have not yet been downloaded.
 *
 * @see UpdateableApp The data that is bound to this view.
 * @see R.layout#updateable_app_list_item The view that this binds to.
 * @see UpdateableAppListItemController Used for binding the {@link App} to
 * the {@link R.layout#updateable_app_list_item}
 */
public class UpdateableApp extends AppUpdateData {

    public final App app;
    public final Apk apk;

    public UpdateableApp(AppCompatActivity activity, App app, Apk apk) {
        super(activity);
        this.app = app;
        this.apk = apk;
    }

    public static class Delegate extends AdapterDelegate<List<AppUpdateData>> {

        private final AppCompatActivity activity;

        public Delegate(AppCompatActivity activity) {
            this.activity = activity;
        }

        @Override
        protected boolean isForViewType(@NonNull List<AppUpdateData> items, int position) {
            return items.get(position) instanceof UpdateableApp;
        }

        @NonNull
        @Override
        protected RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent) {
            return new UpdateableAppListItemController(activity, activity.getLayoutInflater()
                    .inflate(R.layout.updateable_app_list_item, parent, false));
        }

        @Override
        protected void onBindViewHolder(@NonNull List<AppUpdateData> items, int position,
                                        @NonNull RecyclerView.ViewHolder holder, @NonNull List<Object> payloads) {
            UpdateableApp app = (UpdateableApp) items.get(position);
            ((UpdateableAppListItemController) holder).bindModel(app.app, app.apk, null);
        }
    }

}

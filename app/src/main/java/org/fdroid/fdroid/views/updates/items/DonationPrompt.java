package org.fdroid.fdroid.views.updates.items;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.hannesdorfmann.adapterdelegates3.AdapterDelegate;

import java.util.List;

/**
 * The app (if any) which we should prompt the user about potentially donating to (due to having
 * updated several times).
 */
public class DonationPrompt extends AppUpdateData {

    public DonationPrompt(Activity activity) {
        super(activity);
    }

    public static class Delegate extends AdapterDelegate<List<AppUpdateData>> {

        @Override
        protected boolean isForViewType(@NonNull List<AppUpdateData> items, int position) {
            return items.get(position) instanceof DonationPrompt;
        }

        @NonNull
        @Override
        protected RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent) {
            return new ViewHolder(new TextView(parent.getContext()));
        }

        @Override
        protected void onBindViewHolder(@NonNull List<AppUpdateData> items, int position, @NonNull RecyclerView.ViewHolder holder, @NonNull List<Object> payloads) {
            DonationPrompt app = (DonationPrompt) items.get(position);
            ((ViewHolder) holder).bindApp(app);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }

        public void bindApp(DonationPrompt app) {
            ((TextView) itemView).setText("");
        }
    }

}

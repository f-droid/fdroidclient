package org.fdroid.fdroid.views.categories;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import org.fdroid.database.AppOverviewItem;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

class AppPreviewAdapter extends RecyclerView.Adapter<AppCardController> {

    private List<AppOverviewItem> items = Collections.emptyList();
    private final AppCompatActivity activity;
    private final List<String> antiFeatures;
    private final Set<String> shownAntiFeatures = Preferences.get().showAppsWithAntiFeatures();
    private final boolean showOtherAntiFeatures;
    private final boolean hideIncompatibleVersions = !Preferences.get().showIncompatibleVersions();

    AppPreviewAdapter(AppCompatActivity activity) {
        this.activity = activity;
        antiFeatures = Arrays.asList(activity.getResources().getStringArray(R.array.antifeaturesValues));
        String otherAntiFeatures = activity.getResources().getString(R.string.antiothers_key);
        showOtherAntiFeatures = shownAntiFeatures.contains(otherAntiFeatures);
    }

    @NonNull
    @Override
    public AppCardController onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new AppCardController(activity, activity.getLayoutInflater()
                .inflate(R.layout.app_card_normal, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull AppCardController holder, int position) {
        holder.bindApp(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    void setAppCursor(List<AppOverviewItem> items) {
        if (this.items == items) {
            // don't notify when the cursor did not change
            return;
        }
        Iterator<AppOverviewItem> iterator = items.iterator();
        while (iterator.hasNext()) {
            AppOverviewItem item = iterator.next();
            if (isFilteredByAntiFeature(item, antiFeatures, shownAntiFeatures, showOtherAntiFeatures)) {
                iterator.remove();
            } else if (hideIncompatibleVersions && !item.isCompatible()) {
                iterator.remove();
            }
        }
        this.items = items;
        notifyDataSetChanged();
    }

    private boolean isFilteredByAntiFeature(AppOverviewItem item, List<String> antiFeatures,
                                            Set<String> showAntiFeatures, boolean showOther) {
        for (String antiFeature : item.getAntiFeatureKeys()) {
            // is it part of the known anti-features?
            if (antiFeatures.contains(antiFeature)) {
                // it gets filtered not part of the ones that we show
                if (!showAntiFeatures.contains(antiFeature)) return true;
            } else if (!showOther) {
                // gets filtered if we should no show unknown anti-features
                return true;
            }
        }
        return false;
    }
}

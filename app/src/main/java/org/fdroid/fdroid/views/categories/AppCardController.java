package org.fdroid.fdroid.views.categories;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.nostra13.universalimageloader.core.ImageLoader;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.views.AppDetailsActivity;

/**
 * The {@link AppCardController} can bind an app to several different layouts, as long as the layout
 * contains the following elements:
 * + {@link R.id#icon} ({@link ImageView}, required)
 * + {@link R.id#summary} ({@link TextView}, required)
 * + {@link R.id#new_tag} ({@link TextView}, optional)
 */
public class AppCardController extends RecyclerView.ViewHolder
        implements View.OnClickListener {

    /**
     * After this many days, don't consider showing the "New" tag next to an app.
     */
    private static final int DAYS_TO_CONSIDER_NEW = 14;

    @NonNull
    private final ImageView icon;

    /**
     * Text starting with the app name (in bold) followed by a short summary of the app.
     */
    @NonNull
    private final TextView summary;

    /**
     * A little blue tag which says "New" to indicate an app was added to the repository recently.
     */
    @Nullable
    private final TextView newTag;

    @Nullable
    private App currentApp;

    private final Activity activity;

    public AppCardController(Activity activity, View itemView) {
        super(itemView);

        this.activity = activity;

        icon = (ImageView) findViewAndEnsureNonNull(itemView, R.id.icon);
        summary = (TextView) findViewAndEnsureNonNull(itemView, R.id.summary);

        newTag = (TextView) itemView.findViewById(R.id.new_tag);

        itemView.setOnClickListener(this);
    }

    /**
     * The contract that this controller has is that it will consume any layout resource, given
     * it has some specific view types (with specific IDs) available. This helper function will
     * throw an {@link IllegalArgumentException} if the view doesn't exist,
     */
    @NonNull
    private View findViewAndEnsureNonNull(View view, @IdRes int res) {
        View found = view.findViewById(res);
        if (found == null) {
            String resName = activity.getResources().getResourceName(res);
            throw new IllegalArgumentException("Layout for AppCardController requires " + resName);
        }

        return found;
    }

    public void bindApp(@NonNull App app) {
        currentApp = app;

        summary.setText(Utils.formatAppNameAndSummary(app.name, app.summary));

        if (newTag != null) {
            if (isConsideredNew(app)) {
                newTag.setVisibility(View.VISIBLE);
            } else {
                newTag.setVisibility(View.GONE);
            }
        }

        ImageLoader.getInstance().displayImage(app.iconUrl, icon, Utils.getRepoAppDisplayImageOptions());
    }

    private boolean isConsideredNew(@NonNull App app) {
        //noinspection SimplifiableIfStatement
        if (app.added == null || app.lastUpdated == null || !app.added.equals(app.lastUpdated)) {
            return false;
        }

        return Utils.daysSince(app.added) <= DAYS_TO_CONSIDER_NEW;
    }

    /**
     * When the user clicks/touches an app card, we launch the {@link AppDetailsActivity} activity in response.
     */
    @Override
    public void onClick(View v) {
        if (currentApp == null) {
            return;
        }

        Intent intent = new Intent(activity, AppDetailsActivity.class);
        intent.putExtra(AppDetailsActivity.EXTRA_APPID, currentApp.packageName);
        if (Build.VERSION.SDK_INT >= 21) {
            Pair<View, String> iconTransitionPair = Pair.create((View) icon,
                    activity.getString(R.string.transition_app_item_icon));

            // unchecked since the right type is passed as 2nd varargs arg: Pair<View, String>
            @SuppressWarnings("unchecked")
            Bundle b = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, iconTransitionPair).toBundle();
            activity.startActivity(intent, b);
        } else {
            activity.startActivity(intent);
        }
    }
}

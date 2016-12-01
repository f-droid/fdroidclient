package org.fdroid.fdroid.views.categories;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import org.fdroid.fdroid.AppDetails;
import org.fdroid.fdroid.AppDetails2;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.views.apps.FeatureImage;

import java.util.Date;

/**
 * The {@link AppCardController} can bind an app to several different layouts, as long as the layout
 * contains the following elements:
 *  + {@link R.id#icon} ({@link ImageView}, required)
 *  + {@link R.id#summary} ({@link TextView}, required)
 *  + {@link R.id#featured_image} ({@link ImageView}, optional)
 *  + {@link R.id#status} ({@link TextView}, optional)
 */
public class AppCardController extends RecyclerView.ViewHolder implements ImageLoadingListener, View.OnClickListener {
    @NonNull
    private final ImageView icon;

    @NonNull
    private final TextView summary;

    @Nullable
    private final TextView status;

    @Nullable
    private final FeatureImage featuredImage;

    @Nullable
    private App currentApp;

    private final Activity activity;
    private final DisplayImageOptions displayImageOptions;

    private final Date recentCuttoffDate;

    public AppCardController(Activity activity, View itemView) {
        super(itemView);

        this.activity = activity;

        recentCuttoffDate = Preferences.get().calcMaxHistory();

        icon = (ImageView) findViewAndEnsureNonNull(itemView, R.id.icon);
        summary = (TextView) findViewAndEnsureNonNull(itemView, R.id.summary);

        featuredImage = (FeatureImage) itemView.findViewById(R.id.featured_image);
        status = (TextView) itemView.findViewById(R.id.status);

        displayImageOptions = Utils.getImageLoadingOptions().build();

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

        if (status != null) {
            if (app.added != null && app.added.after(recentCuttoffDate) && (app.lastUpdated == null || app.added.equals(app.lastUpdated))) {
                status.setText(activity.getString(R.string.category_Whats_New));
                status.setVisibility(View.VISIBLE);
            } else if (app.lastUpdated != null && app.lastUpdated.after(recentCuttoffDate)) {
                status.setText(activity.getString(R.string.category_Recently_Updated));
                status.setVisibility(View.VISIBLE);
            } else {
                status.setVisibility(View.GONE);
            }
        }

        if (featuredImage != null) {
            featuredImage.setPalette(null);
            featuredImage.setImageDrawable(null);
        }

        ImageLoader.getInstance().displayImage(app.iconUrl, icon, displayImageOptions, this);
    }

    /**
     * When the user clicks/touches an app card, we launch the {@link AppDetails2} activity in response.
     */
    @Override
    public void onClick(View v) {
        if (currentApp == null) {
            return;
        }

        Intent intent = new Intent(activity, AppDetails2.class);
        intent.putExtra(AppDetails.EXTRA_APPID, currentApp.packageName);
        if (Build.VERSION.SDK_INT >= 21) {
            Pair<View, String> iconTransitionPair = Pair.create((View) icon, activity.getString(R.string.transition_app_item_icon));

            @SuppressWarnings("unchecked") // We are passing the right type as the second varargs argument (i.e. a Pair<View, String>).
            Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, iconTransitionPair).toBundle();

            activity.startActivity(intent, bundle);
        } else {
            activity.startActivity(intent);
        }
    }

    // =============================================================================================
    //  Icon loader callbacks
    //
    //  Most are unused, the main goal is to specify a background colour for the featured image if
    //  no featured image is specified in the apps metadata.
    // =============================================================================================

    @Override
    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
        if (featuredImage != null) {
            new Palette.Builder(loadedImage).generate(new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(Palette palette) {
                    featuredImage.setPalette(palette);
                }
            });
        }
    }

    @Override
    public void onLoadingStarted(String imageUri, View view) {
        // Do nothing
    }

    @Override
    public void onLoadingCancelled(String imageUri, View view) {
        // Do nothing
    }

    @Override
    public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
        // Do nothing
    }

}

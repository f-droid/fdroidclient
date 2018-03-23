package org.fdroid.fdroid.views.categories;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import org.fdroid.fdroid.AppDetails2;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.views.apps.FeatureImage;

/**
 * The {@link AppCardController} can bind an app to several different layouts, as long as the layout
 * contains the following elements:
 * + {@link R.id#icon} ({@link ImageView}, required)
 * + {@link R.id#summary} ({@link TextView}, required)
 * + {@link R.id#new_tag} ({@link TextView}, optional)
 * + {@link R.id#featured_image} ({@link ImageView}, optional)
 */
public class AppCardController extends RecyclerView.ViewHolder
        implements ImageLoadingListener, View.OnClickListener {

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

    /**
     * Wide and short image for branding the app. If it is not present in the metadata then F-Droid
     * will draw some abstract art instead.
     */
    @Nullable
    private final FeatureImage featuredImage;

    @Nullable
    private App currentApp;

    private final Activity activity;

    public AppCardController(Activity activity, View itemView) {
        super(itemView);

        this.activity = activity;

        icon = (ImageView) findViewAndEnsureNonNull(itemView, R.id.icon);
        summary = (TextView) findViewAndEnsureNonNull(itemView, R.id.summary);

        featuredImage = (FeatureImage) itemView.findViewById(R.id.featured_image);
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

        ImageLoader.getInstance().displayImage(app.iconUrl, icon, Utils.getRepoAppDisplayImageOptions(), this);

        if (featuredImage != null) {
            featuredImage.setColour(ContextCompat.getColor(activity, R.color.fdroid_blue));
            featuredImage.setImageDrawable(null);

            // Note: We could call the convenience function
            // loadImageAndDisplay(ImageLoader, DisplayImageOptions, String, String)
            // which includes a fallback for when currentApp.featureGraphic is empty. However we need
            // to take care of also loading the icon (regardless of whether there is a featureGraphic
            // or not for this app) so that we can display the icon to the user. We will use the
            // load complete listener for the icon to decide whether we need to extract the colour
            // from that icon and assign to the `FeatureImage` (or whether we should wait for the
            // feature image to be loaded).
            if (!TextUtils.isEmpty(app.featureGraphic)) {
                featuredImage.loadImageAndDisplay(ImageLoader.getInstance(),
                        Utils.getRepoAppDisplayImageOptions(), app.getFeatureGraphicUrl(activity));
            }
        }
    }

    private boolean isConsideredNew(@NonNull App app) {
        //noinspection SimplifiableIfStatement
        if (app.added == null || app.lastUpdated == null || !app.added.equals(app.lastUpdated)) {
            return false;
        }

        return Utils.daysSince(app.added) <= DAYS_TO_CONSIDER_NEW;
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
        intent.putExtra(AppDetails2.EXTRA_APPID, currentApp.packageName);
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

    // =============================================================================================
    //  Icon loader callbacks
    //
    //  Most are unused, the main goal is to specify a background colour for the featured image if
    //  no featured image is specified in the apps metadata. If an image is specified, then it will
    //  get loaded using the `FeatureImage.loadImageAndDisplay()` method and so we don't need to do
    //  anything special here.
    // =============================================================================================

    @Override
    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
        if (currentApp != null
                && TextUtils.isEmpty(currentApp.featureGraphic)
                && featuredImage != null
                && loadedImage != null) {
            new Palette.Builder(loadedImage).generate(new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(Palette palette) {
                    featuredImage.setColorAndAnimateChange(palette.getDominantColor(Color.LTGRAY));
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

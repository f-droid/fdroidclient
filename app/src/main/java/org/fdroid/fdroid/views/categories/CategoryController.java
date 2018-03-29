package org.fdroid.fdroid.views.categories;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.Schema;
import org.fdroid.fdroid.views.apps.AppListActivity;
import org.fdroid.fdroid.views.apps.FeatureImage;

import java.util.Locale;
import java.util.Random;

public class CategoryController extends RecyclerView.ViewHolder implements LoaderManager.LoaderCallbacks<Cursor> {
    private final Button viewAll;
    private final TextView heading;
    private final FeatureImage image;
    private final AppPreviewAdapter appCardsAdapter;
    private final FrameLayout background;

    private final Activity activity;
    private final LoaderManager loaderManager;
    private final DisplayImageOptions displayImageOptions;
    private static int categoryItemCount = 20;

    private String currentCategory;

    CategoryController(final Activity activity, LoaderManager loaderManager, View itemView) {
        super(itemView);

        this.activity = activity;
        this.loaderManager = loaderManager;

        appCardsAdapter = new AppPreviewAdapter(activity);

        viewAll = (Button) itemView.findViewById(R.id.button);
        viewAll.setOnClickListener(onViewAll);

        heading = (TextView) itemView.findViewById(R.id.name);
        image = (FeatureImage) itemView.findViewById(R.id.category_image);
        background = (FrameLayout) itemView.findViewById(R.id.category_background);

        RecyclerView appCards = (RecyclerView) itemView.findViewById(R.id.app_cards);
        appCards.setAdapter(appCardsAdapter);
        appCards.addItemDecoration(new ItemDecorator(activity));

        displayImageOptions = Utils.getDefaultDisplayImageOptionsBuilder()
                .displayer(new FadeInBitmapDisplayer(100, true, true, false))
                .build();
    }

    public static String translateCategory(Context context, String categoryName) {
        int categoryNameId = getCategoryResource(context, categoryName, "string", false);
        return categoryNameId == 0 ? categoryName : context.getString(categoryNameId);
    }

    void bindModel(@NonNull String categoryName) {
        currentCategory = categoryName;

        String translatedName = translateCategory(activity, categoryName);
        heading.setText(translatedName);
        heading.setContentDescription(activity.getString(R.string.tts_category_name, translatedName));

        viewAll.setVisibility(View.INVISIBLE);

        loaderManager.initLoader(currentCategory.hashCode(), null, this);
        loaderManager.initLoader(currentCategory.hashCode() + 1, null, this);

        @ColorInt int backgroundColour = getBackgroundColour(activity, categoryName);
        background.setBackgroundColor(backgroundColour);

        int categoryImageId = getCategoryResource(activity, categoryName, "drawable", true);
        if (categoryImageId == 0) {
            image.setColour(backgroundColour);
            image.setImageDrawable(null);
        } else {
            image.setColour(ContextCompat.getColor(activity, R.color.fdroid_blue));
            ImageLoader.getInstance().displayImage("drawable://" + categoryImageId, image, displayImageOptions);
        }
    }

    /**
     * @param requiresLowerCaseId Previously categories were translated using strings such as "category_Reading"
     *                            for the "Reading" category. Now we also need to have drawable resources such as
     *                            "category_reading". Note how drawables must have only lower case letters, whereas
     *                            we already have upper case letters in strings.xml. Hence this flag.
     */
    private static int getCategoryResource(Context context, @NonNull String categoryName, String resourceType,
                                           boolean requiresLowerCaseId) {
        String suffix = categoryName.replace(" & ", "_").replace(" ", "_").replace("'", "");
        if (requiresLowerCaseId) {
            suffix = suffix.toLowerCase(Locale.ENGLISH);
        }
        return context.getResources().getIdentifier("category_" + suffix, resourceType, context.getPackageName());
    }

    public static int getBackgroundColour(Context context, @NonNull String categoryName) {
        int colourId = getCategoryResource(context, categoryName, "color", true);
        if (colourId > 0) {
            return ContextCompat.getColor(context, colourId);
        }

        // Seed based on the categoryName, so that each time we try to choose a colour for the same
        // category it will look the same for each different user, and each different session.
        Random random = new Random(categoryName.toLowerCase(Locale.ENGLISH).hashCode());

        float[] hsv = new float[3];
        hsv[0] = random.nextFloat() * 360;
        hsv[1] = 0.4f;
        hsv[2] = 0.5f;
        return Color.HSVToColor(hsv);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == currentCategory.hashCode() + 1) {
            return new CursorLoader(
                    activity,
                    AppProvider.getCategoryUri(currentCategory),
                    new String[]{Schema.AppMetadataTable.Cols._COUNT},
                    null,
                    null,
                    null
            );
        } else {
            return new CursorLoader(
                    activity,
                    AppProvider.getTopFromCategoryUri(currentCategory, categoryItemCount),
                    new String[]{
                            Schema.AppMetadataTable.Cols.NAME,
                            Schema.AppMetadataTable.Cols.Package.PACKAGE_NAME,
                            Schema.AppMetadataTable.Cols.SUMMARY,
                            Schema.AppMetadataTable.Cols.ICON_URL,
                    },
                    null,
                    null,
                    Schema.AppMetadataTable.Cols.NAME
            );
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        int topAppsId = currentCategory.hashCode();
        int countAllAppsId = topAppsId + 1;

        // Anything other than these IDs indicates that the loader which just finished finished
        // is no longer the one this view holder is interested in, due to the user having
        // scrolled away already during the asynchronous query being run.
        if (loader.getId() == topAppsId) {
            appCardsAdapter.setAppCursor(cursor);
        } else if (loader.getId() == countAllAppsId) {
            cursor.moveToFirst();
            int numAppsInCategory = cursor.getInt(0);
            viewAll.setVisibility(View.VISIBLE);
            Resources r = activity.getResources();
            viewAll.setText(r.getQuantityString(R.plurals.button_view_all_apps_in_category, numAppsInCategory,
                    numAppsInCategory));
            viewAll.setContentDescription(r.getQuantityString(R.plurals.tts_view_all_in_category, numAppsInCategory,
                    numAppsInCategory, currentCategory));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        appCardsAdapter.setAppCursor(null);
    }

    @SuppressWarnings("FieldCanBeLocal")
    private final View.OnClickListener onViewAll = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (currentCategory == null) {
                return;
            }

            Intent intent = new Intent(activity, AppListActivity.class);
            intent.putExtra(AppListActivity.EXTRA_CATEGORY, currentCategory);
            activity.startActivity(intent);
        }
    };

    /**
     * Applies excessive padding to the start of the first item. This is so that the category artwork
     * can peek out and make itself visible. This is RTL friendly.
     *
     * @see org.fdroid.fdroid.R.dimen#category_preview__app_list__padding__horizontal
     * @see org.fdroid.fdroid.R.dimen#category_preview__app_list__padding__horizontal__first
     */
    private static class ItemDecorator extends RecyclerView.ItemDecoration {
        private final Context context;

        ItemDecorator(Context context) {
            this.context = context.getApplicationContext();
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            Resources r = context.getResources();
            int horizontalPadding = (int) r.getDimension(R.dimen.category_preview__app_list__padding__horizontal);
            int horizontalPaddingFirst = (int) r.getDimension(
                    R.dimen.category_preview__app_list__padding__horizontal__first);
            int horizontalPaddingLast = (int) r.getDimension(
                    R.dimen.category_preview__app_list__padding__horizontal__last);
            boolean isLtr = ViewCompat.getLayoutDirection(parent) == ViewCompat.LAYOUT_DIRECTION_LTR;
            int itemPosition = parent.getChildLayoutPosition(view);
            boolean first = itemPosition == 0;
            boolean end = itemPosition == categoryItemCount - 1;

            // Leave this "paddingEnd" local variable here for clarity when converting from
            // left/right to start/end for RTL friendly layout.
            // noinspection UnnecessaryLocalVariable
            int paddingEnd = end ? horizontalPaddingLast : horizontalPadding;
            int paddingStart = first ? horizontalPaddingFirst : horizontalPadding;

            int paddingLeft = isLtr ? paddingStart : paddingEnd;
            int paddingRight = isLtr ? paddingEnd : paddingStart;
            outRect.set(paddingLeft, 0, paddingRight, 0);
        }
    }
}

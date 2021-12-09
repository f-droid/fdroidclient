package org.fdroid.fdroid.views.categories;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.Schema;
import org.fdroid.fdroid.data.Schema.AppMetadataTable.Cols;
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

    private final AppCompatActivity activity;
    private final LoaderManager loaderManager;
    private static final int NUM_OF_APPS_PER_CATEGORY_ON_OVERVIEW = 20;

    private String currentCategory;

    CategoryController(final AppCompatActivity activity, LoaderManager loaderManager, View itemView) {
        super(itemView);

        this.activity = activity;
        this.loaderManager = loaderManager;

        appCardsAdapter = new AppPreviewAdapter(activity);

        viewAll = (Button) itemView.findViewById(R.id.view_all_button);
        viewAll.setOnClickListener(onViewAll);

        heading = (TextView) itemView.findViewById(R.id.name);
        image = (FeatureImage) itemView.findViewById(R.id.category_image);
        background = (FrameLayout) itemView.findViewById(R.id.category_background);

        RecyclerView appCards = (RecyclerView) itemView.findViewById(R.id.app_cards);
        appCards.setAdapter(appCardsAdapter);
        appCards.addItemDecoration(new ItemDecorator(activity));
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
            Glide.with(activity).load(categoryImageId).into(image);
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

    /**
     * Return either the total apps in the category, or the entries to display
     * for a category, depending on the value of {@code id}.  This uses a sort
     * similar to the one in {@link org.fdroid.fdroid.views.main.LatestViewBinder#onCreateLoader(int, Bundle)}.
     * The difference is that this does not treat "new" app any differently.
     *
     * @see AppProvider#getCategoryUri(String)
     * @see AppProvider#getTopFromCategoryUri(String, int)
     * @see AppProvider#query(android.net.Uri, String[], String, String[], String)
     * @see AppProvider#TOP_FROM_CATEGORY
     * @see org.fdroid.fdroid.views.main.LatestViewBinder#onCreateLoader(int, Bundle)
     */
    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        final String table = Schema.AppMetadataTable.NAME;
        final String added = table + "." + Cols.ADDED;
        final String lastUpdated = table + "." + Cols.LAST_UPDATED;
        if (id == currentCategory.hashCode() + 1) {
            return new CursorLoader(
                    activity,
                    AppProvider.getCategoryUri(currentCategory),
                    new String[]{Schema.AppMetadataTable.Cols._COUNT},
                    Utils.getAntifeatureSQLFilter(activity),
                    null,
                    null
            );
        } else {
            return new CursorLoader(
                    activity,
                    AppProvider.getTopFromCategoryUri(currentCategory, NUM_OF_APPS_PER_CATEGORY_ON_OVERVIEW),
                    new String[]{
                            Schema.AppMetadataTable.Cols.NAME,
                            Schema.AppMetadataTable.Cols.Package.PACKAGE_NAME,
                            Schema.AppMetadataTable.Cols.SUMMARY,
                            Schema.AppMetadataTable.Cols.ICON_URL,
                            Schema.AppMetadataTable.Cols.ICON,
                            Schema.AppMetadataTable.Cols.REPO_ID,
                    },
                    Utils.getAntifeatureSQLFilter(activity),
                    null,
                    table + "." + Cols.IS_LOCALIZED + " DESC"
                            + ", " + table + "." + Cols.NAME + " IS NULL ASC"
                            + ", CASE WHEN " + table + "." + Cols.ICON + " IS NULL"
                            + "        AND " + table + "." + Cols.ICON_URL + " IS NULL"
                            + "        THEN 1 ELSE 0 END"
                            + ", " + table + "." + Cols.SUMMARY + " IS NULL ASC"
                            + ", " + table + "." + Cols.DESCRIPTION + " IS NULL ASC"
                            + ", CASE WHEN " + table + "." + Cols.PHONE_SCREENSHOTS + " IS NULL"
                            + "        AND " + table + "." + Cols.SEVEN_INCH_SCREENSHOTS + " IS NULL"
                            + "        AND " + table + "." + Cols.TEN_INCH_SCREENSHOTS + " IS NULL"
                            + "        AND " + table + "." + Cols.TV_SCREENSHOTS + " IS NULL"
                            + "        AND " + table + "." + Cols.WEAR_SCREENSHOTS + " IS NULL"
                            + "        AND " + table + "." + Cols.FEATURE_GRAPHIC + " IS NULL"
                            + "        AND " + table + "." + Cols.PROMO_GRAPHIC + " IS NULL"
                            + "        AND " + table + "." + Cols.TV_BANNER + " IS NULL"
                            + "        THEN 1 ELSE 0 END"
                            + ", " + lastUpdated + " DESC"
                            + ", " + added + " ASC"
            );
        }
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        int topAppsId = currentCategory.hashCode();
        int countAllAppsId = topAppsId + 1;

        // Anything other than these IDs indicates that the loader which just finished
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
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
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
        public void getItemOffsets(Rect outRect, @NonNull View view, @NonNull RecyclerView parent,
                                   @NonNull RecyclerView.State state) {
            Resources r = context.getResources();
            int horizontalPadding = (int) r.getDimension(R.dimen.category_preview__app_list__padding__horizontal);
            int horizontalPaddingFirst = (int) r.getDimension(
                    R.dimen.category_preview__app_list__padding__horizontal__first);
            int horizontalPaddingLast = (int) r.getDimension(
                    R.dimen.category_preview__app_list__padding__horizontal__last);
            boolean isLtr = ViewCompat.getLayoutDirection(parent) == ViewCompat.LAYOUT_DIRECTION_LTR;
            int itemPosition = parent.getChildLayoutPosition(view);
            boolean first = itemPosition == 0;
            boolean end = itemPosition == NUM_OF_APPS_PER_CATEGORY_ON_OVERVIEW - 1;

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

package org.fdroid.fdroid.views.main;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.FrameLayout;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.CategoryProvider;
import org.fdroid.fdroid.data.Schema;
import org.fdroid.fdroid.views.categories.CategoryAdapter;

/**
 * Responsible for ensuring that the categories view is inflated and then populated correctly.
 * Will start a loader to get the list of categories from the database and populate a recycler
 * view with relevant info about each.
 */
class CategoriesViewBinder implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int LOADER_ID = 429820532;

    private final CategoryAdapter categoryAdapter;
    private final AppCompatActivity activity;

    CategoriesViewBinder(AppCompatActivity activity, FrameLayout parent) {
        this.activity = activity;

        View categoriesView = activity.getLayoutInflater().inflate(R.layout.main_tab_categories, parent, true);

        categoryAdapter = new CategoryAdapter(activity, activity.getSupportLoaderManager());

        RecyclerView categoriesList = (RecyclerView) categoriesView.findViewById(R.id.category_list);
        categoriesList.setHasFixedSize(true);
        categoriesList.setLayoutManager(new LinearLayoutManager(activity));
        categoriesList.setAdapter(categoryAdapter);

        activity.getSupportLoaderManager().initLoader(LOADER_ID, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id != LOADER_ID) {
            return null;
        }

        return new CursorLoader(
                activity,
                CategoryProvider.getAllCategories(),
                Schema.CategoryTable.Cols.ALL,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (loader.getId() != LOADER_ID) {
            return;
        }

        categoryAdapter.setCategoriesCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (loader.getId() != LOADER_ID) {
            return;
        }

        categoryAdapter.setCategoriesCursor(null);
    }

}

package org.fdroid.fdroid.views.main;

import android.content.Intent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.fdroid.database.Category;
import org.fdroid.database.FDroidDatabase;
import org.fdroid.database.FDroidDatabaseHolder;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.UpdateService;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.panic.HidingManager;
import org.fdroid.fdroid.views.apps.AppListActivity;
import org.fdroid.fdroid.views.categories.CategoryAdapter;
import org.fdroid.fdroid.views.categories.CategoryController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

/**
 * Responsible for ensuring that the categories view is inflated and then populated correctly.
 * Will start a loader to get the list of categories from the database and populate a recycler
 * view with relevant info about each.
 */
class CategoriesViewBinder implements Observer<List<Category>> {
    public static final String TAG = "CategoriesViewBinder";

    private final CategoryAdapter categoryAdapter;
    private final AppCompatActivity activity;
    private final TextView emptyState;
    private final RecyclerView categoriesList;

    CategoriesViewBinder(final AppCompatActivity activity, FrameLayout parent) {
        this.activity = activity;
        FDroidDatabase db = FDroidDatabaseHolder.getDb(activity);
        Transformations.distinctUntilChanged(db.getRepositoryDao().getLiveCategories()).observe(activity, this);

        View categoriesView = activity.getLayoutInflater().inflate(R.layout.main_tab_categories, parent, true);

        categoryAdapter = new CategoryAdapter(activity, db);

        emptyState = (TextView) categoriesView.findViewById(R.id.empty_state);

        categoriesList = (RecyclerView) categoriesView.findViewById(R.id.category_list);
        categoriesList.setHasFixedSize(true);
        categoriesList.setLayoutManager(new LinearLayoutManager(activity));
        categoriesList.setAdapter(categoryAdapter);

        final SwipeRefreshLayout swipeToRefresh =
                (SwipeRefreshLayout) categoriesView.findViewById(R.id.swipe_to_refresh);
        Utils.applySwipeLayoutColors(swipeToRefresh);
        swipeToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeToRefresh.setRefreshing(false);
                UpdateService.updateNow(activity);
            }
        });

        FloatingActionButton searchFab = (FloatingActionButton) categoriesView.findViewById(R.id.fab_search);
        searchFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.startActivity(new Intent(activity, AppListActivity.class));
            }
        });
        searchFab.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (Preferences.get().hideOnLongPressSearch()) {
                    HidingManager.showHideDialog(activity);
                    return true;
                } else {
                    return false;
                }
            }
        });
    }

    /**
     * Gets all categories from the DB and stores them in memory to provide to the {@link CategoryAdapter}.
     */
    @Override
    public void onChanged(List<Category> categories) {
        List<String> categoryNames = new ArrayList<>(categories.size());
        for (Category c : categories) {
            categoryNames.add(c.getId());
        }
        Collections.sort(categoryNames, new Comparator<String>() {
            @Override
            public int compare(String categoryOne, String categoryTwo) {
                String localizedCategoryOne = CategoryController.translateCategory(activity, categoryOne);
                String localizedCategoryTwo = CategoryController.translateCategory(activity, categoryTwo);
                return localizedCategoryOne.compareTo(localizedCategoryTwo);
            }
        });

        categoryAdapter.setCategories(categoryNames);

        if (categoryAdapter.getItemCount() == 0) {
            emptyState.setVisibility(View.VISIBLE);
            categoriesList.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            categoriesList.setVisibility(View.VISIBLE);
        }
    }

}

package org.fdroid.fdroid.views.main;

import android.content.Intent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.LocaleListCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.fdroid.database.Category;
import org.fdroid.database.FDroidDatabase;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.UpdateService;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.DBHelper;
import org.fdroid.fdroid.panic.HidingManager;
import org.fdroid.fdroid.views.apps.AppListActivity;
import org.fdroid.fdroid.views.categories.CategoryAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
        FDroidDatabase db = DBHelper.getDb(activity);
        Transformations.distinctUntilChanged(db.getRepositoryDao().getLiveCategories()).observe(activity, this);

        View categoriesView = activity.getLayoutInflater().inflate(R.layout.main_tab_categories, parent, true);

        categoryAdapter = new CategoryAdapter(activity, db);

        emptyState = categoriesView.findViewById(R.id.empty_state);

        categoriesList = categoriesView.findViewById(R.id.category_list);
        categoriesList.setHasFixedSize(true);
        categoriesList.setLayoutManager(new LinearLayoutManager(activity));
        categoriesList.setAdapter(categoryAdapter);

        final SwipeRefreshLayout swipeToRefresh =
                categoriesView.findViewById(R.id.swipe_to_refresh);
        Utils.applySwipeLayoutColors(swipeToRefresh);
        swipeToRefresh.setOnRefreshListener(() -> {
            swipeToRefresh.setRefreshing(false);
            UpdateService.updateNow(activity);
        });

        FloatingActionButton searchFab = categoriesView.findViewById(R.id.fab_search);
        searchFab.setOnClickListener(v -> activity.startActivity(new Intent(activity, AppListActivity.class)));
        searchFab.setOnLongClickListener(view -> {
            if (Preferences.get().hideOnLongPressSearch()) {
                HidingManager.showHideDialog(activity);
                return true;
            } else {
                return false;
            }
        });
    }

    /**
     * Gets all categories from the DB and stores them in memory to provide to the {@link CategoryAdapter}.
     */
    @Override
    public void onChanged(List<Category> categories) {
        LocaleListCompat localeListCompat = LocaleListCompat.getDefault();
        Collections.sort(categories, (o1, o2) -> {
            String name1 = o1.getName(localeListCompat);
            if (name1 == null) name1 = o1.getId();
            String name2 = o2.getName(localeListCompat);
            if (name2 == null) name2 = o2.getId();
            return name1.compareToIgnoreCase(name2);
        });
        // TODO force-adding nightly category here can be removed once fdroidserver LTS supports defining categories
        ArrayList<Category> c = new ArrayList<>(categories);
        Map<String, String> name = Collections.singletonMap("en-US", activity.getString(R.string.category_Nightly));
        c.add(new Category(42L, "nightly", Collections.emptyMap(), name, Collections.emptyMap()));
        categoryAdapter.setCategories(c);

        if (categoryAdapter.getItemCount() == 0) {
            emptyState.setVisibility(View.VISIBLE);
            categoriesList.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            categoriesList.setVisibility(View.VISIBLE);
        }
    }

}

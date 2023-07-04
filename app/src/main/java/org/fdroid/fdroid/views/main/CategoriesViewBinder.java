package org.fdroid.fdroid.views.main;

import android.content.Intent;
import android.util.ArraySet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
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
import org.fdroid.fdroid.views.categories.CategoryItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Responsible for ensuring that the categories view is inflated and then populated correctly.
 * Will start a loader to get the list of categories from the database and populate a recycler
 * view with relevant info about each.
 */
class CategoriesViewBinder implements Observer<List<Category>> {
    public static final String TAG = "CategoriesViewBinder";

    private final FDroidDatabase db;
    private final String[] defaultCategories;
    private final CategoryAdapter categoryAdapter;
    private final TextView emptyState;
    private final RecyclerView categoriesList;
    @Nullable
    private Disposable disposable;

    CategoriesViewBinder(final AppCompatActivity activity, FrameLayout parent) {
        db = DBHelper.getDb(activity);
        Transformations.distinctUntilChanged(db.getRepositoryDao().getLiveCategories()).observe(activity, this);
        defaultCategories = activity.getResources().getStringArray(R.array.defaultCategories);

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
        if (disposable != null) disposable.dispose();
        disposable = Single.fromCallable(() -> loadCategoryItems(categories))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onItemsLoaded);
    }

    private void onItemsLoaded(List<CategoryItem> items) {
        if (items.size() == 0) {
            emptyState.setVisibility(View.VISIBLE);
            categoriesList.setVisibility(View.GONE);
        } else {
            categoryAdapter.setCategories(items);
            emptyState.setVisibility(View.GONE);
            categoriesList.setVisibility(View.VISIBLE);
        }
    }

    @WorkerThread
    private List<CategoryItem> loadCategoryItems(List<Category> categories) {
        // get items
        ArrayList<CategoryItem> items = new ArrayList<>();
        ArraySet<String> ids = new ArraySet<>(categories.size());
        for (Category c : categories) {
            int numApps = db.getAppDao().getNumberOfAppsInCategory(c.getId());
            if (numApps > 0) {
                ids.add(c.getId());
                CategoryItem item = new CategoryItem(c, numApps);
                items.add(item);
            } else {
                Log.d(TAG, "Not adding " + c.getId() + " because it has no apps.");
            }
        }
        // add default categories, if they are not in already
        for (String id : defaultCategories) {
            if (!ids.contains(id)) {
                int numApps = db.getAppDao().getNumberOfAppsInCategory(id);
                if (numApps > 0) {
                    // name and icon gets set in CategoryController, if not given here
                    Category c = new Category(2L, id, Collections.emptyMap(), Collections.emptyMap(),
                            Collections.emptyMap());
                    CategoryItem item = new CategoryItem(c, numApps);
                    items.add(item);
                } else {
                    Log.d(TAG, "Not adding default " + id + " because it has no apps.");
                }
            }
        }
        // sort items
        LocaleListCompat localeListCompat = LocaleListCompat.getDefault();
        Collections.sort(items, (o1, o2) -> {
            ids.add(o2.category.getId());
            String name1 = o1.category.getName(localeListCompat);
            if (name1 == null) name1 = o1.category.getId();
            String name2 = o2.category.getName(localeListCompat);
            if (name2 == null) name2 = o2.category.getId();
            return name1.compareToIgnoreCase(name2);
        });
        return items;
    }

}

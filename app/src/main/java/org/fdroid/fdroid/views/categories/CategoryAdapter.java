package org.fdroid.fdroid.views.categories;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import org.fdroid.database.AppOverviewItem;
import org.fdroid.database.Category;
import org.fdroid.database.FDroidDatabase;
import org.fdroid.fdroid.R;

import java.util.HashMap;
import java.util.List;

public class CategoryAdapter extends ListAdapter<CategoryItem, CategoryController> {

    private final AppCompatActivity activity;
    private final FDroidDatabase db;
    private final HashMap<Category, LiveData<List<AppOverviewItem>>> liveData = new HashMap<>();

    public CategoryAdapter(AppCompatActivity activity, FDroidDatabase db) {
        super(new DiffUtil.ItemCallback<CategoryItem>() {
            @Override
            public boolean areItemsTheSame(@NonNull CategoryItem oldItem, @NonNull CategoryItem newItem) {
                return oldItem.category.equals(newItem.category);
            }

            @Override
            public boolean areContentsTheSame(@NonNull CategoryItem oldItem, @NonNull CategoryItem newItem) {
                return false;
            }
        });

        this.activity = activity;
        this.db = db;
    }

    @NonNull
    @Override
    public CategoryController onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new CategoryController(activity, activity.getLayoutInflater()
                .inflate(R.layout.category_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryController holder, int position) {
        CategoryItem item = getItem(position);
        holder.bindModel(item, liveData.get(item.category));
    }

    public void setCategories(@NonNull List<CategoryItem> items) {
        submitList(items);
        for (CategoryItem item : items) {
            int num = CategoryController.NUM_OF_APPS_PER_CATEGORY_ON_OVERVIEW;
            // we are getting the LiveData here and not in the ViewHolder, so the data gets cached here
            // this prevents reloads when scrolling
            liveData.put(item.category, db.getAppDao().getAppOverviewItems(item.category.getId(), num));
        }
    }
}

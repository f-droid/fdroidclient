package org.fdroid.fdroid.views.categories;

import android.view.ViewGroup;

import org.fdroid.database.AppOverviewItem;
import org.fdroid.database.Category;
import org.fdroid.database.FDroidDatabase;
import org.fdroid.fdroid.R;

import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

public class CategoryAdapter extends ListAdapter<Category, CategoryController> {

    private final AppCompatActivity activity;
    private final FDroidDatabase db;
    private final HashMap<Category, LiveData<List<AppOverviewItem>>> liveData = new HashMap<>();

    public CategoryAdapter(AppCompatActivity activity, FDroidDatabase db) {
        super(new DiffUtil.ItemCallback<Category>() {
            @Override
            public boolean areItemsTheSame(Category oldItem, Category newItem) {
                return oldItem.equals(newItem);
            }

            @Override
            public boolean areContentsTheSame(Category oldItem, Category newItem) {
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
        Category category = getItem(position);
        holder.bindModel(category, liveData.get(category));
    }

    public void setCategories(@NonNull List<Category> categories) {
        submitList(categories);
        for (Category category: categories) {
            int num = CategoryController.NUM_OF_APPS_PER_CATEGORY_ON_OVERVIEW;
            // we are getting the LiveData here and not in the ViewHolder, so the data gets cached here
            // this prevents reloads when scrolling
            liveData.put(category, db.getAppDao().getAppOverviewItems(category.getId(), num));
        }
    }

}

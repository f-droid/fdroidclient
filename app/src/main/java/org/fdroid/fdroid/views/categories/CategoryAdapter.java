package org.fdroid.fdroid.views.categories;

import android.util.Log;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class CategoryAdapter extends ListAdapter<Category, CategoryController> {

    private final AppCompatActivity activity;
    private final FDroidDatabase db;
    private final HashMap<Category, LiveData<List<AppOverviewItem>>> liveData = new HashMap<>();

    public CategoryAdapter(AppCompatActivity activity, FDroidDatabase db) {
        super(new DiffUtil.ItemCallback<Category>() {
            @Override
            public boolean areItemsTheSame(@NonNull Category oldItem, @NonNull Category newItem) {
                return oldItem.equals(newItem);
            }

            @Override
            public boolean areContentsTheSame(@NonNull Category oldItem, @NonNull Category newItem) {
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
        holder.bindModel(category, liveData.get(category), this::onNoApps);
    }

    public void setCategories(@NonNull List<Category> categories) {
        submitList(categories);
        for (Category category : categories) {
            int num = CategoryController.NUM_OF_APPS_PER_CATEGORY_ON_OVERVIEW;
            // we are getting the LiveData here and not in the ViewHolder, so the data gets cached here
            // this prevents reloads when scrolling
            liveData.put(category, db.getAppDao().getAppOverviewItems(category.getId(), num));
        }
    }

    private void onNoApps(Category category) {
        ArrayList<Category> categories = new ArrayList<>(getCurrentList());
        Iterator<Category> itr = categories.iterator();
        while (itr.hasNext()) {
            Category c = itr.next();
            if (c.getId().equals(category.getId())) {
                Log.d("CategoryAdapter", "Removing " + category.getId() + " without apps.");
                itr.remove();
                break;
            }
        }
        submitList(categories);
    }
}

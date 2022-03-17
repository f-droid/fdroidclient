package org.fdroid.fdroid.views.categories;

import android.view.ViewGroup;

import org.fdroid.database.AppOverviewItem;
import org.fdroid.database.FDroidDatabase;
import org.fdroid.fdroid.R;

import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

public class CategoryAdapter extends ListAdapter<String, CategoryController> {

    private final AppCompatActivity activity;
    private final FDroidDatabase db;
    private final HashMap<String, LiveData<List<AppOverviewItem>>> liveData = new HashMap<>();

    public CategoryAdapter(AppCompatActivity activity, FDroidDatabase db) {
        super(new DiffUtil.ItemCallback<String>() {
            @Override
            public boolean areItemsTheSame(String oldItem, String newItem) {
                return oldItem.equals(newItem);
            }

            @Override
            public boolean areContentsTheSame(String oldItem, String newItem) {
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
        String categoryName = getItem(position);
        holder.bindModel(categoryName, liveData.get(categoryName));
    }

    public void setCategories(@NonNull List<String> unlocalizedCategoryNames) {
        submitList(unlocalizedCategoryNames);
        for (String name: unlocalizedCategoryNames) {
            int num = CategoryController.NUM_OF_APPS_PER_CATEGORY_ON_OVERVIEW;
            // we are getting the LiveData here and not in the ViewHolder, so the data gets cached here
            // this prevents reloads when scrolling
            liveData.put(name, db.getAppDao().getAppOverviewItems(name, num));
        }
    }

}

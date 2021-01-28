package org.fdroid.fdroid.views.categories;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.loader.app.LoaderManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import android.view.ViewGroup;

import org.fdroid.fdroid.R;

import java.util.List;

public class CategoryAdapter extends ListAdapter<String, CategoryController> {

    private final AppCompatActivity activity;
    private final LoaderManager loaderManager;

    public CategoryAdapter(AppCompatActivity activity, LoaderManager loaderManager) {
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
        this.loaderManager = loaderManager;
    }

    @NonNull
    @Override
    public CategoryController onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new CategoryController(activity, loaderManager, activity.getLayoutInflater()
                .inflate(R.layout.category_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryController holder, int position) {
        holder.bindModel(getItem(position));
    }

    public void setCategories(@NonNull List<String> unlocalizedCategoryNames) {
        submitList(unlocalizedCategoryNames);
    }

}

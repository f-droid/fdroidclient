package org.fdroid.fdroid.views.categories;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import org.fdroid.fdroid.R;

import java.util.Collections;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryController> {

    @NonNull
    private List<String> unlocalizedCategoryNames = Collections.<String>emptyList();

    private final Activity activity;
    private final LoaderManager loaderManager;

    public CategoryAdapter(Activity activity, LoaderManager loaderManager) {
        this.activity = activity;
        this.loaderManager = loaderManager;
    }

    @Override
    public CategoryController onCreateViewHolder(ViewGroup parent, int viewType) {
        return new CategoryController(activity, loaderManager, activity.getLayoutInflater()
                .inflate(R.layout.category_item, parent, false));
    }

    @Override
    public void onBindViewHolder(CategoryController holder, int position) {
        holder.bindModel(unlocalizedCategoryNames.get(position));
    }

    @Override
    public int getItemCount() {
        return unlocalizedCategoryNames.size();
    }

    public void setCategories(@NonNull List<String> unlocalizedCategoryNames) {
        this.unlocalizedCategoryNames = unlocalizedCategoryNames;
        notifyDataSetChanged();
    }

}

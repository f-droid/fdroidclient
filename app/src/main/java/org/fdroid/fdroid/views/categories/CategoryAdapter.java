package org.fdroid.fdroid.views.categories;

import android.app.Activity;
import android.database.Cursor;
import android.support.v4.app.LoaderManager;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.Schema;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryController> {

    private Cursor cursor;
    private final Activity activity;
    private final LoaderManager loaderManager;

    public CategoryAdapter(Activity activity, LoaderManager loaderManager) {
        this.activity = activity;
        this.loaderManager = loaderManager;
    }

    @Override
    public CategoryController onCreateViewHolder(ViewGroup parent, int viewType) {
        return new CategoryController(activity, loaderManager, activity.getLayoutInflater().inflate(R.layout.category_item, parent, false));
    }

    @Override
    public void onBindViewHolder(CategoryController holder, int position) {
        cursor.moveToPosition(position);
        holder.bindModel(cursor.getString(cursor.getColumnIndex(Schema.CategoryTable.Cols.NAME)));
    }

    @Override
    public int getItemCount() {
        return cursor == null ? 0 : cursor.getCount();
    }

    public void setCategoriesCursor(Cursor cursor) {
        this.cursor = cursor;
        notifyDataSetChanged();
    }

}

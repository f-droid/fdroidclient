package org.fdroid.fdroid.views.filebrowser;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.fdroid.fdroid.R;

import java.io.File;

public class fileBrowserAdapter extends RecyclerView.Adapter<fileBrowserHolder> {

    private File[] files;
    private RecyclerView mRecyclerView;


    public void setFiles(File[] files) {
        this.files = files;
        notifyDataSetChanged();
    }

    public void refresh() {
        notifyDataSetChanged();
    }


    @Override
    public int getItemCount() {
        return files.length;
    }


    void result(File file) {
        //Return Values
    }


    public final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int itemPosition = mRecyclerView.getChildLayoutPosition(view);
            result(files[itemPosition]);
        }
    };

    @NonNull
    @Override
    public fileBrowserHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.activity_filebrowser_list_item, viewGroup, false);
        view.setOnClickListener(mOnClickListener);
        return new fileBrowserHolder(view);
    }


    @Override
    public void onBindViewHolder(fileBrowserHolder holder, int i) {
        holder.bind(files[i]);
    }


    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;
    }

}

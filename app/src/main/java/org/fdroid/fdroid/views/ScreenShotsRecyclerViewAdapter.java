package org.fdroid.fdroid.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.App;

public class ScreenShotsRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements LinearLayoutManagerSnapHelper.LinearSnapHelperListener {
    private final App app;
    private final DisplayImageOptions displayImageOptions;
    private View selectedView;
    private int selectedPosition;
    private final int selectedItemElevation;
    private final int unselectedItemMargin;

    public ScreenShotsRecyclerViewAdapter(Context context, App app) {
        super();
        this.app = app;
        selectedPosition = 0;
        selectedItemElevation = context.getResources().getDimensionPixelSize(R.dimen.details_screenshot_selected_elevation);
        unselectedItemMargin = context.getResources().getDimensionPixelSize(R.dimen.details_screenshot_margin);
        displayImageOptions = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .imageScaleType(ImageScaleType.NONE)
                .showImageOnLoading(R.drawable.ic_repo_app_default)
                .showImageForEmptyUri(R.drawable.ic_repo_app_default)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .build();
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ScreenShotViewHolder vh = (ScreenShotViewHolder) holder;
        setViewSelected(vh.itemView, position == selectedPosition);
        if (position == selectedPosition)
            this.selectedView = vh.itemView;
        ImageLoader.getInstance().displayImage(app.iconUrlLarge, vh.image, displayImageOptions);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.app_details2_screenshot_item, parent, false);
        return new ScreenShotViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return 7;
    }

    @Override
    public void onSnappedToView(View view, int snappedPosition) {
        setViewSelected(selectedView, false);
        selectedView = view;
        selectedPosition = snappedPosition;
        setViewSelected(selectedView, true);
    }

    private void setViewSelected(View view, boolean selected) {
        if (view != null) {
            RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams)view.getLayoutParams();
            if (selected)
                lp.setMargins(0,selectedItemElevation,0,selectedItemElevation);
            else
                lp.setMargins(0,unselectedItemMargin,0,unselectedItemMargin);
            ViewCompat.setElevation(view, selected ? selectedItemElevation : selectedItemElevation / 2);
            view.setLayoutParams(lp);
        }
    }

    private class ScreenShotViewHolder extends RecyclerView.ViewHolder {
        final ImageView image;

        ScreenShotViewHolder(View view) {
            super(view);
            image = (ImageView) view.findViewById(R.id.image);
        }

        @Override
        public String toString() {
            return super.toString() + " screenshot";
        }
    }
}

package org.fdroid.fdroid.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.App;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Loads and displays the small screenshots that are inline in {@link AppDetailsActivity}
 */
class ScreenShotsRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final String[] screenshots;
    private final DisplayImageOptions displayImageOptions;
    private final Listener listener;

    ScreenShotsRecyclerViewAdapter(Context context, App app, Listener listener) {
        super();
        this.listener = listener;

        screenshots = app.getAllScreenshots(context);

        Drawable screenShotPlaceholder = ContextCompat.getDrawable(context, R.drawable.screenshot_placeholder);
        displayImageOptions = Utils.getDefaultDisplayImageOptionsBuilder()
                .showImageOnFail(screenShotPlaceholder)
                .showImageOnLoading(screenShotPlaceholder)
                .showImageForEmptyUri(screenShotPlaceholder)
                .build();
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
        final ScreenShotViewHolder vh = (ScreenShotViewHolder) holder;
        ImageLoader.getInstance().displayImage(screenshots[position],
                vh.image, displayImageOptions);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.app_details2_screenshot_item, parent, false);
        return new ScreenShotViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return screenshots.length;
    }

    public interface Listener {
        /**
         * @param position zero based position of the screenshot
         *                 that has been clicked upon
         */
        void onScreenshotClick(int position);
    }

    private class ScreenShotViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final ImageView image;

        ScreenShotViewHolder(View view) {
            super(view);
            image = (ImageView) view.findViewById(R.id.image);
            image.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            listener.onScreenshotClick(getAdapterPosition());
        }

        @Override
        public String toString() {
            return super.toString() + " screenshot";
        }
    }
}

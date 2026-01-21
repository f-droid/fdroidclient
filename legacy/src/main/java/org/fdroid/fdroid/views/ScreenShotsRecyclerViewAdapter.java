package org.fdroid.fdroid.views;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.request.RequestOptions;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.App;
import org.fdroid.index.v2.FileV2;

import java.util.List;

/**
 * Loads and displays the small screenshots that are inline in {@link AppDetailsActivity}
 */
class ScreenShotsRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final long repoId;
    private final List<FileV2> screenshots;
    private final RequestOptions displayImageOptions;
    private final Listener listener;

    ScreenShotsRecyclerViewAdapter(App app, Listener listener) {
        super();
        this.repoId = app.repoId;
        this.listener = listener;

        screenshots = app.getAllScreenshots();

        displayImageOptions = new RequestOptions()
                .fallback(R.drawable.screenshot_placeholder)
                .error(R.drawable.screenshot_placeholder);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
        final ScreenShotViewHolder vh = (ScreenShotViewHolder) holder;
        App.loadWithGlide(vh.itemView.getContext(), repoId, screenshots.get(position))
                .apply(displayImageOptions).into(vh.image);
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
        return screenshots.size();
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
            image = view.findViewById(R.id.image);
            image.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            listener.onScreenshotClick(getAdapterPosition());
        }

        @NonNull
        @Override
        public String toString() {
            return super.toString() + " screenshot";
        }
    }
}

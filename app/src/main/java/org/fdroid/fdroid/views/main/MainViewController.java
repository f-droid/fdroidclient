package org.fdroid.fdroid.views.main;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.views.myapps.MyAppsViewBinder;
import org.fdroid.fdroid.views.swap.SwapWorkflowActivity;

/**
 * Decides which view on the main screen to attach to a given {@link FrameLayout}. This class
 * doesn't know which view it will be rendering at the time it is constructed. Rather, at some
 * point in the future the {@link MainViewAdapter} will have information about which view we
 * are required to render, and will invoke the relevant "bind*()" method on this class.
 */
class MainViewController extends RecyclerView.ViewHolder {

    private final AppCompatActivity activity;
    private final FrameLayout frame;

    MainViewController(AppCompatActivity activity, FrameLayout frame) {
        super(frame);
        this.activity = activity;
        this.frame = frame;
    }

    public void clearViews() {
        frame.removeAllViews();
    }

    /**
     * @see WhatsNewViewBinder
     */
    public void bindWhatsNewView() {
        new WhatsNewViewBinder(activity, frame);
    }

    /**
     * @see MyAppsViewBinder
     */
    public void bindMyApps() {
        new MyAppsViewBinder(activity, frame);
    }

    /**
     * @see CategoriesViewBinder
     */
    public void bindCategoriesView() {
        new CategoriesViewBinder(activity, frame);
    }

    /**
     * A splash screen encouraging people to start the swap process.
     * The swap process is quite heavy duty in that it fires up Bluetooth and/or WiFi in
     * order to scan for peers. As such, it is quite convenient to have a more lightweight view to show
     * in the main navigation that doesn't automatically start doing things when the user touches the
     * navigation menu in the bottom navigation.
     */
    public void bindSwapView() {
        View swapView = activity.getLayoutInflater().inflate(R.layout.main_tab_swap, frame, true);

        Button startButton = (Button) swapView.findViewById(R.id.button);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.startActivity(new Intent(activity, SwapWorkflowActivity.class));
            }
        });
    }

    public void bindSettingsView() {
    }
}

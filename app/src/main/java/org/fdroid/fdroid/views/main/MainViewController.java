package org.fdroid.fdroid.views.main;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.widget.FrameLayout;

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

}

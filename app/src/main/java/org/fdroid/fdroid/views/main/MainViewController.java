package org.fdroid.fdroid.views.main;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.views.fragments.PreferencesFragment;
import org.fdroid.fdroid.views.swap.SwapWorkflowActivity;
import org.fdroid.fdroid.views.updates.UpdatesViewBinder;

/**
 * Decides which view on the main screen to attach to a given {@link FrameLayout}. This class
 * doesn't know which view it will be rendering at the time it is constructed. Rather, at some
 * point in the future the {@link MainViewAdapter} will have information about which view we
 * are required to render, and will invoke the relevant "bind*()" method on this class.
 */
class MainViewController extends RecyclerView.ViewHolder {

    private final AppCompatActivity activity;
    private final FrameLayout frame;

    @Nullable
    private UpdatesViewBinder updatesView = null;

    MainViewController(AppCompatActivity activity, FrameLayout frame) {
        super(frame);
        this.activity = activity;
        this.frame = frame;
    }

    /**
     * @see WhatsNewViewBinder
     */
    public void bindWhatsNewView() {
        new WhatsNewViewBinder(activity, frame);
    }

    /**
     * @see UpdatesViewBinder
     */
    public void bindUpdates() {
        if (updatesView == null) {
            updatesView = new UpdatesViewBinder(activity, frame);
        }

        updatesView.bind();
    }

    public void unbindUpdates() {
        if (updatesView != null) {
            updatesView.unbind();
        }
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

        // To allow for whitelabel versions of F-Droid, make sure not to hardcode "F-Droid" into our
        // translation here.
        TextView subtext = (TextView) swapView.findViewById(R.id.text2);
        subtext.setText(activity.getString(R.string.nearby_splash__both_parties_need_fdroid,
                activity.getString(R.string.app_name)));

        Button startButton = (Button) swapView.findViewById(R.id.button);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.startActivity(new Intent(activity, SwapWorkflowActivity.class));
            }
        });
    }

    /**
     * Attaches a {@link PreferencesFragment} to the view. Everything else is managed by the
     * fragment itself, so no further work needs to be done by this view binder.
     * <p>
     * Note: It is tricky to attach a {@link Fragment} to a view from this view holder. This is due
     * to the way in which the {@link RecyclerView} will reuse existing views and ask us to
     * put a settings fragment in there at arbitrary times. Usually it wont be the same view we
     * attached the fragment to last time, which causes weirdness. The solution is to use code from
     * the com.lsjwzh.widget.recyclerviewpager.FragmentStatePagerAdapter which manages this.
     * The code has been ported to {@link SettingsView}.
     *
     * @see SettingsView
     */
    public void bindSettingsView() {
        activity.getLayoutInflater().inflate(R.layout.main_tab_settings, frame, true);
    }
}

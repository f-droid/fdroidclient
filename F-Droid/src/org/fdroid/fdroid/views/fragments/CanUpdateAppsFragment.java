
package org.fdroid.fdroid.views.fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.installer.Installer;
import org.fdroid.fdroid.views.AppListAdapter;
import org.fdroid.fdroid.views.CanUpdateAppListAdapter;

public class CanUpdateAppsFragment extends AppListFragment {

    // copied from ListFragment
    static final int INTERNAL_EMPTY_ID = 0x00ff0001;
    static final int INTERNAL_PROGRESS_CONTAINER_ID = 0x00ff0002;
    static final int INTERNAL_LIST_CONTAINER_ID = 0x00ff0003;
    // added for update button
    static final int UPDATE_ALL_BUTTON_ID = 0x00ff0004;

    private Button mUpdateAllButton;
    private Installer mInstaller;

    @Override
    protected AppListAdapter getAppListAdapter() {
        return new CanUpdateAppListAdapter(getActivity(), null);
    }

    @Override
    protected String getFromTitle() {
        return getString(R.string.tab_updates);
    }

    @Override
    protected Uri getDataUri() {
        return AppProvider.getCanUpdateUri();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mInstaller = Installer.getActivityInstaller(getActivity(), getActivity()
                .getPackageManager(), null);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mUpdateAllButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO

            }
        });
    }

    // TODO: not really called again after coming back from preference
    @Override
    public void onResume() {
        super.onResume();

        if (mInstaller.supportsUnattendedOperations()) {
//            mUpdateAllButton.setVisibility(View.VISIBLE);
            mUpdateAllButton.setVisibility(View.GONE);
        } else {
            mUpdateAllButton.setVisibility(View.GONE);
        }
    }

    /**
     * Copied from ListFragment and added Button on top of list. We do not use a
     * custom layout here, because this breaks the progress bar functionality of
     * ListFragment.
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final Context context = getActivity();

        FrameLayout root = new FrameLayout(context);

        // ------------------------------------------------------------------

        LinearLayout pframe = new LinearLayout(context);
        pframe.setId(INTERNAL_PROGRESS_CONTAINER_ID);
        pframe.setOrientation(LinearLayout.VERTICAL);
        pframe.setVisibility(View.GONE);
        pframe.setGravity(Gravity.CENTER);

        ProgressBar progress = new ProgressBar(context, null,
                android.R.attr.progressBarStyleLarge);
        pframe.addView(progress, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(pframe, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // ------------------------------------------------------------------

        FrameLayout lframe = new FrameLayout(context);
        lframe.setId(INTERNAL_LIST_CONTAINER_ID);

        TextView tv = new TextView(getActivity());
        tv.setId(INTERNAL_EMPTY_ID);
        tv.setGravity(Gravity.CENTER);
        lframe.addView(tv, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Added update all button
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        mUpdateAllButton = new Button(context);
        mUpdateAllButton.setId(UPDATE_ALL_BUTTON_ID);
        mUpdateAllButton.setText(R.string.update_all);
        mUpdateAllButton.setCompoundDrawablesWithIntrinsicBounds(
                getResources().getDrawable(R.drawable.ic_menu_refresh), null, null, null);

        linearLayout.addView(mUpdateAllButton, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ListView lv = new ListView(getActivity());
        lv.setId(android.R.id.list);
        lv.setDrawSelectorOnTop(false);
        linearLayout.addView(lv, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        lframe.addView(linearLayout, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        root.addView(lframe, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // ------------------------------------------------------------------

        root.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        return root;
    }

}

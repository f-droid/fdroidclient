package org.fdroid.fdroid.views;

import static org.junit.Assert.assertEquals;

import android.app.Application;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.appcompat.view.ContextThemeWrapper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;

import org.fdroid.database.AppPrefs;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.TestUtils;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;
import org.fdroid.index.v2.FileV2;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Config(application = Application.class)
@RunWith(RobolectricTestRunner.class)
public class AppDetailsAdapterTest {

    private final AppPrefs appPrefs = new AppPrefs("com.example.app", 0, null);
    private Context context;

    @Before
    public void setup() {
        // Must manually set the theme again here other than in AndroidManifest,xml
        // https://github.com/mozilla-mobile/fenix/pull/15646#issuecomment-707345798
        ApplicationProvider.getApplicationContext().setTheme(R.style.Theme_App);
        context = new ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.Theme_App);

        Preferences.setupForTests(context);
    }

    @Test
    public void appWithNoVersionsOrScreenshots() {
        App app = TestUtils.getApp();
        AppDetailsRecyclerViewAdapter adapter = new AppDetailsRecyclerViewAdapter(context, app, dummyCallbacks);
        adapter.updateItems(TestUtils.getApp(), Collections.emptyList(), appPrefs);
        populateViewHolders(adapter);

        assertEquals(3, adapter.getItemCount());
    }

    @Test
    public void appWithScreenshots() {
        App app = TestUtils.getApp();
        app.phoneScreenshots = new ArrayList<>(2);
        app.phoneScreenshots.add(FileV2.fromPath("screenshot1.png"));
        app.phoneScreenshots.add(FileV2.fromPath("screenshot2.png"));

        AppDetailsRecyclerViewAdapter adapter = new AppDetailsRecyclerViewAdapter(context, app, dummyCallbacks);
        adapter.updateItems(app, Collections.emptyList(), appPrefs);
        populateViewHolders(adapter);

        assertEquals(4, adapter.getItemCount());
    }

    @Test
    public void appWithVersions() {
        App app = TestUtils.getApp();
        app.preferredSigner = "eaa1d713b9c2a0475234a86d6539f910";
        List<Apk> apks = new ArrayList<>();
        apks.add(TestUtils.getApk(1));
        apks.add(TestUtils.getApk(2));
        apks.add(TestUtils.getApk(3));
        app.installedApk = apks.get(0);

        AppDetailsRecyclerViewAdapter adapter = new AppDetailsRecyclerViewAdapter(context, app, dummyCallbacks);
        adapter.updateItems(app, apks, appPrefs);
        populateViewHolders(adapter);

        // Starts collapsed, not showing versions at all. (also showing permissions)
        assertEquals(4, adapter.getItemCount());

        adapter.setShowVersions(true);
        assertEquals(7, adapter.getItemCount());

        adapter.setShowVersions(false);
        assertEquals(4, adapter.getItemCount());
    }

    /**
     * Ensures that every single item in the adapter gets its view holder created and bound.
     * Doesn't care about what type of holder it should be, the adapter is able to figure all that
     * out for us .
     */
    private void populateViewHolders(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter) {
        ViewGroup parent = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.app_details2_links, null);
        for (int i = 0; i < adapter.getItemCount(); i++) {
            RecyclerView.ViewHolder viewHolder = adapter.createViewHolder(parent, adapter.getItemViewType(i));
            adapter.bindViewHolder(viewHolder, i);
        }
    }

    private final AppDetailsRecyclerViewAdapter.AppDetailsRecyclerViewAdapterCallbacks dummyCallbacks
            = new AppDetailsRecyclerViewAdapter.AppDetailsRecyclerViewAdapterCallbacks() {
                @Override
                public boolean isAppDownloading() {
                    return false;
                }

                @Override
                public void enableAndroidBeam() {

                }

                @Override
                public void disableAndroidBeam() {

                }

                @Override
                public void openUrl(String url) {

                }

                @Override
                public void installApk(Apk apk) {

                }

                @Override
                public void uninstallApk() {

                }

                @Override
                public void installCancel() {

                }

                @Override
                public void launchApk() {

                }
            };

}

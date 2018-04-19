package org.fdroid.fdroid.views;

import android.app.Application;
import android.content.ContentValues;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import org.fdroid.fdroid.Assert;
import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.AppProviderTest;
import org.fdroid.fdroid.data.DBHelper;
import org.fdroid.fdroid.data.FDroidProviderTest;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoProviderTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

@Config(constants = BuildConfig.class, application = Application.class)
@RunWith(RobolectricTestRunner.class)
public class AppDetailsAdapterTest extends FDroidProviderTest {

    private App app;

    @Before
    public void setup() {
        ImageLoader.getInstance().init(ImageLoaderConfiguration.createDefault(context));
        Preferences.setupForTests(context);

        Repo repo = RepoProviderTest.insertRepo(context, "http://www.example.com/fdroid/repo", "", "", "Test Repo");
        app = AppProviderTest.insertApp(contentResolver, context, "com.example.app", "Test App",
                new ContentValues(), repo.getId());
    }

    @After
    public void teardown() {
        ImageLoader.getInstance().destroy();
        DBHelper.clearDbHelperSingleton();
    }

    @Test
    public void appWithNoVersionsOrScreenshots() {
        AppDetailsRecyclerViewAdapter adapter = new AppDetailsRecyclerViewAdapter(context, app, dummyCallbacks);
        populateViewHolders(adapter);

        assertEquals(3, adapter.getItemCount());
    }

    @Test
    public void appWithScreenshots() {
        app.phoneScreenshots = new String[]{"screenshot1.png", "screenshot2.png"};

        AppDetailsRecyclerViewAdapter adapter = new AppDetailsRecyclerViewAdapter(context, app, dummyCallbacks);
        populateViewHolders(adapter);

        assertEquals(4, adapter.getItemCount());

    }

    @Test
    public void appWithVersions() {
        Assert.insertApk(context, app, 1);
        Assert.insertApk(context, app, 2);
        Assert.insertApk(context, app, 3);

        AppDetailsRecyclerViewAdapter adapter = new AppDetailsRecyclerViewAdapter(context, app, dummyCallbacks);
        populateViewHolders(adapter);

        // Starts collapsed, now showing versions at all.
        assertEquals(3, adapter.getItemCount());

        adapter.setShowVersions(true);
        assertEquals(6, adapter.getItemCount());

        adapter.setShowVersions(false);
        assertEquals(3, adapter.getItemCount());
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

    private final AppDetailsRecyclerViewAdapter.AppDetailsRecyclerViewAdapterCallbacks dummyCallbacks = new AppDetailsRecyclerViewAdapter.AppDetailsRecyclerViewAdapterCallbacks() { // NOCHECKSTYLE LineLength
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
        public void installApk() {

        }

        @Override
        public void installApk(Apk apk) {

        }

        @Override
        public void upgradeApk() {

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

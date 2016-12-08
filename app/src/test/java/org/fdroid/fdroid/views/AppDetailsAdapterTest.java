package org.fdroid.fdroid.views;

import android.app.Application;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.FDroidProvider;
import org.fdroid.fdroid.data.FDroidProviderTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

// TODO: Use sdk=24 when Robolectric supports this
@Config(constants = BuildConfig.class, application = Application.class, sdk = 23)
@RunWith(RobolectricGradleTestRunner.class)
public class AppDetailsAdapterTest extends FDroidProviderTest {

    @Before
    public void setup() {
        ImageLoader.getInstance().init(ImageLoaderConfiguration.createDefault(context));
    }

    @After
    public void teardown() {
        ImageLoader.getInstance().destroy();
        FDroidProvider.clearDbHelperSingleton();
    }

    @Test
    public void appWithNoVersions() {
        App app = new App();
        app.name = "Test App";
        app.description = "Test App <b>Description</b>";

        AppDetailsRecyclerViewAdapter adapter = new AppDetailsRecyclerViewAdapter(context, app, dummyCallbacks);
        populateViewHolders(adapter);

        assertEquals(5, adapter.getItemCount());

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

    private final AppDetailsRecyclerViewAdapter.AppDetailsRecyclerViewAdapterCallbacks dummyCallbacks = new AppDetailsRecyclerViewAdapter.AppDetailsRecyclerViewAdapterCallbacks() {
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

package org.fdroid.fdroid.views;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.App;
import org.fdroid.index.v2.FileV2;

import java.util.List;

/**
 * Full screen view of an apps screenshots to swipe through. This will always
 * download the image, even if the user has said not to use "unmetered" networks,
 * e.g. WiFi.  That is because the user has to click on the thumbnail in
 * {@link AppDetailsActivity} in order to bring up this activity.
 * That makes it a specific request for that image, rather than regular
 * background loading.
 */
public class ScreenShotsActivity extends AppCompatActivity {

    private static final String EXTRA_REPO_ID = "EXTRA_REPO_ID";
    private static final String EXTRA_SCREENSHOT_LIST = "EXTRA_SCREENSHOT_LIST";
    private static final String EXTRA_START_POSITION = "EXTRA_START_POSITION";

    private static boolean allowDownload = true;

    static Intent getStartIntent(Context context, long repoId, List<FileV2> screenshots,
                                 int startPosition) {
        Intent intent = new Intent(context, ScreenShotsActivity.class);
        intent.putExtra(EXTRA_REPO_ID, repoId);
        intent.putStringArrayListExtra(EXTRA_SCREENSHOT_LIST, Utils.toString(screenshots));
        intent.putExtra(EXTRA_START_POSITION, startPosition);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        FDroidApp fdroidApp = (FDroidApp) getApplication();
        fdroidApp.setSecureWindow(this);

        fdroidApp.applyPureBlackBackgroundInDarkTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screenshots);

        long repoId = getIntent().getLongExtra(EXTRA_REPO_ID, 1);
        List<String> list = getIntent().getStringArrayListExtra(EXTRA_SCREENSHOT_LIST);
        List<FileV2> screenshots = Utils.fileV2FromStrings(list);
        int startPosition = getIntent().getIntExtra(EXTRA_START_POSITION, 0);

        ViewPager viewPager = findViewById(R.id.screenshot_view_pager);
        ScreenShotPagerAdapter adapter = new ScreenShotPagerAdapter(getSupportFragmentManager(), repoId, screenshots);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(startPosition);

        // display some nice animation while swiping
        viewPager.setPageTransformer(true, new DepthPageTransformer());
    }

    @Override
    protected void onResume() {
        super.onResume();
        allowDownload = Preferences.get().isOnDemandDownloadAllowed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        allowDownload = Preferences.get().isBackgroundDownloadAllowed();
    }

    private static class ScreenShotPagerAdapter extends FragmentStatePagerAdapter {

        private final long repoId;
        private final List<FileV2> screenshots;

        ScreenShotPagerAdapter(FragmentManager fragmentManager, long repoId, List<FileV2> screenshots) {
            super(fragmentManager);
            this.repoId = repoId;
            this.screenshots = screenshots;
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return ScreenShotPageFragment.newInstance(repoId, screenshots.get(position));
        }

        @Override
        public int getCount() {
            return screenshots.size();
        }
    }

    /**
     * A single screenshot page.
     */
    public static class ScreenShotPageFragment extends Fragment {

        private static final String ARG_REPO_ID = "ARG_REPO_ID";
        private static final String ARG_SCREENSHOT_URL = "ARG_SCREENSHOT_URL";

        static ScreenShotPageFragment newInstance(long repoId, @NonNull FileV2 screenshotUrl) {
            ScreenShotPageFragment fragment = new ScreenShotPageFragment();
            Bundle args = new Bundle();
            args.putLong(ARG_REPO_ID, repoId);
            args.putString(ARG_SCREENSHOT_URL, screenshotUrl.serialize());
            fragment.setArguments(args);
            return fragment;
        }

        private long repoId;
        private FileV2 screenshot;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            repoId = requireArguments().getLong(ARG_REPO_ID);
            screenshot = FileV2.deserialize(requireArguments().getString(ARG_SCREENSHOT_URL));
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.activity_screenshots_page, container, false);

            ImageView screenshotView = rootView.findViewById(R.id.screenshot);
            App.loadWithGlide(requireContext(), repoId, screenshot)
                    .onlyRetrieveFromCache(!allowDownload)
                    .error(R.drawable.screenshot_placeholder)
                    .fallback(R.drawable.screenshot_placeholder)
                    .into(screenshotView);
            return rootView;
        }
    }

    public static class DepthPageTransformer implements ViewPager.PageTransformer {

        public void transformPage(@NonNull View view, float position) {
            int pageWidth = view.getWidth();

            if (position <= 0) {
                // use the default slide transition when moving to the left page
                view.setAlpha(1);
                view.setTranslationX(0);

            } else if (position <= 1) {
                // fade the page out.
                view.setAlpha(1 - position);

                // add parallax effect
                view.setTranslationX(pageWidth * -position / 2);

            } else {
                // this page is way off-screen to the right.
                view.setAlpha(0);
            }
        }
    }
}

package org.fdroid.fdroid;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.text.AllCapsTransformationMethod;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.privileged.views.AppDiff;
import org.fdroid.fdroid.privileged.views.AppSecurityPermissions;
import org.fdroid.fdroid.views.ApkListAdapter;
import org.fdroid.fdroid.views.LinearLayoutManagerSnapHelper;
import org.fdroid.fdroid.views.ScreenShotsRecyclerViewAdapter;

import java.util.ArrayList;

import static android.support.v7.widget.RecyclerView.NO_POSITION;

public class AppDetails2 extends AppCompatActivity {

    private static final String TAG = "AppDetails2";

    private App mApp;
    private RecyclerView mRecyclerView;
    private AppDetailsRecyclerViewAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_details2);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(""); // Nice and clean toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        App app = null;
        String packageName = getPackageNameFromIntent(getIntent());
        if (!TextUtils.isEmpty(packageName)) {
            app = AppProvider.Helper.findHighestPriorityMetadata(getContentResolver(), packageName);
        }
        setApp(app); // Will call finish if empty or unknown

        mRecyclerView = (RecyclerView) findViewById(R.id.rvDetails);
        mAdapter = new AppDetailsRecyclerViewAdapter(this);
        LinearLayoutManager lm = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        lm.setStackFromEnd(false);
        mRecyclerView.setLayoutManager(lm);
        mRecyclerView.setAdapter(mAdapter);
    }

    private String getPackageNameFromIntent(Intent intent) {
        if (!intent.hasExtra(AppDetails.EXTRA_APPID)) {
            Log.e(TAG, "No package name found in the intent!");
            return null;
        }
        return intent.getStringExtra(AppDetails.EXTRA_APPID);
    }

    /**
     * If passed null, this will show a message to the user ("Could not find app ..." or something
     * like that) and then finish the activity.
     */
    private void setApp(App newApp) {
        if (newApp == null) {
            Toast.makeText(this, R.string.no_such_app, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        mApp = newApp;
    }

    public class AppDetailsRecyclerViewAdapter
            extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private final int VIEWTYPE_HEADER = 0;
        private final int VIEWTYPE_SCREENSHOTS = 1;
        private final int VIEWTYPE_WHATS_NEW = 2;
        private final int VIEWTYPE_LINKS = 3;
        private final int VIEWTYPE_PERMISSIONS = 4;
        private final int VIEWTYPE_VERSIONS = 5;

        private final Context mContext;
        private ArrayList<Integer> mItems;
        private final ApkListAdapter mApkListAdapter;

        public AppDetailsRecyclerViewAdapter(Context context) {
            mContext = context;
            mApkListAdapter = new ApkListAdapter(mContext, mApp);
            updateItems();
        }

        private void updateItems() {
            if (mItems == null)
                mItems = new ArrayList<>();
            else
                mItems.clear();
            mItems.add(VIEWTYPE_HEADER);
            mItems.add(VIEWTYPE_SCREENSHOTS);
            mItems.add(VIEWTYPE_WHATS_NEW);
            mItems.add(VIEWTYPE_LINKS);
            if (shouldShowPermissions())
                mItems.add(VIEWTYPE_PERMISSIONS);
            mItems.add(VIEWTYPE_VERSIONS);
        }

        private boolean shouldShowPermissions() {
            // Figure out if we should show permissions section
            Apk curApk = null;
            for (int i = 0; i < mApkListAdapter.getCount(); i++) {
                final Apk apk = mApkListAdapter.getItem(i);
                if (apk.versionCode == mApp.suggestedVersionCode) {
                    curApk = apk;
                    break;
                }
            }
            final boolean curApkCompatible = curApk != null && curApk.compatible;
            if (!mApkListAdapter.isEmpty() && (curApkCompatible || Preferences.get().showIncompatibleVersions())) {
               return true;
            }
            return false;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == VIEWTYPE_HEADER) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.app_details2_header, parent, false);
                return new HeaderViewHolder(view);
            } else if (viewType == VIEWTYPE_SCREENSHOTS) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.app_details2_screenshots, parent, false);
                return new ScreenShotsViewHolder(view);
            } else if (viewType == VIEWTYPE_WHATS_NEW) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.app_details2_whatsnew, parent, false);
                return new WhatsNewViewHolder(view);
            } else if (viewType == VIEWTYPE_LINKS) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.app_details2_links, parent, false);
                return new ExpandableLinearLayoutViewHolder(view);
            } else if (viewType == VIEWTYPE_PERMISSIONS) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.app_details2_links, parent, false);
                return new ExpandableLinearLayoutViewHolder(view);
            } else if (viewType == VIEWTYPE_VERSIONS) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.app_details2_versions, parent, false);
                return new VersionsViewHolder(view);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
            int viewType = mItems.get(position);
            if (viewType == VIEWTYPE_HEADER) {
                final HeaderViewHolder vh = (HeaderViewHolder) holder;
                ImageLoader.getInstance().displayImage(mApp.iconUrlLarge, vh.iconView, vh.displayImageOptions);
                vh.titleView.setText(mApp.name);
                if (!TextUtils.isEmpty(mApp.author)) {
                    vh.authorView.setText(getString(R.string.by_author) + " " + mApp.author);
                    vh.authorView.setVisibility(View.VISIBLE);
                } else {
                    vh.authorView.setVisibility(View.GONE);
                }
                vh.summaryView.setText(mApp.summary);
                final Spanned desc = Html.fromHtml(mApp.description, null, new Utils.HtmlTagHandler());
                vh.descriptionView.setMovementMethod(LinkMovementMethod.getInstance());
                vh.descriptionView.setText(trimNewlines(desc));
                if (vh.descriptionView.getText() instanceof Spannable) {
                Spannable spannable = (Spannable) vh.descriptionView.getText();
                    URLSpan[] spans = spannable.getSpans(0, spannable.length(), URLSpan.class);
                    for (URLSpan span : spans) {
                        int start = spannable.getSpanStart(span);
                        int end = spannable.getSpanEnd(span);
                        int flags = spannable.getSpanFlags(span);
                        spannable.removeSpan(span);
                        // Create out own safe link span
                        SafeURLSpan safeUrlSpan = new SafeURLSpan(span.getURL());
                        spannable.setSpan(safeUrlSpan, start, end, flags);
                    }
                }
                vh.descriptionView.post(new Runnable() {
                    @Override
                    public void run() {
                        if (vh.descriptionView.getLineCount() < HeaderViewHolder.MAX_LINES) {
                            vh.descriptionMoreView.setVisibility(View.GONE);
                        } else {
                            vh.descriptionMoreView.setVisibility(View.VISIBLE);
                        }
                    }
                });
                vh.buttonSecondaryView.setText(R.string.menu_uninstall);
                vh.buttonSecondaryView.setVisibility(mApp.isInstalled() ? View.VISIBLE : View.INVISIBLE);
                vh.buttonPrimaryView.setText(R.string.menu_install);
                vh.buttonPrimaryView.setVisibility(View.VISIBLE);

/*                if (appDetails.activeDownloadUrlString != null) {
                    btMain.setText(R.string.downloading);
                    btMain.setEnabled(false);
                } else if (!app.isInstalled() && app.suggestedVersionCode > 0 &&
                        appDetails.adapter.getCount() > 0) {
                    // Check count > 0 due to incompatible apps resulting in an empty list.
                    // If App isn't installed
                    installed = false;
                    statusView.setText(R.string.details_notinstalled);
                    NfcHelper.disableAndroidBeam(appDetails);
                    // Set Install button and hide second button
                    btMain.setText(R.string.menu_install);
                    btMain.setOnClickListener(mOnClickListener);
                    btMain.setEnabled(true);
                } else if (app.isInstalled()) {
                    // If App is installed
                    installed = true;
                    statusView.setText(getString(R.string.details_installed, app.installedVersionName));
                    NfcHelper.setAndroidBeam(appDetails, app.packageName);
                    if (app.canAndWantToUpdate(appDetails)) {
                        updateWanted = true;
                        btMain.setText(R.string.menu_upgrade);
                    } else {
                        updateWanted = false;
                        if (appDetails.packageManager.getLaunchIntentForPackage(app.packageName) != null) {
                            btMain.setText(R.string.menu_launch);
                        } else {
                            btMain.setText(R.string.menu_uninstall);
                        }
                    }
                    btMain.setOnClickListener(mOnClickListener);
                    btMain.setEnabled(true);
                }

                TextView currentVersion = (TextView) view.findViewById(R.id.current_version);
                if (!appDetails.getApks().isEmpty()) {
                    currentVersion.setText(appDetails.getApks().getItem(0).versionName + " (" + app.license + ")");
                } else {
                    currentVersion.setVisibility(View.GONE);
                    btMain.setVisibility(View.GONE);
                }*/

            } else if (viewType == VIEWTYPE_SCREENSHOTS) {
                ScreenShotsViewHolder vh = (ScreenShotsViewHolder) holder;
                LinearLayoutManager lm = new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false);
                vh.recyclerView.setLayoutManager(lm);
                ScreenShotsRecyclerViewAdapter adapter = new ScreenShotsRecyclerViewAdapter(vh.itemView.getContext(), mApp);
                vh.recyclerView.setAdapter(adapter);
                vh.recyclerView.setHasFixedSize(true);
                vh.recyclerView.setNestedScrollingEnabled(false);
                LinearLayoutManagerSnapHelper helper = new LinearLayoutManagerSnapHelper(lm);
                helper.setLinearSnapHelperListener(adapter);
                helper.attachToRecyclerView(vh.recyclerView);
            } else if (viewType == VIEWTYPE_WHATS_NEW) {
                WhatsNewViewHolder vh = (WhatsNewViewHolder) holder;
                vh.textView.setText("WHATS NEW GOES HERE");
            } else if (viewType == VIEWTYPE_LINKS) {
                final ExpandableLinearLayoutViewHolder vh = (ExpandableLinearLayoutViewHolder) holder;
                vh.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean shouldBeVisible = (vh.contentView.getVisibility() != View.VISIBLE);
                        vh.contentView.setVisibility(shouldBeVisible ? View.VISIBLE : View.GONE);
                        TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(vh.headerView, R.drawable.ic_website, 0, shouldBeVisible ? R.drawable.ic_expand_less_grey600 : R.drawable.ic_expand_more_grey600, 0);
                    }
                });
                vh.headerView.setText(R.string.links);
                TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(vh.headerView, R.drawable.ic_website, 0, R.drawable.ic_expand_more_grey600, 0);
                vh.contentView.removeAllViews();

                // Source button
                if (!TextUtils.isEmpty(mApp.sourceURL)) {
                    addLinkItemView(vh.contentView, R.string.menu_source, R.drawable.ic_source_code, mApp.sourceURL);
                }

                // Issues button
                if (!TextUtils.isEmpty(mApp.trackerURL)) {
                    addLinkItemView(vh.contentView, R.string.menu_issues, R.drawable.ic_issues, mApp.trackerURL);
                }

                // Changelog button
                if (!TextUtils.isEmpty(mApp.changelogURL)) {
                    addLinkItemView(vh.contentView, R.string.menu_changelog, R.drawable.ic_changelog, mApp.changelogURL);
                }

                // Website button
                if (!TextUtils.isEmpty(mApp.webURL)) {
                    addLinkItemView(vh.contentView, R.string.menu_website, R.drawable.ic_website, mApp.webURL);
                }

                // Email button
                if (!TextUtils.isEmpty(mApp.email)) {
                    final String subject = Uri.encode(getString(R.string.app_details_subject, mApp.name));
                    String url = "mailto:" + mApp.email + "?subject=" + subject;
                    addLinkItemView(vh.contentView, R.string.menu_email, R.drawable.ic_email, url);
                }

                // Donate button
                if (!TextUtils.isEmpty(mApp.donateURL)) {
                    addLinkItemView(vh.contentView, R.string.menu_donate, R.drawable.ic_donate, mApp.donateURL);
                }

                // Bitcoin
                if (!TextUtils.isEmpty(mApp.bitcoinAddr)) {
                    addLinkItemView(vh.contentView, R.string.menu_bitcoin, R.drawable.ic_bitcoin, "bitcoin:" + mApp.bitcoinAddr);
                }

                // Litecoin
                if (!TextUtils.isEmpty(mApp.litecoinAddr)) {
                    addLinkItemView(vh.contentView, R.string.menu_litecoin, R.drawable.ic_litecoin, "litecoin:" + mApp.litecoinAddr);
                }

                // Flattr
                if (!TextUtils.isEmpty(mApp.flattrID)) {
                    addLinkItemView(vh.contentView, R.string.menu_flattr, R.drawable.ic_flattr, "https://flattr.com/thing/" + mApp.flattrID);
                }
            } else if (viewType == VIEWTYPE_PERMISSIONS) {
                final ExpandableLinearLayoutViewHolder vh = (ExpandableLinearLayoutViewHolder) holder;
                vh.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean shouldBeVisible = (vh.contentView.getVisibility() != View.VISIBLE);
                        vh.contentView.setVisibility(shouldBeVisible ? View.VISIBLE : View.GONE);
                        TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(vh.headerView, R.drawable.ic_lock_24dp_grey600, 0, shouldBeVisible ? R.drawable.ic_expand_less_grey600 : R.drawable.ic_expand_more_grey600, 0);
                    }
                });
                vh.headerView.setText(R.string.permissions);
                TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(vh.headerView, R.drawable.ic_lock_24dp_grey600, 0, R.drawable.ic_expand_more_grey600, 0);
                vh.contentView.removeAllViews();
                AppDiff appDiff = new AppDiff(getPackageManager(), mApkListAdapter.getItem(0));
                AppSecurityPermissions perms = new AppSecurityPermissions(mContext, appDiff.pkgInfo);
                vh.contentView.addView(perms.getPermissionsView(AppSecurityPermissions.WHICH_ALL));
            } else if (viewType == VIEWTYPE_VERSIONS) {
                final VersionsViewHolder vh = (VersionsViewHolder) holder;
                vh.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean shouldBeVisible = (vh.contentView.getVisibility() != View.VISIBLE);
                        vh.contentView.setVisibility(shouldBeVisible ? View.VISIBLE : View.GONE);
                        TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(vh.headerView, R.drawable.ic_source_code, 0, shouldBeVisible ? R.drawable.ic_expand_less_grey600 : R.drawable.ic_expand_more_grey600, 0);
                    }
                });
                vh.contentView.setAdapter(mApkListAdapter);
            }
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        @Override
        public int getItemViewType(int position) {
            return mItems.get(position);
        }

        public class HeaderViewHolder extends RecyclerView.ViewHolder {
            private static final int MAX_LINES = 5;

            final ImageView iconView;
            final TextView titleView;
            final TextView authorView;
            final TextView summaryView;
            final TextView descriptionView;
            final TextView descriptionMoreView;
            final Button buttonPrimaryView;
            final Button buttonSecondaryView;
            final DisplayImageOptions displayImageOptions;

            HeaderViewHolder(View view) {
                super(view);
                iconView = (ImageView) view.findViewById(R.id.icon);
                titleView = (TextView) view.findViewById(R.id.title);
                authorView = (TextView) view.findViewById(R.id.author);
                summaryView = (TextView) view.findViewById(R.id.summary);
                descriptionView = (TextView) view.findViewById(R.id.description);
                descriptionMoreView = (TextView) view.findViewById(R.id.description_more);
                buttonPrimaryView = (Button) view.findViewById(R.id.primaryButtonView);
                buttonSecondaryView = (Button) view.findViewById(R.id.secondaryButtonView);
                displayImageOptions = new DisplayImageOptions.Builder()
                        .cacheInMemory(true)
                        .cacheOnDisk(true)
                        .imageScaleType(ImageScaleType.NONE)
                        .showImageOnLoading(R.drawable.ic_repo_app_default)
                        .showImageForEmptyUri(R.drawable.ic_repo_app_default)
                        .bitmapConfig(Bitmap.Config.RGB_565)
                        .build();
                descriptionView.setMaxLines(MAX_LINES);
                descriptionView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                descriptionMoreView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Remember current scroll position so that we can restore it
                        LinearLayoutManager lm = (LinearLayoutManager)mRecyclerView.getLayoutManager();
                        int pos = lm.findFirstVisibleItemPosition();
                        int posOffset = 0;
                        if (pos != NO_POSITION) {
                            View firstChild = mRecyclerView.getChildAt(0);
                            posOffset = (firstChild == null) ? 0 : (firstChild.getTop()); // - mRecyclerView.getPaddingTop());
                        }
                        if (TextViewCompat.getMaxLines(descriptionView) != MAX_LINES) {
                            descriptionView.setMaxLines(MAX_LINES);
                            descriptionMoreView.setText(R.string.more);
                        } else {
                            descriptionView.setMaxLines(Integer.MAX_VALUE);
                            descriptionMoreView.setText(R.string.less);
                        }
                        if (pos != NO_POSITION) {
                            // Restore scroll position
                            lm.scrollToPositionWithOffset(pos, posOffset);
                        }
                    }
                });
                // Set ALL caps (in a way compatible with SDK 10)
                AllCapsTransformationMethod allCapsTransformation = new AllCapsTransformationMethod(view.getContext());
                buttonPrimaryView.setTransformationMethod(allCapsTransformation);
                buttonSecondaryView.setTransformationMethod(allCapsTransformation);
                descriptionMoreView.setTransformationMethod(allCapsTransformation);
            }
        }

        public class ScreenShotsViewHolder extends RecyclerView.ViewHolder {
            final RecyclerView recyclerView;

            ScreenShotsViewHolder(View view) {
                super(view);
                recyclerView = (RecyclerView) view.findViewById(R.id.screenshots);
            }
        }

        public class WhatsNewViewHolder extends RecyclerView.ViewHolder {
            final TextView textView;

            WhatsNewViewHolder(View view) {
                super(view);
                textView = (TextView) view.findViewById(R.id.text);
            }
        }

        public class ExpandableLinearLayoutViewHolder extends RecyclerView.ViewHolder {
            final TextView headerView;
            final LinearLayout contentView;

            ExpandableLinearLayoutViewHolder(View view) {
                super(view);
                headerView = (TextView) view.findViewById(R.id.information);
                contentView = (LinearLayout) view.findViewById(R.id.ll_content);
            }
        }

        public class VersionsViewHolder extends RecyclerView.ViewHolder {
            final TextView headerView;
            final ListView contentView;

            VersionsViewHolder(View view) {
                super(view);
                headerView = (TextView) view.findViewById(R.id.information);
                contentView = (ListView) view.findViewById(R.id.lv_content);
            }
        }

        private void addLinkItemView(ViewGroup parent, int resIdText, int resIdDrawable, String url) {
            TextView view = (TextView) LayoutInflater.from(parent.getContext()).inflate(R.layout.app_details2_link_item, parent, false);
            view.setTag(url);
            view.setText(resIdText);
            TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(view, resIdDrawable, 0, 0, 0);
            parent.addView(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onLinkClicked((String) v.getTag());
                }
            });
        }

        private void onLinkClicked(String url) {
            if (!TextUtils.isEmpty(url)) {
                tryOpenUri(url);
            }
        }
    }

    private void tryOpenUri(String s) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(s));
        if (intent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this,
                    getString(R.string.no_handler_app, intent.getDataString()),
                    Toast.LENGTH_LONG).show();
            return;
        }
        startActivity(intent);
    }

    // The HTML formatter adds "\n\n" at the end of every paragraph. This
    // is desired between paragraphs, but not at the end of the whole
    // string as it adds unwanted spacing at the end of the TextView.
    // Remove all trailing newlines.
    // Use this function instead of a trim() as that would require
    // converting to String and thus losing formatting (e.g. bold).
    private static CharSequence trimNewlines(CharSequence s) {
        if (s == null || s.length() < 1) {
            return s;
        }
        int i;
        for (i = s.length() - 1; i >= 0; i--) {
            if (s.charAt(i) != '\n') {
                break;
            }
        }
        if (i == s.length() - 1) {
            return s;
        }
        return s.subSequence(0, i + 1);
    }

    @SuppressLint("ParcelCreator")
    private static final class SafeURLSpan extends URLSpan {
        public SafeURLSpan(String url) {
            super(url);
        }

        @Override
        public void onClick(View widget) {
            try {
                super.onClick(widget);
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(widget.getContext(),
                        widget.getContext().getString(R.string.no_handler_app, getURL()),
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}

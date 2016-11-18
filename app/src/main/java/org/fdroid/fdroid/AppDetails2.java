package org.fdroid.fdroid;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
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
import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.Schema;
import org.fdroid.fdroid.installer.InstallManagerService;
import org.fdroid.fdroid.installer.Installer;
import org.fdroid.fdroid.installer.InstallerFactory;
import org.fdroid.fdroid.installer.InstallerService;
import org.fdroid.fdroid.net.Downloader;
import org.fdroid.fdroid.net.DownloaderService;
import org.fdroid.fdroid.privileged.views.AppDiff;
import org.fdroid.fdroid.privileged.views.AppSecurityPermissions;
import org.fdroid.fdroid.views.ApkListAdapter;
import org.fdroid.fdroid.views.LinearLayoutManagerSnapHelper;
import org.fdroid.fdroid.views.ScreenShotsRecyclerViewAdapter;
import org.fdroid.fdroid.views.ShareChooserDialog;

import java.util.ArrayList;

import static android.support.v7.widget.RecyclerView.NO_POSITION;

public class AppDetails2 extends AppCompatActivity implements ShareChooserDialog.ShareChooserDialogListener {
    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    private static final String TAG = "AppDetails2";

    private static final int REQUEST_ENABLE_BLUETOOTH = 2;
    private static final int REQUEST_PERMISSION_DIALOG = 3;
    private static final int REQUEST_UNINSTALL_DIALOG = 4;

    private FDroidApp mFDroidApp;
    private App mApp;
    private RecyclerView mRecyclerView;
    private AppDetailsRecyclerViewAdapter mAdapter;
    private LocalBroadcastManager mLocalBroadcastManager;
    private String mActiveDownloadUrlString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mFDroidApp = (FDroidApp) getApplication();
        //mFDroidApp.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_details2);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(""); // Nice and clean toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (!reset(getPackageNameFromIntent(getIntent()))) {
            finish();
            return;
        }

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean ret = super.onCreateOptionsMenu(menu);
        if (ret) {
            getMenuInflater().inflate(R.menu.details2, menu);
        }
        return ret;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (mApp == null) {
            return true;
        }
        MenuItem itemIgnoreAll = menu.findItem(R.id.action_ignore_all);
        if (itemIgnoreAll != null) {
            itemIgnoreAll.setChecked(mApp.getPrefs(this).ignoreAllUpdates);
        }
        MenuItem itemIgnoreThis = menu.findItem(R.id.action_ignore_this);
        if (itemIgnoreThis != null) {
            itemIgnoreThis.setVisible(mApp.hasUpdates());
            itemIgnoreThis.setChecked(mApp.getPrefs(this).ignoreThisUpdate >= mApp.suggestedVersionCode);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_share) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, mApp.name);
            shareIntent.putExtra(Intent.EXTRA_TEXT, mApp.name + " (" + mApp.summary + ") - https://f-droid.org/app/" + mApp.packageName);

            boolean showNearbyItem = mApp.isInstalled() && mFDroidApp.bluetoothAdapter != null;
            ShareChooserDialog.createChooser((CoordinatorLayout) findViewById(R.id.rootCoordinator), this, this, shareIntent, showNearbyItem);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onNearby() {
        /*
                 * If Bluetooth has not been enabled/turned on, then
                 * enabling device discoverability will automatically enable Bluetooth
                 */
        Intent discoverBt = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverBt.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 121);
        startActivityForResult(discoverBt, REQUEST_ENABLE_BLUETOOTH);
        // if this is successful, the Bluetooth transfer is started
    }

    @Override
    public void onResolvedShareIntent(Intent shareIntent) {
        startActivity(shareIntent);
    }

    public class AppDetailsRecyclerViewAdapter
            extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private final int VIEWTYPE_HEADER = 0;
        private final int VIEWTYPE_SCREENSHOTS = 1;
        private final int VIEWTYPE_WHATS_NEW = 2;
        private final int VIEWTYPE_DONATE = 3;
        private final int VIEWTYPE_LINKS = 4;
        private final int VIEWTYPE_PERMISSIONS = 5;
        private final int VIEWTYPE_VERSIONS = 6;

        private final Context mContext;
        private ArrayList<Integer> mItems;
        private ApkListAdapter mApkListAdapter;

        public AppDetailsRecyclerViewAdapter(Context context) {
            mContext = context;
            updateItems();
        }

        public void updateItems() {
            mApkListAdapter = new ApkListAdapter(mContext, mApp);
            if (mItems == null)
                mItems = new ArrayList<>();
            else
                mItems.clear();
            addItem(VIEWTYPE_HEADER);
            addItem(VIEWTYPE_SCREENSHOTS);
            addItem(VIEWTYPE_WHATS_NEW);
            addItem(VIEWTYPE_DONATE);
            addItem(VIEWTYPE_LINKS);
            addItem(VIEWTYPE_PERMISSIONS);
            addItem(VIEWTYPE_VERSIONS);
        }

        private void addItem(int item) {
            // Gives us a chance to hide sections that are not used, e.g. the donate section when
            // we have no donation links.
            if (item == VIEWTYPE_DONATE) {
                if (!shouldShowDonate())
                    return;
            } else if (item == VIEWTYPE_PERMISSIONS) {
                if (!shouldShowPermissions())
                    return;
            }
            mItems.add(item);
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

        private boolean shouldShowDonate() {
            return uriIsSetAndCanBeOpened(mApp.donateURL) ||
                    uriIsSetAndCanBeOpened(mApp.bitcoinAddr) ||
                    uriIsSetAndCanBeOpened(mApp.litecoinAddr) ||
                    uriIsSetAndCanBeOpened(mApp.flattrID);
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
            } else if (viewType == VIEWTYPE_DONATE) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.app_details2_donate, parent, false);
                return new DonateViewHolder(view);
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
                        .inflate(R.layout.app_details2_links, parent, false);
                return new ExpandableLinearLayoutViewHolder(view);
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
                vh.buttonSecondaryView.setVisibility(isAppInstalled() ? View.VISIBLE : View.INVISIBLE);
                vh.buttonSecondaryView.setOnClickListener(mOnUnInstallClickListener);
                vh.buttonPrimaryView.setText(R.string.menu_install);
                vh.buttonPrimaryView.setVisibility(mApkListAdapter.getCount() > 0 ? View.VISIBLE : View.GONE);
                if (mActiveDownloadUrlString != null) {
                    vh.buttonPrimaryView.setText(R.string.downloading);
                    vh.buttonPrimaryView.setEnabled(false);
                } else if (!isAppInstalled() && mApp.suggestedVersionCode > 0 &&
                        mApkListAdapter.getCount() > 0) {
                    // Check count > 0 due to incompatible apps resulting in an empty list.
                    // If App isn't installed
                    //installed = false;
                    //statusView.setText(R.string.details_notinstalled);
                    NfcHelper.disableAndroidBeam(AppDetails2.this);
                    // Set Install button and hide second button
                    vh.buttonPrimaryView.setText(R.string.menu_install);
                    vh.buttonPrimaryView.setOnClickListener(mOnInstallClickListener);
                    vh.buttonPrimaryView.setEnabled(true);
                } else if (isAppInstalled()) {
                    // If App is installed
                    //installed = true;
                    //statusView.setText(getString(R.string.details_installed, app.installedVersionName));
                    NfcHelper.setAndroidBeam(AppDetails2.this, mApp.packageName);
                    if (mApp.canAndWantToUpdate(AppDetails2.this)) {
                        //updateWanted = true;
                        vh.buttonPrimaryView.setText(R.string.menu_upgrade);
                        vh.buttonPrimaryView.setOnClickListener(mOnUpgradeClickListener);
                    } else {
                        //updateWanted = false;
                        if (getPackageManager().getLaunchIntentForPackage(mApp.packageName) != null) {
                            vh.buttonPrimaryView.setText(R.string.menu_launch);
                            vh.buttonPrimaryView.setOnClickListener(mOnLaunchClickListener);
                        } else {
                            vh.buttonPrimaryView.setVisibility(View.GONE);
                            //vh.buttonPrimaryView.setText(R.string.menu_uninstall);
                        }
                    }
                    vh.buttonPrimaryView.setEnabled(true);
                }

                /*TextView currentVersion = (TextView) view.findViewById(R.id.current_version);
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
                if (vh.snapHelper != null)
                    vh.snapHelper.attachToRecyclerView(null);
                vh.snapHelper = new LinearLayoutManagerSnapHelper(lm);
                vh.snapHelper.setLinearSnapHelperListener(adapter);
                vh.snapHelper.attachToRecyclerView(vh.recyclerView);
            } else if (viewType == VIEWTYPE_WHATS_NEW) {
                WhatsNewViewHolder vh = (WhatsNewViewHolder) holder;
                vh.textView.setText("WHATS NEW GOES HERE");
            } else if (viewType == VIEWTYPE_DONATE) {
                DonateViewHolder vh = (DonateViewHolder) holder;
                vh.contentView.removeAllViews();

                // Donate button
                if (uriIsSetAndCanBeOpened(mApp.donateURL)) {
                    addLinkItemView(vh.contentView, R.string.menu_donate, R.drawable.ic_donate, mApp.donateURL);
                }

                // Bitcoin
                if (uriIsSetAndCanBeOpened(mApp.bitcoinAddr)) {
                    addLinkItemView(vh.contentView, R.string.menu_bitcoin, R.drawable.ic_bitcoin, "bitcoin:" + mApp.bitcoinAddr);
                }

                // Litecoin
                if (uriIsSetAndCanBeOpened(mApp.litecoinAddr)) {
                    addLinkItemView(vh.contentView, R.string.menu_litecoin, R.drawable.ic_litecoin, "litecoin:" + mApp.litecoinAddr);
                }

                // Flattr
                if (uriIsSetAndCanBeOpened(mApp.flattrID)) {
                    addLinkItemView(vh.contentView, R.string.menu_flattr, R.drawable.ic_flattr, "https://flattr.com/thing/" + mApp.flattrID);
                }
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
                if (uriIsSetAndCanBeOpened(mApp.sourceURL)) {
                    addLinkItemView(vh.contentView, R.string.menu_source, R.drawable.ic_source_code, mApp.sourceURL);
                }

                // Issues button
                if (uriIsSetAndCanBeOpened(mApp.trackerURL)) {
                    addLinkItemView(vh.contentView, R.string.menu_issues, R.drawable.ic_issues, mApp.trackerURL);
                }

                // Changelog button
                if (uriIsSetAndCanBeOpened(mApp.changelogURL)) {
                    addLinkItemView(vh.contentView, R.string.menu_changelog, R.drawable.ic_changelog, mApp.changelogURL);
                }

                // Website button
                if (uriIsSetAndCanBeOpened(mApp.webURL)) {
                    addLinkItemView(vh.contentView, R.string.menu_website, R.drawable.ic_website, mApp.webURL);
                }

                // Email button
                if (uriIsSetAndCanBeOpened(mApp.email)) {
                    final String subject = Uri.encode(getString(R.string.app_details_subject, mApp.name));
                    String url = "mailto:" + mApp.email + "?subject=" + subject;
                    addLinkItemView(vh.contentView, R.string.menu_email, R.drawable.ic_email, url);
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
                final ExpandableLinearLayoutViewHolder vh = (ExpandableLinearLayoutViewHolder) holder;
                vh.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean shouldBeVisible = (vh.contentView.getVisibility() != View.VISIBLE);
                        vh.contentView.setVisibility(shouldBeVisible ? View.VISIBLE : View.GONE);
                        TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(vh.headerView, R.drawable.ic_source_code, 0, shouldBeVisible ? R.drawable.ic_expand_less_grey600 : R.drawable.ic_expand_more_grey600, 0);
                        vh.itemView.requestLayout();
                    }
                });
                vh.contentView.removeAllViews();
                for (int i = 0; i < mApkListAdapter.getCount(); i++) {
                    View view = mApkListAdapter.getView(i, null, vh.contentView);
                    vh.contentView.addView(view, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                }
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
            LinearLayoutManagerSnapHelper snapHelper;

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

        public class DonateViewHolder extends RecyclerView.ViewHolder {
            final TextView textView;
            final LinearLayout contentView;

            DonateViewHolder(View view) {
                super(view);
                textView = (TextView) view.findViewById(R.id.text);
                contentView = (LinearLayout) view.findViewById(R.id.ll_information);
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

        private View.OnClickListener mOnInstallClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Apk apkToInstall = ApkProvider.Helper.findApkFromAnyRepo(AppDetails2.this, mApp.packageName, mApp.suggestedVersionCode);

                // If not installed, install
                //btMain.setEnabled(false);
                //btMain.setText(R.string.system_install_installing);

                installApk(apkToInstall);
            }
        };

        private View.OnClickListener mOnUnInstallClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uninstallApk();
            }
        };

        private View.OnClickListener mOnUpgradeClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Apk apkToInstall = ApkProvider.Helper.findApkFromAnyRepo(AppDetails2.this, mApp.packageName, mApp.suggestedVersionCode);
                installApk(apkToInstall);
            }
        };

        private View.OnClickListener mOnLaunchClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchApk(mApp.packageName);
            }
        };
    }

    private boolean uriIsSetAndCanBeOpened(String s) {
        if (TextUtils.isEmpty(s))
            return false;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(s));
        return (intent.resolveActivity(getPackageManager()) != null);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BLUETOOTH:
                mFDroidApp.sendViaBluetooth(this, resultCode, mApp.packageName);
                break;
            case REQUEST_PERMISSION_DIALOG:
                if (resultCode == Activity.RESULT_OK) {
                    Uri uri = data.getData();
                    Apk apk = ApkProvider.Helper.findByUri(this, uri, Schema.ApkTable.Cols.ALL);
                    startInstall(apk);
                }
                break;
            case REQUEST_UNINSTALL_DIALOG:
                if (resultCode == Activity.RESULT_OK) {
                    startUninstall();
                }
                break;
        }
    }

    // Install the version of this app denoted by 'app.curApk'.
    private void installApk(final Apk apk) {
        if (isFinishing()) {
            return;
        }

        if (!apk.compatible) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.installIncompatible);
            builder.setPositiveButton(R.string.yes,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog,
                                            int whichButton) {
                            initiateInstall(apk);
                        }
                    });
            builder.setNegativeButton(R.string.no,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog,
                                            int whichButton) {
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
            return;
        }
        if (mApp.installedSig != null && apk.sig != null
                && !apk.sig.equals(mApp.installedSig)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.SignatureMismatch).setPositiveButton(
                    R.string.ok,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
            return;
        }
        initiateInstall(apk);
    }

    private void initiateInstall(Apk apk) {
        Installer installer = InstallerFactory.create(this, apk);
        Intent intent = installer.getPermissionScreen();
        if (intent != null) {
            // permission screen required
            Utils.debugLog(TAG, "permission screen required");
            startActivityForResult(intent, REQUEST_PERMISSION_DIALOG);
            return;
        }

        startInstall(apk);
    }

    private void startInstall(Apk apk) {
        mActiveDownloadUrlString = apk.getUrl();
        registerDownloaderReceiver();
        InstallManagerService.queue(this, mApp, apk);
    }

    /**
     * Queue for uninstall based on the instance variable {@link #app}
     */
    private void uninstallApk() {
        Apk apk = mApp.installedApk;
        if (apk == null) {
            // TODO ideally, app would be refreshed immediately after install, then this
            // workaround would be unnecessary
            try {
                PackageInfo pi = getPackageManager().getPackageInfo(mApp.packageName, 0);
                apk = ApkProvider.Helper.findApkFromAnyRepo(this, pi.packageName, pi.versionCode);
                mApp.installedApk = apk;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                return; // not installed
            }
        }
        Installer installer = InstallerFactory.create(this, apk);
        Intent intent = installer.getUninstallScreen();
        if (intent != null) {
            // uninstall screen required
            Utils.debugLog(TAG, "screen screen required");
            startActivityForResult(intent, REQUEST_UNINSTALL_DIALOG);
            return;
        }

        startUninstall();
    }

    private void startUninstall() {
        registerUninstallReceiver();
        InstallerService.uninstall(this, mApp.installedApk);
    }

    private void launchApk(String packageName) {
        Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
        startActivity(intent);
    }

    private void registerUninstallReceiver() {
        mLocalBroadcastManager.registerReceiver(uninstallReceiver,
                Installer.getUninstallIntentFilter(mApp.packageName));
    }

    private void unregisterUninstallReceiver() {
        mLocalBroadcastManager.unregisterReceiver(uninstallReceiver);
    }

    private void registerDownloaderReceiver() {
        if (mActiveDownloadUrlString != null) { // if a download is active
            String url = mActiveDownloadUrlString;
            mLocalBroadcastManager.registerReceiver(downloadReceiver,
                    DownloaderService.getIntentFilter(url));
        }
    }

    private void unregisterDownloaderReceiver() {
        mLocalBroadcastManager.unregisterReceiver(downloadReceiver);
    }

    private void unregisterInstallReceiver() {
        mLocalBroadcastManager.unregisterReceiver(installReceiver);
    }

    private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Downloader.ACTION_STARTED:
                    //if (headerFragment != null) {
                    //    headerFragment.startProgress();
                    //}
                    break;
                case Downloader.ACTION_PROGRESS:
                    //if (headerFragment != null) {
                    //    headerFragment.updateProgress(intent.getIntExtra(Downloader.EXTRA_BYTES_READ, -1),
                    //            intent.getIntExtra(Downloader.EXTRA_TOTAL_BYTES, -1));
                    //}
                    break;
                case Downloader.ACTION_COMPLETE:
                    // Starts the install process one the download is complete.
                    cleanUpFinishedDownload();
                    mLocalBroadcastManager.registerReceiver(installReceiver,
                            Installer.getInstallIntentFilter(intent.getData()));
                    break;
                case Downloader.ACTION_INTERRUPTED:
                    if (intent.hasExtra(Downloader.EXTRA_ERROR_MESSAGE)) {
                        String msg = intent.getStringExtra(Downloader.EXTRA_ERROR_MESSAGE)
                                + " " + intent.getDataString();
                        Toast.makeText(context, R.string.download_error, Toast.LENGTH_SHORT).show();
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                    } else { // user canceled
                        Toast.makeText(context, R.string.details_notinstalled, Toast.LENGTH_LONG).show();
                    }
                    cleanUpFinishedDownload();
                    break;
                default:
                    throw new RuntimeException("intent action not handled!");
            }
        }
    };

    private final BroadcastReceiver installReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Installer.ACTION_INSTALL_STARTED:
                    //headerFragment.startProgress(false);
                    //headerFragment.showIndeterminateProgress(getString(R.string.installing));
                    break;
                case Installer.ACTION_INSTALL_COMPLETE:
                    //headerFragment.removeProgress();
                    unregisterInstallReceiver();
                    onAppChanged();
                    break;
                case Installer.ACTION_INSTALL_INTERRUPTED:
                    //headerFragment.removeProgress();
                    onAppChanged();

                    String errorMessage =
                            intent.getStringExtra(Installer.EXTRA_ERROR_MESSAGE);

                    if (!TextUtils.isEmpty(errorMessage)) {
                        Log.e(TAG, "install aborted with errorMessage: " + errorMessage);

                        String title = String.format(
                                getString(R.string.install_error_notify_title),
                                mApp.name);

                        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(AppDetails2.this);
                        alertBuilder.setTitle(title);
                        alertBuilder.setMessage(errorMessage);
                        alertBuilder.setNeutralButton(android.R.string.ok, null);
                        alertBuilder.create().show();
                    }
                    unregisterInstallReceiver();
                    break;
                case Installer.ACTION_INSTALL_USER_INTERACTION:
                    PendingIntent installPendingIntent =
                            intent.getParcelableExtra(Installer.EXTRA_USER_INTERACTION_PI);

                    try {
                        installPendingIntent.send();
                    } catch (PendingIntent.CanceledException e) {
                        Log.e(TAG, "PI canceled", e);
                    }

                    break;
                default:
                    throw new RuntimeException("intent action not handled!");
            }
        }
    };

    private final BroadcastReceiver uninstallReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Installer.ACTION_UNINSTALL_STARTED:
                    //headerFragment.startProgress(false);
                    //headerFragment.showIndeterminateProgress(getString(R.string.uninstalling));
                    break;
                case Installer.ACTION_UNINSTALL_COMPLETE:
                    //headerFragment.removeProgress();
                    onAppChanged();
                    unregisterUninstallReceiver();
                    break;
                case Installer.ACTION_UNINSTALL_INTERRUPTED:
                    //headerFragment.removeProgress();

                    String errorMessage =
                            intent.getStringExtra(Installer.EXTRA_ERROR_MESSAGE);

                    if (!TextUtils.isEmpty(errorMessage)) {
                        Log.e(TAG, "uninstall aborted with errorMessage: " + errorMessage);

                        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(AppDetails2.this);
                        alertBuilder.setTitle(R.string.uninstall_error_notify_title);
                        alertBuilder.setMessage(errorMessage);
                        alertBuilder.setNeutralButton(android.R.string.ok, null);
                        alertBuilder.create().show();
                    }
                    unregisterUninstallReceiver();
                    break;
                case Installer.ACTION_UNINSTALL_USER_INTERACTION:
                    PendingIntent uninstallPendingIntent =
                            intent.getParcelableExtra(Installer.EXTRA_USER_INTERACTION_PI);

                    try {
                        uninstallPendingIntent.send();
                    } catch (PendingIntent.CanceledException e) {
                        Log.e(TAG, "PI canceled", e);
                    }

                    break;
                default:
                    throw new RuntimeException("intent action not handled!");
            }
        }
    };

    // Reset the display and list contents. Used when entering the activity, and
    // also when something has been installed/uninstalled.
    // Return true if the app was found, false otherwise.
    private boolean reset(String packageName) {

        Utils.debugLog(TAG, "Getting application details for " + packageName);
        App newApp = null;

        calcActiveDownloadUrlString(packageName);

        if (!TextUtils.isEmpty(packageName)) {
            newApp = AppProvider.Helper.findHighestPriorityMetadata(getContentResolver(), packageName);
        }

        setApp(newApp);
        return this.mApp != null;
    }

    private void calcActiveDownloadUrlString(String packageName) {
        String urlString = getPreferences(MODE_PRIVATE).getString(packageName, null);
        if (DownloaderService.isQueuedOrActive(urlString)) {
            mActiveDownloadUrlString = urlString;
        } else {
            // this URL is no longer active, remove it
            getPreferences(MODE_PRIVATE).edit().remove(packageName).apply();
        }
    }

    /**
     * Remove progress listener, suppress progress bar, set downloadHandler to null.
     */
    private void cleanUpFinishedDownload() {
        mActiveDownloadUrlString = null;
        //if (headerFragment != null) {
        //    headerFragment.removeProgress();
        //}
        unregisterDownloaderReceiver();
    }

    private void onAppChanged() {
        mRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                if (!reset(mApp.packageName)) {
                    AppDetails2.this.finish();
                    return;
                }
                AppDetailsRecyclerViewAdapter adapter = (AppDetailsRecyclerViewAdapter)mRecyclerView.getAdapter();
                adapter.updateItems();
                adapter.notifyDataSetChanged();
                supportInvalidateOptionsMenu();
            }
        });
    }

    private boolean isAppInstalled() {
        return mApp.isInstalled();
    }
}

package org.fdroid.fdroid.views;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.text.AllCapsTransformationMethod;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.InstalledAppProvider;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.privileged.views.AppDiff;
import org.fdroid.fdroid.privileged.views.AppSecurityPermissions;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import static android.support.v7.widget.RecyclerView.NO_POSITION;

public class AppDetailsRecyclerViewAdapter
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface AppDetailsRecyclerViewAdapterCallbacks {
        boolean isAppDownloading();
        boolean isAppInstalled();
        void enableAndroidBeam();
        void disableAndroidBeam();
        void openUrl(String url);
        void installApk();
        void installApk(Apk apk);
        void upgradeApk();
        void uninstallApk();
        void installCancel();
        void launchApk();
    }

    private static final int VIEWTYPE_HEADER = 0;
    private static final int VIEWTYPE_SCREENSHOTS = 1;
    private static final int VIEWTYPE_WHATS_NEW = 2;
    private static final int VIEWTYPE_DONATE = 3;
    private static final int VIEWTYPE_LINKS = 4;
    private static final int VIEWTYPE_PERMISSIONS = 5;
    private static final int VIEWTYPE_VERSIONS = 6;
    private static final int VIEWTYPE_VERSION = 7;

    private final Context mContext;
    @NonNull
    private App mApp;
    private final AppDetailsRecyclerViewAdapterCallbacks mCallbacks;
    private RecyclerView mRecyclerView;
    private ArrayList<Object> mItems;
    private ArrayList<Apk> mVersions;
    private boolean mShowVersions;

    private HeaderViewHolder mHeaderView;

    public AppDetailsRecyclerViewAdapter(Context context, @NonNull App app, AppDetailsRecyclerViewAdapterCallbacks callbacks) {
        mContext = context;
        mCallbacks = callbacks;
        mApp = app;
        updateItems(app);
    }

    public void updateItems(@NonNull App app) {
        mApp = app;

        // Get versions
        mVersions = new ArrayList<>();
        final List<Apk> apks = ApkProvider.Helper.findByPackageName(mContext, mApp.packageName);
        for (final Apk apk : apks) {
            if (apk.compatible || Preferences.get().showIncompatibleVersions()) {
                mVersions.add(apk);
            }
        }

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

    private void setShowVersions(boolean showVersions) {
        mShowVersions = showVersions;
        mItems.removeAll(mVersions);
        if (showVersions) {
            mItems.addAll(mItems.indexOf(VIEWTYPE_VERSIONS) + 1, mVersions);
        }
        notifyDataSetChanged();
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
        for (int i = 0; i < mVersions.size(); i++) {
            final Apk apk = mVersions.get(i);
            if (apk.versionCode == mApp.suggestedVersionCode) {
                curApk = apk;
                break;
            }
        }
        final boolean curApkCompatible = curApk != null && curApk.compatible;
        return mVersions.size() > 0 && (curApkCompatible || Preferences.get().showIncompatibleVersions());
    }

    private boolean shouldShowDonate() {
        return uriIsSetAndCanBeOpened(mApp.donateURL) ||
                uriIsSetAndCanBeOpened(mApp.getBitcoinUri()) ||
                uriIsSetAndCanBeOpened(mApp.getLitecoinUri()) ||
                uriIsSetAndCanBeOpened(mApp.getFlattrUri());
    }

    public void clearProgress() {
        setProgress(0, 0, 0);
    }

    public void setProgress(int bytesDownloaded, int totalBytes, int resIdString) {
        if (mHeaderView != null) {
            mHeaderView.setProgress(bytesDownloaded, totalBytes, resIdString);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEWTYPE_HEADER) {
            View view = inflater.inflate(R.layout.app_details2_header, parent, false);
            return new HeaderViewHolder(view);
        } else if (viewType == VIEWTYPE_SCREENSHOTS) {
            View view = inflater.inflate(R.layout.app_details2_screenshots, parent, false);
            return new ScreenShotsViewHolder(view);
        } else if (viewType == VIEWTYPE_WHATS_NEW) {
            View view = inflater.inflate(R.layout.app_details2_whatsnew, parent, false);
            return new WhatsNewViewHolder(view);
        } else if (viewType == VIEWTYPE_DONATE) {
            View view = inflater.inflate(R.layout.app_details2_donate, parent, false);
            return new DonateViewHolder(view);
        } else if (viewType == VIEWTYPE_LINKS) {
            View view = inflater.inflate(R.layout.app_details2_links, parent, false);
            return new ExpandableLinearLayoutViewHolder(view);
        } else if (viewType == VIEWTYPE_PERMISSIONS) {
            View view = inflater.inflate(R.layout.app_details2_links, parent, false);
            return new ExpandableLinearLayoutViewHolder(view);
        } else if (viewType == VIEWTYPE_VERSIONS) {
            View view = inflater.inflate(R.layout.app_details2_links, parent, false);
            return new ExpandableLinearLayoutViewHolder(view);
        } else if (viewType == VIEWTYPE_VERSION) {
            View view = inflater.inflate(R.layout.apklistitem, parent, false);
            return new VersionViewHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        int viewType = getItemViewType(position);
        if (viewType == VIEWTYPE_HEADER) {
            final HeaderViewHolder vh = (HeaderViewHolder) holder;
            mHeaderView = vh;
            ImageLoader.getInstance().displayImage(mApp.iconUrlLarge, vh.iconView, vh.displayImageOptions);
            vh.titleView.setText(mApp.name);
            if (!TextUtils.isEmpty(mApp.author)) {
                vh.authorView.setText(mContext.getString(R.string.by_author) + " " + mApp.author);
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
            vh.buttonSecondaryView.setVisibility(mCallbacks.isAppInstalled() ? View.VISIBLE : View.INVISIBLE);
            vh.buttonSecondaryView.setOnClickListener(mOnUnInstallClickListener);
            vh.buttonPrimaryView.setText(R.string.menu_install);
            vh.buttonPrimaryView.setVisibility(mVersions.size() > 0 ? View.VISIBLE : View.GONE);
            if (mCallbacks.isAppDownloading()) {
                vh.buttonPrimaryView.setText(R.string.downloading);
                vh.buttonPrimaryView.setEnabled(false);
            } else if (!mCallbacks.isAppInstalled() && mApp.suggestedVersionCode > 0 && mVersions.size() > 0) {
                // Check count > 0 due to incompatible apps resulting in an empty list.
                mCallbacks.disableAndroidBeam();
                // Set Install button and hide second button
                vh.buttonPrimaryView.setText(R.string.menu_install);
                vh.buttonPrimaryView.setOnClickListener(mOnInstallClickListener);
                vh.buttonPrimaryView.setEnabled(true);
            } else if (mCallbacks.isAppInstalled()) {
                mCallbacks.enableAndroidBeam();
                if (mApp.canAndWantToUpdate(mContext)) {
                    vh.buttonPrimaryView.setText(R.string.menu_upgrade);
                    vh.buttonPrimaryView.setOnClickListener(mOnUpgradeClickListener);
                } else {
                    if (mContext.getPackageManager().getLaunchIntentForPackage(mApp.packageName) != null) {
                        vh.buttonPrimaryView.setText(R.string.menu_launch);
                        vh.buttonPrimaryView.setOnClickListener(mOnLaunchClickListener);
                    } else {
                        vh.buttonPrimaryView.setVisibility(View.GONE);
                    }
                }
                vh.buttonPrimaryView.setEnabled(true);
            }
            if (mCallbacks.isAppDownloading()) {
                vh.buttonLayout.setVisibility(View.GONE);
                vh.progressLayout.setVisibility(View.VISIBLE);
            } else {
                vh.buttonLayout.setVisibility(View.VISIBLE);
                vh.progressLayout.setVisibility(View.GONE);
            }
            vh.progressCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mCallbacks.installCancel();
                }
            });

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
            if (uriIsSetAndCanBeOpened(mApp.getBitcoinUri())) {
                addLinkItemView(vh.contentView, R.string.menu_bitcoin, R.drawable.ic_bitcoin, mApp.getBitcoinUri());
            }

            // Litecoin
            if (uriIsSetAndCanBeOpened(mApp.getLitecoinUri())) {
                addLinkItemView(vh.contentView, R.string.menu_litecoin, R.drawable.ic_litecoin, mApp.getLitecoinUri());
            }

            // Flattr
            if (uriIsSetAndCanBeOpened(mApp.getFlattrUri())) {
                addLinkItemView(vh.contentView, R.string.menu_flattr, R.drawable.ic_flattr, mApp.getFlattrUri());
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
            final String subject = Uri.encode(mContext.getString(R.string.app_details_subject, mApp.name));
            String emailUrl = mApp.email == null ? null : ("mailto:" + mApp.email + "?subject=" + subject);
            if (uriIsSetAndCanBeOpened(emailUrl)) {
                addLinkItemView(vh.contentView, R.string.menu_email, R.drawable.ic_email, emailUrl);
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
            AppDiff appDiff = new AppDiff(mContext.getPackageManager(), mVersions.get(0));
            AppSecurityPermissions perms = new AppSecurityPermissions(mContext, appDiff.pkgInfo);
            vh.contentView.addView(perms.getPermissionsView(AppSecurityPermissions.WHICH_ALL));
        } else if (viewType == VIEWTYPE_VERSIONS) {
            final ExpandableLinearLayoutViewHolder vh = (ExpandableLinearLayoutViewHolder) holder;
            vh.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setShowVersions(!mShowVersions);
                }
            });
            vh.headerView.setText(R.string.versions);
            TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(vh.headerView, R.drawable.ic_access_time_24dp_grey600, 0, mShowVersions ? R.drawable.ic_expand_less_grey600 : R.drawable.ic_expand_more_grey600, 0);
        } else if (viewType == VIEWTYPE_VERSION) {
            final VersionViewHolder vh = (VersionViewHolder) holder;
            java.text.DateFormat df = DateFormat.getDateFormat(mContext);
            final Apk apk = (Apk)mItems.get(position);

            vh.version.setText(mContext.getString(R.string.version)
                    + " " + apk.versionName
                    + (apk.versionCode == mApp.suggestedVersionCode ? "  ☆" : ""));

            vh.status.setText(getInstalledStatus(apk));

            vh.repository.setText(mContext.getString(R.string.repo_provider,
                    RepoProvider.Helper.findById(mContext, apk.repo).getName()));

            if (apk.size > 0) {
                vh.size.setText(Utils.getFriendlySize(apk.size));
                vh.size.setVisibility(View.VISIBLE);
            } else {
                vh.size.setVisibility(View.GONE);
            }

            if (!Preferences.get().expertMode()) {
                vh.api.setVisibility(View.GONE);
            } else if (apk.minSdkVersion > 0 && apk.maxSdkVersion < Apk.SDK_VERSION_MAX_VALUE) {
                vh.api.setText(mContext.getString(R.string.minsdk_up_to_maxsdk,
                        Utils.getAndroidVersionName(apk.minSdkVersion),
                        Utils.getAndroidVersionName(apk.maxSdkVersion)));
                vh.api.setVisibility(View.VISIBLE);
            } else if (apk.minSdkVersion > 0) {
                vh.api.setText(mContext.getString(R.string.minsdk_or_later,
                        Utils.getAndroidVersionName(apk.minSdkVersion)));
                vh.api.setVisibility(View.VISIBLE);
            } else if (apk.maxSdkVersion > 0) {
                vh.api.setText(mContext.getString(R.string.up_to_maxsdk,
                        Utils.getAndroidVersionName(apk.maxSdkVersion)));
                vh.api.setVisibility(View.VISIBLE);
            }

            if (apk.srcname != null) {
                vh.buildtype.setText("source");
            } else {
                vh.buildtype.setText("bin");
            }

            if (apk.added != null) {
                vh.added.setText(mContext.getString(R.string.added_on,
                        df.format(apk.added)));
                vh.added.setVisibility(View.VISIBLE);
            } else {
                vh.added.setVisibility(View.GONE);
            }

            if (Preferences.get().expertMode() && apk.nativecode != null) {
                vh.nativecode.setText(TextUtils.join(" ", apk.nativecode));
                vh.nativecode.setVisibility(View.VISIBLE);
            } else {
                vh.nativecode.setVisibility(View.GONE);
            }

            if (apk.incompatibleReasons != null) {
                vh.incompatibleReasons.setText(
                        mContext.getResources().getString(
                                R.string.requires_features,
                                TextUtils.join(", ", apk.incompatibleReasons)));
                vh.incompatibleReasons.setVisibility(View.VISIBLE);
            } else {
                vh.incompatibleReasons.setVisibility(View.GONE);
            }

            // Disable it all if it isn't compatible...
            final View[] views = {
                    vh.itemView,
                    vh.version,
                    vh.status,
                    vh.repository,
                    vh.size,
                    vh.api,
                    vh.buildtype,
                    vh.added,
                    vh.nativecode,
            };
            for (final View v : views) {
                v.setEnabled(apk.compatible);
            }
            vh.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mCallbacks.installApk(apk);
                }
            });
        }
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        if (holder instanceof HeaderViewHolder) {
            mHeaderView = null;
        }
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (mItems.get(position) instanceof Apk)
            return VIEWTYPE_VERSION;
        return (Integer)mItems.get(position);
    }

    public class HeaderViewHolder extends RecyclerView.ViewHolder {
        private static final int MAX_LINES = 5;

        final ImageView iconView;
        final TextView titleView;
        final TextView authorView;
        final TextView summaryView;
        final TextView descriptionView;
        final TextView descriptionMoreView;
        final View buttonLayout;
        final Button buttonPrimaryView;
        final Button buttonSecondaryView;
        final View progressLayout;
        final ProgressBar progressBar;
        final TextView progressLabel;
        final TextView progressPercent;
        final View progressCancel;
        final DisplayImageOptions displayImageOptions;

        HeaderViewHolder(View view) {
            super(view);
            iconView = (ImageView) view.findViewById(R.id.icon);
            titleView = (TextView) view.findViewById(R.id.title);
            authorView = (TextView) view.findViewById(R.id.author);
            summaryView = (TextView) view.findViewById(R.id.summary);
            descriptionView = (TextView) view.findViewById(R.id.description);
            descriptionMoreView = (TextView) view.findViewById(R.id.description_more);
            buttonLayout = view.findViewById(R.id.button_layout);
            buttonPrimaryView = (Button) view.findViewById(R.id.primaryButtonView);
            buttonSecondaryView = (Button) view.findViewById(R.id.secondaryButtonView);
            progressLayout = view.findViewById(R.id.progress_layout);
            progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
            progressLabel = (TextView) view.findViewById(R.id.progress_label);
            progressPercent = (TextView) view.findViewById(R.id.progress_percent);
            progressCancel = view.findViewById(R.id.progress_cancel);
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

        public void setProgress(int bytesDownloaded, int totalBytes, int resIdString) {
            if (bytesDownloaded == 0 && totalBytes == 0) {
                // Remove progress bar
                progressLayout.setVisibility(View.GONE);
                buttonLayout.setVisibility(View.VISIBLE);
            } else {
                progressBar.setMax(totalBytes);
                progressBar.setProgress(bytesDownloaded);
                progressBar.setIndeterminate(totalBytes == -1);
                if (resIdString != 0) {
                    progressLabel.setText(resIdString);
                    progressPercent.setText("");
                } else if (totalBytes > 0 && bytesDownloaded >= 0) {
                    float percent = bytesDownloaded * 100 / totalBytes;
                    progressLabel.setText(Utils.getFriendlySize(bytesDownloaded) + " / " + Utils.getFriendlySize(totalBytes));
                    NumberFormat format = NumberFormat.getPercentInstance();
                    format.setMaximumFractionDigits(0);
                    progressPercent.setText(format.format(percent / 100));
                } else if (bytesDownloaded >= 0) {
                    progressLabel.setText(Utils.getFriendlySize(bytesDownloaded));
                    progressPercent.setText("");
                }

                // Make sure it's visible
                if (progressLayout.getVisibility() != View.VISIBLE) {
                    progressLayout.setVisibility(View.VISIBLE);
                    buttonLayout.setVisibility(View.GONE);
                }
            }
        }
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        mRecyclerView = null;
        super.onDetachedFromRecyclerView(recyclerView);
    }

    private class ScreenShotsViewHolder extends RecyclerView.ViewHolder {
        final RecyclerView recyclerView;
        LinearLayoutManagerSnapHelper snapHelper;

        ScreenShotsViewHolder(View view) {
            super(view);
            recyclerView = (RecyclerView) view.findViewById(R.id.screenshots);
        }
    }

    private class WhatsNewViewHolder extends RecyclerView.ViewHolder {
        final TextView textView;

        WhatsNewViewHolder(View view) {
            super(view);
            textView = (TextView) view.findViewById(R.id.text);
        }
    }

    private class DonateViewHolder extends RecyclerView.ViewHolder {
        final TextView textView;
        final LinearLayout contentView;

        DonateViewHolder(View view) {
            super(view);
            textView = (TextView) view.findViewById(R.id.information);
            contentView = (LinearLayout) view.findViewById(R.id.ll_information);
        }
    }

    private class ExpandableLinearLayoutViewHolder extends RecyclerView.ViewHolder {
        final TextView headerView;
        final LinearLayout contentView;

        ExpandableLinearLayoutViewHolder(View view) {
            super(view);
            headerView = (TextView) view.findViewById(R.id.information);
            contentView = (LinearLayout) view.findViewById(R.id.ll_content);
        }
    }

    private class VersionViewHolder extends RecyclerView.ViewHolder {
        final TextView version;
        final TextView status;
        final TextView repository;
        final TextView size;
        final TextView api;
        final TextView incompatibleReasons;
        final TextView buildtype;
        final TextView added;
        final TextView nativecode;

        VersionViewHolder(View view) {
            super(view);
            version = (TextView) view.findViewById(R.id.version);
            status = (TextView) view.findViewById(R.id.status);
            repository = (TextView) view.findViewById(R.id.repository);
            size = (TextView) view.findViewById(R.id.size);
            api = (TextView) view.findViewById(R.id.api);
            incompatibleReasons = (TextView) view.findViewById(R.id.incompatible_reasons);
            buildtype = (TextView) view.findViewById(R.id.buildtype);
            added = (TextView) view.findViewById(R.id.added);
            nativecode = (TextView) view.findViewById(R.id.nativecode);

            int margin = mContext.getResources().getDimensionPixelSize(R.dimen.layout_horizontal_margin);
            int padding = mContext.getResources().getDimensionPixelSize(R.dimen.details_activity_padding);
            ViewCompat.setPaddingRelative(view, margin + padding + ViewCompat.getPaddingStart(view), view.getPaddingTop(), ViewCompat.getPaddingEnd(view), view.getPaddingBottom());
        }
    }

    private void addLinkItemView(ViewGroup parent, int resIdText, int resIdDrawable, final String url) {
        TextView view = (TextView) LayoutInflater.from(parent.getContext()).inflate(R.layout.app_details2_link_item, parent, false);
        view.setText(resIdText);
        TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(view, resIdDrawable, 0, 0, 0);
        parent.addView(view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onLinkClicked(url);
            }
        });
    }

    private String getInstalledStatus(final Apk apk) {
        // Definitely not installed.
        if (apk.versionCode != mApp.installedVersionCode) {
            return mContext.getString(R.string.app_not_installed);
        }
        // Definitely installed this version.
        if (apk.sig != null && apk.sig.equals(mApp.installedSig)) {
            return mContext.getString(R.string.app_installed);
        }
        // Installed the same version, but from someplace else.
        final String installerPkgName;
        try {
            installerPkgName = mContext.getPackageManager().getInstallerPackageName(mApp.packageName);
        } catch (IllegalArgumentException e) {
            Log.w("AppDetailsAdapter", "Application " + mApp.packageName + " is not installed anymore");
            return mContext.getString(R.string.app_not_installed);
        }
        if (TextUtils.isEmpty(installerPkgName)) {
            return mContext.getString(R.string.app_inst_unknown_source);
        }
        final String installerLabel = InstalledAppProvider
                .getApplicationLabel(mContext, installerPkgName);
        return mContext.getString(R.string.app_inst_known_source, installerLabel);
    }

    private void onLinkClicked(String url) {
        if (!TextUtils.isEmpty(url)) {
            mCallbacks.openUrl(url);
        }
    }

    private View.OnClickListener mOnInstallClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mCallbacks.installApk();
        }
    };

    private View.OnClickListener mOnUnInstallClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mCallbacks.uninstallApk();
        }
    };

    private View.OnClickListener mOnUpgradeClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mCallbacks.upgradeApk();
        }
    };

    private View.OnClickListener mOnLaunchClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mCallbacks.launchApk();
        }
    };

    private boolean uriIsSetAndCanBeOpened(String s) {
        if (TextUtils.isEmpty(s))
            return false;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(s));
        return (intent.resolveActivity(mContext.getPackageManager()) != null);
    }

    // The HTML formatter adds "\n\n" at the end of every paragraph. This
    // is desired between paragraphs, but not at the end of the whole
    // string as it adds unwanted spacing at the end of the TextView.
    // Remove all trailing newlines.
    // Use this function instead of a trim() as that would require
    // converting to String and thus losing formatting (e.g. bold).
    private static CharSequence trimNewlines(CharSequence s) {
        if (TextUtils.isEmpty(s)) {
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
        SafeURLSpan(String url) {
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

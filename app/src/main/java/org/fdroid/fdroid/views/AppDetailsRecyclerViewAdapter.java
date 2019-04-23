package org.fdroid.fdroid.views;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.nostra13.universalimageloader.core.ImageLoader;
import org.apache.commons.io.FilenameUtils;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.installer.Installer;
import org.fdroid.fdroid.privileged.views.AppDiff;
import org.fdroid.fdroid.privileged.views.AppSecurityPermissions;
import org.fdroid.fdroid.views.main.MainActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("LineLength")
public class AppDetailsRecyclerViewAdapter
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface AppDetailsRecyclerViewAdapterCallbacks {

        boolean isAppDownloading();

        void enableAndroidBeam();

        void disableAndroidBeam();

        void openUrl(String url);

        void installApk();

        void installApk(Apk apk);

        void uninstallApk();

        void installCancel();

        void launchApk();

    }

    private static final int VIEWTYPE_HEADER = 0;
    private static final int VIEWTYPE_SCREENSHOTS = 1;
    private static final int VIEWTYPE_DONATE = 2;
    private static final int VIEWTYPE_LINKS = 3;
    private static final int VIEWTYPE_PERMISSIONS = 4;
    private static final int VIEWTYPE_VERSIONS = 5;
    private static final int VIEWTYPE_NO_VERSIONS = 6;
    private static final int VIEWTYPE_VERSION = 7;

    private final Context context;
    @NonNull
    private App app;
    private final AppDetailsRecyclerViewAdapterCallbacks callbacks;
    private RecyclerView recyclerView;
    private List<Object> items;
    private List<Apk> versions;
    private List<Apk> compatibleVersionsDifferentSig;
    private boolean showVersions;

    private HeaderViewHolder headerView;

    private Apk downloadedApk;
    private final HashMap<String, Boolean> versionsExpandTracker = new HashMap<>();

    public AppDetailsRecyclerViewAdapter(Context context, @NonNull App app, AppDetailsRecyclerViewAdapterCallbacks callbacks) {
        this.context = context;
        this.callbacks = callbacks;
        this.app = app;
        updateItems(app);
    }

    public void updateItems(@NonNull App app) {
        this.app = app;

        // Get versions
        versions = new ArrayList<>();
        compatibleVersionsDifferentSig = new ArrayList<>();
        final List<Apk> apks = ApkProvider.Helper.findByPackageName(context, this.app.packageName);
        ensureInstalledApkExists(apks);
        boolean showIncompatibleVersions = Preferences.get().showIncompatibleVersions();
        for (final Apk apk : apks) {
            boolean allowByCompatibility = apk.compatible || showIncompatibleVersions;
            boolean allowBySig = this.app.installedSig == null || showIncompatibleVersions || TextUtils.equals(this.app.installedSig, apk.sig);
            if (allowByCompatibility) {
                compatibleVersionsDifferentSig.add(apk);
                if (allowBySig) {
                    versions.add(apk);
                    if (!versionsExpandTracker.containsKey(apk.apkName)) {
                        versionsExpandTracker.put(apk.apkName, false);
                    }
                }
            }
        }

        if (items == null) {
            items = new ArrayList<>();
        } else {
            items.clear();
        }
        addItem(VIEWTYPE_HEADER);
        if (app.getAllScreenshots(context).length > 0) {
            addItem(VIEWTYPE_SCREENSHOTS);
        }
        addItem(VIEWTYPE_DONATE);
        addItem(VIEWTYPE_LINKS);
        addItem(VIEWTYPE_PERMISSIONS);
        if (versions.size() > 0) {
            addItem(VIEWTYPE_VERSIONS);
            if (showVersions) {
                setShowVersions(true);
            }
        } else {
            addItem(VIEWTYPE_NO_VERSIONS);
        }

        notifyDataSetChanged();
    }

    private void ensureInstalledApkExists(final List<Apk> apks) {
        Apk installedApk = app.getInstalledApk(this.context);
        // These conditions should be enough to determine if the installedApk
        // is a generated dummy or a proper APK containing data from a repository.
        if (installedApk != null && installedApk.added == null && installedApk.sig == null) {
            installedApk.compatible = true;
            installedApk.sig = app.installedSig;
            installedApk.maxSdkVersion = -1;
            apks.add(installedApk);
        }
    }

    void setShowVersions(boolean showVersions) {
        setShowVersions(showVersions, false);
    }

    void setShowVersions(boolean showVersions, boolean scrollTo) {
        this.showVersions = showVersions;
        boolean itemsWereRemoved = items.removeAll(versions);
        int startIndex = items.indexOf(VIEWTYPE_VERSIONS) + 1;

        // When adding/removing items, be sure to only notifyItemInserted and notifyItemRemoved
        // rather than notifyDatasetChanged(). If we only notify about the entire thing, then
        // everything gets rebuilt, including the expandable "Versions" item. By rebuilding that
        // item it will interrupt the nice material-design-style-ripple from the background.
        if (showVersions) {
            items.addAll(startIndex, versions);
            notifyItemRangeInserted(startIndex, versions.size());
            if (recyclerView != null && scrollTo) {
                final LinearSmoothScroller smoothScroller = new LinearSmoothScroller(context) {
                    @Override
                    protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
                        // The default speed of smooth scrolling doesn't look good
                        // and it's too fast when it happens while inserting
                        // multiple recycler view items
                        return 75f / displayMetrics.densityDpi;
                    }
                };
                // Expanding the version list reveals up to 5 items by default
                int visibleVersionLimit = Math.min(versions.size(), 5);
                smoothScroller.setTargetPosition(startIndex + visibleVersionLimit - 1);
                recyclerView.getLayoutManager().startSmoothScroll(smoothScroller);
            }
        } else if (itemsWereRemoved) {
            notifyItemRangeRemoved(startIndex, versions.size());
            if (recyclerView != null && scrollTo) {
                recyclerView.smoothScrollToPosition(startIndex - 1);
            }
        }
    }

    private void addItem(int item) {
        // Gives us a chance to hide sections that are not used, e.g. the donate section when
        // we have no donation links.
        if (item == VIEWTYPE_DONATE && !shouldShowDonate()) {
            return;
        } else if (item == VIEWTYPE_PERMISSIONS && !shouldShowPermissions()) {
            return;
        }
        items.add(item);
    }

    private boolean shouldShowPermissions() {
        // Figure out if we should show permissions section
        Apk curApk = getSuggestedApk();
        final boolean curApkCompatible = curApk != null && curApk.compatible;
        return versions.size() > 0 && (curApkCompatible || Preferences.get().showIncompatibleVersions());
    }

    private Apk getSuggestedApk() {
        Apk curApk = null;
        String appropriateSig = app.getMostAppropriateSignature();
        for (int i = 0; i < versions.size(); i++) {
            final Apk apk = versions.get(i);
            if (apk.versionCode == app.suggestedVersionCode && TextUtils.equals(apk.sig, appropriateSig)) {
                curApk = apk;
                break;
            }
        }
        return curApk;
    }

    private boolean shouldShowDonate() {
        return uriIsSetAndCanBeOpened(app.donate) ||
                uriIsSetAndCanBeOpened(app.getBitcoinUri()) ||
                uriIsSetAndCanBeOpened(app.getLitecoinUri()) ||
                uriIsSetAndCanBeOpened(app.getFlattrUri()) ||
                uriIsSetAndCanBeOpened(app.getLiberapayUri());
    }

    private void notifyVersionViewsChanged() {
        int startIndex = items.indexOf(VIEWTYPE_VERSIONS) + 1;
        notifyItemRangeChanged(startIndex, versions.size());
    }

    public void notifyAboutDownloadedApk(final Apk apk) {
        downloadedApk = apk;
        notifyVersionViewsChanged();
    }

    public void clearProgress() {
        if (headerView != null) {
            headerView.clearProgress();
        }
        if (downloadedApk != null) {
            notifyVersionViewsChanged();
            downloadedApk = null;
        }
    }

    public void setIndeterminateProgress(int resIdString) {
        if (headerView != null) {
            headerView.setIndeterminateProgress(resIdString);
        }
    }

    public void setProgress(long bytesDownloaded, long totalBytes) {
        if (headerView != null) {
            headerView.setProgress(bytesDownloaded, totalBytes);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case VIEWTYPE_HEADER:
                View header = inflater.inflate(R.layout.app_details2_header, parent, false);
                return new HeaderViewHolder(header);
            case VIEWTYPE_SCREENSHOTS:
                View screenshots = inflater.inflate(R.layout.app_details2_screenshots, parent, false);
                return new ScreenShotsViewHolder(screenshots);
            case VIEWTYPE_DONATE:
                View donate = inflater.inflate(R.layout.app_details2_donate, parent, false);
                return new DonateViewHolder(donate);
            case VIEWTYPE_LINKS:
                View links = inflater.inflate(R.layout.app_details2_links, parent, false);
                return new LinksViewHolder(links);
            case VIEWTYPE_PERMISSIONS:
                View permissions = inflater.inflate(R.layout.app_details2_links, parent, false);
                return new PermissionsViewHolder(permissions);
            case VIEWTYPE_VERSIONS:
                View versionsView = inflater.inflate(R.layout.app_details2_links, parent, false);
                return new VersionsViewHolder(versionsView);
            case VIEWTYPE_NO_VERSIONS:
                View noVersionsView = inflater.inflate(R.layout.app_details2_links, parent, false);
                return new NoVersionsViewHolder(noVersionsView);
            case VIEWTYPE_VERSION:
                View version = inflater.inflate(R.layout.app_details2_version_item, parent, false);
                return new VersionViewHolder(version);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, int position) {
        int viewType = getItemViewType(position);
        switch (viewType) {
            case VIEWTYPE_HEADER:
                HeaderViewHolder header = (HeaderViewHolder) holder;
                headerView = header;
                header.bindModel();
                break;

            // These don't have any specific requirements, they all get their state from the outer class.
            case VIEWTYPE_SCREENSHOTS:
            case VIEWTYPE_DONATE:
            case VIEWTYPE_LINKS:
            case VIEWTYPE_PERMISSIONS:
            case VIEWTYPE_VERSIONS:
            case VIEWTYPE_NO_VERSIONS:
                ((AppDetailsViewHolder) holder).bindModel();
                break;

            case VIEWTYPE_VERSION:
                final Apk apk = (Apk) items.get(position);
                ((VersionViewHolder) holder).bindModel(apk);
                break;
        }
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        if (holder instanceof HeaderViewHolder) {
            headerView = null;
        }
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (items.get(position) instanceof Apk) {
            return VIEWTYPE_VERSION;
        }
        return (Integer) items.get(position);
    }

    public class HeaderViewHolder extends RecyclerView.ViewHolder {
        private static final int MAX_LINES = 5;

        final ImageView iconView;
        final TextView titleView;
        final TextView authorView;
        final TextView lastUpdateView;
        final TextView whatsNewView;
        final TextView descriptionView;
        final TextView descriptionMoreView;
        final TextView antiFeaturesLabelView;
        final TextView antiFeaturesView;
        final View antiFeaturesWarningView;
        final View buttonLayout;
        final Button buttonPrimaryView;
        final Button buttonSecondaryView;
        final View progressLayout;
        final ProgressBar progressBar;
        final TextView progressLabel;
        final TextView progressPercent;
        final View progressCancel;
        boolean descriptionIsExpanded;

        HeaderViewHolder(View view) {
            super(view);
            iconView = (ImageView) view.findViewById(R.id.icon);
            titleView = (TextView) view.findViewById(R.id.title);
            authorView = (TextView) view.findViewById(R.id.author);
            lastUpdateView = (TextView) view.findViewById(R.id.text_last_update);
            whatsNewView = (TextView) view.findViewById(R.id.whats_new);
            descriptionView = (TextView) view.findViewById(R.id.description);
            descriptionMoreView = (TextView) view.findViewById(R.id.description_more);
            antiFeaturesLabelView = (TextView) view.findViewById(R.id.label_anti_features);
            antiFeaturesView = (TextView) view.findViewById(R.id.text_anti_features);
            antiFeaturesWarningView = view.findViewById(R.id.anti_features_warning);
            buttonLayout = view.findViewById(R.id.button_layout);
            buttonPrimaryView = (Button) view.findViewById(R.id.primaryButtonView);
            buttonSecondaryView = (Button) view.findViewById(R.id.secondaryButtonView);
            progressLayout = view.findViewById(R.id.progress_layout);
            progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
            progressLabel = (TextView) view.findViewById(R.id.progress_label);
            progressPercent = (TextView) view.findViewById(R.id.progress_percent);
            progressCancel = view.findViewById(R.id.progress_cancel);
            descriptionView.setMaxLines(MAX_LINES);
            descriptionView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            descriptionMoreView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Make this "header section" the focused child, so that RecyclerView will use
                    // it as the anchor in the layout process. Otherwise the RV might select another
                    // view as the anchor, resulting in that the top of this view is instead scrolled
                    // off the screen. Refer to LinearLayoutManager.updateAnchorFromChildren(...).
                    recyclerView.requestChildFocus(itemView, itemView);
                    if (TextViewCompat.getMaxLines(descriptionView) != MAX_LINES) {
                        descriptionView.setMaxLines(MAX_LINES);
                        descriptionMoreView.setText(R.string.more);
                        descriptionIsExpanded = false;
                    } else {
                        descriptionView.setMaxLines(Integer.MAX_VALUE);
                        descriptionMoreView.setText(R.string.less);
                        descriptionIsExpanded = true;
                    }
                    updateAntiFeaturesWarning();
                }
            });
        }

        public void clearProgress() {
            progressLayout.setVisibility(View.GONE);
            buttonLayout.setVisibility(View.VISIBLE);
        }

        public void setIndeterminateProgress(int resIdString) {
            progressLayout.setVisibility(View.VISIBLE);
            buttonLayout.setVisibility(View.GONE);
            progressBar.setIndeterminate(true);
            progressLabel.setText(resIdString);
            progressLabel.setContentDescription(context.getString(R.string.downloading));
            progressPercent.setText("");
            if (resIdString == R.string.installing || resIdString == R.string.uninstalling) {
                progressCancel.setVisibility(View.GONE);
            } else {
                progressCancel.setVisibility(View.VISIBLE);
            }
        }

        public void setProgress(long bytesDownloaded, long totalBytes) {
            progressLayout.setVisibility(View.VISIBLE);
            buttonLayout.setVisibility(View.GONE);
            progressCancel.setVisibility(View.VISIBLE);

            progressBar.setMax(Utils.bytesToKb(totalBytes));
            progressBar.setProgress(Utils.bytesToKb(bytesDownloaded));
            progressBar.setIndeterminate(totalBytes <= 0);
            progressLabel.setContentDescription("");
            if (totalBytes > 0 && bytesDownloaded >= 0) {
                int percent = Utils.getPercent(bytesDownloaded, totalBytes);
                progressLabel.setText(Utils.getFriendlySize(bytesDownloaded)
                        + " / " + Utils.getFriendlySize(totalBytes));
                progressLabel.setContentDescription(context.getString(R.string.app__tts__downloading_progress,
                        percent));
                progressPercent.setText(String.format(Locale.ENGLISH, "%d%%", percent));
            } else if (bytesDownloaded >= 0) {
                progressLabel.setText(Utils.getFriendlySize(bytesDownloaded));
                progressLabel.setContentDescription(context.getString(R.string.downloading));
                progressPercent.setText("");
            }
        }

        public void bindModel() {
            ImageLoader.getInstance().displayImage(app.iconUrl, iconView, Utils.getRepoAppDisplayImageOptions());
            titleView.setText(app.name);
            if (!TextUtils.isEmpty(app.authorName)) {
                authorView.setText(context.getString(R.string.by_author_format, app.authorName));
                authorView.setVisibility(View.VISIBLE);
            } else {
                authorView.setVisibility(View.GONE);
            }
            if (app.lastUpdated != null) {
                Resources res = lastUpdateView.getContext().getResources();
                lastUpdateView.setText(Utils.formatLastUpdated(res, app.lastUpdated));
                lastUpdateView.setVisibility(View.VISIBLE);
            } else {
                lastUpdateView.setVisibility(View.GONE);
            }

            Apk suggestedApk = getSuggestedApk();
            if (suggestedApk == null || TextUtils.isEmpty(app.whatsNew)) {
                whatsNewView.setVisibility(View.GONE);
            } else {
                //noinspection deprecation Ignore deprecation because the suggested way is only available in API 24.
                Locale locale = context.getResources().getConfiguration().locale;

                StringBuilder sbWhatsNew = new StringBuilder();
                sbWhatsNew.append(whatsNewView.getContext().getString(R.string.details_new_in_version,
                        suggestedApk.versionName).toUpperCase(locale));
                sbWhatsNew.append("\n\n");
                sbWhatsNew.append(app.whatsNew);
                whatsNewView.setText(sbWhatsNew);
                whatsNewView.setVisibility(View.VISIBLE);

                // Set focus on the header section to prevent auto scrolling to
                // the changelog if its content becomes too long to fit on screen.
                recyclerView.requestChildFocus(itemView, itemView);
            }
            final Spanned desc = Html.fromHtml(app.description, null, new Utils.HtmlTagHandler());
            descriptionView.setMovementMethod(LinkMovementMethod.getInstance());
            descriptionView.setText(trimTrailingNewlines(desc));
            if (descriptionView.getText() instanceof Spannable) {
                Spannable spannable = (Spannable) descriptionView.getText();
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
            descriptionView.post(new Runnable() {
                @Override
                public void run() {
                    if (descriptionView.getLineCount() <= HeaderViewHolder.MAX_LINES && app.antiFeatures == null) {
                        descriptionMoreView.setVisibility(View.GONE);
                    } else {
                        descriptionMoreView.setVisibility(View.VISIBLE);
                    }
                }
            });
            if (app.antiFeatures != null && app.antiFeatures.length > 0) {
                StringBuilder sb = new StringBuilder();
                for (String af : app.antiFeatures) {
                    String afdesc = descAntiFeature(af);
                    sb.append("<p><a href=\"https://f-droid.org/wiki/page/Antifeature:")
                            .append(af)
                            .append("\">")
                            .append(afdesc)
                            .append("</a></p>");
                }
                antiFeaturesView.setText(trimTrailingNewlines(Html.fromHtml(sb.toString())));
                antiFeaturesView.setMovementMethod(LinkMovementMethod.getInstance());
            } else {
                antiFeaturesView.setVisibility(View.GONE);
            }

            updateAntiFeaturesWarning();
            buttonPrimaryView.setText(R.string.menu_install);
            buttonPrimaryView.setVisibility(versions.size() > 0 ? View.VISIBLE : View.GONE);
            buttonSecondaryView.setText(R.string.menu_uninstall);
            buttonSecondaryView.setVisibility(app.isUninstallable(context) ? View.VISIBLE : View.INVISIBLE);
            buttonSecondaryView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    callbacks.uninstallApk();
                }
            });
            if (callbacks.isAppDownloading()) {
                buttonPrimaryView.setText(R.string.downloading);
                buttonPrimaryView.setEnabled(false);
                buttonLayout.setVisibility(View.GONE);
                progressLayout.setVisibility(View.VISIBLE);
            } else if (!app.isInstalled(context) && suggestedApk != null) {
                // Check count > 0 due to incompatible apps resulting in an empty list.
                callbacks.disableAndroidBeam();
                progressLayout.setVisibility(View.GONE);
                // Set Install button and hide second button
                buttonPrimaryView.setText(R.string.menu_install);
                buttonPrimaryView.setEnabled(true);
                buttonLayout.setVisibility(View.VISIBLE);
                buttonPrimaryView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        callbacks.installApk();
                    }
                });
            } else if (app.isInstalled(context)) {
                callbacks.enableAndroidBeam();
                if (app.canAndWantToUpdate(context) && suggestedApk != null) {
                    buttonPrimaryView.setText(R.string.menu_upgrade);
                    buttonPrimaryView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            callbacks.installApk();
                        }
                    });
                } else {
                    Apk mediaApk = app.getMediaApkifInstalled(context);
                    if (context.getPackageManager().getLaunchIntentForPackage(app.packageName) != null) {
                        buttonPrimaryView.setText(R.string.menu_launch);
                        buttonPrimaryView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                callbacks.launchApk();
                            }
                        });
                    } else if (!app.isApk && mediaApk != null) {
                        final File installedFile = new File(mediaApk.getMediaInstallPath(context), mediaApk.apkName);
                        if (!installedFile.toString().startsWith(context.getApplicationInfo().dataDir)) {
                            final Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                            Uri uri = FileProvider.getUriForFile(context, Installer.AUTHORITY, installedFile);
                            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                                    FilenameUtils.getExtension(installedFile.getName()));
                            viewIntent.setDataAndType(uri, mimeType);
                            if (Build.VERSION.SDK_INT < 19) {
                                viewIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            } else {
                                viewIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                            }
                            if (context.getPackageManager().queryIntentActivities(viewIntent, 0).size() > 0) {
                                buttonPrimaryView.setText(R.string.menu_open);
                                buttonPrimaryView.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        try {
                                            context.startActivity(viewIntent);
                                        } catch (ActivityNotFoundException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                            } else {
                                buttonPrimaryView.setVisibility(View.GONE);
                            }
                        } else {
                            buttonPrimaryView.setVisibility(View.GONE);
                        }
                    } else {
                        buttonPrimaryView.setVisibility(View.GONE);
                    }
                }
                buttonPrimaryView.setEnabled(true);
                buttonLayout.setVisibility(View.VISIBLE);
                progressLayout.setVisibility(View.GONE);
            } else {
                buttonLayout.setVisibility(View.VISIBLE);
                progressLayout.setVisibility(View.GONE);
            }
            progressCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    callbacks.installCancel();
                }
            });

        }

        private void updateAntiFeaturesWarning() {
            if (app.antiFeatures == null || TextUtils.isEmpty(antiFeaturesView.getText())) {
                antiFeaturesLabelView.setVisibility(View.GONE);
                antiFeaturesView.setVisibility(View.GONE);
                antiFeaturesWarningView.setVisibility(View.GONE);
            } else {
                antiFeaturesLabelView.setVisibility(descriptionIsExpanded ? View.VISIBLE : View.GONE);
                antiFeaturesView.setVisibility(descriptionIsExpanded ? View.VISIBLE : View.GONE);
                antiFeaturesWarningView.setVisibility(descriptionIsExpanded ? View.GONE : View.VISIBLE);
            }
        }

        private String descAntiFeature(String af) {
            switch (af) {
                case "Ads":
                    return itemView.getContext().getString(R.string.antiadslist);
                case "Tracking":
                    return itemView.getContext().getString(R.string.antitracklist);
                case "NonFreeNet":
                    return itemView.getContext().getString(R.string.antinonfreenetlist);
                case "NonFreeAdd":
                    return itemView.getContext().getString(R.string.antinonfreeadlist);
                case "NonFreeDep":
                    return itemView.getContext().getString(R.string.antinonfreedeplist);
                case "UpstreamNonFree":
                    return itemView.getContext().getString(R.string.antiupstreamnonfreelist);
                case "NonFreeAssets":
                    return itemView.getContext().getString(R.string.antinonfreeassetslist);
                case "DisabledAlgorithm":
                    return itemView.getContext().getString(R.string.antidisabledalgorithmlist);
                case "KnownVuln":
                    return itemView.getContext().getString(R.string.antiknownvulnlist);
                case "NoSourceSince":
                    return itemView.getContext().getString(R.string.antinosourcesince);
                default:
                    return af;
            }
        }
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        this.recyclerView = null;
        super.onDetachedFromRecyclerView(recyclerView);
    }

    private abstract static class AppDetailsViewHolder extends RecyclerView.ViewHolder {
        AppDetailsViewHolder(View itemView) {
            super(itemView);
        }

        public abstract void bindModel();
    }

    private class ScreenShotsViewHolder extends AppDetailsViewHolder
            implements ScreenShotsRecyclerViewAdapter.Listener {
        final RecyclerView recyclerView;
        LinearLayoutManagerSnapHelper snapHelper;

        ScreenShotsViewHolder(View view) {
            super(view);
            recyclerView = (RecyclerView) view.findViewById(R.id.screenshots);
        }

        @Override
        public void bindModel() {
            LinearLayoutManager lm = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
            recyclerView.setLayoutManager(lm);
            ScreenShotsRecyclerViewAdapter adapter = new ScreenShotsRecyclerViewAdapter(itemView.getContext(), app, this);
            recyclerView.setAdapter(adapter);
            recyclerView.setHasFixedSize(true);
            recyclerView.setNestedScrollingEnabled(false);
            if (snapHelper != null) {
                snapHelper.attachToRecyclerView(null);
            }
            snapHelper = new LinearLayoutManagerSnapHelper(lm);
            snapHelper.attachToRecyclerView(recyclerView);
        }

        @Override
        public void onScreenshotClick(int position) {
            context.startActivity(ScreenShotsActivity.getStartIntent(context, app.packageName, position));
        }
    }

    private class DonateViewHolder extends AppDetailsViewHolder {
        final TextView donateHeading;
        final GridLayout donationOptionsLayout;

        DonateViewHolder(View view) {
            super(view);
            donateHeading = (TextView) view.findViewById(R.id.donate_header);
            donationOptionsLayout = (GridLayout) view.findViewById(R.id.donation_options);
        }

        @Override
        public void bindModel() {
            if (TextUtils.isEmpty(app.authorName)) {
                donateHeading.setText(context.getString(R.string.app_details_donate_prompt_unknown_author, app.name));
            } else {
                String author = "<strong>" + app.authorName + "</strong>";
                donateHeading.setText(Html.fromHtml(context.getString(R.string.app_details_donate_prompt, app.name, author)));
            }

            donationOptionsLayout.removeAllViews();

            // Donate button
            if (uriIsSetAndCanBeOpened(app.donate)) {
                addDonateOption(R.layout.donate_generic, app.donate);
            }

            // Bitcoin
            if (uriIsSetAndCanBeOpened(app.getBitcoinUri())) {
                addDonateOption(R.layout.donate_bitcoin, app.getBitcoinUri());
            }

            // Litecoin
            if (uriIsSetAndCanBeOpened(app.getLitecoinUri())) {
                addDonateOption(R.layout.donate_litecoin, app.getLitecoinUri());
            }

            // Flattr
            if (uriIsSetAndCanBeOpened(app.getFlattrUri())) {
                addDonateOption(R.layout.donate_flattr, app.getFlattrUri());
            }
            // LiberaPay
            if (uriIsSetAndCanBeOpened(app.getLiberapayUri())) {
                addDonateOption(R.layout.donate_liberapay, app.getLiberapayUri());
            }
        }

        private void addDonateOption(@LayoutRes int layout, final String uri) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View option = inflater.inflate(layout, donationOptionsLayout, false);
            option.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onLinkClicked(uri);
                }
            });
            donationOptionsLayout.addView(option);
        }
    }

    private abstract static class ExpandableLinearLayoutViewHolder extends AppDetailsViewHolder {
        final TextView headerView;
        final LinearLayout contentView;

        ExpandableLinearLayoutViewHolder(View view) {
            super(view);
            headerView = (TextView) view.findViewById(R.id.information);
            contentView = (LinearLayout) view.findViewById(R.id.ll_content);
        }

        @DrawableRes
        protected abstract int getIcon();

        /**
         * Depending on whether we are expanded or not, update the icon which indicates whether the
         * user can expand/collapse this item.
         */
        protected void updateExpandableItem(boolean isExpanded) {
            TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(headerView, getIcon(), 0, isExpanded ? R.drawable.ic_expand_less_grey600 : R.drawable.ic_expand_more_grey600, 0);
        }
    }

    private class VersionsViewHolder extends ExpandableLinearLayoutViewHolder {

        VersionsViewHolder(View view) {
            super(view);
        }

        @Override
        public void bindModel() {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setShowVersions(!showVersions, true);
                    updateExpandableItem(showVersions);
                }
            });
            headerView.setText(R.string.versions);
            updateExpandableItem(showVersions);
        }

        @DrawableRes
        protected int getIcon() {
            return R.drawable.ic_access_time_24dp_grey600;
        }
    }

    private class NoVersionsViewHolder extends AppDetailsViewHolder {
        final TextView headerView;

        NoVersionsViewHolder(View view) {
            super(view);
            headerView = (TextView) view.findViewById(R.id.information);
            TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(headerView, R.drawable.ic_access_time_24dp_grey600, 0, 0, 0);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    explainIncompatibleVersions();
                }
            });
        }

        @Override
        public void bindModel() {
            Context context = headerView.getContext();
            if (hasCompatibleApksDifferentSigs()) {
                headerView.setText(context.getString(R.string.app_details__no_versions__no_compatible_signatures));
            } else {
                headerView.setText(context.getString(R.string.app_details__no_versions__none_compatible_with_device));
            }
        }

        /**
         * Show a dialog to the user explaining the reaons there are no compatible versions.
         * This will either be due to device features (e.g. NFC, API levels, etc) or being signed
         * by a different certificate (as is often the case with apps from Google Play signed by
         * upstream).
         */
        private void explainIncompatibleVersions() {
            String preferenceName = context.getString(R.string.show_incompat_versions);
            String showIncompatible = context.getString(
                    R.string.app_details__no_versions__show_incompat_versions, preferenceName);

            String message;
            String title;
            if (hasCompatibleApksDifferentSigs()) {
                title = context.getString(R.string.app_details__no_versions__no_compatible_signatures);
                message = context.getString(R.string.app_details__no_versions__explain_incompatible_signatures) +
                        "\n\n" + showIncompatible;
            } else {
                title = context.getString(R.string.app_details__no_versions__none_compatible_with_device);
                message = showIncompatible;
            }

            new AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(R.string.menu_settings, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(context, MainActivity.class);
                            intent.putExtra(MainActivity.EXTRA_VIEW_SETTINGS, true);
                            context.startActivity(intent);
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        }

        private boolean hasCompatibleApksDifferentSigs() {
            return compatibleVersionsDifferentSig != null && compatibleVersionsDifferentSig.size() > 0;
        }
    }

    private class PermissionsViewHolder extends ExpandableLinearLayoutViewHolder {

        PermissionsViewHolder(View view) {
            super(view);
        }

        @Override
        public void bindModel() {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean shouldBeVisible = contentView.getVisibility() != View.VISIBLE;
                    contentView.setVisibility(shouldBeVisible ? View.VISIBLE : View.GONE);
                    updateExpandableItem(shouldBeVisible);
                    if (shouldBeVisible && recyclerView != null) {
                        ((LinearLayoutManager) recyclerView.getLayoutManager()).scrollToPositionWithOffset(items.indexOf(VIEWTYPE_PERMISSIONS), 0);
                    }
                }
            });
            headerView.setText(R.string.permissions);
            updateExpandableItem(false);
            contentView.removeAllViews();
            AppDiff appDiff = new AppDiff(context, versions.get(0));
            AppSecurityPermissions perms = new AppSecurityPermissions(context, appDiff.apkPackageInfo);
            contentView.addView(perms.getPermissionsView(AppSecurityPermissions.WHICH_ALL));
        }

        @DrawableRes
        protected int getIcon() {
            return R.drawable.ic_lock_24dp_grey600;
        }
    }

    private class LinksViewHolder extends ExpandableLinearLayoutViewHolder {

        LinksViewHolder(View view) {
            super(view);
        }

        @Override
        public void bindModel() {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean shouldBeVisible = contentView.getVisibility() != View.VISIBLE;
                    contentView.setVisibility(shouldBeVisible ? View.VISIBLE : View.GONE);
                    updateExpandableItem(shouldBeVisible);
                    if (shouldBeVisible && recyclerView != null) {
                        ((LinearLayoutManager) recyclerView.getLayoutManager()).scrollToPositionWithOffset(items.indexOf(VIEWTYPE_LINKS), 0);
                    }
                }
            });
            headerView.setText(R.string.links);
            updateExpandableItem(false);
            contentView.removeAllViews();

            // License link
            if (!TextUtils.isEmpty(app.license)) {
                String firstLicense = app.license.split(",")[0];
                String url = "https://spdx.org/licenses/" + firstLicense + ".html";
                if (uriIsSetAndCanBeOpened(url)) {
                    addLinkItemView(contentView, R.string.menu_license, R.drawable.ic_license, url, app.license);
                }
            }

            // Video link
            if (uriIsSetAndCanBeOpened(app.video)) {
                addLinkItemView(contentView, R.string.menu_video, R.drawable.ic_video, app.video);
            }

            // Source button
            if (uriIsSetAndCanBeOpened(app.sourceCode)) {
                addLinkItemView(contentView, R.string.menu_source, R.drawable.ic_source_code, app.sourceCode);
            }

            // Issues button
            if (uriIsSetAndCanBeOpened(app.issueTracker)) {
                addLinkItemView(contentView, R.string.menu_issues, R.drawable.ic_issues, app.issueTracker);
            }

            // Translation button
            if (uriIsSetAndCanBeOpened(app.translation)) {
                addLinkItemView(contentView, R.string.menu_translation, R.drawable.ic_translation, app.translation);
            }

            // Changelog button
            if (uriIsSetAndCanBeOpened(app.changelog)) {
                addLinkItemView(contentView, R.string.menu_changelog, R.drawable.ic_changelog, app.changelog);
            }

            // Website button
            if (uriIsSetAndCanBeOpened(app.webSite)) {
                addLinkItemView(contentView, R.string.menu_website, R.drawable.ic_website, app.webSite);
            }

            // Email button
            final String subject = Uri.encode(context.getString(R.string.app_details_subject, app.name));
            String emailUrl = app.authorEmail == null ? null : ("mailto:" + app.authorEmail + "?subject=" + subject);
            if (uriIsSetAndCanBeOpened(emailUrl)) {
                addLinkItemView(contentView, R.string.menu_email, R.drawable.ic_email, emailUrl);
            }
        }

        @DrawableRes
        protected int getIcon() {
            return R.drawable.ic_website;
        }
    }

    private class VersionViewHolder extends RecyclerView.ViewHolder {
        final TextView version;
        final TextView statusInstalled;
        final TextView statusSuggested;
        final TextView statusIncompatible;
        final TextView versionCode;
        final TextView added;
        final ImageView expandArrow;
        final View expandedLayout;
        final TextView repository;
        final TextView size;
        final TextView api;
        final Button buttonInstallUpgrade;
        final Button buttonDowngrade;
        Button buttonAction;
        final View busyIndicator;
        final TextView incompatibleReasons;
        final TextView targetArch;

        private Apk apk;

        VersionViewHolder(View view) {
            super(view);
            version = (TextView) view.findViewById(R.id.version);
            statusInstalled = (TextView) view.findViewById(R.id.status_installed);
            statusSuggested = (TextView) view.findViewById(R.id.status_suggested);
            statusIncompatible = (TextView) view.findViewById(R.id.status_incompatible);
            versionCode = view.findViewById(R.id.versionCode);
            added = (TextView) view.findViewById(R.id.added);
            expandArrow = (ImageView) view.findViewById(R.id.expand_arrow);
            expandedLayout = (View) view.findViewById(R.id.expanded_layout);
            repository = (TextView) view.findViewById(R.id.repository);
            size = (TextView) view.findViewById(R.id.size);
            api = (TextView) view.findViewById(R.id.api);
            buttonInstallUpgrade = (Button) view.findViewById(R.id.button_install_upgrade);
            buttonDowngrade = (Button) view.findViewById(R.id.button_downgrade);
            busyIndicator = (View) view.findViewById(R.id.busy_indicator);
            incompatibleReasons = (TextView) view.findViewById(R.id.incompatible_reasons);
            targetArch = (TextView) view.findViewById(R.id.target_arch);

            int margin = context.getResources().getDimensionPixelSize(R.dimen.layout_horizontal_margin);
            int padding = context.getResources().getDimensionPixelSize(R.dimen.details_activity_padding);
            ViewCompat.setPaddingRelative(view, margin + padding + ViewCompat.getPaddingStart(view), view.getPaddingTop(), margin + ViewCompat.getPaddingEnd(view), view.getPaddingBottom());
        }

        public void bindModel(final Apk apk) {
            this.apk = apk;

            boolean isAppInstalled = app.isInstalled(context);
            boolean isApkInstalled = apk.versionCode == app.installedVersionCode &&
                    TextUtils.equals(apk.sig, app.installedSig);
            boolean isApkSuggested = apk.versionCode == app.suggestedVersionCode &&
                    TextUtils.equals(apk.sig, app.getMostAppropriateSignature());
            boolean isApkDownloading = callbacks.isAppDownloading() && downloadedApk != null &&
                    downloadedApk.compareTo(apk) == 0 && TextUtils.equals(apk.apkName, downloadedApk.apkName);
            boolean isApkInstalledDummy = apk.versionCode == app.installedVersionCode &&
                    apk.compatible && apk.size == 0 && apk.maxSdkVersion == -1;

            // Version name and statuses
            version.setText(apk.versionName);
            statusSuggested.setVisibility(isApkSuggested && apk.compatible ? View.VISIBLE : View.GONE);
            statusInstalled.setVisibility(isApkInstalled ? View.VISIBLE : View.GONE);
            statusIncompatible.setVisibility(!apk.compatible ? View.VISIBLE : View.GONE);

            // Version name width correction in case it's
            // too long to prevent truncating the statuses
            if (statusSuggested.getVisibility() == View.VISIBLE ||
                    statusInstalled.getVisibility() == View.VISIBLE ||
                    statusIncompatible.getVisibility() == View.VISIBLE) {
                int maxWidth = (int) (Resources.getSystem().getDisplayMetrics().widthPixels * 0.4);
                version.setMaxWidth(maxWidth);
            } else {
                version.setMaxWidth(Integer.MAX_VALUE);
            }

            // Added date
            if (apk.added != null) {
                java.text.DateFormat df = DateFormat.getDateFormat(context);
                added.setVisibility(View.VISIBLE);
                added.setText(context.getString(R.string.added_on, df.format(apk.added)));
            } else {
                added.setVisibility(View.INVISIBLE);
            }

            // Repository name, APK size and required Android version
            Repo repo = RepoProvider.Helper.findById(context, apk.repoId);
            if (repo != null) {
                repository.setVisibility(View.VISIBLE);
                repository.setText(String.format(context.getString(R.string.app_repository), repo.getName()));
            } else {
                repository.setVisibility(View.INVISIBLE);
            }
            size.setText(context.getString(R.string.app_size, Utils.getFriendlySize(apk.size)));
            api.setText(getApiText(apk));


            // Figuring out whether to show Install/Update button or Downgrade button
            buttonDowngrade.setVisibility(View.GONE);
            buttonInstallUpgrade.setVisibility(View.GONE);
            buttonInstallUpgrade.setText(context.getString(R.string.menu_install));
            showActionButton(buttonInstallUpgrade, isApkInstalled, isApkDownloading);
            if (isAppInstalled && !isApkInstalled) {
                if (apk.versionCode > app.installedVersionCode) {
                    // Change the label to indicate that pressing this
                    // button will result in updating the installed app
                    buttonInstallUpgrade.setText(R.string.menu_upgrade);
                } else if (apk.versionCode < app.installedVersionCode) {
                    // The Downgrade button should be shown in this case
                    buttonInstallUpgrade.setVisibility(View.GONE);
                    showActionButton(buttonDowngrade, false, isApkDownloading);
                }
            }

            // Show busy indicator when the APK is being downloaded
            busyIndicator.setVisibility(isApkDownloading ? View.VISIBLE : View.GONE);

            // Display when the expert mode is enabled
            if (Preferences.get().expertMode()) {
                versionCode.setText(String.format(Locale.ENGLISH, " (%d) ", apk.versionCode));
                // Display incompatible reasons when the app isn't compatible
                if (!apk.compatible) {
                    String incompatibleReasonsText = getIncompatibleReasonsText(apk);
                    if (incompatibleReasonsText != null) {
                        incompatibleReasons.setVisibility(View.VISIBLE);
                        incompatibleReasons.setText(incompatibleReasonsText);
                    } else {
                        incompatibleReasons.setVisibility(View.GONE);
                    }
                    targetArch.setVisibility(View.GONE);
                } else {
                    // Display target architecture when the app is compatible
                    String targetArchText = getTargetArchText(apk);
                    if (targetArchText != null) {
                        targetArch.setVisibility(View.VISIBLE);
                        targetArch.setText(targetArchText);
                    } else {
                        targetArch.setVisibility(View.GONE);
                    }
                    incompatibleReasons.setVisibility(View.GONE);
                }
            } else {
                versionCode.setText("");
                incompatibleReasons.setVisibility(View.GONE);
                targetArch.setVisibility(View.GONE);
            }

            // Expand the view if it was previously expanded or when downloading
            expand(versionsExpandTracker.get(apk.apkName) || isApkDownloading);

            // Toggle expanded view when clicking the whole version item,
            // unless it's an installed app version dummy item - it doesn't
            // contain any meaningful info, so there is no reason to expand it.
            if (!isApkInstalledDummy) {
                expandArrow.setAlpha(1f);
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        toggleExpanded();
                    }
                });
            } else {
                expandArrow.setAlpha(0.3f);
                itemView.setOnClickListener(null);
            }
        }

        private String getApiText(final Apk apk) {
            String apiText = "Android: ";
            if (apk.minSdkVersion > 0 && apk.maxSdkVersion < Apk.SDK_VERSION_MAX_VALUE) {
                apiText += context.getString(R.string.minsdk_up_to_maxsdk,
                        Utils.getAndroidVersionName(apk.minSdkVersion),
                        Utils.getAndroidVersionName(apk.maxSdkVersion));
            } else if (apk.minSdkVersion > 0) {
                apiText += context.getString(R.string.minsdk_or_later,
                        Utils.getAndroidVersionName(apk.minSdkVersion));
            } else if (apk.maxSdkVersion > 0) {
                apiText += context.getString(R.string.up_to_maxsdk,
                        Utils.getAndroidVersionName(apk.maxSdkVersion));
            }
            return apiText;
        }

        private String getIncompatibleReasonsText(final Apk apk) {
            if (apk.incompatibleReasons != null) {
                return context.getResources().getString(R.string.requires_features,
                        TextUtils.join(", ", apk.incompatibleReasons));
            } else {
                boolean mismatchedSig = app.installedSig != null
                        && !TextUtils.equals(app.installedSig, apk.sig);
                if (mismatchedSig) {
                    return context.getString(R.string.app_details__incompatible_mismatched_signature);
                }
            }
            return null;
        }

        private String getTargetArchText(final Apk apk) {
            if (apk.nativecode == null) {
                return null;
            }
            String currentArch = System.getProperty("os.arch");
            List<String> customArchs = new ArrayList<>();
            for (String arch : apk.nativecode) {
                // Gather only archs different than current arch
                if (!TextUtils.equals(arch, currentArch)) {
                    customArchs.add(arch);
                }
            }
            String archs = TextUtils.join(", ", customArchs);
            if (!archs.isEmpty()) {
                // Reuse "Requires: ..." string to display this
                return context.getResources().getString(R.string.requires_features, archs);
            }
            return null;
        }

        private void showActionButton(Button button, boolean isApkInstalled, boolean isApkDownloading) {
            buttonAction = button;
            if (isApkDownloading) {
                // Don't show the button in this case
                // as the busy indicator will take its place
                buttonAction.setVisibility(View.GONE);
            } else {
                // The button should be shown but it should be also disabled
                // if either the APK isn't compatible or it's already installed
                // or also when some other APK is currently being downloaded
                buttonAction.setVisibility(View.VISIBLE);
                boolean buttonActionDisabled = !apk.compatible || isApkInstalled ||
                        callbacks.isAppDownloading();
                buttonAction.setEnabled(!buttonActionDisabled);
                buttonAction.setAlpha(buttonActionDisabled ? 0.15f : 1f);
                buttonAction.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        callbacks.installApk(apk);
                    }
                });
            }
        }

        private void expand(boolean expand) {
            versionsExpandTracker.put(apk.apkName, expand);
            expandedLayout.setVisibility(expand ? View.VISIBLE : View.GONE);
            versionCode.setVisibility(expand ? View.VISIBLE : View.GONE);
            expandArrow.setImageDrawable(ContextCompat.getDrawable(context, expand ?
                    R.drawable.ic_expand_less_grey600 : R.drawable.ic_expand_more_grey600));

            // This is required to make these labels
            // auto-scrollable when they are too long
            version.setSelected(expand);
            repository.setSelected(expand);
            size.setSelected(expand);
            api.setSelected(expand);
        }

        private void toggleExpanded() {
            if (busyIndicator.getVisibility() == View.VISIBLE) {
                // Don't allow collapsing the view when the busy indicator
                // is shown because the APK is being downloaded and it's quite important
                return;
            }

            boolean expand = !versionsExpandTracker.get(apk.apkName);
            expand(expand);

            if (expand) {
                // Scroll the versions view to a correct position so it can show the whole item
                final LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                final int currentPosition = getAdapterPosition();
                if (currentPosition >= lm.findLastCompletelyVisibleItemPosition()) {
                    // Do it only if the item is near the bottom of current viewport
                    recyclerView.getViewTreeObserver()
                            .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                                @Override
                                public void onGlobalLayout() {
                                    // Expanded item dimensions should be already calculated at this moment
                                    // so it's possible to correctly scroll to a given position
                                    recyclerView.smoothScrollToPosition(currentPosition);
                                    recyclerView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                                }
                            });
                }
            }
        }
    }

    private void addLinkItemView(ViewGroup parent, int resIdText, int resIdDrawable, final String url) {
        addLinkItemView(parent, resIdText, resIdDrawable, url, null);
    }

    private void addLinkItemView(ViewGroup parent, int resIdText, int resIdDrawable, final String url, String formatArg) {
        TextView view = (TextView) LayoutInflater.from(parent.getContext()).inflate(R.layout.app_details2_link_item, parent, false);
        if (formatArg == null) {
            view.setText(resIdText);
        } else {
            String text = parent.getContext().getString(resIdText, formatArg);
            view.setText(text);
        }
        TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(view, resIdDrawable, 0, 0, 0);
        parent.addView(view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onLinkClicked(url);
            }
        });
    }

    private void onLinkClicked(String url) {
        if (!TextUtils.isEmpty(url)) {
            callbacks.openUrl(url);
        }
    }

    private boolean uriIsSetAndCanBeOpened(String s) {
        if (TextUtils.isEmpty(s)) {
            return false;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(s));
        return intent.resolveActivity(context.getPackageManager()) != null;
    }

    /**
     * The HTML formatter adds "\n\n" at the end of every paragraph. This
     * is desired between paragraphs, but not at the end of the whole
     * string as it adds unwanted spacing at the end of the TextView.
     * Remove all trailing newlines.
     * Use this function instead of a trim() as that would require
     * converting to String and thus losing formatting (e.g. bold).
     */
    public static CharSequence trimTrailingNewlines(CharSequence s) {
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

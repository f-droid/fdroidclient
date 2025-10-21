package org.fdroid.fdroid.views;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.Formatter;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.compose.ui.platform.ComposeView;
import androidx.compose.ui.platform.ViewCompositionStrategy;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.os.ConfigurationCompat;
import androidx.core.os.LocaleListCompat;
import androidx.core.text.HtmlCompat;
import androidx.core.text.util.LinkifyCompat;
import androidx.core.widget.TextViewCompat;
import androidx.gridlayout.widget.GridLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import org.apache.commons.io.FilenameUtils;
import org.fdroid.database.AppPrefs;
import org.fdroid.database.Repository;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.installer.Installer;
import org.fdroid.fdroid.installer.SessionInstallManager;
import org.fdroid.fdroid.privileged.views.AppDiff;
import org.fdroid.fdroid.privileged.views.AppSecurityPermissions;
import org.fdroid.fdroid.views.appdetails.AntiFeaturesListingView;
import org.fdroid.fdroid.views.appdetails.RepoChooserKt;
import org.fdroid.fdroid.views.apps.AppListActivity;
import org.fdroid.fdroid.views.main.MainActivity;
import org.fdroid.index.v2.FileV2;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@SuppressWarnings("LineLength")
public class AppDetailsRecyclerViewAdapter
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final String TAG = "AppDetailsRecyclerViewA";

    public interface AppDetailsRecyclerViewAdapterCallbacks {

        boolean isAppDownloading();

        void openUrl(String url);

        void installApk(Apk apk);

        void uninstallApk();

        void installCancel();

        void launchApk();

        void onRepoChanged(long repoId);

        void onPreferredRepoChanged(long repoId);
    }

    private static final int VIEWTYPE_HEADER = 0;
    private static final int VIEWTYPE_SCREENSHOTS = 1;
    private static final int VIEWTYPE_DONATE = 2;
    private static final int VIEWTYPE_LINKS = 3;
    private static final int VIEWTYPE_PERMISSIONS = 4;
    private static final int VIEWTYPE_VERSIONS = 5;
    private static final int VIEWTYPE_NO_VERSIONS = 6;
    private static final int VIEWTYPE_VERSIONS_LOADING = 7;
    private static final int VIEWTYPE_VERSION = 8;
    private static final int VIEWTYPE_MORE_APPS = 9;

    private final Context context;
    @Nullable
    private App app;
    private final AppDetailsRecyclerViewAdapterCallbacks callbacks;
    private RecyclerView recyclerView;
    private final List<Object> items = new ArrayList<>();
    private final List<Repository> repos = new ArrayList<>();
    @Nullable
    private Long preferredRepoId = null;
    private final List<Apk> versions = new ArrayList<>();
    private final List<Apk> compatibleVersionsDifferentSigner = new ArrayList<>();
    private boolean versionsLoading = true;
    private boolean showVersions;
    private boolean showAuthorApps;

    private HeaderViewHolder headerView;

    private Apk downloadedApk;
    @Nullable
    private Apk suggestedApk;
    private final HashMap<String, Boolean> versionsExpandTracker = new HashMap<>();

    public AppDetailsRecyclerViewAdapter(Context context, @Nullable App app, AppDetailsRecyclerViewAdapterCallbacks callbacks) {
        this.context = context;
        this.callbacks = callbacks;
        this.app = app;
        // add header early for icon transition animation
        addItem(VIEWTYPE_HEADER);
    }

    public void updateItems(@NonNull App app, @Nullable List<Apk> apks, @NonNull AppPrefs appPrefs) {
        this.app = app;
        versionsLoading = apks == null;

        items.clear();
        versions.clear();
        suggestedApk = null;

        // Get versions
        compatibleVersionsDifferentSigner.clear();
        boolean showIncompatibleVersions = Preferences.get().showIncompatibleVersions();
        if (apks != null) {
            for (final Apk apk : apks) {
                boolean allowByCompatibility = apk.compatible || showIncompatibleVersions;
                String installedSigner = app.installedSigner;
                boolean allowBySigner = installedSigner == null
                        || showIncompatibleVersions || TextUtils.equals(installedSigner, apk.signer);
                if (allowByCompatibility) {
                    compatibleVersionsDifferentSigner.add(apk);
                    if (allowBySigner) {
                        versions.add(apk);
                        if (!versionsExpandTracker.containsKey(apk.getApkPath())) {
                            versionsExpandTracker.put(apk.getApkPath(), false);
                        }
                    }
                }
            }
        }
        if (apks != null) {
            final Apk foundApk = app.findSuggestedApk(apks, appPrefs);
            // only use suggested APK, if app not installed, or signer matches installed signer
            // because otherwise, there's no use in suggesting it as we can't install it anyway
            if (app.installedSigner == null || (foundApk != null && app.installedSigner.equals(foundApk.signer))) {
                suggestedApk = foundApk;
            }
        }

        addItem(VIEWTYPE_HEADER);
        if (!app.getAllScreenshots().isEmpty()) addItem(VIEWTYPE_SCREENSHOTS);
        addItem(VIEWTYPE_DONATE);
        addItem(VIEWTYPE_LINKS);
        addItem(VIEWTYPE_PERMISSIONS);
        if (versionsLoading) {
            addItem(VIEWTYPE_VERSIONS_LOADING);
        } else if (versions.isEmpty()) {
            addItem(VIEWTYPE_NO_VERSIONS);
        } else {
            addItem(VIEWTYPE_VERSIONS);
            if (showVersions) {
                setShowVersions(true);
            }
        }
        if (showAuthorApps) {
            // item might also be added later when a repo update adds more apps by this author
            addItem(VIEWTYPE_MORE_APPS);
        }

        //noinspection NotifyDataSetChanged // too hard to know what exactly has changed
        notifyDataSetChanged();
    }

    void setRepos(List<Repository> repos, long preferredRepoId) {
        this.repos.clear();
        this.repos.addAll(repos);
        this.preferredRepoId = preferredRepoId;
        notifyItemChanged(0); // header changed
    }

    void setShowVersions(boolean showVersions) {
        setShowVersions(showVersions, false);
    }

    private void setShowVersions(boolean showVersions, boolean scrollTo) {
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
                final LinearSmoothScroller smoothScroller = getLinearSmoothScroller(startIndex);
                Objects.requireNonNull(recyclerView.getLayoutManager()).startSmoothScroll(smoothScroller);
            }
        } else if (itemsWereRemoved) {
            notifyItemRangeRemoved(startIndex, versions.size());
            if (recyclerView != null && scrollTo) {
                recyclerView.smoothScrollToPosition(startIndex - 1);
            }
        }
    }

    void setHasAuthorMoreApps(boolean showAuthorApps) {
        if (this.showAuthorApps == showAuthorApps) return;
        this.showAuthorApps = showAuthorApps;

        notifyItemChanged(0); // onclick for author in header
        if (showAuthorApps) {
            // item might be added when a repo update adds more apps by this author
            addItem(VIEWTYPE_MORE_APPS);
            notifyItemInserted(items.size() - 1);
        } else {
            // should usually be the last item...
            int index = items.indexOf(VIEWTYPE_MORE_APPS);
            if (index == -1) return;
            items.remove(index);
            notifyItemRemoved(index);
        }
    }

    @NonNull
    private LinearSmoothScroller getLinearSmoothScroller(int startIndex) {
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
        return smoothScroller;
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
        if (app == null) return false;
        // Figure out if we should show permissions section
        Apk curApk = app.installedApk == null ? suggestedApk : app.installedApk;
        final boolean curApkCompatible = curApk != null && curApk.compatible;
        return !versions.isEmpty() && (curApkCompatible || Preferences.get().showIncompatibleVersions());
    }

    private boolean shouldShowDonate() {
        if (app == null) return false;
        return uriIsSet(app.donate) ||
                uriIsSet(app.getBitcoinUri()) ||
                uriIsSet(app.getLitecoinUri()) ||
                uriIsSet(app.getFlattrUri()) ||
                uriIsSet(app.getLiberapayUri()) ||
                uriIsSet(app.getOpenCollectiveUri());
    }

    private void notifyVersionViewsChanged() {
        int startIndex = items.indexOf(VIEWTYPE_VERSIONS) + 1;
        notifyItemRangeChanged(startIndex, versions.size());
    }

    void notifyAboutDownloadedApk(final Apk apk) {
        downloadedApk = apk;
        notifyVersionViewsChanged();
    }

    void clearProgress() {
        if (headerView != null) {
            headerView.clearProgress();
        }
        if (downloadedApk != null) {
            notifyVersionViewsChanged();
            downloadedApk = null;
        }
    }

    void setIndeterminateProgress(int resIdString) {
        if (headerView != null) {
            headerView.setIndeterminateProgress(resIdString);
        }
    }

    void setProgress(long bytesDownloaded, long totalBytes) {
        if (headerView != null) {
            headerView.setProgress(bytesDownloaded, totalBytes);
        }
    }

    private void openAppListWithAuthorFilter(App app) {
        Intent intent = new Intent(context, AppListActivity.class);
        intent.putExtra(AppListActivity.EXTRA_AUTHOR_NAME, app.authorName);
        context.startActivity(intent);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return switch (viewType) {
            case VIEWTYPE_HEADER -> {
                View header = inflater.inflate(R.layout.app_details2_header, parent, false);
                yield new HeaderViewHolder(header);
            }
            case VIEWTYPE_SCREENSHOTS -> {
                View screenshots = inflater.inflate(R.layout.app_details2_screenshots, parent, false);
                yield new ScreenShotsViewHolder(screenshots);
            }
            case VIEWTYPE_DONATE -> {
                View donate = inflater.inflate(R.layout.app_details2_donate, parent, false);
                yield new DonateViewHolder(donate);
            }
            case VIEWTYPE_LINKS -> {
                View links = inflater.inflate(R.layout.app_details2_links, parent, false);
                yield new LinksViewHolder(links);
            }
            case VIEWTYPE_PERMISSIONS -> {
                View permissions = inflater.inflate(R.layout.app_details2_links, parent, false);
                yield new PermissionsViewHolder(permissions);
            }
            case VIEWTYPE_VERSIONS -> {
                View versionsView = inflater.inflate(R.layout.app_details2_links, parent, false);
                yield new VersionsViewHolder(versionsView);
            }
            case VIEWTYPE_NO_VERSIONS -> {
                View noVersionsView = inflater.inflate(R.layout.app_details2_links, parent, false);
                yield new NoVersionsViewHolder(noVersionsView);
            }
            case VIEWTYPE_VERSIONS_LOADING -> {
                View loadingView = inflater.inflate(R.layout.app_details2_loading, parent, false);
                yield new VersionsLoadingViewHolder(loadingView);
            }
            case VIEWTYPE_VERSION -> {
                View version = inflater.inflate(R.layout.app_details2_version_item, parent, false);
                yield new VersionViewHolder(version);
            }
            case VIEWTYPE_MORE_APPS -> {
                View moreApps = inflater.inflate(R.layout.app_details2_more_apps, parent, false);
                yield new MoreAppsViewHolder(moreApps);
            }
            default -> throw new IllegalStateException("Unknown view type: " + viewType);
        };
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
            case VIEWTYPE_VERSIONS_LOADING:
            case VIEWTYPE_MORE_APPS:
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
        final ComposeView repoChooserView;
        final TextView warningView;
        final TextView summaryView;
        final TextView whatsNewView;
        final TextView descriptionView;
        final Button descriptionMoreView;
        final View antiFeaturesSectionView;
        final TextView antiFeaturesLabelView;
        final View antiFeaturesWarningView;
        final AntiFeaturesListingView antiFeaturesListingView;
        final Button buttonPrimaryView;
        final Button buttonSecondaryView;
        final View progressLayout;
        final LinearProgressIndicator progressBar;
        final TextView progressLabel;
        final TextView progressPercent;
        final View progressCancel;
        boolean descriptionIsExpanded;

        HeaderViewHolder(View view) {
            super(view);
            iconView = view.findViewById(R.id.icon);
            titleView = view.findViewById(R.id.title);
            authorView = view.findViewById(R.id.author);
            lastUpdateView = view.findViewById(R.id.text_last_update);
            repoChooserView = view.findViewById(R.id.repoChooserView);
            repoChooserView.setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed.INSTANCE);
            warningView = view.findViewById(R.id.warning);
            summaryView = view.findViewById(R.id.summary);
            whatsNewView = view.findViewById(R.id.latest);
            descriptionView = view.findViewById(R.id.description);
            descriptionMoreView = view.findViewById(R.id.description_more);
            antiFeaturesSectionView = view.findViewById(R.id.anti_features_section);
            antiFeaturesLabelView = view.findViewById(R.id.label_anti_features);
            antiFeaturesWarningView = view.findViewById(R.id.anti_features_warning);
            antiFeaturesListingView = view.findViewById(R.id.anti_features_full_listing);
            buttonPrimaryView = view.findViewById(R.id.primaryButtonView);
            buttonSecondaryView = view.findViewById(R.id.secondaryButtonView);
            progressLayout = view.findViewById(R.id.progress_layout);
            progressBar = view.findViewById(R.id.progress_bar);
            progressLabel = view.findViewById(R.id.progress_label);
            progressPercent = view.findViewById(R.id.progress_percent);
            progressCancel = view.findViewById(R.id.progress_cancel);
            descriptionView.setMaxLines(MAX_LINES);
            descriptionView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            descriptionMoreView.setOnClickListener(v -> {
                TransitionManager.beginDelayedTransition(recyclerView, null);
                if (descriptionView.getMaxLines() != MAX_LINES) {
                    descriptionView.setMaxLines(MAX_LINES);
                    descriptionMoreView.setText(R.string.more);
                    descriptionIsExpanded = false;
                } else {
                    descriptionView.setMaxLines(Integer.MAX_VALUE);
                    descriptionMoreView.setText(R.string.less);
                    descriptionIsExpanded = true;
                }
                updateAntiFeaturesWarning();
            });
        }

        void clearProgress() {
            progressLayout.setVisibility(View.GONE);
            buttonPrimaryView.setVisibility(versions.isEmpty() ? View.GONE : View.VISIBLE);
            buttonSecondaryView.setVisibility(app != null && app.isUninstallable(context) ?
                    View.VISIBLE : View.GONE);
        }

        void setIndeterminateProgress(int resIdString) {
            if (!progressBar.isIndeterminate()) {
                progressBar.hide();
                progressBar.setIndeterminate(true);
            }
            progressBar.show();
            progressLayout.setVisibility(View.VISIBLE);
            buttonPrimaryView.setVisibility(View.GONE);
            buttonSecondaryView.setVisibility(View.GONE);
            progressLabel.setText(resIdString);
            progressLabel.setContentDescription(context.getString(R.string.downloading));
            progressPercent.setText("");
            if (resIdString == R.string.installing || resIdString == R.string.uninstalling) {
                progressCancel.setVisibility(View.GONE);
            } else {
                progressCancel.setVisibility(View.VISIBLE);
            }
        }

        void setProgress(long bytesDownloaded, long totalBytes) {
            progressLayout.setVisibility(View.VISIBLE);
            buttonPrimaryView.setVisibility(View.GONE);
            buttonSecondaryView.setVisibility(View.GONE);
            progressCancel.setVisibility(View.VISIBLE);

            if (totalBytes <= 0) {
                if (!progressBar.isIndeterminate()) {
                    progressBar.hide();
                    progressBar.setIndeterminate(true);
                }
            } else {
                progressBar.setProgressCompat(Utils.getPercent(bytesDownloaded, totalBytes), true);
            }
            progressBar.show();

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

        void bindModel() {
            if (app == null) return;
            Utils.setIconFromRepoOrPM(app, iconView, iconView.getContext());
            titleView.setText(app.name);
            if (!TextUtils.isEmpty(app.authorName)) {
                authorView.setText(context.getString(R.string.by_author_format, app.authorName));
                authorView.setVisibility(View.VISIBLE);
                if (showAuthorApps) {
                    authorView.setOnClickListener(v -> openAppListWithAuthorFilter(app));
                } else {
                    authorView.setOnClickListener(null);
                }
            } else {
                authorView.setVisibility(View.GONE);
            }
            if (app.lastUpdated != null) {
                Resources res = lastUpdateView.getContext().getResources();
                String lastUpdated = Utils.formatLastUpdated(res, app.lastUpdated);
                String text;
                if (Preferences.get().expertMode() && suggestedApk != null && suggestedApk.apkFile != null
                        && suggestedApk.apkFile.getSize() != null) {
                    String size = Formatter.formatFileSize(context, suggestedApk.apkFile.getSize());
                    text = lastUpdated + " (" + size + ")";
                } else {
                    text = lastUpdated;
                }
                lastUpdateView.setText(text);
                lastUpdateView.setVisibility(View.VISIBLE);
            } else {
                lastUpdateView.setVisibility(View.GONE);
            }
            if (app != null && preferredRepoId != null) {
                Set<String> defaultAddresses = Preferences.get().getDefaultRepoAddresses(context);
                Repository repo = FDroidApp.getRepoManager(context).getRepository(app.repoId);
                // show repo banner, if
                // * app is in more than one repo, or
                // * app is from a non-default repo
                if (repos.size() > 1 || (repo != null && !defaultAddresses.contains(repo.getAddress()))) {
                    RepoChooserKt.setContentRepoChooser(repoChooserView, repos, app.repoId, preferredRepoId,
                            r -> callbacks.onRepoChanged(r.getRepoId()), callbacks::onPreferredRepoChanged);
                    repoChooserView.setVisibility(View.VISIBLE);
                } else {
                    repoChooserView.setVisibility(View.GONE);
                }
            } else {
                repoChooserView.setVisibility(View.GONE);
            }

            if (suggestedApk == null && repos.size() > 1 && app.installedSigner != null && preferredRepoId != null
                    && preferredRepoId == app.repoId && !versionsLoading) {
                // current repo is preferred, app is installed, but has no suggested version from this repo
                int color = ContextCompat.getColor(context, R.color.fdroid_red);
                warningView.setBackgroundColor(color);
                warningView.setText(R.string.warning_no_compat_versions);
                warningView.setVisibility(View.VISIBLE);
            } else if (SessionInstallManager.canBeUsed(context) && suggestedApk != null
                    && !SessionInstallManager.isTargetSdkSupported(suggestedApk.targetSdkVersion)) {
                int color = MaterialColors.getColor(warningView, R.attr.warning);
                warningView.setBackgroundColor(color);
                warningView.setText(R.string.warning_target_sdk);
                warningView.setVisibility(View.VISIBLE);
            } else {
                warningView.setVisibility(View.GONE);
            }
            if (!TextUtils.isEmpty(app.summary)) {
                summaryView.setText(app.summary);
                summaryView.setVisibility(View.VISIBLE);
            } else {
                summaryView.setVisibility(View.GONE);
            }
            if (suggestedApk == null || TextUtils.isEmpty(app.whatsNew)) {
                whatsNewView.setVisibility(View.GONE);
                summaryView.setBackgroundResource(0); // make background of summary transparent
            } else {
                final LocaleListCompat localeList =
                        ConfigurationCompat.getLocales(context.getResources().getConfiguration());
                Locale locale = localeList.get(0);

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
            final Spanned desc = HtmlCompat.fromHtml(app.description, HtmlCompat.FROM_HTML_MODE_LEGACY,
                    null, new Utils.HtmlTagHandler());
            descriptionView.setMovementMethod(LinkMovementMethod.getInstance());
            descriptionView.setText(trimTrailingNewlines(desc));
            LinkifyCompat.addLinks(descriptionView, Linkify.WEB_URLS);

            if (descriptionView.getText() instanceof Spannable spannable) {
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
            descriptionView.post(() -> {
                boolean hasNoAntiFeatures = app.antiFeatures == null || app.antiFeatures.length == 0;
                if (descriptionView.getLineCount() <= HeaderViewHolder.MAX_LINES && hasNoAntiFeatures) {
                    descriptionMoreView.setVisibility(View.GONE);
                } else {
                    descriptionMoreView.setVisibility(View.VISIBLE);
                }
            });

            antiFeaturesListingView.setApp(app);
            updateAntiFeaturesWarning();

            boolean hasCompatibleVersion = false;
            for (Apk apk : versions) {
                if (apk.compatible) {
                    hasCompatibleVersion = true;
                    break;
                }
            }
            boolean showPrimaryButton = hasCompatibleVersion || app.isInstalled(context);

            buttonPrimaryView.setText(R.string.menu_install);
            buttonPrimaryView.setVisibility(showPrimaryButton ? View.VISIBLE : View.GONE);
            buttonSecondaryView.setText(R.string.menu_uninstall);
            buttonSecondaryView.setVisibility(app.isUninstallable(context) ? View.VISIBLE : View.GONE);
            buttonSecondaryView.setOnClickListener(v -> callbacks.uninstallApk());
            if (callbacks.isAppDownloading()) {
                buttonPrimaryView.setText(R.string.downloading);
                buttonPrimaryView.setEnabled(false);
                buttonPrimaryView.setVisibility(View.GONE);
                buttonSecondaryView.setVisibility(View.GONE);
                progressLayout.setVisibility(View.VISIBLE);
            } else if (!app.isInstalled(context) && suggestedApk != null) {
                // Check count > 0 due to incompatible apps resulting in an empty list.
                progressLayout.setVisibility(View.GONE);
                // Set Install button and hide second button
                buttonPrimaryView.setText(R.string.menu_install);
                buttonPrimaryView.setEnabled(true);
                buttonPrimaryView.setOnClickListener(v -> callbacks.installApk(suggestedApk));
            } else if (app.isInstalled(context)) {
                if (app.canAndWantToUpdate(suggestedApk)) {
                    buttonPrimaryView.setText(R.string.menu_upgrade);
                    buttonPrimaryView.setOnClickListener(v -> callbacks.installApk(suggestedApk));
                } else {
                    Apk mediaApk = app.getMediaApkifInstalled(context);
                    if (!context.getPackageName().equals(app.packageName) &&
                            context.getPackageManager().getLaunchIntentForPackage(app.packageName) != null) {
                        buttonPrimaryView.setText(R.string.menu_launch);
                        buttonPrimaryView.setOnClickListener(v -> callbacks.launchApk());
                    } else if (!app.isApk && mediaApk != null) {
                        final File installedFile = mediaApk.getInstalledMediaFile(context);
                        if (!installedFile.toString().startsWith(context.getApplicationInfo().dataDir)) {
                            final Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                            Uri uri = FileProvider.getUriForFile(context, Installer.AUTHORITY, installedFile);
                            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                                    FilenameUtils.getExtension(installedFile.getName()));
                            viewIntent.setDataAndType(uri, mimeType);
                            viewIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                            if (!context.getPackageManager().queryIntentActivities(viewIntent, 0).isEmpty()) {
                                buttonPrimaryView.setText(R.string.menu_open);
                                buttonPrimaryView.setOnClickListener(v -> {
                                    try {
                                        context.startActivity(viewIntent);
                                    } catch (ActivityNotFoundException e) {
                                        Log.e(TAG, "Error starting activity: ", e);
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
                progressLayout.setVisibility(View.GONE);
            } else {
                progressLayout.setVisibility(View.GONE);
            }
            progressCancel.setOnClickListener(v -> callbacks.installCancel());
            if (versionsLoading) {
                progressLayout.setVisibility(View.VISIBLE);
                progressLabel.setVisibility(View.GONE);
                progressCancel.setVisibility(View.GONE);
                progressPercent.setVisibility(View.GONE);
                progressBar.setIndeterminate(true);
                progressBar.setVisibility(View.VISIBLE);
            } else {
                progressLabel.setVisibility(View.VISIBLE);
                progressCancel.setVisibility(View.VISIBLE);
                progressPercent.setVisibility(View.VISIBLE);
            }
            // Hide primary buttons when current repo is not the preferred one.
            // This requires the user to prefer the repo first, if they want to install/update from it.
            if (preferredRepoId != null && preferredRepoId != app.repoId) {
                // we don't need to worry about making it visible, because changing current repo refreshes this view
                buttonPrimaryView.setVisibility(View.GONE);
                buttonSecondaryView.setVisibility(View.GONE);
            }
        }

        private void updateAntiFeaturesWarning() {
            if (app != null && (app.antiFeatures == null || app.antiFeatures.length == 0)) {
                antiFeaturesSectionView.setVisibility(View.GONE);
            } else if (descriptionIsExpanded) {
                antiFeaturesSectionView.setVisibility(View.VISIBLE);
                antiFeaturesWarningView.setVisibility(View.GONE);
                antiFeaturesLabelView.setVisibility(View.VISIBLE);
                antiFeaturesListingView.setVisibility(View.VISIBLE);
            } else {
                antiFeaturesSectionView.setVisibility(View.VISIBLE);
                antiFeaturesWarningView.setVisibility(View.VISIBLE);
                antiFeaturesLabelView.setVisibility(View.GONE);
                antiFeaturesListingView.setVisibility(View.GONE);
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

        ScreenShotsViewHolder(View view) {
            super(view);
            recyclerView = view.findViewById(R.id.screenshots);
            ItemDecorator itemDecorator = new ItemDecorator(context);
            recyclerView.addItemDecoration(itemDecorator);
        }

        @Override
        public void bindModel() {
            if (app == null) return;
            LinearLayoutManager lm = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
            recyclerView.setLayoutManager(lm);
            ScreenShotsRecyclerViewAdapter adapter = new ScreenShotsRecyclerViewAdapter(app, this);
            recyclerView.setAdapter(adapter);
            recyclerView.setHasFixedSize(true);
            recyclerView.setNestedScrollingEnabled(false);
        }

        @Override
        public void onScreenshotClick(int position) {
            List<FileV2> screenshots = Objects.requireNonNull(app).getAllScreenshots();
            context.startActivity(ScreenShotsActivity.getStartIntent(context, app.repoId, screenshots, position));
        }

        private static class ItemDecorator extends RecyclerView.ItemDecoration {
            private final Context context;

            ItemDecorator(Context context) {
                this.context = context.getApplicationContext();
            }

            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, RecyclerView parent,
                                       @NonNull RecyclerView.State state) {
                int position = parent.getChildAdapterPosition(view);
                int padding = (int) context.getResources().getDimension(R.dimen.details_activity_padding_screenshot);
                if (position == 0) {
                    outRect.set(padding, view.getPaddingTop(), view.getPaddingRight(), view.getPaddingBottom());
                } else {
                    outRect.set(view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(), view.getPaddingBottom());
                }
            }
        }
    }

    private class DonateViewHolder extends AppDetailsViewHolder {
        final TextView donateHeading;
        final GridLayout donationOptionsLayout;

        DonateViewHolder(View view) {
            super(view);
            donateHeading = view.findViewById(R.id.donate_header);
            donationOptionsLayout = view.findViewById(R.id.donation_options);
        }

        @Override
        public void bindModel() {
            if (app == null) return;
            if (TextUtils.isEmpty(app.authorName)) {
                donateHeading.setText(context.getString(R.string.app_details_donate_prompt_unknown_author, app.name));
            } else {
                String author = "<strong>" + app.authorName + "</strong>";
                final String prompt = context.getString(R.string.app_details_donate_prompt, app.name, author);
                donateHeading.setText(HtmlCompat.fromHtml(prompt, HtmlCompat.FROM_HTML_MODE_LEGACY));
            }

            donationOptionsLayout.removeAllViews();

            // OpenCollective
            if (uriIsSet(app.getOpenCollectiveUri())) {
                addDonateOption(R.layout.donate_opencollective, app.getOpenCollectiveUri());
            }

            // LiberaPay
            if (uriIsSet(app.getLiberapayUri())) {
                addDonateOption(R.layout.donate_liberapay, app.getLiberapayUri());
            }

            // Bitcoin
            if (uriIsSet(app.getBitcoinUri())) {
                addDonateOption(R.layout.donate_bitcoin, app.getBitcoinUri());
            }

            // Litecoin
            if (uriIsSet(app.getLitecoinUri())) {
                addDonateOption(R.layout.donate_litecoin, app.getLitecoinUri());
            }

            // Flattr
            if (uriIsSet(app.getFlattrUri())) {
                addDonateOption(R.layout.donate_generic, app.getFlattrUri());
            }

            // Donate button
            if (uriIsSet(app.donate)) {
                addDonateOption(R.layout.donate_generic, app.donate);
            }
        }

        /**
         * Show the donate button, but only if it is an HTTPS URL.  The
         * {@code https://} is then stripped off when URLs are directly displayed.
         */
        private void addDonateOption(@LayoutRes int layout, final String uri) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View option = inflater.inflate(layout, donationOptionsLayout, false);
            if (layout == R.layout.donate_generic) {
                if (!uri.toLowerCase(Locale.ENGLISH).startsWith("https://")) {
                    return;
                }
                ((TextView) option).setText(uri.substring(8));
            }
            option.setOnClickListener(v -> onLinkClicked(uri));
            donationOptionsLayout.addView(option);
        }
    }

    private abstract static class ExpandableLinearLayoutViewHolder extends AppDetailsViewHolder {
        final TextView headerView;
        final LinearLayout contentView;

        ExpandableLinearLayoutViewHolder(View view) {
            super(view);
            headerView = view.findViewById(R.id.information);
            contentView = view.findViewById(R.id.ll_content);
        }

        @DrawableRes
        protected abstract int getIcon();

        /**
         * Depending on whether we are expanded or not, update the icon which indicates whether the
         * user can expand/collapse this item.
         */
        void updateExpandableItem(boolean isExpanded) {
            headerView.setCompoundDrawablesRelativeWithIntrinsicBounds(getIcon(), 0,
                    isExpanded ? R.drawable.ic_expand_less : R.drawable.ic_expand_more, 0);
        }
    }

    private class VersionsViewHolder extends ExpandableLinearLayoutViewHolder {

        VersionsViewHolder(View view) {
            super(view);
        }

        @Override
        public void bindModel() {
            itemView.setOnClickListener(v -> {
                setShowVersions(!showVersions, true);
                updateExpandableItem(showVersions);
            });
            headerView.setText(R.string.versions);
            updateExpandableItem(showVersions);
        }

        @Override
        @DrawableRes
        protected int getIcon() {
            return R.drawable.ic_versions;
        }
    }

    private class NoVersionsViewHolder extends AppDetailsViewHolder {
        final TextView headerView;

        NoVersionsViewHolder(View view) {
            super(view);
            headerView = view.findViewById(R.id.information);
            headerView.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_versions, 0, 0, 0);
            TextViewCompat.setCompoundDrawableTintList(headerView, ColorStateList.valueOf(Color.parseColor("#B4B4B4")));
            itemView.setOnClickListener(v -> explainIncompatibleVersions());
        }

        @Override
        public void bindModel() {
            Context context = headerView.getContext();
            if (hasCompatibleApksDifferentSigners()) {
                headerView.setText(context.getString(R.string.app_details__no_versions__no_compatible_signers));
            } else {
                headerView.setText(context.getString(R.string.app_details__no_versions__none_compatible_with_device));
            }
        }

        /**
         * Show a dialog to the user explaining the reasons there are no compatible versions.
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
            if (hasCompatibleApksDifferentSigners()) {
                title = context.getString(R.string.app_details__no_versions__no_compatible_signers);
                message = context.getString(R.string.app_details__no_versions__explain_incompatible_signers) +
                        "\n\n" + showIncompatible;
            } else {
                title = context.getString(R.string.app_details__no_versions__none_compatible_with_device);
                message = showIncompatible;
            }

            new MaterialAlertDialogBuilder(context)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(R.string.menu_settings, (dialog, which) -> {
                        Intent intent = new Intent(context, MainActivity.class);
                        intent.putExtra(MainActivity.EXTRA_VIEW_SETTINGS, true);
                        context.startActivity(intent);
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        }

        private boolean hasCompatibleApksDifferentSigners() {
            return !compatibleVersionsDifferentSigner.isEmpty();
        }
    }

    private static class VersionsLoadingViewHolder extends AppDetailsViewHolder {
        VersionsLoadingViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        public void bindModel() {
        }
    }

    private class PermissionsViewHolder extends ExpandableLinearLayoutViewHolder {

        PermissionsViewHolder(View view) {
            super(view);
        }

        @Override
        public void bindModel() {
            itemView.setOnClickListener(v -> {
                boolean shouldBeVisible = contentView.getVisibility() != View.VISIBLE;
                contentView.setVisibility(shouldBeVisible ? View.VISIBLE : View.GONE);
                updateExpandableItem(shouldBeVisible);
                final LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (shouldBeVisible && recyclerView != null && layoutManager != null) {
                    layoutManager.scrollToPositionWithOffset(items.indexOf(VIEWTYPE_PERMISSIONS), 0);
                }
            });
            headerView.setText(R.string.permissions);
            updateExpandableItem(false);
            contentView.removeAllViews();
            if (!versions.isEmpty()) {
                AppDiff appDiff = new AppDiff(context, versions.get(0));
                AppSecurityPermissions perms = new AppSecurityPermissions(context, appDiff.apkPackageInfo);
                contentView.addView(perms.getPermissionsView(AppSecurityPermissions.WHICH_ALL));
            }
        }

        @Override
        @DrawableRes
        protected int getIcon() {
            return R.drawable.ic_lock;
        }
    }

    private class LinksViewHolder extends ExpandableLinearLayoutViewHolder {

        LinksViewHolder(View view) {
            super(view);
        }

        @Override
        public void bindModel() {
            if (app == null) return;
            itemView.setOnClickListener(v -> {
                boolean shouldBeVisible = contentView.getVisibility() != View.VISIBLE;
                contentView.setVisibility(shouldBeVisible ? View.VISIBLE : View.GONE);
                updateExpandableItem(shouldBeVisible);
                final LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (shouldBeVisible && recyclerView != null && layoutManager != null) {
                    layoutManager.scrollToPositionWithOffset(items.indexOf(VIEWTYPE_LINKS), 0);
                }
            });
            headerView.setText(R.string.links);
            updateExpandableItem(false);
            contentView.removeAllViews();

            // License link
            if (!TextUtils.isEmpty(app.license)) {
                String url;
                if (app.license.equals("PublicDomain")) {
                    url = "https://en.wikipedia.org/wiki/Public_domain";
                } else {
                    url = "https://spdx.org/licenses/" + app.license + ".html";
                }
                if (uriIsSet(url)) {
                    addLinkItemView(contentView, R.string.menu_license, R.drawable.ic_license, url, app.license);
                }
            }

            // Video link
            if (uriIsSet(app.video)) {
                addLinkItemView(contentView, R.string.menu_video, R.drawable.ic_video, app.video);
            }

            // Source button
            if (uriIsSet(app.sourceCode)) {
                addLinkItemView(contentView, R.string.menu_source, R.drawable.ic_source_code, app.sourceCode);
            }

            // Issues button
            if (uriIsSet(app.issueTracker)) {
                addLinkItemView(contentView, R.string.menu_issues, R.drawable.ic_error, app.issueTracker);
            }

            // Translation button
            if (uriIsSet(app.translation)) {
                addLinkItemView(contentView, R.string.menu_translation, R.drawable.ic_translation, app.translation);
            }

            // Changelog button
            if (uriIsSet(app.changelog)) {
                addLinkItemView(contentView, R.string.menu_changelog, R.drawable.ic_changelog, app.changelog);
            }

            // Website button
            if (uriIsSet(app.webSite)) {
                addLinkItemView(contentView, R.string.menu_website, R.drawable.ic_website, app.webSite);
            }

            // Email button
            final String subject = Uri.encode(context.getString(R.string.app_details_subject, app.name));
            String emailUrl = app.authorEmail == null ? null : ("mailto:" + app.authorEmail + "?subject=" + subject);
            if (uriIsSet(emailUrl)) {
                addLinkItemView(contentView, R.string.menu_email, R.drawable.ic_email, emailUrl);
            }
        }

        @Override
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
        final TextView size;
        final TextView api;
        final Button buttonInstallUpgrade;
        final View busyIndicator;
        final TextView incompatibleReasons;
        final TextView targetArch;

        private Apk apk;

        VersionViewHolder(View view) {
            super(view);
            version = view.findViewById(R.id.version);
            statusInstalled = view.findViewById(R.id.status_installed);
            statusSuggested = view.findViewById(R.id.status_suggested);
            statusIncompatible = view.findViewById(R.id.status_incompatible);
            versionCode = view.findViewById(R.id.versionCode);
            added = view.findViewById(R.id.added);
            expandArrow = view.findViewById(R.id.expand_arrow);
            expandedLayout = view.findViewById(R.id.expanded_layout);
            size = view.findViewById(R.id.size);
            api = view.findViewById(R.id.api);
            buttonInstallUpgrade = view.findViewById(R.id.button_install_upgrade);
            busyIndicator = view.findViewById(R.id.busy_indicator);
            incompatibleReasons = view.findViewById(R.id.incompatible_reasons);
            targetArch = view.findViewById(R.id.target_arch);

            int margin = context.getResources().getDimensionPixelSize(R.dimen.layout_horizontal_margin);
            int padding = context.getResources().getDimensionPixelSize(R.dimen.details_activity_padding);
            view.setPaddingRelative(margin + padding + view.getPaddingStart(), view.getPaddingTop(), margin + view.getPaddingEnd(), view.getPaddingBottom());
        }

        void bindModel(final Apk apk) {
            if (app == null) return;
            this.apk = apk;

            boolean isAppInstalled = app.isInstalled(context);
            boolean isApkInstalled = apk.versionCode == app.installedVersionCode &&
                    TextUtils.equals(apk.signer, app.installedSigner);
            boolean isApkSuggested = apk.equals(suggestedApk);
            boolean isApkDownloading = callbacks.isAppDownloading() && downloadedApk != null &&
                    downloadedApk.compareTo(apk) == 0 &&
                    TextUtils.equals(apk.getApkPath(), downloadedApk.getApkPath());
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

            size.setText(context.getString(R.string.app_size, Utils.getFriendlySize(apk.size)));
            api.setText(getApiText(apk));

            // Figuring out whether to show Install or Update button
            buttonInstallUpgrade.setVisibility(View.GONE);
            buttonInstallUpgrade.setText(context.getString(R.string.menu_install));
            showActionButton(buttonInstallUpgrade, isApkInstalled, isApkDownloading);
            if (isAppInstalled && !isApkInstalled) {
                if (apk.versionCode > app.installedVersionCode) {
                    // Change the label to indicate that pressing this
                    // button will result in updating the installed app
                    buttonInstallUpgrade.setText(R.string.menu_upgrade);
                } else if (apk.versionCode < app.installedVersionCode) {
                    buttonInstallUpgrade.setVisibility(View.GONE);
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
            Boolean expandedVersion = versionsExpandTracker.get(apk.getApkPath());
            expand(Boolean.TRUE.equals(expandedVersion) || isApkDownloading);

            // Toggle expanded view when clicking the whole version item,
            // unless it's an installed app version dummy item - it doesn't
            // contain any meaningful info, so there is no reason to expand it.
            if (!isApkInstalledDummy) {
                expandArrow.setAlpha(1f);
                itemView.setOnClickListener(v -> toggleExpanded());
            } else {
                expandArrow.setAlpha(0.3f);
                itemView.setOnClickListener(null);
            }
            // Copy version name to clipboard when long clicking the whole version item
            if (apk.versionName != null) {
                itemView.setOnLongClickListener(v -> {
                    Utils.copyToClipboard(context, app.name, apk.versionName);
                    return true;
                });
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
                Objects.requireNonNull(app);
                if (app.installedSigner != null
                        && !TextUtils.equals(app.installedSigner, apk.signer)) {
                    return context.getString(R.string.app_details__incompatible_mismatched_signers);
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
            if (isApkDownloading) {
                // Don't show the button in this case
                // as the busy indicator will take its place
                button.setVisibility(View.GONE);
            } else {
                // The button should be shown but it should be also disabled
                // if either the APK isn't compatible or it's already installed
                // or also when some other APK is currently being downloaded
                button.setVisibility(View.VISIBLE);
                boolean buttonActionDisabled = !apk.compatible || isApkInstalled ||
                        callbacks.isAppDownloading();
                button.setEnabled(!buttonActionDisabled);
                button.setAlpha(buttonActionDisabled ? 0.15f : 1f);
                button.setOnClickListener(v -> callbacks.installApk(apk));
            }
        }

        private void expand(boolean expand) {
            versionsExpandTracker.put(apk.getApkPath(), expand);
            expandedLayout.setVisibility(expand ? View.VISIBLE : View.GONE);
            versionCode.setVisibility(expand ? View.VISIBLE : View.GONE);
            expandArrow.setImageDrawable(ContextCompat.getDrawable(context, expand ?
                    R.drawable.ic_expand_less : R.drawable.ic_expand_more));

            // This is required to make these labels
            // auto-scrollable when they are too long
            version.setSelected(expand);
            size.setSelected(expand);
            api.setSelected(expand);
        }

        private void toggleExpanded() {
            if (busyIndicator.getVisibility() == View.VISIBLE) {
                // Don't allow collapsing the view when the busy indicator
                // is shown because the APK is being downloaded and it's quite important
                return;
            }

            boolean expand = Boolean.FALSE.equals(versionsExpandTracker.get(apk.getApkPath()));
            expand(expand);

            if (expand) {
                // Scroll the versions view to a correct position so it can show the whole item
                final LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                final int currentPosition = getBindingAdapterPosition();
                if (lm != null && currentPosition >= lm.findLastCompletelyVisibleItemPosition()) {
                    // Do it only if the item is near the bottom of current viewport
                    recyclerView.getViewTreeObserver()
                            .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                                @Override
                                public void onGlobalLayout() {
                                    // Expanded item dimensions should be already calculated at this moment
                                    // so it's possible to correctly scroll to a given position
                                    recyclerView.smoothScrollToPosition(currentPosition);
                                    recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                }
                            });
                }
            }
        }
    }

    private class MoreAppsViewHolder extends AppDetailsViewHolder {
        final MaterialButton appsByAuthor;

        MoreAppsViewHolder(View view) {
            super(view);
            appsByAuthor = view.findViewById(R.id.apps_by_author);
        }

        @Override
        public void bindModel() {
            if (app == null) return;
            if (TextUtils.isEmpty(app.authorName)) {
                appsByAuthor.setVisibility(View.GONE);
            } else {
                appsByAuthor.setVisibility(View.VISIBLE);
                appsByAuthor.setOnClickListener(v -> openAppListWithAuthorFilter(app));
                appsByAuthor.setText(context.getString(R.string.app_details_more_apps_by_author, app.authorName));
            }

        }
    }

    private void addLinkItemView(ViewGroup parent, int resIdText, int resIdDrawable, final String url) {
        addLinkItemView(parent, resIdText, resIdDrawable, url, null);
    }

    private void addLinkItemView(ViewGroup parent, int resIdText, int resIdDrawable, final String url, String formatArg) {
        TextView view = (TextView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.app_details2_link_item, parent, false);
        final String text;
        if (formatArg == null) {
            text = parent.getContext().getString(resIdText);
        } else {
            text = parent.getContext().getString(resIdText, formatArg);
        }
        view.setText(text);
        view.setCompoundDrawablesRelativeWithIntrinsicBounds(resIdDrawable, 0, 0, 0);
        parent.addView(view);
        view.setOnClickListener(v -> onLinkClicked(url));
        view.setOnLongClickListener(v -> {
            Utils.copyToClipboard(context, text, url, R.string.copied_url_to_clipboard);
            return true;
        });
    }

    private void onLinkClicked(String url) {
        if (!TextUtils.isEmpty(url)) {
            callbacks.openUrl(url);
        }
    }

    private boolean uriIsSet(String s) {
        return !TextUtils.isEmpty(s);
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

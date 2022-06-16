package org.fdroid.fdroid.data;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;

import org.fdroid.database.AppListItem;
import org.fdroid.database.Repository;
import org.fdroid.database.UpdatableApp;
import org.fdroid.download.DownloadRequest;
import org.fdroid.download.Mirror;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.net.TreeUriDownloader;
import org.fdroid.index.v2.FileV2;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import info.guardianproject.netcipher.NetCipher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.ConfigurationCompat;
import androidx.core.os.LocaleListCompat;

/**
 * Represents an application, its availability, and its current installed state.
 * This represents the app in general, for a specific version of this app, see
 * {@link Apk}.
 * <p>
 * <b>Do not rename these instance variables without careful consideration!</b>
 * They are mapped to JSON field names, the {@code fdroidserver} internal variable
 * names, and the {@code fdroiddata} YAML field names.  Only the instance variables
 * decorated with {@code @JsonIgnore} are not directly mapped.
 *
 * @see <a href="https://gitlab.com/fdroid/fdroiddata">fdroiddata</a>
 * @see <a href="https://gitlab.com/fdroid/fdroidserver">fdroidserver</a>
 */
public class App implements Comparable<App>, Parcelable {

    private static final String TAG = "App";

    /**
     * {@link LocaleListCompat} for finding the right app description material.
     * It is set globally static to a) cache this value, since there are thousands
     * of {@link App} entries, and b) make it easy to test}
     */
    public static LocaleListCompat systemLocaleList;

    public static LocaleListCompat getLocales() {
        LocaleListCompat cached = systemLocaleList;
        if (cached == null) {
            cached = ConfigurationCompat.getLocales(Resources.getSystem().getConfiguration());
            systemLocaleList = cached;
        }
        return cached;
    }

    // these properties are not from the index metadata, but represent the state on the device
    /**
     * True if compatible with the device (i.e. if at least one apk is)
     */
    public boolean compatible;
    public Apk installedApk; // might be null if not installed
    public String installedSig;
    public int installedVersionCode;
    public String installedVersionName;
    private long id;
    public org.fdroid.database.AppPrefs prefs;
    public String preferredSigner;
    public boolean isApk;

    /**
     * Has this {@code App} been localized into one of the user's current locales.
     */
    boolean isLocalized;

    /**
     * This is primarily for the purpose of saving app metadata when parsing an index.xml file.
     * At most other times, we don't particularly care which repo an {@link App} object came from.
     * It is pretty much transparent, because the metadata will be populated from the repo with
     * the highest priority. The UI doesn't care normally _which_ repo provided the metadata.
     * This is required for getting the full URL to the various graphics and screenshots.
     */
    public long repoId;

    // the remaining properties are set directly from the index metadata
    public String packageName = "unknown";
    public String name = "Unknown";

    public String summary = "Unknown application";
    public String iconFromApk;

    public String description;

    /**
     * A descriptive text for what has changed in this version.
     */
    public String whatsNew;

    public String featureGraphic;
    public String promoGraphic;
    public String tvBanner;

    public String[] phoneScreenshots = new String[0];
    public String[] sevenInchScreenshots = new String[0];
    public String[] tenInchScreenshots = new String[0];
    public String[] tvScreenshots = new String[0];
    public String[] wearScreenshots = new String[0];

    public String license;

    public String authorName;
    public String authorEmail;

    public String webSite;

    public String issueTracker;

    public String sourceCode;

    public String translation;

    public String video;

    public String changelog;

    public String donate;

    public String bitcoin;

    public String litecoin;

    public String flattrID;

    public String liberapay;

    public String openCollective;

    /**
     * This matches {@code CurrentVersion} in build metadata files.
     *
     * @see <a href="https://f-droid.org/docs/Build_Metadata_Reference/#CurrentVersion">CurrentVersion</a>
     */
    @Deprecated
    public String suggestedVersionName;

    /**
     * This matches {@code CurrentVersionCode} in build metadata files. Java
     * inits {@code int}s to 0.  Since it is valid to have a negative Version
     * Code, this is inited to {@link Integer#MIN_VALUE};
     *
     * @see <a href="https://f-droid.org/docs/Build_Metadata_Reference/#CurrentVersionCode">CurrentVersionCode</a>
     */
    @Deprecated
    public int suggestedVersionCode = Integer.MIN_VALUE;

    /**
     * Unlike other public fields, this is only accessible via a getter, to
     * emphasise that setting it wont do anything.
     */
    private String autoInstallVersionName;

    /**
     * The version that will be automatically installed if the user does not
     * choose a specific version.
     * TODO this should probably be converted to init to {@link Integer#MIN_VALUE} like {@link #suggestedVersionCode}
     */
    public int autoInstallVersionCode;

    public Date added;
    public Date lastUpdated;

    /**
     * List of categories (as defined in the metadata documentation) or null if there aren't any.
     * This is only populated when parsing a repository. If you need to know about the categories
     * an app is in any other part of F-Droid, use the database.
     */
    public String[] categories;

    /**
     * List of anti-features (as defined in the metadata documentation) or null if there aren't any.
     */
    @Nullable
    public String[] antiFeatures;

    /**
     * Requires root access (only ever used for root)
     */
    @Nullable
    @Deprecated
    public String[] requirements;

    /**
     * URL to download the app's icon. (Set only from localized block, see also
     * {@link #iconFromApk} and {@link #getIconPath(Context)} (Context)}
     */
    public String iconUrl;

    @Override
    public int compareTo(@NonNull App app) {
        return name.compareToIgnoreCase(app.name);
    }

    public App() {
    }

    public App(final UpdatableApp app) {
        id = 0;
        repoId = app.getUpdate().getRepoId();
        setPackageName(app.getPackageName());
        name = app.getName() == null ? "" : app.getName();
        summary = app.getSummary() == null ? "" : app.getSummary();
        installedVersionCode = (int) app.getInstalledVersionCode();
        autoInstallVersionCode = (int) app.getUpdate().getManifest().getVersionCode();
        FileV2 icon = app.getIcon(getLocales());
        iconUrl = icon == null ? null : icon.getName();
        iconFromApk = icon == null ? null : icon.getName();
    }

    public App(final org.fdroid.database.App app, @Nullable PackageInfo packageInfo) {
        id = 0;
        repoId = app.getRepoId();
        compatible = app.getMetadata().isCompatible();
        setPackageName(app.getPackageName());
        name = app.getName() == null ? "" : app.getName();
        summary = app.getSummary() == null ? "" : app.getSummary();
        String desc = app.getDescription(getLocales());
        setDescription(desc == null ? "" : desc);
        license = app.getMetadata().getLicense();
        authorName = app.getMetadata().getAuthorName();
        authorEmail = app.getMetadata().getAuthorEmail();
        webSite = app.getMetadata().getWebSite();
        issueTracker = app.getMetadata().getIssueTracker();
        sourceCode = app.getMetadata().getSourceCode();
        translation = app.getMetadata().getTranslation();
        video = app.getVideo(getLocales());
        changelog = app.getMetadata().getChangelog();
        List<String> donateList = app.getMetadata().getDonate();
        if (donateList != null && !donateList.isEmpty()) {
            donate = donateList.get(0);
        }
        bitcoin = app.getMetadata().getBitcoin();
        litecoin = app.getMetadata().getLitecoin();
        flattrID = app.getMetadata().getFlattrID();
        liberapay = app.getMetadata().getLiberapay();
        openCollective = app.getMetadata().getBitcoin();
        preferredSigner = app.getMetadata().getPreferredSigner();
        added = new Date(app.getMetadata().getAdded());
        lastUpdated = new Date(app.getMetadata().getLastUpdated());
        FileV2 icon = app.getIcon(getLocales());
        iconUrl = icon == null ? null : icon.getName();
        iconFromApk = icon == null ? null : icon.getName();
        FileV2 featureGraphic = app.getFeatureGraphic(getLocales());
        this.featureGraphic = featureGraphic == null ? null : featureGraphic.getName();
        FileV2 promoGraphic = app.getPromoGraphic(getLocales());
        this.promoGraphic = promoGraphic == null ? null : promoGraphic.getName();
        FileV2 tvBanner = app.getPromoGraphic(getLocales());
        this.tvBanner = tvBanner == null ? null : tvBanner.getName();
        List<FileV2> phoneFiles = app.getPhoneScreenshots(getLocales());
        phoneScreenshots = new String[phoneFiles.size()];
        for (int i = 0; i < phoneFiles.size(); i++) {
            phoneScreenshots[i] = phoneFiles.get(i).getName();
        }
        List<FileV2> sevenInchFiles = app.getSevenInchScreenshots(getLocales());
        sevenInchScreenshots = new String[sevenInchFiles.size()];
        for (int i = 0; i < sevenInchFiles.size(); i++) {
            phoneScreenshots[i] = sevenInchFiles.get(i).getName();
        }
        List<FileV2> tenInchFiles = app.getTenInchScreenshots(getLocales());
        tenInchScreenshots = new String[tenInchFiles.size()];
        for (int i = 0; i < tenInchFiles.size(); i++) {
            phoneScreenshots[i] = tenInchFiles.get(i).getName();
        }
        List<FileV2> tvFiles = app.getTvScreenshots(getLocales());
        tvScreenshots = new String[tvFiles.size()];
        for (int i = 0; i < tvFiles.size(); i++) {
            phoneScreenshots[i] = tvFiles.get(i).getName();
        }
        List<FileV2> wearFiles = app.getWearScreenshots(getLocales());
        wearScreenshots = new String[wearFiles.size()];
        for (int i = 0; i < wearFiles.size(); i++) {
            phoneScreenshots[i] = wearFiles.get(i).getName();
        }
        setInstalled(packageInfo);
    }

    public App(AppListItem item) {
        repoId = item.getRepoId();
        setPackageName(item.getPackageName());
        name = item.getName() == null ? "" : item.getName();
        summary = item.getSummary() == null ? "" : item.getSummary();
        FileV2 iconFile = item.getIcon(getLocales());
        iconFromApk = iconFile == null ? null : iconFile.getName();
        installedVersionCode = item.getInstalledVersionCode() == null ? 0 : item.getInstalledVersionCode().intValue();
        installedVersionName = item.getInstalledVersionName();
        antiFeatures = item.getAntiFeatureKeys().toArray(new String[0]);
        compatible = item.isCompatible();
    }

    public void setInstalled(@Nullable PackageInfo packageInfo) {
        installedVersionCode = packageInfo == null ? 0 : packageInfo.versionCode;
        installedVersionName = packageInfo == null ? null : packageInfo.versionName;
        installedSig = packageInfo == null ? null : Utils.getPackageSigner(packageInfo);
    }

    /**
     * Updates this App instance with information from the APKs.
     *
     * @param apks The APKs available for this app.
     */
    public void update(Context context, List<Apk> apks, org.fdroid.database.AppPrefs appPrefs) {
        this.prefs = appPrefs;
        for (Apk apk: apks) {
            boolean apkIsInstalled = (apk.versionCode == installedVersionCode &&
                    TextUtils.equals(apk.sig, installedSig)) || (!apk.isApk() && apk.isMediaInstalled(context));
            if (apkIsInstalled) {
                installedApk = apk;
                installedVersionCode = (int) apk.versionCode;
                installedVersionName = apk.versionName;
                break;
            }
        }
        Apk apk = findSuggestedApk(apks, appPrefs);
        if (apk == null) return;
        // update the autoInstallVersionCode, if needed
        if (autoInstallVersionCode <= 0 && installedVersionCode < apk.versionCode) {
            // FIXME versionCode is a long nowadays
            autoInstallVersionCode = (int) apk.versionCode;
            autoInstallVersionName = apk.versionName;
        }
        antiFeatures = apk.antiFeatures;
        whatsNew = apk.whatsNew;
        isApk = apk.isApk();
    }

    /**
     * In order to format all in coming descriptions before they are written
     * out to the database and used elsewhere, this is needed to intercept
     * the setting of {@link App#description} to insert the format method.
     */
    private void setDescription(String description) { // NOPMD
        this.description = formatDescription(description);
    }

    /**
     * Set the Package Name property while ensuring it is sanitized.
     */
    void setPackageName(String packageName) {
        if (Utils.isSafePackageName(packageName)) {
            this.packageName = packageName;
        } else {
            throw new IllegalArgumentException("Repo index app entry includes unsafe packageName: '"
                    + packageName + "'");
        }
    }

    /**
     * Returns the app description text with all newlines replaced by {@code <br>}
     */
    public static String formatDescription(String description) {
        return description.replace("\n", "<br>");
    }

    /**
     * Get the URL with the standard path for displaying in a browser.
     */
    @Nullable
    public Uri getShareUri() {
        Repository repo = FDroidApp.getRepo(repoId);
        if (repo == null || repo.getWebBaseUrl() == null) return null;
        return Uri.parse(repo.getWebBaseUrl()).buildUpon()
                .path(packageName)
                .build();
    }

    public RequestBuilder<Drawable> loadWithGlide(Context context) {
        Repository repo = FDroidApp.getRepo(repoId);
        if (repo == null) { // This is also used for apps that do not have a repo
            return Glide.with(context).load((Drawable) null);
        }
        if (repo.getAddress().startsWith("content://")) {
            String sb = repo.getAddress() + TreeUriDownloader.ESCAPED_SLASH + getIconPath(context);
            return Glide.with(context).load(sb);
        } else if (repo.getAddress().startsWith("file://")) {
            return Glide.with(context).load(getIconPath(context));
        } else {
            String path = getIconPath(context);
            return Glide.with(context).load(getDownloadRequest(repo, path));
        }
    }

    @Nullable
    public DownloadRequest getIconDownloadRequest(Context context) {
        String path = getIconPath(context);
        return getDownloadRequest(repoId, path);
    }

    @Nullable
    public DownloadRequest getFeatureGraphicDownloadRequest() {
        if (TextUtils.isEmpty(featureGraphic)) {
            return null;
        }
        String path = featureGraphic;
        return getDownloadRequest(repoId, path);
    }

    @Nullable
    public static DownloadRequest getDownloadRequest(long repoId, @Nullable String path) {
        if (path == null) return null;
        Repository repo = FDroidApp.getRepo(repoId);
        if (repo == null) return null;
        return getDownloadRequest(repo, path);
    }

    @Nullable
    public static DownloadRequest getDownloadRequest(@NonNull Repository repo, @Nullable String path) {
        if (path == null) return null;
        List<Mirror> mirrors = repo.getMirrors();
        return new DownloadRequest(path, mirrors, NetCipher.getProxy(), null, null);
    }

    public String getIconPath(Context context) {
        String path;
        if (TextUtils.isEmpty(iconUrl)) {
            if (TextUtils.isEmpty(iconFromApk)) {
                return null;
            }
            if (iconFromApk.endsWith(".xml")) {
                // We cannot use xml resources as icons. F-Droid server should not include them
                // https://gitlab.com/fdroid/fdroidserver/issues/344
                return null;
            }
            String iconsDir = Utils.getIconsDir(context, 1.0);
            path = getPath(iconsDir, iconFromApk);
        } else {
            path = iconUrl;
        }
        return path;
    }

    /**
     * Gets the path relative to the repo root.
     * Can be used to create URLs for use with mirrors.
     * Attention: This does NOT encode for use in URLs.
     */
    public static String getPath(String... pathElements) {
        /* Each String in pathElements might contain a /, should keep these as path elements */
        ArrayList<String> elements = new ArrayList<>();
        for (String element : pathElements) {
            Collections.addAll(elements, element.split("/"));
        }
        // build up path WITHOUT encoding the segments, this will happen later when turned into URL
        StringBuilder sb = new StringBuilder();
        for (String element : elements) {
            sb.append(element).append("/");
        }
        sb.deleteCharAt(sb.length() - 1); // remove trailing slash
        return sb.toString();
    }

    public ArrayList<String> getAllScreenshots() {
        ArrayList<String> list = new ArrayList<>();
        if (phoneScreenshots != null) {
            Collections.addAll(list, phoneScreenshots);
        }
        if (sevenInchScreenshots != null) {
            Collections.addAll(list, sevenInchScreenshots);
        }
        if (tenInchScreenshots != null) {
            Collections.addAll(list, tenInchScreenshots);
        }
        if (tvScreenshots != null) {
            Collections.addAll(list, tvScreenshots);
        }
        if (wearScreenshots != null) {
            Collections.addAll(list, wearScreenshots);
        }
        return list;
    }

    /**
     * Get the directory where APK Expansion Files aka OBB files are stored for the app as
     * specified by {@code packageName}.
     *
     * @see <a href="https://developer.android.com/google/play/expansion-files.html">APK Expansion Files</a>
     */
    public static File getObbDir(String packageName) {
        return new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/Android/obb/" + packageName);
    }

    /**
     * Attempts to find the installed {@link Apk} in the given list of APKs. If not found, will lookup the
     * the details of the installed app and use that to instantiate an {@link Apk} to be returned.
     * <p>
     * Cases where an {@link Apk} will not be found in the database and for which we fall back to
     * the {@link PackageInfo} include:
     * <li>System apps which are provided by a repository, but for which the version code bundled
     * with the system is not included in the repository.</li>
     * <li>Regular apps from a repository, where the installed version is old enough that it is no
     * longer available in the repository.</li>
     */
    @Nullable
    public Apk getInstalledApk(Context context, List<Apk> apks) {
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(packageName, 0);
            // If we are here, the package is actually installed, so we better find something
            Apk foundApk = null;
            for (Apk apk : apks) {
                if (apk.versionCode == pi.versionCode) {
                    foundApk = apk;
                    break;
                }
            }
            if (foundApk == null) foundApk = new Apk(pi);
            return foundApk;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    public boolean isInstalled(Context context) {
        // First check isApk() before isMediaInstalled() because the latter is quite expensive,
        // hitting the database for each apk version, then the disk to check for installed media.
        return installedVersionCode > 0 || (!isApk() && isMediaInstalled(context));
    }

    private boolean isApk() {
        return isApk;
    }

    public boolean isMediaInstalled(Context context) {
        return getMediaApkifInstalled(context) != null;
    }

    /**
     * Gets the installed media apk from all the apks of this {@link App}, if any.
     *
     * @return The installed media {@link Apk} if it exists, null otherwise.
     */
    public Apk getMediaApkifInstalled(Context context) {
        if (this.installedApk != null && !this.installedApk.isApk() && this.installedApk.isMediaInstalled(context)) {
            return this.installedApk;
        }
        return null;
    }

    /**
     * True if there are new versions (apks) available
     */
    @Deprecated
    public boolean hasUpdates() {
        boolean updates = false;
        if (autoInstallVersionCode > 0) {
            updates = installedVersionCode > 0 && installedVersionCode < autoInstallVersionCode;
        }
        return updates;
    }

    /**
     * True if there are new versions (apks) available
     */
    public boolean hasUpdates(List<Apk> sortedApks, org.fdroid.database.AppPrefs appPrefs) {
        Apk suggestedApk = findSuggestedApk(sortedApks, appPrefs);
        boolean updates = false;
        if (suggestedApk != null) {
            updates = installedVersionCode > 0 && installedVersionCode < suggestedApk.versionCode;
        }
        return updates;
    }

    @Nullable
    public Apk findSuggestedApk(List<Apk> apks, org.fdroid.database.AppPrefs appPrefs) {
        String releaseChannel;
        if (appPrefs.getReleaseChannels().contains(Apk.RELEASE_CHANNEL_BETA)) {
            releaseChannel = Apk.RELEASE_CHANNEL_BETA;
        } else {
            releaseChannel = Preferences.get().getReleaseChannel();
        }
        return findSuggestedApk(apks, releaseChannel);
    }

    /**
     * Finds the APK we suggest to install.
     * @param apks a list of APKs sorted by version code (highest first).
     * @param releaseChannel the key of the release channel to be considered.
     * @return The Apk we suggest to install or null, if we didn't find any.
     */
    @Nullable
    public Apk findSuggestedApk(List<Apk> apks, String releaseChannel) {
        final String mostAppropriateSignature = getMostAppropriateSignature();
        Apk apk = null;
        for (Apk a : apks) {
            // only consider compatible APKs
            if (!a.compatible) continue;
            // if we have a signature, but it doesn't match, don't use this APK
            if (mostAppropriateSignature != null && !a.sig.equals(mostAppropriateSignature)) continue;
            // if the signature matches and we want the highest version code, take this as list is sorted.
            if (a.releaseChannels.contains(releaseChannel)) {
                apk = a;
                break;
            }
        }
        // use the first of the list, before we don't choose anything
        if (apk == null && apks.size() > 0) {
            apk = apks.get(0);
        }
        return apk;
    }

    /**
     * True if there are new versions (apks) available and the user wants to be notified about them
     */
    public boolean canAndWantToUpdate(@Nullable Apk suggestedApk) {
        if (suggestedApk == null) return false;
        if (installedVersionCode >= suggestedApk.versionCode) return false;
        final org.fdroid.database.AppPrefs prefs = this.prefs;
        return prefs == null || !prefs.shouldIgnoreUpdate(autoInstallVersionCode);
    }

    /**
     * @return if the given app should be filtered out based on the
     * {@link Preferences#PREF_SHOW_ANTI_FEATURES Show Anti-Features Setting}
     */
    public boolean isDisabledByAntiFeatures(Context context) {
        if (this.antiFeatures == null) {
            return false;
        }

        List<String> chooseableAntiFeatures = Arrays.asList(
                context.getResources().getStringArray(R.array.antifeaturesValues)
        );

        Set<String> shownAntiFeatures = Preferences.get().showAppsWithAntiFeatures();

        for (String antiFeature : this.antiFeatures) {
            if (chooseableAntiFeatures.contains(antiFeature)) {
                if (!shownAntiFeatures.contains(antiFeature)) {
                    return true;
                }
            } else {
                if (!shownAntiFeatures.contains(context.getResources().getString(R.string.antiothers_key))) {
                    return true;
                }
            }
        }

        return false;
    }

    @Nullable
    public String getBitcoinUri() {
        return TextUtils.isEmpty(bitcoin) ? null : "bitcoin:" + bitcoin;
    }

    @Nullable
    public String getLitecoinUri() {
        return TextUtils.isEmpty(bitcoin) ? null : "litecoin:" + bitcoin;
    }

    @Nullable
    public String getOpenCollectiveUri() {
        return TextUtils.isEmpty(openCollective) ? null : "https://opencollective.com/"
                + openCollective + "/donate/";
    }

    @Nullable
    public String getFlattrUri() {
        return TextUtils.isEmpty(flattrID) ? null : "https://flattr.com/thing/" + flattrID;
    }

    @Nullable
    public String getLiberapayUri() {
        return TextUtils.isEmpty(liberapay) ? null : "https://liberapay.com/" + liberapay;
    }

    /**
     * {@link PackageManager} doesn't give us {@code minSdkVersion}, {@code targetSdkVersion},
     * and {@code maxSdkVersion}, so we have to parse it straight from {@code <uses-sdk>} in
     * {@code AndroidManifest.xml}.  If {@code targetSdkVersion} is not set, then it is
     * equal to {@code minSdkVersion}
     *
     * @see <a href="https://developer.android.com/guide/topics/manifest/uses-sdk-element.html">&lt;uses-sdk&gt;</a>
     */
    @SuppressWarnings("unused") // TODO port to lib
    private static int[] getMinTargetMaxSdkVersions(Context context, String packageName) {
        int minSdkVersion = Apk.SDK_VERSION_MIN_VALUE;
        int targetSdkVersion = Apk.SDK_VERSION_MIN_VALUE;
        int maxSdkVersion = Apk.SDK_VERSION_MAX_VALUE;
        try {
            AssetManager am = context.createPackageContext(packageName, 0).getAssets();
            XmlResourceParser xml = am.openXmlResourceParser("AndroidManifest.xml");
            int eventType = xml.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && "uses-sdk".equals(xml.getName())) {
                    for (int j = 0; j < xml.getAttributeCount(); j++) {
                        if (xml.getAttributeName(j).equals("minSdkVersion")) {
                            minSdkVersion = Integer.parseInt(xml.getAttributeValue(j));
                        } else if (xml.getAttributeName(j).equals("targetSdkVersion")) {
                            targetSdkVersion = Integer.parseInt(xml.getAttributeValue(j));
                        } else if (xml.getAttributeName(j).equals("maxSdkVersion")) {
                            maxSdkVersion = Integer.parseInt(xml.getAttributeValue(j));
                        }
                    }
                    break;
                }
                eventType = xml.nextToken();
            }
        } catch (PackageManager.NameNotFoundException
                | IOException
                | XmlPullParserException
                | NumberFormatException e) {
            Log.e(TAG, "Could not get min/max sdk version", e);
        }
        if (targetSdkVersion < minSdkVersion) {
            targetSdkVersion = minSdkVersion;
        }
        return new int[]{minSdkVersion, targetSdkVersion, maxSdkVersion};
    }

    public long getId() {
        return id;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public boolean isUninstallable(Context context) {
        if (this.isMediaInstalled(context)) {
            return true;
        } else if (this.isInstalled(context)) {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo appInfo;
            try {
                appInfo = pm.getApplicationInfo(this.packageName,
                        PackageManager.GET_UNINSTALLED_PACKAGES);
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }

            // System apps aren't uninstallable.
            final boolean isSystem = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            return !isSystem && this.isInstalled(context);
        } else {
            return false;
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(this.compatible ? (byte) 1 : (byte) 0);
        dest.writeString(this.packageName);
        dest.writeString(this.name);
        dest.writeLong(this.repoId);
        dest.writeString(this.summary);
        dest.writeString(this.iconFromApk);
        dest.writeString(this.description);
        dest.writeString(this.whatsNew);
        dest.writeString(this.license);
        dest.writeString(this.authorName);
        dest.writeString(this.authorEmail);
        dest.writeString(this.webSite);
        dest.writeString(this.issueTracker);
        dest.writeString(this.sourceCode);
        dest.writeString(this.translation);
        dest.writeString(this.video);
        dest.writeString(this.changelog);
        dest.writeString(this.donate);
        dest.writeString(this.bitcoin);
        dest.writeString(this.litecoin);
        dest.writeString(this.flattrID);
        dest.writeString(this.liberapay);
        dest.writeString(this.openCollective);
        dest.writeString(this.preferredSigner);
        dest.writeString(this.suggestedVersionName);
        dest.writeInt(this.suggestedVersionCode);
        dest.writeString(this.autoInstallVersionName);
        dest.writeInt(this.autoInstallVersionCode);
        dest.writeLong(this.added != null ? this.added.getTime() : -1);
        dest.writeLong(this.lastUpdated != null ? this.lastUpdated.getTime() : -1);
        dest.writeStringArray(this.categories);
        dest.writeStringArray(this.antiFeatures);
        dest.writeStringArray(this.requirements);
        dest.writeString(this.iconUrl);
        dest.writeString(this.featureGraphic);
        dest.writeString(this.promoGraphic);
        dest.writeString(this.tvBanner);
        dest.writeStringArray(this.phoneScreenshots);
        dest.writeStringArray(this.sevenInchScreenshots);
        dest.writeStringArray(this.tenInchScreenshots);
        dest.writeStringArray(this.tvScreenshots);
        dest.writeStringArray(this.wearScreenshots);
        dest.writeByte(this.isApk ? (byte) 1 : (byte) 0);
        dest.writeByte(this.isLocalized ? (byte) 1 : (byte) 0);
        dest.writeString(this.installedVersionName);
        dest.writeInt(this.installedVersionCode);
        dest.writeParcelable(this.installedApk, flags);
        dest.writeString(this.installedSig);
        dest.writeLong(this.id);
    }

    protected App(Parcel in) {
        this.compatible = in.readByte() != 0;
        this.packageName = in.readString();
        this.name = in.readString();
        this.repoId = in.readLong();
        this.summary = in.readString();
        this.iconFromApk = in.readString();
        this.description = in.readString();
        this.whatsNew = in.readString();
        this.license = in.readString();
        this.authorName = in.readString();
        this.authorEmail = in.readString();
        this.webSite = in.readString();
        this.issueTracker = in.readString();
        this.sourceCode = in.readString();
        this.translation = in.readString();
        this.video = in.readString();
        this.changelog = in.readString();
        this.donate = in.readString();
        this.bitcoin = in.readString();
        this.litecoin = in.readString();
        this.flattrID = in.readString();
        this.liberapay = in.readString();
        this.openCollective = in.readString();
        this.preferredSigner = in.readString();
        this.suggestedVersionName = in.readString();
        this.suggestedVersionCode = in.readInt();
        this.autoInstallVersionName = in.readString();
        this.autoInstallVersionCode = in.readInt();
        long tmpAdded = in.readLong();
        this.added = tmpAdded == -1 ? null : new Date(tmpAdded);
        long tmpLastUpdated = in.readLong();
        this.lastUpdated = tmpLastUpdated == -1 ? null : new Date(tmpLastUpdated);
        this.categories = in.createStringArray();
        this.antiFeatures = in.createStringArray();
        this.requirements = in.createStringArray();
        this.iconUrl = in.readString();
        this.featureGraphic = in.readString();
        this.promoGraphic = in.readString();
        this.tvBanner = in.readString();
        this.phoneScreenshots = in.createStringArray();
        this.sevenInchScreenshots = in.createStringArray();
        this.tenInchScreenshots = in.createStringArray();
        this.tvScreenshots = in.createStringArray();
        this.wearScreenshots = in.createStringArray();
        this.isApk = in.readByte() != 0;
        this.isLocalized = in.readByte() != 0;
        this.installedVersionName = in.readString();
        this.installedVersionCode = in.readInt();
        this.installedApk = in.readParcelable(Apk.class.getClassLoader());
        this.installedSig = in.readString();
        this.id = in.readLong();
    }

    public static final Parcelable.Creator<App> CREATOR = new Parcelable.Creator<App>() {
        @Override
        public App createFromParcel(Parcel source) {
            return new App(source);
        }

        @Override
        public App[] newArray(int size) {
            return new App[size];
        }
    };

    /**
     * Choose the signature which we should encourage the user to install.
     * Usually, we want the {@link #preferredSigner} rather than any random signature.
     * However, if the app is installed, then we override this and instead want to only encourage
     * the user to try and install versions with that signature (because thats all the OS will let
     * them do).
     * <p>
     * Will return null for any {@link App} which represents media (instead of an apk) and thus
     * doesn't have a signer.
     */
    @Nullable
    public String getMostAppropriateSignature() {
        if (!TextUtils.isEmpty(installedSig)) {
            return installedSig;
        } else if (!TextUtils.isEmpty(preferredSigner)) {
            return preferredSigner;
        }

        return null;
    }

}

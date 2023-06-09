package org.fdroid.fdroid.data;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.pm.PackageInfoCompat;
import androidx.core.os.ConfigurationCompat;
import androidx.core.os.LocaleListCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;

import org.fdroid.IndexFile;
import org.fdroid.database.AppListItem;
import org.fdroid.database.Repository;
import org.fdroid.database.UpdatableApp;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.index.v2.FileV2;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    public String installedSigner;
    public long installedVersionCode;
    public String installedVersionName;
    public org.fdroid.database.AppPrefs prefs;
    public String preferredSigner;
    public boolean isApk;

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

    public String description;

    /**
     * A descriptive text for what has changed in this version.
     */
    public String whatsNew;

    public FileV2 featureGraphic;
    private FileV2 promoGraphic;
    private FileV2 tvBanner;

    public List<FileV2> phoneScreenshots = Collections.emptyList();
    private List<FileV2> sevenInchScreenshots = Collections.emptyList();
    private List<FileV2> tenInchScreenshots = Collections.emptyList();
    private List<FileV2> tvScreenshots = Collections.emptyList();
    private List<FileV2> wearScreenshots = Collections.emptyList();

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

    private String flattrID;

    public String liberapay;

    private String openCollective;

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
     * emphasise that setting it won't do anything.
     */
    private String autoInstallVersionName;

    /**
     * The version that will be automatically installed if the user does not
     * choose a specific version.
     * TODO this should probably be converted to init to {@link Integer#MIN_VALUE} like {@link #suggestedVersionCode}
     */
    public long autoInstallVersionCode;

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
    public Map<String, String> antiFeatureReasons = new HashMap<>();

    public FileV2 iconFile;

    @Override
    public int compareTo(@NonNull App app) {
        return name.compareToIgnoreCase(app.name);
    }

    public App() {
    }

    public App(final UpdatableApp app) {
        repoId = app.getUpdate().getRepoId();
        setPackageName(app.getPackageName());
        name = app.getName() == null ? "" : app.getName();
        summary = app.getSummary() == null ? "" : app.getSummary();
        installedVersionCode = app.getInstalledVersionCode();
        autoInstallVersionCode = app.getUpdate().getManifest().getVersionCode();
        iconFile = app.getIcon(getLocales());
    }

    public App(final org.fdroid.database.App app, @Nullable PackageInfo packageInfo) {
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
        openCollective = app.getMetadata().getOpenCollective();
        preferredSigner = app.getMetadata().getPreferredSigner();
        added = new Date(app.getMetadata().getAdded());
        lastUpdated = new Date(app.getMetadata().getLastUpdated());
        iconFile = app.getIcon(getLocales());
        featureGraphic = app.getFeatureGraphic(getLocales());
        promoGraphic = app.getPromoGraphic(getLocales());
        tvBanner = app.getPromoGraphic(getLocales());
        phoneScreenshots = app.getPhoneScreenshots(getLocales());
        sevenInchScreenshots = app.getSevenInchScreenshots(getLocales());
        tenInchScreenshots = app.getTenInchScreenshots(getLocales());
        tvScreenshots = app.getTvScreenshots(getLocales());
        wearScreenshots = app.getWearScreenshots(getLocales());
        setInstalled(packageInfo);
    }

    public App(AppListItem item) {
        repoId = item.getRepoId();
        setPackageName(item.getPackageName());
        name = item.getName() == null ? "" : item.getName();
        summary = item.getSummary() == null ? "" : item.getSummary();
        iconFile = item.getIcon(getLocales());
        installedVersionCode = item.getInstalledVersionCode() == null ? 0 : item.getInstalledVersionCode();
        installedVersionName = item.getInstalledVersionName();
        antiFeatures = item.getAntiFeatureKeys().toArray(new String[0]);
        compatible = item.isCompatible();
        preferredSigner = item.getPreferredSigner();
    }

    public void setInstalled(@Nullable PackageInfo packageInfo) {
        installedVersionCode = packageInfo == null ? 0 : PackageInfoCompat.getLongVersionCode(packageInfo);
        installedVersionName = packageInfo == null ? null : packageInfo.versionName;
        installedSigner = packageInfo == null ? null : Utils.getPackageSigner(packageInfo);
    }

    /**
     * Updates this App instance with information from the APKs.
     *
     * @param apks The APKs available for this app.
     */
    public void update(Context context, List<Apk> apks, org.fdroid.database.AppPrefs appPrefs) {
        this.prefs = appPrefs;
        for (Apk apk : apks) {
            boolean apkIsInstalled = (apk.versionCode == installedVersionCode &&
                    TextUtils.equals(apk.signer, installedSigner)) || (!apk.isApk() && apk.isMediaInstalled(context));
            if (apkIsInstalled) {
                installedApk = apk;
                installedVersionCode = apk.versionCode;
                installedVersionName = apk.versionName;
                break;
            }
        }
        Apk apk = findSuggestedApk(apks, appPrefs);
        if (apk == null) return;
        // update the autoInstallVersionCode, if needed
        if (autoInstallVersionCode <= 0 && installedVersionCode < apk.versionCode) {
            autoInstallVersionCode = apk.versionCode;
            autoInstallVersionName = apk.versionName;
        }
        antiFeatures = apk.antiFeatures;
        antiFeatureReasons = apk.antiFeatureReasons;
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
    private void setPackageName(String packageName) {
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
    private static String formatDescription(String description) {
        return description.replace("\n", "<br>");
    }

    /**
     * Get the URL with the standard path for displaying in a browser.
     */
    @Nullable
    public Uri getShareUri(Context context) {
        Repository repo = FDroidApp.getRepoManager(context).getRepository(repoId);
        if (repo == null || repo.getWebBaseUrl() == null) return null;
        return Uri.parse(repo.getWebBaseUrl()).buildUpon()
                .path(packageName)
                .build();
    }

    public RequestBuilder<Drawable> loadWithGlide(Context context, IndexFile file) {
        return loadWithGlide(context, repoId, file);
    }

    public static RequestBuilder<Drawable> loadWithGlide(Context context, long repoId, IndexFile file) {
        Repository repo = FDroidApp.getRepoManager(context).getRepository(repoId);
        if (repo == null) { // This is also used for apps that do not have a repo
            return Glide.with(context).load(R.drawable.ic_repo_app_default);
        }
        String address = Utils.getRepoAddress(repo);
        if (address.startsWith("content://")) {
            String sb = Utils.getUri(address, file.getName().split("/")).toString();
            return Glide.with(context).load(sb);
        } else if (address.startsWith("file://")) {
            return Glide.with(context).load(file);
        } else {
            return Glide.with(context).load(Utils.getDownloadRequest(repo, file));
        }
    }

    public static RequestBuilder<Bitmap> loadBitmapWithGlide(Context context, long repoId,
                                                             IndexFile file) {
        Repository repo = FDroidApp.getRepoManager(context).getRepository(repoId);
        if (repo == null) {
            Log.e(TAG, "Repo not found: " + repoId);
            return Glide.with(context).asBitmap().load(R.drawable.ic_repo_app_default);
        }
        String address = Utils.getRepoAddress(repo);
        if (address.startsWith("content://")) {
            String sb = file == null ?
                    null : Utils.getUri(address, file.getName().split("/")).toString();
            return Glide.with(context).asBitmap().load(sb);
        } else if (address.startsWith("file://")) {
            return Glide.with(context).asBitmap().load(file.getName());
        } else {
            return Glide.with(context).asBitmap().load(Utils.getDownloadRequest(repo, file));
        }
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

    public List<FileV2> getAllScreenshots() {
        ArrayList<FileV2> list = new ArrayList<>(phoneScreenshots);
        list.addAll(sevenInchScreenshots);
        list.addAll(tenInchScreenshots);
        list.addAll(tvScreenshots);
        list.addAll(wearScreenshots);
        return list;
    }

    /**
     * Get the directory where APK Expansion Files aka OBB files are stored for the app as
     * specified by {@code packageName}.
     *
     * @see <a href="https://developer.android.com/google/play/expansion-files.html">APK Expansion Files</a>
     */
    static File getObbDir(String packageName) {
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
                if (apk.versionCode == PackageInfoCompat.getLongVersionCode(pi)) {
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

    private boolean isMediaInstalled(Context context) {
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
     *
     * @param apks           a list of APKs sorted by version code (highest first).
     * @param releaseChannel the key of the release channel to be considered.
     * @return The Apk we suggest to install or null, if we didn't find any.
     */
    @Nullable
    public Apk findSuggestedApk(List<Apk> apks, String releaseChannel) {
        final String mostAppropriateSigner = getMostAppropriateSigner();
        Apk apk = null;
        for (Apk a : apks) {
            // only consider compatible APKs
            if (!a.compatible) continue;
            // if we have a signer, but it doesn't match, don't use this APK
            if (mostAppropriateSigner != null && !mostAppropriateSigner.equals(a.signer)) continue;
            // stable release channel is always allowed, otherwise must include given channel
            final String stable = Apk.RELEASE_CHANNEL_STABLE;
            boolean isReleaseChannelAllowed = stable.equals(releaseChannel) ?
                    a.releaseChannels.contains(stable) :
                    a.releaseChannels.contains(stable) || a.releaseChannels.contains(releaseChannel);
            // if signer matches and we want the highest version code, take this as list is sorted
            if (isReleaseChannelAllowed) {
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
        dest.writeString(this.iconFile == null ? null : this.iconFile.serialize());
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
        dest.writeLong(this.autoInstallVersionCode);
        dest.writeLong(this.added != null ? this.added.getTime() : -1);
        dest.writeLong(this.lastUpdated != null ? this.lastUpdated.getTime() : -1);
        dest.writeStringArray(this.categories);
        dest.writeStringArray(this.antiFeatures);
        dest.writeString(this.featureGraphic == null ? null : this.featureGraphic.serialize());
        dest.writeString(this.promoGraphic == null ? null : this.promoGraphic.serialize());
        dest.writeString(this.tvBanner == null ? null : this.tvBanner.serialize());
        dest.writeStringList(Utils.toString(this.phoneScreenshots));
        dest.writeStringList(Utils.toString(this.sevenInchScreenshots));
        dest.writeStringList(Utils.toString(this.tenInchScreenshots));
        dest.writeStringList(Utils.toString(this.tvScreenshots));
        dest.writeStringList(Utils.toString(this.wearScreenshots));
        dest.writeByte(this.isApk ? (byte) 1 : (byte) 0);
        dest.writeString(this.installedVersionName);
        dest.writeLong(this.installedVersionCode);
        dest.writeParcelable(this.installedApk, flags);
        dest.writeString(this.installedSigner);
    }

    protected App(Parcel in) {
        this.compatible = in.readByte() != 0;
        this.packageName = in.readString();
        this.name = in.readString();
        this.repoId = in.readLong();
        this.summary = in.readString();
        this.iconFile = FileV2.deserialize(in.readString());
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
        this.autoInstallVersionCode = in.readLong();
        long tmpAdded = in.readLong();
        this.added = tmpAdded == -1 ? null : new Date(tmpAdded);
        long tmpLastUpdated = in.readLong();
        this.lastUpdated = tmpLastUpdated == -1 ? null : new Date(tmpLastUpdated);
        this.categories = in.createStringArray();
        this.antiFeatures = in.createStringArray();
        this.featureGraphic = FileV2.deserialize(in.readString());
        this.promoGraphic = FileV2.deserialize(in.readString());
        this.tvBanner = FileV2.deserialize(in.readString());
        this.phoneScreenshots = Utils.fileV2FromStrings(in.createStringArrayList());
        this.sevenInchScreenshots = Utils.fileV2FromStrings(in.createStringArrayList());
        this.tenInchScreenshots = Utils.fileV2FromStrings(in.createStringArrayList());
        this.tvScreenshots = Utils.fileV2FromStrings(in.createStringArrayList());
        this.wearScreenshots = Utils.fileV2FromStrings(in.createStringArrayList());
        this.isApk = in.readByte() != 0;
        this.installedVersionName = in.readString();
        this.installedVersionCode = in.readLong();
        this.installedApk = in.readParcelable(Apk.class.getClassLoader());
        this.installedSigner = in.readString();
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
     * Choose the APK Signing Certificate aka "signer" which we should
     * encourage the user to install. Usually, we want the
     * {@link #preferredSigner} rather than any random signer. However, if the
     * package is installed, then we override this and instead want to only
     * encourage the user to try and install versions with that signer (because
     * that is all the OS will let them do).
     * <p>
     * Will return null for any {@link App} which represents media (instead of an apk) and thus
     * doesn't have a signer.
     */
    @Nullable
    private String getMostAppropriateSigner() {
        if (!TextUtils.isEmpty(installedSigner)) {
            return installedSigner;
        } else if (!TextUtils.isEmpty(preferredSigner)) {
            return preferredSigner;
        }

        return null;
    }

}

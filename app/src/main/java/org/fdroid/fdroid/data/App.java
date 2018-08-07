package org.fdroid.fdroid.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.os.Build;
import android.os.Environment;
import android.os.LocaleList;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Schema.AppMetadataTable.Cols;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents an application, its availability, and its current installed state.
 * This represents the app in general, for a specific version of this app, see
 * {@link Apk}.
 * <p>
 * <b>Do not rename these instance variables without careful consideration!</b>
 * They are mapped to JSON field names, the {@code fdroidserver} internal variable
 * names, and the {@code fdroiddata} YAML field names.  Only the instance variables
 * decorated with {@code @JsonIgnore} are not directly mapped.
 * <p>
 * <b>NOTE:</b>If an instance variable is only meant for internal state, and not for
 * representing data coming from the server, then it must also be decorated with
 * {@code @JsonIgnore} to prevent abuse!  The tests for
 * {@link org.fdroid.fdroid.IndexV1Updater} will also have to be updated.
 *
 * @see <a href="https://gitlab.com/fdroid/fdroiddata">fdroiddata</a>
 * @see <a href="https://gitlab.com/fdroid/fdroidserver">fdroidserver</a>
 */
public class App extends ValueObject implements Comparable<App>, Parcelable {

    @JsonIgnore
    private static final String TAG = "App";

    // these properties are not from the index metadata, but represent the state on the device
    /**
     * True if compatible with the device (i.e. if at least one apk is)
     */
    @JsonIgnore
    public boolean compatible;
    @JsonIgnore
    public Apk installedApk; // might be null if not installed
    @JsonIgnore
    public String installedSig;
    @JsonIgnore
    public int installedVersionCode;
    @JsonIgnore
    public String installedVersionName;
    @JsonIgnore
    private long id;
    @JsonIgnore
    private AppPrefs prefs;
    @JsonIgnore
    @NonNull
    public String preferredSigner;
    @JsonIgnore
    public boolean isApk;

    /**
     * This is primarily for the purpose of saving app metadata when parsing an index.xml file.
     * At most other times, we don't particularly care which repo an {@link App} object came from.
     * It is pretty much transparent, because the metadata will be populated from the repo with
     * the highest priority. The UI doesn't care normally _which_ repo provided the metadata.
     * This is required for getting the full URL to the various graphics and screenshots.
     */
    @JacksonInject("repoId")
    public long repoId;

    // the remaining properties are set directly from the index metadata
    public String packageName = "unknown";
    public String name = "Unknown";

    public String summary = "Unknown application";
    public String icon;

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

    public String video;

    public String changelog;

    public String donate;

    public String bitcoin;

    public String litecoin;

    public String flattrID;

    public String liberapayID;

    public String upstreamVersionName;

    /**
     * The index-v1 metadata uses the term `suggestedVersionCode` but we need that
     * value to end up in the `upstreamVersionCode` property here. These variables
     * need to be renamed across the whole F-Droid ecosystem to make sense.
     *
     * @see <a href="https://gitlab.com/fdroid/fdroidclient/issues/1063">issue #1063</a>
     */
    @JsonProperty("suggestedVersionCode")
    public int upstreamVersionCode;

    /**
     * Unlike other public fields, this is only accessible via a getter, to
     * emphasise that setting it wont do anything. In order to change this,
     * you need to change suggestedVersionCode to an apk which is in the
     * apk table.
     */
    private String suggestedVersionName;

    public int suggestedVersionCode;

    public Date added;
    public Date lastUpdated;

    /**
     * List of categories (as defined in the metadata documentation) or null if there aren't any.
     * This is only populated when parsing a repository. If you need to know about the categories
     * an app is in any other part of F-Droid, use the {@link CategoryProvider}.
     */
    public String[] categories;

    /**
     * List of anti-features (as defined in the metadata documentation) or null if there aren't any.
     */
    public String[] antiFeatures;

    /**
     * Requires root access (only ever used for root)
     */
    @Deprecated
    public String[] requirements;

    /**
     * URL to download the app's icon.
     */
    public String iconUrl;

    public static String getIconName(String packageName, int versionCode) {
        return packageName + "_" + versionCode + ".png";
    }

    @Override
    public int compareTo(@NonNull App app) {
        return name.compareToIgnoreCase(app.name);
    }

    public App() {
    }

    public App(final Cursor cursor) {

        checkCursorPosition(cursor);

        final int cursorColumnCount = cursor.getColumnCount();
        for (int i = 0; i < cursorColumnCount; i++) {
            final String n = cursor.getColumnName(i);
            switch (n) {
                case Cols.ROW_ID:
                    id = cursor.getLong(i);
                    break;
                case Cols.REPO_ID:
                    repoId = cursor.getLong(i);
                    break;
                case Cols.IS_COMPATIBLE:
                    compatible = cursor.getInt(i) == 1;
                    break;
                case Cols.Package.PACKAGE_NAME:
                    packageName = cursor.getString(i);
                    break;
                case Cols.NAME:
                    name = cursor.getString(i);
                    break;
                case Cols.SUMMARY:
                    summary = cursor.getString(i);
                    break;
                case Cols.ICON:
                    icon = cursor.getString(i);
                    break;
                case Cols.DESCRIPTION:
                    description = cursor.getString(i);
                    break;
                case Cols.WHATSNEW:
                    whatsNew = cursor.getString(i);
                    break;
                case Cols.LICENSE:
                    license = cursor.getString(i);
                    break;
                case Cols.AUTHOR_NAME:
                    authorName = cursor.getString(i);
                    break;
                case Cols.AUTHOR_EMAIL:
                    authorEmail = cursor.getString(i);
                    break;
                case Cols.WEBSITE:
                    webSite = cursor.getString(i);
                    break;
                case Cols.ISSUE_TRACKER:
                    issueTracker = cursor.getString(i);
                    break;
                case Cols.SOURCE_CODE:
                    sourceCode = cursor.getString(i);
                    break;
                case Cols.VIDEO:
                    video = cursor.getString(i);
                    break;
                case Cols.CHANGELOG:
                    changelog = cursor.getString(i);
                    break;
                case Cols.DONATE:
                    donate = cursor.getString(i);
                    break;
                case Cols.BITCOIN:
                    bitcoin = cursor.getString(i);
                    break;
                case Cols.LITECOIN:
                    litecoin = cursor.getString(i);
                    break;
                case Cols.FLATTR_ID:
                    flattrID = cursor.getString(i);
                    break;
                case Cols.LIBERAPAY_ID:
                    liberapayID = cursor.getString(i);
                    break;
                case Cols.SuggestedApk.VERSION_NAME:
                    suggestedVersionName = cursor.getString(i);
                    break;
                case Cols.PREFERRED_SIGNER:
                    preferredSigner = cursor.getString(i);
                    break;
                case Cols.SUGGESTED_VERSION_CODE:
                    suggestedVersionCode = cursor.getInt(i);
                    break;
                case Cols.UPSTREAM_VERSION_CODE:
                    upstreamVersionCode = cursor.getInt(i);
                    break;
                case Cols.UPSTREAM_VERSION_NAME:
                    upstreamVersionName = cursor.getString(i);
                    break;
                case Cols.ADDED:
                    added = Utils.parseDate(cursor.getString(i), null);
                    break;
                case Cols.LAST_UPDATED:
                    lastUpdated = Utils.parseDate(cursor.getString(i), null);
                    break;
                case Cols.ANTI_FEATURES:
                    antiFeatures = Utils.parseCommaSeparatedString(cursor.getString(i));
                    break;
                case Cols.REQUIREMENTS:
                    requirements = Utils.parseCommaSeparatedString(cursor.getString(i));
                    break;
                case Cols.ICON_URL:
                    iconUrl = cursor.getString(i);
                    break;
                case Cols.FEATURE_GRAPHIC:
                    featureGraphic = cursor.getString(i);
                    break;
                case Cols.PROMO_GRAPHIC:
                    promoGraphic = cursor.getString(i);
                    break;
                case Cols.TV_BANNER:
                    tvBanner = cursor.getString(i);
                    break;
                case Cols.PHONE_SCREENSHOTS:
                    phoneScreenshots = Utils.parseCommaSeparatedString(cursor.getString(i));
                    break;
                case Cols.SEVEN_INCH_SCREENSHOTS:
                    sevenInchScreenshots = Utils.parseCommaSeparatedString(cursor.getString(i));
                    break;
                case Cols.TEN_INCH_SCREENSHOTS:
                    tenInchScreenshots = Utils.parseCommaSeparatedString(cursor.getString(i));
                    break;
                case Cols.TV_SCREENSHOTS:
                    tvScreenshots = Utils.parseCommaSeparatedString(cursor.getString(i));
                    break;
                case Cols.WEAR_SCREENSHOTS:
                    wearScreenshots = Utils.parseCommaSeparatedString(cursor.getString(i));
                    break;
                case Cols.IS_APK:
                    isApk = cursor.getInt(i) == 1;
                    break;
                case Cols.InstalledApp.VERSION_CODE:
                    installedVersionCode = cursor.getInt(i);
                    break;
                case Cols.InstalledApp.VERSION_NAME:
                    installedVersionName = cursor.getString(i);
                    break;
                case Cols.InstalledApp.SIGNATURE:
                    installedSig = cursor.getString(i);
                    break;
                case "_id":
                    break;
                default:
                    Log.e(TAG, "Unknown column name " + n);
            }
        }
    }

    /**
     * Instantiate from a locally installed package.
     * <p>
     * Initializes an {@link App} instances from an APK file. Since the file
     * could in the cache, and files can disappear from the cache at any time,
     * this needs to be quite defensive ensuring that {@code apkFile} still
     * exists.
     */
    @Nullable
    public static App getInstance(Context context, PackageManager pm, String packageName)
            throws CertificateEncodingException, IOException, PackageManager.NameNotFoundException {
        App app = new App();
        PackageInfo packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
        SanitizedFile apkFile = SanitizedFile.knownSanitized(packageInfo.applicationInfo.publicSourceDir);
        app.installedApk = new Apk();
        if (apkFile.canRead()) {
            String hashType = "sha256";
            String hash = Utils.getBinaryHash(apkFile, hashType);
            if (TextUtils.isEmpty(hash)) {
                return null;
            }
            app.installedApk.hashType = hashType;
            app.installedApk.hash = hash;
        }

        app.setFromPackageInfo(pm, packageInfo);
        app.initInstalledApk(context, app.installedApk, packageInfo, apkFile);
        return app;
    }

    /**
     * In order to format all in coming descriptions before they are written
     * out to the database and used elsewhere, this is needed to intercept
     * the setting of {@link App#description} to insert the format method.
     */
    @JsonProperty("description")
    private void setDescription(String description) { // NOPMD
        this.description = formatDescription(description);
    }

    /**
     * Parses the {@code localized} block in the incoming index metadata,
     * choosing the best match in terms of locale/language while filling as
     * many fields as possible.  It first sets up a locale list based on user
     * preference and the locales available for this app, then picks the texts
     * based on that list.  One thing that makes this tricky is that any given
     * locale block in the index might not have all the fields.  So when filling
     * out each value, it needs to go through the whole preference list each time,
     * rather than just taking the whole block for a specific locale.  This is to
     * ensure that there is something to show, as often as possible.
     * <p>
     * It is still possible that the fields will be loaded directly by Jackson
     * without any locale info.  This comes from the old-style, inline app metadata
     * fields that do not have locale info.  They should not be used if the
     * {@code localized} block is included in the index.  Also, null strings in
     * the {@code localized} block should not overwrite Name/Summary/Description
     * strings with empty/null if they were set directly by Jackson.
     * <p>
     * Choosing the locale to use follows two sets of rules, one for Android versions
     * older than {@code android-24} and the other for {@code android-24} or newer.
     * The system-wide language preference list was added in {@code android-24}.
     * <ul>
     * <li>{@code >= android-24}<ol>
     * <li>the country variant {@code de-AT} from the user locale list
     * <li>only the language {@code de} from the above locale
     * <li>{@code en-US} since its the most common English for software
     * <li>the first available {@code en} locale
     * </ol></li>
     * <li>{@code < android-24}<ol>
     * <li>the country variant from the user locale: {@code de-AT}
     * <li>only the language from the above locale:  {@code de}
     * <li>all available locales with the same language:  {@code de-BE}
     * <li>{@code en-US} since its the most common English for software
     * <li>all available {@code en} locales
     * </ol></li>
     * </ul>
     * On {@code >= android-24}, it is by design that this does not fallback to other
     * country-specific locales, e.g. {@code fr-CH} does not fall back on {@code fr-FR}.
     * If someone wants to fallback to {@code fr-FR}, they can add it to the system
     * language list.  There are many cases where it is inappropriate to fallback to a
     * different country-specific locale, for example {@code de-DE --> de-CH} or
     * {@code zh-CN --> zh-TW}.
     * <p>
     * On {@code < android-24}, the user can only set a single
     * locale with a country as an option, so here it makes sense to try to fallback
     * on other country-specific locales, rather than English.
     */
    @JsonProperty("localized")
    private void setLocalized(Map<String, Map<String, Object>> localized) { // NOPMD
        Locale defaultLocale = Locale.getDefault();
        String languageTag = defaultLocale.getLanguage();
        String countryTag = defaultLocale.getCountry();
        String localeTag;
        if (TextUtils.isEmpty(countryTag)) {
            localeTag = languageTag;
        } else {
            localeTag = languageTag + "-" + countryTag;
        }

        Set<String> availableLocales = localized.keySet();
        Set<String> localesToUse = new LinkedHashSet<>();
        if (availableLocales.contains(localeTag)) {
            localesToUse.add(localeTag);
        }
        if (availableLocales.contains(languageTag)) {
            localesToUse.add(languageTag);
        }
        if (Build.VERSION.SDK_INT >= 24) {
            LocaleList localeList = Resources.getSystem().getConfiguration().getLocales();
            String[] sortedLocaleList = localeList.toLanguageTags().split(",");
            Arrays.sort(sortedLocaleList, new java.util.Comparator<String>() {
                @Override
                public int compare(String s1, String s2) {
                    return s1.length() - s2.length();
                }
            });
            for (String toUse : sortedLocaleList) {
                localesToUse.add(toUse);
                for (String l : availableLocales) {
                    if (l.equals(toUse.split("-")[0])) {
                        localesToUse.add(l);
                        break;
                    }
                }
            }
        } else {
            for (String l : availableLocales) {
                if (l.startsWith(languageTag)) {
                    localesToUse.add(l);
                }
            }
        }
        if (availableLocales.contains("en-US")) {
            localesToUse.add("en-US");
        }
        for (String l : availableLocales) {
            if (l.startsWith("en")) {
                localesToUse.add(l);
                break;
            }
        }

        whatsNew = getLocalizedEntry(localized, localesToUse, "whatsNew");
        String value = getLocalizedEntry(localized, localesToUse, "video");
        if (!TextUtils.isEmpty(value)) {
            video = value.split("\n", 1)[0];
        }
        value = getLocalizedEntry(localized, localesToUse, "name");
        if (!TextUtils.isEmpty(value)) {
            name = value;
        }
        value = getLocalizedEntry(localized, localesToUse, "summary");
        if (!TextUtils.isEmpty(value)) {
            summary = value;
        }
        value = getLocalizedEntry(localized, localesToUse, "description");
        if (!TextUtils.isEmpty(value)) {
            description = formatDescription(value);
        }

        featureGraphic = getLocalizedGraphicsEntry(localized, localesToUse, "featureGraphic");
        promoGraphic = getLocalizedGraphicsEntry(localized, localesToUse, "promoGraphic");
        tvBanner = getLocalizedGraphicsEntry(localized, localesToUse, "tvBanner");

        wearScreenshots = getLocalizedListEntry(localized, localesToUse, "wearScreenshots");
        phoneScreenshots = getLocalizedListEntry(localized, localesToUse, "phoneScreenshots");
        sevenInchScreenshots = getLocalizedListEntry(localized, localesToUse, "sevenInchScreenshots");
        tenInchScreenshots = getLocalizedListEntry(localized, localesToUse, "tenInchScreenshots");
        tvScreenshots = getLocalizedListEntry(localized, localesToUse, "tvScreenshots");
    }

    /**
     * Returns the right localized version of this entry, based on an immitation of
     * the logic that Android/Java uses.  On Android >= 24, this can get the
     * "Language Priority List", but it doesn't always seem to be properly sorted.
     * So this method has to kind of fake it by using {@link Locale#getDefault()}
     * as the first entry, then sorting the rest based on length (e.g. {@code de-AT}
     * before {@code de}).
     *
     * @see LocaleList
     * @see Locale#getDefault()
     * @see java.util.Locale.LanguageRange
     */
    private String getLocalizedEntry(Map<String, Map<String, Object>> localized,
                                     Set<String> locales, String key) {
        try {
            for (String locale : locales) {
                if (localized.containsKey(locale)) {
                    String value = (String) localized.get(locale).get(key);
                    if (value != null) {
                        return value;
                    }
                }
            }
        } catch (ClassCastException e) {
            Utils.debugLog(TAG, e.getMessage());
        }
        return null;
    }

    private String getLocalizedGraphicsEntry(Map<String, Map<String, Object>> localized,
                                             Set<String> locales, String key) {
        try {
            for (String locale : locales) {
                Map<String, Object> entry = localized.get(locale);
                if (entry != null) {
                    Object value = entry.get(key);
                    if (value != null && value.toString().length() > 0) {
                        return locale + "/" + value;
                    }
                }
            }
        } catch (ClassCastException e) {
            Utils.debugLog(TAG, e.getMessage());
        }
        return null;
    }

    private String[] getLocalizedListEntry(Map<String, Map<String, Object>> localized,
                                           Set<String> locales, String key) {
        try {
            for (String locale : locales) {
                if (localized.containsKey(locale)) {
                    ArrayList<String> entry = (ArrayList<String>) localized.get(locale).get(key);
                    if (entry != null && entry.size() > 0) {
                        String[] result = new String[entry.size()];
                        int i = 0;
                        for (String e : entry) {
                            result[i] = locale + "/" + key + "/" + e;
                            i++;
                        }
                        return result;
                    }
                }
            }
        } catch (ClassCastException e) {
            Utils.debugLog(TAG, e.getMessage());
        }
        return new String[0];
    }

    /**
     * Returns the app description text with all newlines replaced by {@code <br>}
     */
    public static String formatDescription(String description) {
        return description.replace("\n", "<br>");
    }

    public String getFeatureGraphicUrl(Context context) {
        if (TextUtils.isEmpty(featureGraphic)) {
            return null;
        }
        Repo repo = RepoProvider.Helper.findById(context, repoId);
        return repo.address + "/" + packageName + "/" + featureGraphic;
    }

    public String getPromoGraphic(Context context) {
        if (TextUtils.isEmpty(promoGraphic)) {
            return null;
        }
        Repo repo = RepoProvider.Helper.findById(context, repoId);
        return repo.address + "/" + packageName + "/" + promoGraphic;
    }

    public String getTvBanner(Context context) {
        if (TextUtils.isEmpty(tvBanner)) {
            return null;
        }
        Repo repo = RepoProvider.Helper.findById(context, repoId);
        return repo.address + "/" + packageName + "/" + tvBanner;
    }

    public String[] getAllScreenshots(Context context) {
        Repo repo = RepoProvider.Helper.findById(context, repoId);
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
        String[] result = new String[list.size()];
        int i = 0;
        for (String url : list) {
            result[i] = repo.address + "/" + packageName + "/" + url;
            i++;
        }
        return result;
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

    private void setFromPackageInfo(PackageManager pm, PackageInfo packageInfo) {

        this.packageName = packageInfo.packageName;
        final String installerPackageName = pm.getInstallerPackageName(packageName);
        CharSequence installerPackageLabel = null;
        if (!TextUtils.isEmpty(installerPackageName)) {
            try {
                ApplicationInfo installerAppInfo = pm.getApplicationInfo(installerPackageName,
                        PackageManager.GET_META_DATA);
                installerPackageLabel = installerAppInfo.loadLabel(pm);
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "Could not get app info: " + installerPackageName, e);
            }
        }
        if (TextUtils.isEmpty(installerPackageLabel)) {
            installerPackageLabel = installerPackageName;
        }

        ApplicationInfo appInfo = packageInfo.applicationInfo;
        final CharSequence appDescription = appInfo.loadDescription(pm);
        if (TextUtils.isEmpty(appDescription)) {
            this.summary = "(installed by " + installerPackageLabel + ")";
        } else if (appDescription.length() > 40) {
            this.summary = (String) appDescription.subSequence(0, 40);
        } else {
            this.summary = (String) appDescription;
        }
        this.added = new Date(packageInfo.firstInstallTime);
        this.lastUpdated = new Date(packageInfo.lastUpdateTime);
        this.description = "<p>";
        if (!TextUtils.isEmpty(appDescription)) {
            this.description += appDescription + "\n";
        }
        this.description += "(installed by " + installerPackageLabel
                + ", first installed on " + this.added
                + ", last updated on " + this.lastUpdated + ")</p>";

        this.name = (String) appInfo.loadLabel(pm);
        this.icon = getIconName(packageName, packageInfo.versionCode);
        this.installedVersionName = packageInfo.versionName;
        this.installedVersionCode = packageInfo.versionCode;
        this.compatible = true;
    }

    public static void initInstalledObbFiles(Apk apk) {
        File obbdir = getObbDir(apk.packageName);
        FileFilter filter = new RegexFileFilter("(main|patch)\\.[0-9-][0-9]*\\." + apk.packageName + "\\.obb");
        File[] files = obbdir.listFiles(filter);
        if (files == null) {
            return;
        }
        Arrays.sort(files);
        for (File f : files) {
            String filename = f.getName();
            String[] segments = filename.split("\\.");
            if (Integer.parseInt(segments[1]) <= apk.versionCode) {
                if ("main".equals(segments[0])) {
                    apk.obbMainFile = filename;
                    apk.obbMainFileSha256 = Utils.getBinaryHash(f, apk.hashType);
                } else if ("patch".equals(segments[0])) {
                    apk.obbPatchFile = filename;
                    apk.obbPatchFileSha256 = Utils.getBinaryHash(f, apk.hashType);
                }
            }
        }
    }

    @SuppressWarnings("EmptyForIteratorPad")
    private void initInstalledApk(Context context, Apk apk, PackageInfo packageInfo, SanitizedFile apkFile)
            throws IOException, CertificateEncodingException {
        apk.compatible = true;
        apk.versionName = packageInfo.versionName;
        apk.versionCode = packageInfo.versionCode;
        apk.added = this.added;
        int[] minTargetMax = getMinTargetMaxSdkVersions(context, packageName);
        apk.minSdkVersion = minTargetMax[0];
        apk.targetSdkVersion = minTargetMax[1];
        apk.maxSdkVersion = minTargetMax[2];
        apk.packageName = this.packageName;
        apk.requestedPermissions = packageInfo.requestedPermissions;
        apk.apkName = apk.packageName + "_" + apk.versionCode + ".apk";

        initInstalledObbFiles(apk);

        final FeatureInfo[] features = packageInfo.reqFeatures;
        if (features != null && features.length > 0) {
            apk.features = new String[features.length];
            for (int i = 0; i < features.length; i++) {
                apk.features[i] = features[i].name;
            }
        }

        if (!apkFile.canRead()) {
            return;
        }

        apk.installedFile = apkFile;
        JarFile apkJar = new JarFile(apkFile);
        HashSet<String> abis = new HashSet<>(3);
        Pattern pattern = Pattern.compile("^lib/([a-z0-9-]+)/.*");
        for (Enumeration<JarEntry> jarEntries = apkJar.entries(); jarEntries.hasMoreElements(); ) {
            JarEntry jarEntry = jarEntries.nextElement();
            Matcher matcher = pattern.matcher(jarEntry.getName());
            if (matcher.matches()) {
                abis.add(matcher.group(1));
            }
        }
        apk.nativecode = abis.toArray(new String[abis.size()]);

        final JarEntry aSignedEntry = (JarEntry) apkJar.getEntry("AndroidManifest.xml");

        if (aSignedEntry == null) {
            apkJar.close();
            throw new CertificateEncodingException("null signed entry!");
        }

        byte[] rawCertBytes;

        // Due to a bug in android 5.0 lollipop, the inclusion of BouncyCastle causes
        // breakage when verifying the signature of most .jars. For more
        // details, check out https://gitlab.com/fdroid/fdroidclient/issues/111.
        try {
            FDroidApp.disableBouncyCastleOnLollipop();
            final InputStream tmpIn = apkJar.getInputStream(aSignedEntry);
            byte[] buff = new byte[2048];
            //noinspection StatementWithEmptyBody
            while (tmpIn.read(buff, 0, buff.length) != -1) {
                /*
                 * NOP - apparently have to READ from the JarEntry before you can
                 * call getCerficates() and have it return != null. Yay Java.
                 */
            }
            tmpIn.close();

            if (aSignedEntry.getCertificates() == null
                    || aSignedEntry.getCertificates().length == 0) {
                apkJar.close();
                throw new CertificateEncodingException("No Certificates found!");
            }

            final Certificate signer = aSignedEntry.getCertificates()[0];
            rawCertBytes = signer.getEncoded();
        } finally {
            FDroidApp.enableBouncyCastleOnLollipop();
        }
        apkJar.close();

        /*
         * I don't fully understand the loop used here. I've copied it verbatim
         * from getsig.java bundled with FDroidServer. I *believe* it is taking
         * the raw byte encoding of the certificate & converting it to a byte
         * array of the hex representation of the original certificate byte
         * array. This is then MD5 sum'd. It's a really bad way to be doing this
         * if I'm right... If I'm not right, I really don't know! see lines
         * 67->75 in getsig.java bundled with Fdroidserver
         */
        final byte[] fdroidSig = new byte[rawCertBytes.length * 2];
        for (int j = 0; j < rawCertBytes.length; j++) {
            byte v = rawCertBytes[j];
            int d = (v >> 4) & 0xF;
            fdroidSig[j * 2] = (byte) (d >= 10 ? ('a' + d - 10) : ('0' + d));
            d = v & 0xF;
            fdroidSig[j * 2 + 1] = (byte) (d >= 10 ? ('a' + d - 10) : ('0' + d));
        }
        apk.sig = Utils.hashBytes(fdroidSig, "md5");
    }

    /**
     * Attempts to find the installed {@link Apk} from the database. If not found, will lookup the
     * {@link InstalledAppProvider} to find the details of the installed app and use that to
     * instantiate an {@link Apk} to be returned.
     * <p>
     * Cases where an {@link Apk} will not be found in the database and for which we fall back to
     * the {@link InstalledAppProvider} include:
     * <li>System apps which are provided by a repository, but for which the version code bundled
     * with the system is not included in the repository.</li>
     * <li>Regular apps from a repository, where the installed version is old enough that it is no
     * longer available in the repository.</li>
     */
    @Nullable
    public Apk getInstalledApk(Context context) {
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(this.packageName, 0);
            Apk apk = ApkProvider.Helper.findApkFromAnyRepo(context, pi.packageName, pi.versionCode);
            if (apk == null) {
                InstalledApp installedApp = InstalledAppProvider.Helper.findByPackageName(context, pi.packageName);
                if (installedApp == null) {
                    throw new IllegalStateException("No installed app found when trying to uninstall");
                }
                apk = new Apk(installedApp);
            }
            return apk;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    public boolean isValid() {
        if (TextUtils.isEmpty(this.name)
                || TextUtils.isEmpty(this.packageName)) {
            return false;
        }

        if (this.installedApk == null) {
            return false;
        }

        if (TextUtils.isEmpty(this.installedApk.sig)) {
            return false;
        }

        final File apkFile = this.installedApk.installedFile;
        return !(apkFile == null || !apkFile.canRead());

    }

    public ContentValues toContentValues() {

        final ContentValues values = new ContentValues();
        // Intentionally don't put "ROW_ID" in here, because we don't ever want to change that
        // primary key generated by sqlite.
        values.put(Cols.Package.PACKAGE_NAME, packageName);
        values.put(Cols.NAME, name);
        values.put(Cols.REPO_ID, repoId);
        values.put(Cols.SUMMARY, summary);
        values.put(Cols.ICON, icon);
        values.put(Cols.ICON_URL, iconUrl);
        values.put(Cols.DESCRIPTION, description);
        values.put(Cols.WHATSNEW, whatsNew);
        values.put(Cols.LICENSE, license);
        values.put(Cols.AUTHOR_NAME, authorName);
        values.put(Cols.AUTHOR_EMAIL, authorEmail);
        values.put(Cols.WEBSITE, webSite);
        values.put(Cols.ISSUE_TRACKER, issueTracker);
        values.put(Cols.SOURCE_CODE, sourceCode);
        values.put(Cols.VIDEO, video);
        values.put(Cols.CHANGELOG, changelog);
        values.put(Cols.DONATE, donate);
        values.put(Cols.BITCOIN, bitcoin);
        values.put(Cols.LITECOIN, litecoin);
        values.put(Cols.FLATTR_ID, flattrID);
        values.put(Cols.LIBERAPAY_ID, liberapayID);
        values.put(Cols.ADDED, Utils.formatDate(added, ""));
        values.put(Cols.LAST_UPDATED, Utils.formatDate(lastUpdated, ""));
        values.put(Cols.PREFERRED_SIGNER, preferredSigner);
        values.put(Cols.SUGGESTED_VERSION_CODE, suggestedVersionCode);
        values.put(Cols.UPSTREAM_VERSION_NAME, upstreamVersionName);
        values.put(Cols.UPSTREAM_VERSION_CODE, upstreamVersionCode);
        values.put(Cols.ForWriting.Categories.CATEGORIES, Utils.serializeCommaSeparatedString(categories));
        values.put(Cols.ANTI_FEATURES, Utils.serializeCommaSeparatedString(antiFeatures));
        values.put(Cols.REQUIREMENTS, Utils.serializeCommaSeparatedString(requirements));
        values.put(Cols.FEATURE_GRAPHIC, featureGraphic);
        values.put(Cols.PROMO_GRAPHIC, promoGraphic);
        values.put(Cols.TV_BANNER, tvBanner);
        values.put(Cols.PHONE_SCREENSHOTS, Utils.serializeCommaSeparatedString(phoneScreenshots));
        values.put(Cols.SEVEN_INCH_SCREENSHOTS, Utils.serializeCommaSeparatedString(sevenInchScreenshots));
        values.put(Cols.TEN_INCH_SCREENSHOTS, Utils.serializeCommaSeparatedString(tenInchScreenshots));
        values.put(Cols.TV_SCREENSHOTS, Utils.serializeCommaSeparatedString(tvScreenshots));
        values.put(Cols.WEAR_SCREENSHOTS, Utils.serializeCommaSeparatedString(wearScreenshots));
        values.put(Cols.IS_COMPATIBLE, compatible ? 1 : 0);
        values.put(Cols.IS_APK, isApk ? 1 : 0);

        return values;
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
        // This is always null for media files. We could skip the code below completely if it wasn't
        if (this.installedApk != null && !this.installedApk.isApk() && this.installedApk.isMediaInstalled(context)) {
            return this.installedApk;
        }
        // This code comes from AppDetailsRecyclerViewAdapter
        final List<Apk> apks = ApkProvider.Helper.findByPackageName(context, this.packageName);
        for (final Apk apk : apks) {
            boolean allowByCompatability = apk.compatible || Preferences.get().showIncompatibleVersions();
            boolean allowBySig = this.installedSig == null || TextUtils.equals(this.installedSig, apk.sig);
            if (allowByCompatability && allowBySig) {
                if (!apk.isApk()) {
                    if (apk.isMediaInstalled(context)) {
                        return apk;
                    }
                }
            }
        }
        return null;
    }

    /**
     * True if there are new versions (apks) available
     */
    public boolean hasUpdates() {
        boolean updates = false;
        if (suggestedVersionCode > 0) {
            updates = installedVersionCode > 0 && installedVersionCode < suggestedVersionCode;
        }
        return updates;
    }

    public AppPrefs getPrefs(Context context) {
        if (prefs == null) {
            prefs = AppPrefsProvider.Helper.getPrefsOrDefault(context, this);
        }
        return prefs;
    }

    /**
     * True if there are new versions (apks) available and the user wants
     * to be notified about them
     */
    public boolean canAndWantToUpdate(Context context) {
        boolean canUpdate = hasUpdates();
        AppPrefs prefs = getPrefs(context);
        boolean wantsUpdate = !prefs.ignoreAllUpdates && prefs.ignoreThisUpdate < suggestedVersionCode;
        return canUpdate && wantsUpdate && !isDisabledByAntiFeatures();
    }

    /**
     * @return if the given app should be filtered out based on the
     * {@link Preferences#PREF_SHOW_ANTI_FEATURE_APPS Show Anti-Features Setting}
     */
    public boolean isDisabledByAntiFeatures() {
        return this.antiFeatures != null
                && this.antiFeatures.length > 0
                && !Preferences.get().showAppsWithAntiFeatures();
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
    public String getFlattrUri() {
        return TextUtils.isEmpty(flattrID) ? null : "https://flattr.com/thing/" + flattrID;
    }

    @Nullable
    public String getLiberapayUri() {
        return TextUtils.isEmpty(liberapayID) ? null : "https://liberapay.com/~" + liberapayID;
    }


    /**
     * @see App#suggestedVersionName for why this uses a getter while other member variables are
     * publicly accessible.
     */
    public String getSuggestedVersionName() {
        return suggestedVersionName;
    }

    /**
     * {@link PackageManager} doesn't give us {@code minSdkVersion}, {@code targetSdkVersion},
     * and {@code maxSdkVersion}, so we have to parse it straight from {@code <uses-sdk>} in
     * {@code AndroidManifest.xml}.  If {@code targetSdkVersion} is not set, then it is
     * equal to {@code minSdkVersion}
     *
     * @see <a href="https://developer.android.com/guide/topics/manifest/uses-sdk-element.html">&lt;uses-sdk&gt;</a>
     */
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
        } catch (PackageManager.NameNotFoundException | IOException | XmlPullParserException e) {
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
        dest.writeString(this.icon);
        dest.writeString(this.description);
        dest.writeString(this.whatsNew);
        dest.writeString(this.license);
        dest.writeString(this.authorName);
        dest.writeString(this.authorEmail);
        dest.writeString(this.webSite);
        dest.writeString(this.issueTracker);
        dest.writeString(this.sourceCode);
        dest.writeString(this.video);
        dest.writeString(this.changelog);
        dest.writeString(this.donate);
        dest.writeString(this.bitcoin);
        dest.writeString(this.litecoin);
        dest.writeString(this.flattrID);
        dest.writeString(this.liberapayID);
        dest.writeString(this.preferredSigner);
        dest.writeString(this.upstreamVersionName);
        dest.writeInt(this.upstreamVersionCode);
        dest.writeString(this.suggestedVersionName);
        dest.writeInt(this.suggestedVersionCode);
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
        this.icon = in.readString();
        this.description = in.readString();
        this.whatsNew = in.readString();
        this.license = in.readString();
        this.authorName = in.readString();
        this.authorEmail = in.readString();
        this.webSite = in.readString();
        this.issueTracker = in.readString();
        this.sourceCode = in.readString();
        this.video = in.readString();
        this.changelog = in.readString();
        this.donate = in.readString();
        this.bitcoin = in.readString();
        this.litecoin = in.readString();
        this.flattrID = in.readString();
        this.liberapayID = in.readString();
        this.preferredSigner = in.readString();
        this.upstreamVersionName = in.readString();
        this.upstreamVersionCode = in.readInt();
        this.suggestedVersionName = in.readString();
        this.suggestedVersionCode = in.readInt();
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
        this.installedVersionName = in.readString();
        this.installedVersionCode = in.readInt();
        this.installedApk = in.readParcelable(Apk.class.getClassLoader());
        this.installedSig = in.readString();
        this.id = in.readLong();
    }

    @JsonIgnore
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

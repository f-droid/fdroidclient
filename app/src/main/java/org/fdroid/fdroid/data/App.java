package org.fdroid.fdroid.data;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import org.fdroid.fdroid.AppFilter;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Utils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class App extends ValueObject implements Comparable<App> {

    private static final String TAG = "App";

    // True if compatible with the device (i.e. if at least one apk is)
    public boolean compatible;

    public String packageName = "unknown";
    public String name = "Unknown";
    public String summary = "Unknown application";
    public String icon;

    public String description;

    public String license = "Unknown";

    public String author;
    public String email;

    public String webURL;

    public String trackerURL;

    public String sourceURL;

    public String changelogURL;

    public String donateURL;

    public String bitcoinAddr;

    public String litecoinAddr;

    public String flattrID;

    public String upstreamVersionName;
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

    // List of categories (as defined in the metadata
    // documentation) or null if there aren't any.
    public Utils.CommaSeparatedList categories;

    // List of anti-features (as defined in the metadata
    // documentation) or null if there aren't any.
    public Utils.CommaSeparatedList antiFeatures;

    // List of special requirements (such as root privileges) or
    // null if there aren't any.
    public Utils.CommaSeparatedList requirements;

    // True if all updates for this app are to be ignored
    public boolean ignoreAllUpdates;

    // True if the current update for this app is to be ignored
    public int ignoreThisUpdate;

    // To be displayed at 48dp (x1.0)
    public String iconUrl;
    // To be displayed at 72dp (x1.5)
    public String iconUrlLarge;

    public String installedVersionName;

    public int installedVersionCode;

    public Apk installedApk; // might be null if not installed

    public String installedSig;

    public boolean uninstallable;

    public static String getIconName(String packageName, int versionCode) {
        return packageName + "_" + versionCode + ".png";
    }

    @Override
    public int compareTo(App app) {
        return name.compareToIgnoreCase(app.name);
    }

    public App() {
    }

    public App(Parcelable parcelable) {
        this(new ContentValuesCursor((ContentValues) parcelable));
    }

    public App(Cursor cursor) {

        checkCursorPosition(cursor);

        for (int i = 0; i < cursor.getColumnCount(); i++) {
            String n = cursor.getColumnName(i);
            switch (n) {
                case AppProvider.DataColumns.IS_COMPATIBLE:
                    compatible = cursor.getInt(i) == 1;
                    break;
                case AppProvider.DataColumns.PACKAGE_NAME:
                    packageName = cursor.getString(i);
                    break;
                case AppProvider.DataColumns.NAME:
                    name = cursor.getString(i);
                    break;
                case AppProvider.DataColumns.SUMMARY:
                    summary = cursor.getString(i);
                    break;
                case AppProvider.DataColumns.ICON:
                    icon = cursor.getString(i);
                    break;
                case AppProvider.DataColumns.DESCRIPTION:
                    description = cursor.getString(i);
                    break;
                case AppProvider.DataColumns.LICENSE:
                    license = cursor.getString(i);
                    break;
                case AppProvider.DataColumns.AUTHOR:
                    author = cursor.getString(i);
                    break;
                case AppProvider.DataColumns.EMAIL:
                    email = cursor.getString(i);
                    break;
                case AppProvider.DataColumns.WEB_URL:
                    webURL = cursor.getString(i);
                    break;
                case AppProvider.DataColumns.TRACKER_URL:
                    trackerURL = cursor.getString(i);
                    break;
                case AppProvider.DataColumns.SOURCE_URL:
                    sourceURL = cursor.getString(i);
                    break;
                case AppProvider.DataColumns.CHANGELOG_URL:
                    changelogURL = cursor.getString(i);
                    break;
                case AppProvider.DataColumns.DONATE_URL:
                    donateURL = cursor.getString(i);
                    break;
                case AppProvider.DataColumns.BITCOIN_ADDR:
                    bitcoinAddr = cursor.getString(i);
                    break;
                case AppProvider.DataColumns.LITECOIN_ADDR:
                    litecoinAddr = cursor.getString(i);
                    break;
                case AppProvider.DataColumns.FLATTR_ID:
                    flattrID = cursor.getString(i);
                    break;
                case AppProvider.DataColumns.SuggestedApk.VERSION_NAME:
                    suggestedVersionName = cursor.getString(i);
                    break;
                case AppProvider.DataColumns.SUGGESTED_VERSION_CODE:
                    suggestedVersionCode = cursor.getInt(i);
                    break;
                case AppProvider.DataColumns.UPSTREAM_VERSION_CODE:
                    upstreamVersionCode = cursor.getInt(i);
                    break;
                case AppProvider.DataColumns.UPSTREAM_VERSION_NAME:
                    upstreamVersionName = cursor.getString(i);
                    break;
                case AppProvider.DataColumns.ADDED:
                    added = Utils.parseDate(cursor.getString(i), null);
                    break;
                case AppProvider.DataColumns.LAST_UPDATED:
                    lastUpdated = Utils.parseDate(cursor.getString(i), null);
                    break;
                case AppProvider.DataColumns.CATEGORIES:
                    categories = Utils.CommaSeparatedList.make(cursor.getString(i));
                    break;
                case AppProvider.DataColumns.ANTI_FEATURES:
                    antiFeatures = Utils.CommaSeparatedList.make(cursor.getString(i));
                    break;
                case AppProvider.DataColumns.REQUIREMENTS:
                    requirements = Utils.CommaSeparatedList.make(cursor.getString(i));
                    break;
                case AppProvider.DataColumns.IGNORE_ALLUPDATES:
                    ignoreAllUpdates = cursor.getInt(i) == 1;
                    break;
                case AppProvider.DataColumns.IGNORE_THISUPDATE:
                    ignoreThisUpdate = cursor.getInt(i);
                    break;
                case AppProvider.DataColumns.ICON_URL:
                    iconUrl = cursor.getString(i);
                    break;
                case AppProvider.DataColumns.ICON_URL_LARGE:
                    iconUrlLarge = cursor.getString(i);
                    break;
                case AppProvider.DataColumns.InstalledApp.VERSION_CODE:
                    installedVersionCode = cursor.getInt(i);
                    break;
                case AppProvider.DataColumns.InstalledApp.VERSION_NAME:
                    installedVersionName = cursor.getString(i);
                    break;
                case AppProvider.DataColumns.InstalledApp.SIGNATURE:
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
     */
    public App(Context context, PackageManager pm, String packageName)
            throws CertificateEncodingException, IOException, PackageManager.NameNotFoundException {

        PackageInfo packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
        setFromPackageInfo(pm, packageInfo);
        this.installedApk = new Apk();
        SanitizedFile apkFile = SanitizedFile.knownSanitized(packageInfo.applicationInfo.publicSourceDir);
        initApkFromApkFile(context, this.installedApk, packageInfo, apkFile);
    }

    @TargetApi(9)
    private void setFromPackageInfo(PackageManager pm, PackageInfo packageInfo)
            throws CertificateEncodingException, IOException, PackageManager.NameNotFoundException {

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
        } else {
            this.summary = (String) appDescription.subSequence(0, 40);
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
        this.compatible = true;
        boolean system = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        boolean updatedSystemApp = (appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
        this.uninstallable = !system || updatedSystemApp;
    }

    private void initApkFromApkFile(Context context, Apk apk, PackageInfo packageInfo, SanitizedFile apkFile)
            throws IOException, CertificateEncodingException {
        // TODO include signature hash calculation here
        apk.hashType = "sha256";
        apk.hash = Utils.getBinaryHash(apkFile, apk.hashType);
        initInstalledApk(context, apk, packageInfo, apkFile);
    }

    private void initInstalledApk(Context context, Apk apk, PackageInfo packageInfo, SanitizedFile apkFile)
            throws IOException, CertificateEncodingException {
        apk.compatible = true;
        apk.versionName = packageInfo.versionName;
        apk.versionCode = packageInfo.versionCode;
        apk.added = this.added;
        int[] minMaxSdkVersions = getMinMaxSdkVersions(context, packageName);
        apk.minSdkVersion = minMaxSdkVersions[0];
        apk.maxSdkVersion = minMaxSdkVersions[1];
        apk.packageName = this.packageName;
        apk.permissions = Utils.CommaSeparatedList.make(packageInfo.requestedPermissions);
        apk.apkName = apk.packageName + "_" + apk.versionCode + ".apk";
        apk.installedFile = apkFile;

        JarFile apkJar = new JarFile(apkFile);
        HashSet<String> abis = new HashSet<>(3);
        Pattern pattern = Pattern.compile("^lib/([a-z0-9-]+)/.*");
        for (Enumeration<JarEntry> jarEntries = apkJar.entries(); jarEntries.hasMoreElements();) {
            JarEntry jarEntry = jarEntries.nextElement();
            Matcher matcher = pattern.matcher(jarEntry.getName());
            if (matcher.matches()) {
                abis.add(matcher.group(1));
            }
        }
        apk.nativecode = Utils.CommaSeparatedList.make(abis.toArray(new String[abis.size()]));

        final FeatureInfo[] features = packageInfo.reqFeatures;
        if (features != null && features.length > 0) {
            final String[] featureNames = new String[features.length];
            for (int i = 0; i < features.length; i++) {
                featureNames[i] = features[i].name;
            }
            apk.features = Utils.CommaSeparatedList.make(featureNames);
        }

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
            FDroidApp.disableSpongyCastleOnLollipop();
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
            FDroidApp.enableSpongyCastleOnLollipop();
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
        values.put(AppProvider.DataColumns.PACKAGE_NAME, packageName);
        values.put(AppProvider.DataColumns.NAME, name);
        values.put(AppProvider.DataColumns.SUMMARY, summary);
        values.put(AppProvider.DataColumns.ICON, icon);
        values.put(AppProvider.DataColumns.ICON_URL, iconUrl);
        values.put(AppProvider.DataColumns.ICON_URL_LARGE, iconUrlLarge);
        values.put(AppProvider.DataColumns.DESCRIPTION, description);
        values.put(AppProvider.DataColumns.LICENSE, license);
        values.put(AppProvider.DataColumns.AUTHOR, author);
        values.put(AppProvider.DataColumns.EMAIL, email);
        values.put(AppProvider.DataColumns.WEB_URL, webURL);
        values.put(AppProvider.DataColumns.TRACKER_URL, trackerURL);
        values.put(AppProvider.DataColumns.SOURCE_URL, sourceURL);
        values.put(AppProvider.DataColumns.CHANGELOG_URL, changelogURL);
        values.put(AppProvider.DataColumns.DONATE_URL, donateURL);
        values.put(AppProvider.DataColumns.BITCOIN_ADDR, bitcoinAddr);
        values.put(AppProvider.DataColumns.LITECOIN_ADDR, litecoinAddr);
        values.put(AppProvider.DataColumns.FLATTR_ID, flattrID);
        values.put(AppProvider.DataColumns.ADDED, Utils.formatDate(added, ""));
        values.put(AppProvider.DataColumns.LAST_UPDATED, Utils.formatDate(lastUpdated, ""));
        values.put(AppProvider.DataColumns.SUGGESTED_VERSION_CODE, suggestedVersionCode);
        values.put(AppProvider.DataColumns.UPSTREAM_VERSION_NAME, upstreamVersionName);
        values.put(AppProvider.DataColumns.UPSTREAM_VERSION_CODE, upstreamVersionCode);
        values.put(AppProvider.DataColumns.CATEGORIES, Utils.CommaSeparatedList.str(categories));
        values.put(AppProvider.DataColumns.ANTI_FEATURES, Utils.CommaSeparatedList.str(antiFeatures));
        values.put(AppProvider.DataColumns.REQUIREMENTS, Utils.CommaSeparatedList.str(requirements));
        values.put(AppProvider.DataColumns.IS_COMPATIBLE, compatible ? 1 : 0);
        values.put(AppProvider.DataColumns.IGNORE_ALLUPDATES, ignoreAllUpdates ? 1 : 0);
        values.put(AppProvider.DataColumns.IGNORE_THISUPDATE, ignoreThisUpdate);

        return values;
    }

    public boolean isInstalled() {
        return installedVersionCode > 0;
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

    // True if there are new versions (apks) available and the user wants
    // to be notified about them
    public boolean canAndWantToUpdate() {
        boolean canUpdate = hasUpdates();
        boolean wantsUpdate = !ignoreAllUpdates && ignoreThisUpdate < suggestedVersionCode;
        return canUpdate && wantsUpdate && !isFiltered();
    }

    // Whether the app is filtered or not based on AntiFeatures and root
    // permission (set in the Settings page)
    public boolean isFiltered() {
        return new AppFilter().filter(this);
    }

    public String getSuggestedVersionName() {
        return suggestedVersionName;
    }

    /**
     * {@link PackageManager} doesn't give us {@code minSdkVersion} and {@code maxSdkVersion},
     * so we have to parse it straight from {@code <uses-sdk>} in {@code AndroidManifest.xml}.
     */
    private static int[] getMinMaxSdkVersions(Context context, String packageName) {
        int minSdkVersion = Apk.SDK_VERSION_MIN_VALUE;
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
        return new int[]{minSdkVersion, maxSdkVersion};
    }
}

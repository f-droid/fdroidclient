package org.fdroid.fdroid.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.os.Parcel;
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

import org.fdroid.fdroid.data.Schema.AppMetadataTable.Cols;

public class App extends ValueObject implements Comparable<App>, Parcelable {

    private static final String TAG = "App";

    /**
     * True if compatible with the device (i.e. if at least one apk is)
     */
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

    /**
     * List of categories (as defined in the metadata documentation) or null if there aren't any.
     */
    public String[] categories;

    /**
     * List of anti-features (as defined in the metadata documentation) or null if there aren't any.
     */
    public String[] antiFeatures;

    /**
     * List of special requirements (such as root privileges) or null if there aren't any.
     */
    public String[] requirements;

    private AppPrefs prefs;

    /**
     * To be displayed at 48dp (x1.0)
     */
    public String iconUrl;

    /**
     * To be displayed at 72dp (x1.5)
     */
    public String iconUrlLarge;

    public String installedVersionName;

    public int installedVersionCode;

    public Apk installedApk; // might be null if not installed

    public String installedSig;

    private long id;

    public static String getIconName(String packageName, int versionCode) {
        return packageName + "_" + versionCode + ".png";
    }

    @Override
    public int compareTo(App app) {
        return name.compareToIgnoreCase(app.name);
    }

    public App() {
    }

    public App(Cursor cursor) {

        checkCursorPosition(cursor);

        for (int i = 0; i < cursor.getColumnCount(); i++) {
            String n = cursor.getColumnName(i);
            switch (n) {
                case Cols.ROW_ID:
                    id = cursor.getLong(i);
                    break;
                case Cols.IS_COMPATIBLE:
                    compatible = cursor.getInt(i) == 1;
                    break;
                case Cols.PACKAGE_NAME:
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
                case Cols.LICENSE:
                    license = cursor.getString(i);
                    break;
                case Cols.AUTHOR:
                    author = cursor.getString(i);
                    break;
                case Cols.EMAIL:
                    email = cursor.getString(i);
                    break;
                case Cols.WEB_URL:
                    webURL = cursor.getString(i);
                    break;
                case Cols.TRACKER_URL:
                    trackerURL = cursor.getString(i);
                    break;
                case Cols.SOURCE_URL:
                    sourceURL = cursor.getString(i);
                    break;
                case Cols.CHANGELOG_URL:
                    changelogURL = cursor.getString(i);
                    break;
                case Cols.DONATE_URL:
                    donateURL = cursor.getString(i);
                    break;
                case Cols.BITCOIN_ADDR:
                    bitcoinAddr = cursor.getString(i);
                    break;
                case Cols.LITECOIN_ADDR:
                    litecoinAddr = cursor.getString(i);
                    break;
                case Cols.FLATTR_ID:
                    flattrID = cursor.getString(i);
                    break;
                case Cols.SuggestedApk.VERSION_NAME:
                    suggestedVersionName = cursor.getString(i);
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
                case Cols.CATEGORIES:
                    categories = Utils.parseCommaSeparatedString(cursor.getString(i));
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
                case Cols.ICON_URL_LARGE:
                    iconUrlLarge = cursor.getString(i);
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
     */
    public App(Context context, PackageManager pm, String packageName)
            throws CertificateEncodingException, IOException, PackageManager.NameNotFoundException {

        PackageInfo packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
        setFromPackageInfo(pm, packageInfo);
        this.installedApk = new Apk();
        SanitizedFile apkFile = SanitizedFile.knownSanitized(packageInfo.applicationInfo.publicSourceDir);
        initApkFromApkFile(context, this.installedApk, packageInfo, apkFile);
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
        int[] minTargetMax = getMinTargetMaxSdkVersions(context, packageName);
        apk.minSdkVersion = minTargetMax[0];
        apk.targetSdkVersion = minTargetMax[1];
        apk.maxSdkVersion = minTargetMax[2];
        apk.packageName = this.packageName;
        apk.permissions = packageInfo.requestedPermissions;
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
        apk.nativecode = abis.toArray(new String[abis.size()]);

        final FeatureInfo[] features = packageInfo.reqFeatures;
        if (features != null && features.length > 0) {
            apk.features = new String[features.length];
            for (int i = 0; i < features.length; i++) {
                apk.features[i] = features[i].name;
            }
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
        // Intentionally don't put "ROW_ID" in here, because we don't ever want to change that
        // primary key generated by sqlite.
        values.put(Cols.PACKAGE_NAME, packageName);
        values.put(Cols.NAME, name);
        values.put(Cols.SUMMARY, summary);
        values.put(Cols.ICON, icon);
        values.put(Cols.ICON_URL, iconUrl);
        values.put(Cols.ICON_URL_LARGE, iconUrlLarge);
        values.put(Cols.DESCRIPTION, description);
        values.put(Cols.LICENSE, license);
        values.put(Cols.AUTHOR, author);
        values.put(Cols.EMAIL, email);
        values.put(Cols.WEB_URL, webURL);
        values.put(Cols.TRACKER_URL, trackerURL);
        values.put(Cols.SOURCE_URL, sourceURL);
        values.put(Cols.CHANGELOG_URL, changelogURL);
        values.put(Cols.DONATE_URL, donateURL);
        values.put(Cols.BITCOIN_ADDR, bitcoinAddr);
        values.put(Cols.LITECOIN_ADDR, litecoinAddr);
        values.put(Cols.FLATTR_ID, flattrID);
        values.put(Cols.ADDED, Utils.formatDate(added, ""));
        values.put(Cols.LAST_UPDATED, Utils.formatDate(lastUpdated, ""));
        values.put(Cols.SUGGESTED_VERSION_CODE, suggestedVersionCode);
        values.put(Cols.UPSTREAM_VERSION_NAME, upstreamVersionName);
        values.put(Cols.UPSTREAM_VERSION_CODE, upstreamVersionCode);
        values.put(Cols.CATEGORIES, Utils.serializeCommaSeparatedString(categories));
        values.put(Cols.ANTI_FEATURES, Utils.serializeCommaSeparatedString(antiFeatures));
        values.put(Cols.REQUIREMENTS, Utils.serializeCommaSeparatedString(requirements));
        values.put(Cols.IS_COMPATIBLE, compatible ? 1 : 0);

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
        return canUpdate && wantsUpdate && !isFiltered();
    }

    /**
     * Whether the app is filtered or not based on AntiFeatures and root
     * permission (set in the Settings page)
     */
    public boolean isFiltered() {
        return new AppFilter().filter(this);
    }

    public String getSuggestedVersionName() {
        return suggestedVersionName;
    }

    /**
     * {@link PackageManager} doesn't give us {@code minSdkVersion}, {@code targetSdkVersion},
     * and {@code maxSdkVersion}, so we have to parse it straight from {@code <uses-sdk>} in
     * {@code AndroidManifest.xml}.  If {@code targetSdkVersion} is not set, then it is
     * equal to {@code minSdkVersion}
     *
     * @see <a href="https://developer.android.com/guide/topics/manifest/uses-sdk-element.html">&lt;uses-sdk&gt; element</a>
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

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(this.compatible ? (byte) 1 : (byte) 0);
        dest.writeString(this.packageName);
        dest.writeString(this.name);
        dest.writeString(this.summary);
        dest.writeString(this.icon);
        dest.writeString(this.description);
        dest.writeString(this.license);
        dest.writeString(this.author);
        dest.writeString(this.email);
        dest.writeString(this.webURL);
        dest.writeString(this.trackerURL);
        dest.writeString(this.sourceURL);
        dest.writeString(this.changelogURL);
        dest.writeString(this.donateURL);
        dest.writeString(this.bitcoinAddr);
        dest.writeString(this.litecoinAddr);
        dest.writeString(this.flattrID);
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
        dest.writeString(this.iconUrlLarge);
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
        this.summary = in.readString();
        this.icon = in.readString();
        this.description = in.readString();
        this.license = in.readString();
        this.author = in.readString();
        this.email = in.readString();
        this.webURL = in.readString();
        this.trackerURL = in.readString();
        this.sourceURL = in.readString();
        this.changelogURL = in.readString();
        this.donateURL = in.readString();
        this.bitcoinAddr = in.readString();
        this.litecoinAddr = in.readString();
        this.flattrID = in.readString();
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
        this.iconUrlLarge = in.readString();
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
}

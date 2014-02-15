package org.fdroid.fdroid.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import org.fdroid.fdroid.AppFilter;
import org.fdroid.fdroid.Utils;

import java.util.Date;
import java.util.Map;

public class App extends ValueObject implements Comparable<App> {

    // True if compatible with the device (i.e. if at least one apk is)
    public boolean compatible;

    public String id = "unknown";
    public String name = "Unknown";
    public String summary = "Unknown application";
    public String icon;

    public String description;

    public String license = "Unknown";

    public String webURL;

    public String trackerURL;

    public String sourceURL;

    public String donateURL;

    public String bitcoinAddr;

    public String litecoinAddr;

    public String dogecoinAddr;

    public String flattrID;

    public String upstreamVersion;
    public int upstreamVercode;

    /**
     * Unlike other public fields, this is only accessible via a getter, to
     * emphasise that setting it wont do anything. In order to change this,
     * you need to change suggestedVercode to an apk which is in the apk table.
     */
    private String suggestedVersion;
    
    public int suggestedVercode;

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

    // Used internally for tracking during repo updates.
    public boolean updated;

    public String iconUrl;

    @Override
    public int compareTo(App app) {
        return name.compareToIgnoreCase(app.name);
    }

    public App() {

    }

    public App(Cursor cursor) {

        checkCursorPosition(cursor);

        for(int i = 0; i < cursor.getColumnCount(); i ++ ) {
            String column = cursor.getColumnName(i);
            if (column.equals(AppProvider.DataColumns.IS_COMPATIBLE)) {
                compatible = cursor.getInt(i) == 1;
            } else if (column.equals(AppProvider.DataColumns.APP_ID)) {
                id = cursor.getString(i);
            } else if (column.equals(AppProvider.DataColumns.NAME)) {
                name = cursor.getString(i);
            } else if (column.equals(AppProvider.DataColumns.SUMMARY)) {
                summary = cursor.getString(i);
            } else if (column.equals(AppProvider.DataColumns.ICON)) {
                icon = cursor.getString(i);
            } else if (column.equals(AppProvider.DataColumns.DESCRIPTION)) {
                description = cursor.getString(i);
            } else if (column.equals(AppProvider.DataColumns.LICENSE)) {
                license = cursor.getString(i);
            } else if (column.equals(AppProvider.DataColumns.WEB_URL)) {
                webURL = cursor.getString(i);
            } else if (column.equals(AppProvider.DataColumns.TRACKER_URL)) {
                trackerURL = cursor.getString(i);
            } else if (column.equals(AppProvider.DataColumns.SOURCE_URL)) {
                sourceURL = cursor.getString(i);
            } else if (column.equals(AppProvider.DataColumns.DONATE_URL)) {
                donateURL = cursor.getString(i);
            } else if (column.equals(AppProvider.DataColumns.BITCOIN_ADDR)) {
                bitcoinAddr = cursor.getString(i);
            } else if (column.equals(AppProvider.DataColumns.LITECOIN_ADDR)) {
                litecoinAddr = cursor.getString(i);
            } else if (column.equals(AppProvider.DataColumns.DOGECOIN_ADDR)) {
                dogecoinAddr = cursor.getString(i);
            } else if (column.equals(AppProvider.DataColumns.FLATTR_ID)) {
                flattrID = cursor.getString(i);
            } else if (column.equals(AppProvider.DataColumns.SuggestedApk.VERSION)) {
                suggestedVersion = cursor.getString(i);
            } else if (column.equals(AppProvider.DataColumns.SUGGESTED_VERSION_CODE)) {
                suggestedVercode = cursor.getInt(i);
            } else if (column.equals(AppProvider.DataColumns.UPSTREAM_VERSION_CODE)) {
                upstreamVercode = cursor.getInt(i);
            } else if (column.equals(AppProvider.DataColumns.UPSTREAM_VERSION)) {
                upstreamVersion = cursor.getString(i);
            } else if (column.equals(AppProvider.DataColumns.ADDED)) {
                added = ValueObject.toDate(cursor.getString(i));
            } else if (column.equals(AppProvider.DataColumns.LAST_UPDATED)) {
                lastUpdated = ValueObject.toDate(cursor.getString(i));
            } else if (column.equals(AppProvider.DataColumns.CATEGORIES)) {
                categories = Utils.CommaSeparatedList.make(cursor.getString(i));
            } else if (column.equals(AppProvider.DataColumns.ANTI_FEATURES)) {
                antiFeatures = Utils.CommaSeparatedList.make(cursor.getString(i));
            } else if (column.equals(AppProvider.DataColumns.REQUIREMENTS)) {
                requirements = Utils.CommaSeparatedList.make(cursor.getString(i));
            } else if (column.equals(AppProvider.DataColumns.IGNORE_ALLUPDATES)) {
                ignoreAllUpdates = cursor.getInt(i) == 1;
            } else if (column.equals(AppProvider.DataColumns.IGNORE_THISUPDATE)) {
                ignoreThisUpdate = cursor.getInt(i);
            } else if (column.equals(AppProvider.DataColumns.ICON_URL)) {
                iconUrl = cursor.getString(i);
            }
        }
    }

    public ContentValues toContentValues() {

        ContentValues values = new ContentValues();
        values.put(AppProvider.DataColumns.APP_ID, id);
        values.put(AppProvider.DataColumns.NAME, name);
        values.put(AppProvider.DataColumns.SUMMARY, summary);
        values.put(AppProvider.DataColumns.ICON, icon);
        values.put(AppProvider.DataColumns.ICON_URL, iconUrl);
        values.put(AppProvider.DataColumns.DESCRIPTION, description);
        values.put(AppProvider.DataColumns.LICENSE, license);
        values.put(AppProvider.DataColumns.WEB_URL, webURL);
        values.put(AppProvider.DataColumns.TRACKER_URL, trackerURL);
        values.put(AppProvider.DataColumns.SOURCE_URL, sourceURL);
        values.put(AppProvider.DataColumns.DONATE_URL, donateURL);
        values.put(AppProvider.DataColumns.BITCOIN_ADDR, bitcoinAddr);
        values.put(AppProvider.DataColumns.LITECOIN_ADDR, litecoinAddr);
        values.put(AppProvider.DataColumns.DOGECOIN_ADDR, dogecoinAddr);
        values.put(AppProvider.DataColumns.FLATTR_ID, flattrID);
        values.put(AppProvider.DataColumns.ADDED, added == null ? "" : Utils.DATE_FORMAT.format(added));
        values.put(AppProvider.DataColumns.LAST_UPDATED, added == null ? "" : Utils.DATE_FORMAT.format(lastUpdated));
        values.put(AppProvider.DataColumns.SUGGESTED_VERSION_CODE, suggestedVercode);
        values.put(AppProvider.DataColumns.UPSTREAM_VERSION, upstreamVersion);
        values.put(AppProvider.DataColumns.UPSTREAM_VERSION_CODE, upstreamVercode);
        values.put(AppProvider.DataColumns.CATEGORIES, Utils.CommaSeparatedList.str(categories));
        values.put(AppProvider.DataColumns.ANTI_FEATURES, Utils.CommaSeparatedList.str(antiFeatures));
        values.put(AppProvider.DataColumns.REQUIREMENTS, Utils.CommaSeparatedList.str(requirements));
        values.put(AppProvider.DataColumns.IS_COMPATIBLE, compatible ? 1 : 0);
        values.put(AppProvider.DataColumns.IGNORE_ALLUPDATES, ignoreAllUpdates ? 1 : 0);
        values.put(AppProvider.DataColumns.IGNORE_THISUPDATE, ignoreThisUpdate);
        values.put(AppProvider.DataColumns.ICON_URL, iconUrl);

        return values;
    }

    /**
     * Version string for the currently installed version of this apk.
     * If not installed, returns null.
     */
    public String getInstalledVersion(Context context) {
        PackageInfo info = getInstalledInfo(context);
        return info == null ? null : info.versionName;
    }

    /**
     * Version code for the currently installed version of this apk.
     * If not installed, it returns -1.
     */
    public int getInstalledVerCode(Context context) {
        PackageInfo info = getInstalledInfo(context);
        return info == null ? -1 : info.versionCode;
    }

    /**
     * True if installed by the user, false if a system apk or not installed.
     */
    public boolean getUserInstalled(Context context) {
        PackageInfo info = getInstalledInfo(context);
        return info != null && ((info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 1);
    }

    public PackageInfo getInstalledInfo(Context context) {
        Map<String, PackageInfo> installed = Utils.getInstalledApps(context);
        return installed.containsKey(id) ? installed.get(id) : null;
    }

    /**
     * True if there are new versions (apks) available
     */
    public boolean hasUpdates(Context context) {
        boolean updates = false;
        if (suggestedVercode > 0) {
            int installedVerCode = getInstalledVerCode(context);
            updates = (installedVerCode > 0 && installedVerCode < suggestedVercode);
        }
        return updates;
    }

    // True if there are new versions (apks) available and the user wants
    // to be notified about them
    public boolean canAndWantToUpdate(Context context) {
        boolean canUpdate = hasUpdates(context);
        boolean wantsUpdate = !ignoreAllUpdates && ignoreThisUpdate < suggestedVercode;
        return canUpdate && wantsUpdate && !isFiltered();
    }

    // Whether the app is filtered or not based on AntiFeatures and root
    // permission (set in the Settings page)
    public boolean isFiltered() {
        return new AppFilter().filter(this);
    }

    public String getSuggestedVersion() {
        return suggestedVersion;
    }
}

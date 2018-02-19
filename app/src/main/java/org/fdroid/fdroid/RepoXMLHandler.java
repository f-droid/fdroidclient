/*
 * Copyright (C) 2010-12  Ciaran Gultnieks, ciaran@ciarang.com
 * Copyright (C) 2009  Roberto Jacinto, roberto.jacinto@caixamagica.pt
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.fdroid.fdroid;

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoPushRequest;
import org.fdroid.fdroid.data.Schema.ApkTable;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Parses the index.xml into Java data structures.
 */
public class RepoXMLHandler extends DefaultHandler {

    // The repo we're processing.
    private final Repo repo;

    private List<Apk> apksList = new ArrayList<>();

    private App curapp;
    private Apk curapk;

    private String currentApkHashType;

    // After processing the XML, these will be -1 if the index didn't specify
    // them - otherwise it will be the value specified.
    private int repoMaxAge = -1;
    private int repoVersion;
    private long repoTimestamp;
    private String repoDescription;
    private String repoName;
    private String repoIcon;
    private final ArrayList<String> repoMirrors = new ArrayList<>();

    /**
     * Set of requested permissions per package/APK
     */
    private final HashSet<String> requestedPermissionsSet = new HashSet<>();

    /**
     * the X.509 signing certificate stored in the header of index.xml
     */
    private String repoSigningCert;

    private final StringBuilder curchars = new StringBuilder();

    public interface IndexReceiver {
        void receiveRepo(String name, String description, String signingCert, int maxage, int version,
                         long timestamp, String icon, String[] mirrors);

        void receiveApp(App app, List<Apk> packages);

        void receiveRepoPushRequest(RepoPushRequest repoPushRequest);
    }

    private final IndexReceiver receiver;

    public RepoXMLHandler(Repo repo, @NonNull IndexReceiver receiver) {
        this.repo = repo;
        this.receiver = receiver;
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        curchars.append(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {

        if ("application".equals(localName) && curapp != null) {
            onApplicationParsed();
        } else if ("package".equals(localName) && curapk != null && curapp != null) {
            int size = requestedPermissionsSet.size();
            curapk.requestedPermissions = requestedPermissionsSet.toArray(new String[size]);
            requestedPermissionsSet.clear();
            apksList.add(curapk);
            curapk = null;
        } else if ("repo".equals(localName)) {
            onRepoParsed();
        } else if (curchars.length() == 0) {
            // All options below require non-empty content
            return;
        }
        final String str = curchars.toString().trim();
        if (curapk != null) {
            switch (localName) {
                case ApkTable.Cols.VERSION_NAME:
                    curapk.versionName = str;
                    break;
                case "versioncode": // ApkTable.Cols.VERSION_CODE
                    curapk.versionCode = Utils.parseInt(str, -1);
                    break;
                case ApkTable.Cols.SIZE:
                    curapk.size = Utils.parseInt(str, 0);
                    break;
                case ApkTable.Cols.HASH:
                    if (currentApkHashType == null || "md5".equals(currentApkHashType)) {
                        if (curapk.hash == null) {
                            curapk.hash = str;
                            curapk.hashType = "SHA-256";
                        }
                    } else if ("sha256".equals(currentApkHashType)) {
                        curapk.hash = str;
                        curapk.hashType = "SHA-256";
                    }
                    break;
                case ApkTable.Cols.SIGNATURE:
                    curapk.sig = str;
                    // the first APK in the list provides the preferred signature
                    if (curapp.preferredSigner == null) {
                        curapp.preferredSigner = str;
                    }
                    break;
                case ApkTable.Cols.SOURCE_NAME:
                    curapk.srcname = str;
                    break;
                case "apkname": // ApkTable.Cols.NAME
                    curapk.apkName = str;
                    break;
                case "sdkver": // ApkTable.Cols.MIN_SDK_VERSION
                    curapk.minSdkVersion = Utils.parseInt(str, Apk.SDK_VERSION_MIN_VALUE);
                    break;
                case ApkTable.Cols.TARGET_SDK_VERSION:
                    curapk.targetSdkVersion = Utils.parseInt(str, Apk.SDK_VERSION_MIN_VALUE);
                    break;
                case "maxsdkver": // ApkTable.Cols.MAX_SDK_VERSION
                    curapk.maxSdkVersion = Utils.parseInt(str, Apk.SDK_VERSION_MAX_VALUE);
                    if (curapk.maxSdkVersion == 0) {
                        // before fc0df0dcf4dd0d5f13de82d7cd9254b2b48cb62d, this could be 0
                        curapk.maxSdkVersion = Apk.SDK_VERSION_MAX_VALUE;
                    }
                    break;
                case ApkTable.Cols.OBB_MAIN_FILE:
                    curapk.obbMainFile = str;
                    break;
                case ApkTable.Cols.OBB_MAIN_FILE_SHA256:
                    curapk.obbMainFileSha256 = str;
                    break;
                case ApkTable.Cols.OBB_PATCH_FILE:
                    curapk.obbPatchFile = str;
                    break;
                case ApkTable.Cols.OBB_PATCH_FILE_SHA256:
                    curapk.obbPatchFileSha256 = str;
                    break;
                case ApkTable.Cols.ADDED_DATE:
                    curapk.added = Utils.parseDate(str, null);
                    break;
                case "permissions": // together with <uses-permissions* makes ApkTable.Cols.REQUESTED_PERMISSIONS
                    addCommaSeparatedPermissions(str);
                    break;
                case ApkTable.Cols.FEATURES:
                    curapk.features = Utils.parseCommaSeparatedString(str);
                    break;
                case ApkTable.Cols.NATIVE_CODE:
                    curapk.nativecode = Utils.parseCommaSeparatedString(str);
                    break;
            }
        } else if (curapp != null) {
            switch (localName) {
                case "name":
                    curapp.name = str;
                    break;
                case "icon":
                    curapp.icon = str;
                    break;
                case "description":
                    // This is the old-style description. We'll read it
                    // if present, to support old repos, but in newer
                    // repos it will get overwritten straight away!
                    curapp.description = "<p>" + str + "</p>";
                    break;
                case "desc":
                    // New-style description.
                    curapp.description = App.formatDescription(str);
                    break;
                case "summary":
                    curapp.summary = str;
                    break;
                case "license":
                    curapp.license = str;
                    break;
                case "author":
                    curapp.authorName = str;
                    break;
                case "email":
                    curapp.authorEmail = str;
                    break;
                case "source":
                    curapp.sourceCode = str;
                    break;
                case "changelog":
                    curapp.changelog = str;
                    break;
                case "donate":
                    curapp.donate = str;
                    break;
                case "bitcoin":
                    curapp.bitcoin = str;
                    break;
                case "litecoin":
                    curapp.litecoin = str;
                    break;
                case "flattr":
                    curapp.flattrID = str;
                    break;
                case "liberapay":
                    curapp.liberapayID = str;
                    break;
                case "web":
                    curapp.webSite = str;
                    break;
                case "tracker":
                    curapp.issueTracker = str;
                    break;
                case "added":
                    curapp.added = Utils.parseDate(str, null);
                    break;
                case "lastupdated":
                    curapp.lastUpdated = Utils.parseDate(str, null);
                    break;
                case "marketversion":
                    curapp.upstreamVersionName = str;
                    break;
                case "marketvercode":
                    curapp.upstreamVersionCode = Utils.parseInt(str, -1);
                    break;
                case "categories":
                    curapp.categories = Utils.parseCommaSeparatedString(str);
                    break;
                case "antifeatures":
                    curapp.antiFeatures = Utils.parseCommaSeparatedString(str);
                    break;
                case "requirements":
                    curapp.requirements = Utils.parseCommaSeparatedString(str);
                    break;
            }
        } else if ("description".equals(localName)) {
            repoDescription = cleanWhiteSpace(str);
        } else if ("mirror".equals(localName)) {
            repoMirrors.add(str);
        }
    }

    private static final Pattern OLD_FDROID_PERMISSION = Pattern.compile("[A-Z_]+");

    /**
     * It appears that the default Android permissions in android.Manifest.permissions
     * are prefixed with "android.permission." and then the constant name.
     * FDroid just includes the constant name in the apk list, so we prefix it
     * with "android.permission."
     *
     * @see <a href="https://gitlab.com/fdroid/fdroidserver/blob/1afa8cfc/update.py#L91">
     * More info into index - size, permissions, features, sdk version</a>
     */
    public static String fdroidToAndroidPermission(String permission) {
        if (OLD_FDROID_PERMISSION.matcher(permission).matches()) {
            return "android.permission." + permission;
        }

        return permission;
    }

    private void addRequestedPermission(String permission) {
        requestedPermissionsSet.add(permission);
    }

    private void addCommaSeparatedPermissions(String permissions) {
        String[] array = Utils.parseCommaSeparatedString(permissions);
        if (array != null) {
            for (String permission : array) {
                requestedPermissionsSet.add(fdroidToAndroidPermission(permission));
            }
        }
    }

    private void removeRequestedPermission(String permission) {
        requestedPermissionsSet.remove(permission);
    }

    private void onApplicationParsed() {
        receiver.receiveApp(curapp, apksList);
        curapp = null;
        apksList = new ArrayList<>();
        // If the app packageName is already present in this apps list, then it
        // means the same index file has a duplicate app, which should
        // not be allowed.
        // However, I'm thinking that it should be undefined behaviour,
        // because it is probably a bug in the fdroid server that made it
        // happen, and I don't *think* it will crash the client, because
        // the first app will insert, the second one will update the newly
        // inserted one.
    }

    private void onRepoParsed() {
        receiver.receiveRepo(repoName, repoDescription, repoSigningCert, repoMaxAge, repoVersion,
                repoTimestamp, repoIcon, repoMirrors.toArray(new String[repoMirrors.size()]));
    }

    private void onRepoPushRequestParsed(RepoPushRequest repoPushRequest) {
        receiver.receiveRepoPushRequest(repoPushRequest);
    }

    @Override
    public void startElement(String uri, String localName, String qName,
                             Attributes attributes) throws SAXException {
        super.startElement(uri, localName, qName, attributes);

        if ("repo".equals(localName)) {
            repoSigningCert = attributes.getValue("", "pubkey");
            repoMaxAge = Utils.parseInt(attributes.getValue("", "maxage"), -1);
            repoVersion = Utils.parseInt(attributes.getValue("", "version"), -1);
            repoName = cleanWhiteSpace(attributes.getValue("", "name"));
            repoDescription = cleanWhiteSpace(attributes.getValue("", "description"));
            repoTimestamp = parseLong(attributes.getValue("", "timestamp"), 0);
            repoIcon = attributes.getValue("", "icon");
        } else if (RepoPushRequest.INSTALL.equals(localName)
                || RepoPushRequest.UNINSTALL.equals(localName)) {
            if (repo.pushRequests == Repo.PUSH_REQUEST_ACCEPT_ALWAYS) {
                RepoPushRequest r = new RepoPushRequest(
                        localName,
                        attributes.getValue("packageName"),
                        attributes.getValue("versionCode"));
                onRepoPushRequestParsed(r);
            }
        } else if ("application".equals(localName) && curapp == null) {
            curapp = new App();
            curapp.repoId = repo.getId();
            curapp.packageName = attributes.getValue("", "id");

            // To appease the NON NULL constraint in the DB. Usually there is a description, and it
            // is quite difficult to get an app to _not_ have a description when using fdroidserver.
            // However, it shouldn't crash the client when this happens.
            curapp.description = "";
        } else if ("package".equals(localName) && curapp != null && curapk == null) {
            curapk = new Apk();
            curapk.packageName = curapp.packageName;
            curapk.repoId = repo.getId();
            currentApkHashType = null;

        } else if ("hash".equals(localName) && curapk != null) {
            currentApkHashType = attributes.getValue("", "type");
        } else if ("uses-permission".equals(localName) && curapk != null) {
            String maxSdkVersion = attributes.getValue("maxSdkVersion");
            if (maxSdkVersion == null || Build.VERSION.SDK_INT <= Integer.valueOf(maxSdkVersion)) {
                addRequestedPermission(attributes.getValue("name"));
            } else {
                removeRequestedPermission(attributes.getValue("name"));
            }
        } else if ("uses-permission-sdk-23".equals(localName) && curapk != null) {
            String maxSdkVersion = attributes.getValue("maxSdkVersion");
            if (Build.VERSION.SDK_INT >= 23 &&
                    (maxSdkVersion == null || Build.VERSION.SDK_INT <= Integer.valueOf(maxSdkVersion))) {
                addRequestedPermission(attributes.getValue("name"));
            } else {
                removeRequestedPermission(attributes.getValue("name"));
            }
        }
        curchars.setLength(0);
    }

    private static String cleanWhiteSpace(@Nullable String str) {
        return str == null ? null : str.replaceAll("\\s", " ");
    }

    private static long parseLong(String str, long fallback) {
        if (str == null || str.length() == 0) {
            return fallback;
        }
        long result;
        try {
            result = Long.parseLong(str);
        } catch (NumberFormatException e) {
            result = fallback;
        }
        return result;
    }
}

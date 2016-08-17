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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.Repo;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

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

    // the X.509 signing certificate stored in the header of index.xml
    private String repoSigningCert;

    private final StringBuilder curchars = new StringBuilder();

    interface IndexReceiver {
        void receiveRepo(String name, String description, String signingCert, int maxage, int version, long timestamp);

        void receiveApp(App app, List<Apk> packages);
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
                case "version":
                    curapk.versionName = str;
                    break;
                case "versioncode":
                    curapk.versionCode = Utils.parseInt(str, -1);
                    break;
                case "size":
                    curapk.size = Utils.parseInt(str, 0);
                    break;
                case "hash":
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
                case "sig":
                    curapk.sig = str;
                    break;
                case "srcname":
                    curapk.srcname = str;
                    break;
                case "apkname":
                    curapk.apkName = str;
                    break;
                case "sdkver":
                    curapk.minSdkVersion = Utils.parseInt(str, Apk.SDK_VERSION_MIN_VALUE);
                    break;
                case "targetSdkVersion":
                    curapk.targetSdkVersion = Utils.parseInt(str, Apk.SDK_VERSION_MIN_VALUE);
                    break;
                case "maxsdkver":
                    curapk.maxSdkVersion = Utils.parseInt(str, Apk.SDK_VERSION_MAX_VALUE);
                    if (curapk.maxSdkVersion == 0) {
                        // before fc0df0dcf4dd0d5f13de82d7cd9254b2b48cb62d, this could be 0
                        curapk.maxSdkVersion = Apk.SDK_VERSION_MAX_VALUE;
                    }
                    break;
                case "added":
                    curapk.added = Utils.parseDate(str, null);
                    break;
                case "permissions":
                    curapk.permissions = Utils.parseCommaSeparatedString(str);
                    break;
                case "features":
                    curapk.features = Utils.parseCommaSeparatedString(str);
                    break;
                case "nativecode":
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
                    curapp.description = str;
                    break;
                case "summary":
                    curapp.summary = str;
                    break;
                case "license":
                    curapp.license = str;
                    break;
                case "author":
                    curapp.author = str;
                    break;
                case "email":
                    curapp.email = str;
                    break;
                case "source":
                    curapp.sourceURL = str;
                    break;
                case "changelog":
                    curapp.changelogURL = str;
                    break;
                case "donate":
                    curapp.donateURL = str;
                    break;
                case "bitcoin":
                    curapp.bitcoinAddr = str;
                    break;
                case "litecoin":
                    curapp.litecoinAddr = str;
                    break;
                case "flattr":
                    curapp.flattrID = str;
                    break;
                case "web":
                    curapp.webURL = str;
                    break;
                case "tracker":
                    curapp.trackerURL = str;
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
        }
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
        receiver.receiveRepo(repoName, repoDescription, repoSigningCert, repoMaxAge, repoVersion, repoTimestamp);
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
        } else if ("application".equals(localName) && curapp == null) {
            curapp = new App();
            curapp.packageName = attributes.getValue("", "id");

            // To appease the NON NULL constraint in the DB. Usually there is a description, and it
            // is quite difficult to get an app to _not_ have a description when using fdroidserver.
            // However, it shouldn't crash the client when this happens.
            curapp.description = "";
        } else if ("package".equals(localName) && curapp != null && curapk == null) {
            curapk = new Apk();
            curapk.packageName = curapp.packageName;
            curapk.repo = repo.getId();
            currentApkHashType = null;

        } else if ("hash".equals(localName) && curapk != null) {
            currentApkHashType = attributes.getValue("", "type");
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

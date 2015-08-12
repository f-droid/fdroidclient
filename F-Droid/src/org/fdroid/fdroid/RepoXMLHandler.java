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

import android.text.TextUtils;

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

    private final List<App> apps = new ArrayList<>();
    private final List<Apk> apksList = new ArrayList<>();

    private App curapp = null;
    private Apk curapk = null;
    private final StringBuilder curchars = new StringBuilder();

    // After processing the XML, these will be -1 if the index didn't specify
    // them - otherwise it will be the value specified.
    private int version = -1;
    private int maxage = -1;

    /** the X.509 signing certificate stored in the header of index.xml */
    private String signingCertFromIndexXml;

    private String name;
    private String description;
    private String hashType;

    public RepoXMLHandler(Repo repo) {
        this.repo = repo;
        signingCertFromIndexXml = null;
        name = null;
        description = null;
    }

    public List<App> getApps() { return apps; }

    public List<Apk> getApks() { return apksList; }

    public int getMaxAge() { return maxage; }

    public int getVersion() { return version; }

    public String getDescription() { return description; }

    public String getName() { return name; }

    public String getSigningCertFromIndexXml() { return signingCertFromIndexXml; }

    @Override
    public void characters(char[] ch, int start, int length) {
        curchars.append(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {

        super.endElement(uri, localName, qName);
        final String curel = localName;
        final String str = curchars.toString().trim();
        final boolean empty = TextUtils.isEmpty(str);

        if (curel.equals("application") && curapp != null) {
            apps.add(curapp);
            curapp = null;
            // If the app id is already present in this apps list, then it
            // means the same index file has a duplicate app, which should
            // not be allowed.
            // However, I'm thinking that it should be unefined behaviour,
            // because it is probably a bug in the fdroid server that made it
            // happen, and I don't *think* it will crash the client, because
            // the first app will insert, the second one will update the newly
            // inserted one.
        } else if (curel.equals("package") && curapk != null && curapp != null) {
            apksList.add(curapk);
            curapk = null;
        } else if (!empty && curapk != null) {
            switch (curel) {
            case "version":
                curapk.version = str;
                break;
            case "versioncode":
                curapk.vercode = Utils.parseInt(str, -1);
                break;
            case "size":
                curapk.size = Utils.parseInt(str, 0);
                break;
            case "hash":
                if (hashType == null || hashType.equals("md5")) {
                    if (curapk.hash == null) {
                        curapk.hash = str;
                        curapk.hashType = "MD5";
                    }
                } else if (hashType.equals("sha256")) {
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
                curapk.minSdkVersion = Utils.parseInt(str, 0);
                break;
            case "maxsdkver":
                curapk.maxSdkVersion = Utils.parseInt(str, 0);
                break;
            case "added":
                curapk.added = Utils.parseDate(str, null);
                break;
            case "permissions":
                curapk.permissions = Utils.CommaSeparatedList.make(str);
                break;
            case "features":
                curapk.features = Utils.CommaSeparatedList.make(str);
                break;
            case "nativecode":
                curapk.nativecode = Utils.CommaSeparatedList.make(str);
                break;
            }
        } else if (!empty && curapp != null) {
            switch (curel) {
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
            case "dogecoin":
                curapp.dogecoinAddr = str;
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
                curapp.upstreamVersion = str;
                break;
            case "marketvercode":
                curapp.upstreamVercode = Utils.parseInt(str, -1);
                break;
            case "categories":
                curapp.categories = Utils.CommaSeparatedList.make(str);
                break;
            case "antifeatures":
                curapp.antiFeatures = Utils.CommaSeparatedList.make(str);
                break;
            case "requirements":
                curapp.requirements = Utils.CommaSeparatedList.make(str);
                break;
            }
        } else if (!empty && curel.equals("description")) {
            description = cleanWhiteSpace(str);
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {
        super.startElement(uri, localName, qName, attributes);

        if (localName.equals("repo")) {
            signingCertFromIndexXml = attributes.getValue("", "pubkey");
            maxage = Utils.parseInt(attributes.getValue("", "maxage"), -1);
            version = Utils.parseInt(attributes.getValue("", "version"), -1);

            final String nm = attributes.getValue("", "name");
            if (nm != null)
                name = cleanWhiteSpace(nm);
            final String dc = attributes.getValue("", "description");
            if (dc != null)
                description = cleanWhiteSpace(dc);

        } else if (localName.equals("application") && curapp == null) {
            curapp = new App();
            curapp.id = attributes.getValue("", "id");
        } else if (localName.equals("package") && curapp != null && curapk == null) {
            curapk = new Apk();
            curapk.id = curapp.id;
            curapk.repo = repo.getId();
            hashType = null;

        } else if (localName.equals("hash") && curapk != null) {
            hashType = attributes.getValue("", "type");
        }
        curchars.setLength(0);
    }

    private String cleanWhiteSpace(String str) {
        return str.replaceAll("\n", " ").replaceAll("  ", " ");
    }
}

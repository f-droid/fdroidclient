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

import android.os.Bundle;

import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.updater.RepoUpdater;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

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

    // After processing the XML, this will be null if the index specified a
    // public key - otherwise a public key. This is used for TOFU where an
    // index.xml is read on the first connection, and a signed index.jar is
    // expected on all subsequent connections.
    private String pubkey;

    private String name;
    private String description;
    private String hashType;

    private int progressCounter = 0;
    private final ProgressListener progressListener;

    private int totalAppCount;

    public RepoXMLHandler(Repo repo, ProgressListener listener) {
        this.repo = repo;
        pubkey = null;
        name = null;
        description = null;
        progressListener = listener;
    }

    public List<App> getApps() {
        return apps;
    }

    public List<Apk> getApks() {
        return apksList;
    }

    public int getMaxAge() { return maxage; }

    public int getVersion() { return version; }

    public String getDescription() { return description; }

    public String getName() { return name; }

    public String getPubKey() {
        return pubkey;
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        curchars.append(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {

        super.endElement(uri, localName, qName);
        final String curel = localName;
        String str = curchars.toString();
        if (str != null) {
            str = str.trim();
        }

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
        } else if (curapk != null && str != null) {
            switch (curel) {
            case "version":
                curapk.version = str;
                break;
            case "versioncode":
                try {
                    curapk.vercode = Integer.parseInt(str);
                } catch (NumberFormatException ex) {
                    curapk.vercode = -1;
                }
                break;
            case "size":
                try {
                    curapk.size = Integer.parseInt(str);
                } catch (NumberFormatException ex) {
                    curapk.size = 0;
                }
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
                try {
                    curapk.minSdkVersion = Integer.parseInt(str);
                } catch (NumberFormatException ex) {
                    curapk.minSdkVersion = 0;
                }
                break;
            case "maxsdkver":
                try {
                    curapk.maxSdkVersion = Integer.parseInt(str);
                } catch (NumberFormatException ex) {
                    curapk.maxSdkVersion = 0;
                }
                break;
            case "added":
                try {
                    curapk.added = str.length() == 0 ? null : Utils.DATE_FORMAT
                            .parse(str);
                } catch (ParseException e) {
                    curapk.added = null;
                }
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
        } else if (curapp != null && str != null) {
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
                try {
                    curapp.added = str.length() == 0 ? null : Utils.DATE_FORMAT
                            .parse(str);
                } catch (ParseException e) {
                    curapp.added = null;
                }
                break;
            case "lastupdated":
                try {
                    curapp.lastUpdated = str.length() == 0 ? null
                            : Utils.DATE_FORMAT.parse(str);
                } catch (ParseException e) {
                    curapp.lastUpdated = null;
                }
                break;
            case "marketversion":
                curapp.upstreamVersion = str;
                break;
            case "marketvercode":
                try {
                    curapp.upstreamVercode = Integer.parseInt(str);
                } catch (NumberFormatException ex) {
                    curapp.upstreamVercode = -1;
                }
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
        } else if (curel.equals("description")) {
            description = cleanWhiteSpace(str);
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {
        super.startElement(uri, localName, qName, attributes);

        if (localName.equals("repo")) {
            final String pk = attributes.getValue("", "pubkey");
            if (pk != null)
                pubkey = pk;

            final String maxAgeAttr = attributes.getValue("", "maxage");
            if (maxAgeAttr != null) {
                try {
                    maxage = Integer.parseInt(maxAgeAttr);
                } catch (NumberFormatException nfe) {}
            }

            final String versionAttr = attributes.getValue("", "version");
            if (versionAttr != null) {
                try {
                    version = Integer.parseInt(versionAttr);
                } catch (NumberFormatException nfe) {}
            }

            final String nm = attributes.getValue("", "name");
            if (nm != null)
                name = cleanWhiteSpace(nm);
            final String dc = attributes.getValue("", "description");
            if (dc != null)
                description = cleanWhiteSpace(dc);

        } else if (localName.equals("application") && curapp == null) {
            curapp = new App();
            curapp.id = attributes.getValue("", "id");
            /* show progress for the first 25, then start skipping every 25 */
            if (totalAppCount < 25 || progressCounter % (totalAppCount / 25) == 0) {
                Bundle data = new Bundle(1);
                data.putString(RepoUpdater.PROGRESS_DATA_REPO_ADDRESS, repo.address);
                progressListener.onProgress(
                    new ProgressListener.Event(
                        RepoUpdater.PROGRESS_TYPE_PROCESS_XML,
                        progressCounter, totalAppCount, data));
            }
            progressCounter ++;
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

    public void setTotalAppCount(int totalAppCount) {
        this.totalAppCount = totalAppCount;
    }

    private String cleanWhiteSpace(String str) {
        return str.replaceAll("\n", " ").replaceAll("  ", " ");
    }
}

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
import java.util.*;

public class RepoXMLHandler extends DefaultHandler {

    // The repo we're processing.
    private Repo repo;

    private List<App> apps = new ArrayList<App>();
    private List<Apk> apksList = new ArrayList<Apk>();

    private App curapp = null;
    private Apk curapk = null;
    private StringBuilder curchars = new StringBuilder();

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
    private ProgressListener progressListener;

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
        String curel = localName;
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
            if (curel.equals("version")) {
                curapk.version = str;
            } else if (curel.equals("versioncode")) {
                try {
                    curapk.vercode = Integer.parseInt(str);
                } catch (NumberFormatException ex) {
                    curapk.vercode = -1;
                }
            } else if (curel.equals("size")) {
                try {
                    curapk.size = Integer.parseInt(str);
                } catch (NumberFormatException ex) {
                    curapk.size = 0;
                }
            } else if (curel.equals("hash")) {
                if (hashType == null || hashType.equals("md5")) {
                    if (curapk.hash == null) {
                        curapk.hash = str;
                        curapk.hashType = "MD5";
                    }
                } else if (hashType.equals("sha256")) {
                    curapk.hash = str;
                    curapk.hashType = "SHA-256";
                }
            } else if (curel.equals("sig")) {
                curapk.sig = str;
            } else if (curel.equals("srcname")) {
                curapk.srcname = str;
            } else if (curel.equals("apkname")) {
                curapk.apkName = str;
            } else if (curel.equals("sdkver")) {
                try {
                    curapk.minSdkVersion = Integer.parseInt(str);
                } catch (NumberFormatException ex) {
                    curapk.minSdkVersion = 0;
                }
            } else if (curel.equals("maxsdkver")) {
                try {
                    curapk.maxSdkVersion = Integer.parseInt(str);
                } catch (NumberFormatException ex) {
                    curapk.maxSdkVersion = 0;
                }
            } else if (curel.equals("added")) {
                try {
                    curapk.added = str.length() == 0 ? null : Utils.DATE_FORMAT
                            .parse(str);
                } catch (ParseException e) {
                    curapk.added = null;
                }
            } else if (curel.equals("permissions")) {
                curapk.permissions = Utils.CommaSeparatedList.make(str);
            } else if (curel.equals("features")) {
                curapk.features = Utils.CommaSeparatedList.make(str);
            } else if (curel.equals("nativecode")) {
                curapk.nativecode = Utils.CommaSeparatedList.make(str);
            }
        } else if (curapp != null && str != null) {
            if (curel.equals("name")) {
                curapp.name = str;
            } else if (curel.equals("icon")) {
                curapp.icon = str;
            } else if (curel.equals("description")) {
                // This is the old-style description. We'll read it
                // if present, to support old repos, but in newer
                // repos it will get overwritten straight away!
                curapp.description = "<p>" + str + "</p>";
            } else if (curel.equals("desc")) {
                // New-style description.
                curapp.description = str;
            } else if (curel.equals("summary")) {
                curapp.summary = str;
            } else if (curel.equals("license")) {
                curapp.license = str;
            } else if (curel.equals("source")) {
                curapp.sourceURL = str;
            } else if (curel.equals("donate")) {
                curapp.donateURL = str;
            } else if (curel.equals("bitcoin")) {
                curapp.bitcoinAddr = str;
            } else if (curel.equals("litecoin")) {
                curapp.litecoinAddr = str;
            } else if (curel.equals("dogecoin")) {
                curapp.dogecoinAddr = str;
            } else if (curel.equals("flattr")) {
                curapp.flattrID = str;
            } else if (curel.equals("web")) {
                curapp.webURL = str;
            } else if (curel.equals("tracker")) {
                curapp.trackerURL = str;
            } else if (curel.equals("added")) {
                try {
                    curapp.added = str.length() == 0 ? null : Utils.DATE_FORMAT
                            .parse(str);
                } catch (ParseException e) {
                    curapp.added = null;
                }
            } else if (curel.equals("lastupdated")) {
                try {
                    curapp.lastUpdated = str.length() == 0 ? null
                            : Utils.DATE_FORMAT.parse(str);
                } catch (ParseException e) {
                    curapp.lastUpdated = null;
                }
            } else if (curel.equals("marketversion")) {
                curapp.upstreamVersion = str;
            } else if (curel.equals("marketvercode")) {
                try {
                    curapp.upstreamVercode = Integer.parseInt(str);
                } catch (NumberFormatException ex) {
                    curapp.upstreamVercode = -1;
                }
            } else if (curel.equals("categories")) {
                curapp.categories = Utils.CommaSeparatedList.make(str);
            } else if (curel.equals("antifeatures")) {
                curapp.antiFeatures = Utils.CommaSeparatedList.make(str);
            } else if (curel.equals("requirements")) {
                curapp.requirements = Utils.CommaSeparatedList.make(str);
            }
        } else if (curel.equals("description")) {
            description = str;
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {
        super.startElement(uri, localName, qName, attributes);

        if (localName.equals("repo")) {
            String pk = attributes.getValue("", "pubkey");
            if (pk != null)
                pubkey = pk;

            String maxAgeAttr = attributes.getValue("", "maxage");
            if (maxAgeAttr != null) {
                try {
                    maxage = Integer.parseInt(maxAgeAttr);
                } catch (NumberFormatException nfe) {}
            }

            String versionAttr = attributes.getValue("", "version");
            if (versionAttr != null) {
                try {
                    version = Integer.parseInt(versionAttr);
                } catch (NumberFormatException nfe) {}
            }

            String nm = attributes.getValue("", "name");
            if (nm != null)
                name = nm;
            String dc = attributes.getValue("", "description");
            if (dc != null)
                description = dc;

        } else if (localName.equals("application") && curapp == null) {
            curapp = new App();
            curapp.id = attributes.getValue("", "id");
            Bundle progressData = RepoUpdater.createProgressData(repo.address);
            progressCounter ++;
            progressListener.onProgress(
                new ProgressListener.Event(
                    RepoUpdater.PROGRESS_TYPE_PROCESS_XML, progressCounter,
                    totalAppCount, progressData));

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
}

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
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.updater.RepoUpdater;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RepoXMLHandler extends DefaultHandler {

    // The repo we're processing.
    private Repo repo;

    private Map<String, DB.App> apps;
    private List<DB.App> appsList;

    private DB.App curapp = null;
    private DB.Apk curapk = null;
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

    public RepoXMLHandler(Repo repo, List<DB.App> appsList, ProgressListener listener) {
        this.repo = repo;
        this.apps = new HashMap<String, DB.App>();
        for (DB.App app : appsList) this.apps.put(app.id, app);
        this.appsList = appsList;
        pubkey = null;
        name = null;
        description = null;
        progressListener = listener;
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

            // If we already have this application (must be from scanning a
            // different repo) then just merge in the apks.
            DB.App app = apps.get(curapp.id);
            if (app != null) {
                app.apks.addAll(curapp.apks);
            } else {
                appsList.add(curapp);
                apps.put(curapp.id, curapp);
            }

            curapp = null;

        } else if (curel.equals("package") && curapk != null && curapp != null) {
            curapp.apks.add(curapk);
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
                    curapk.detail_size = Integer.parseInt(str);
                } catch (NumberFormatException ex) {
                    curapk.detail_size = 0;
                }
            } else if (curel.equals("hash")) {
                if (hashType == null || hashType.equals("md5")) {
                    if (curapk.detail_hash == null) {
                        curapk.detail_hash = str;
                        curapk.detail_hashType = "MD5";
                    }
                } else if (hashType.equals("sha256")) {
                    curapk.detail_hash = str;
                    curapk.detail_hashType = "SHA-256";
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
            } else if (curel.equals("added")) {
                try {
                    curapk.added = str.length() == 0 ? null : DB.DATE_FORMAT
                            .parse(str);
                } catch (ParseException e) {
                    curapk.added = null;
                }
            } else if (curel.equals("permissions")) {
                curapk.detail_permissions = DB.CommaSeparatedList.make(str);
            } else if (curel.equals("features")) {
                curapk.features = DB.CommaSeparatedList.make(str);
            } else if (curel.equals("nativecode")) {
                curapk.nativecode = DB.CommaSeparatedList.make(str);
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
                curapp.detail_description = "<p>" + str + "</p>";
            } else if (curel.equals("desc")) {
                // New-style description.
                curapp.detail_description = str;
            } else if (curel.equals("summary")) {
                curapp.summary = str;
            } else if (curel.equals("license")) {
                curapp.license = str;
            } else if (curel.equals("source")) {
                curapp.detail_sourceURL = str;
            } else if (curel.equals("donate")) {
                curapp.detail_donateURL = str;
            } else if (curel.equals("bitcoin")) {
                curapp.detail_bitcoinAddr = str;
            } else if (curel.equals("litecoin")) {
                curapp.detail_litecoinAddr = str;
            } else if (curel.equals("dogecoin")) {
                curapp.detail_dogecoinAddr = str;
            } else if (curel.equals("flattr")) {
                curapp.detail_flattrID = str;
            } else if (curel.equals("web")) {
                curapp.detail_webURL = str;
            } else if (curel.equals("tracker")) {
                curapp.detail_trackerURL = str;
            } else if (curel.equals("added")) {
                try {
                    curapp.added = str.length() == 0 ? null : DB.DATE_FORMAT
                            .parse(str);
                } catch (ParseException e) {
                    curapp.added = null;
                }
            } else if (curel.equals("lastupdated")) {
                try {
                    curapp.lastUpdated = str.length() == 0 ? null
                            : DB.DATE_FORMAT.parse(str);
                } catch (ParseException e) {
                    curapp.lastUpdated = null;
                }
            } else if (curel.equals("marketversion")) {
                curapp.curVersion = str;
            } else if (curel.equals("marketvercode")) {
                try {
                    curapp.curVercode = Integer.parseInt(str);
                } catch (NumberFormatException ex) {
                    curapp.curVercode = -1;
                }
            } else if (curel.equals("categories")) {
                curapp.categories = DB.CommaSeparatedList.make(str);
            } else if (curel.equals("antifeatures")) {
                curapp.antiFeatures = DB.CommaSeparatedList.make(str);
            } else if (curel.equals("requirements")) {
                curapp.requirements = DB.CommaSeparatedList.make(str);
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
            curapp = new DB.App();
            curapp.detail_Populated = true;
            curapp.id = attributes.getValue("", "id");
            Bundle progressData = RepoUpdater.createProgressData(repo.address);
            progressCounter ++;
            progressListener.onProgress(
                new ProgressListener.Event(
                    RepoUpdater.PROGRESS_TYPE_PROCESS_XML, progressCounter,
                    totalAppCount, progressData));

        } else if (localName.equals("package") && curapp != null && curapk == null) {
            curapk = new DB.Apk();
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

/*
 * Copyright (C) 2010  Ciaran Gultnieks, ciaran@ciarang.com
 * Copyright (C) 2009  Roberto Jacinto, roberto.jacinto@caixamagica.pt
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Vector;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

public class RepoXMLHandler extends DefaultHandler {

    String mserver;

    private DB db;

    private DB.App curapp = null;
    private DB.Apk curapk = null;
    private String curel = null;

    public RepoXMLHandler(String srv, DB db) {
        mserver = srv;
        this.db = db;
    }

    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {

        super.characters(ch, start, length);

        String str = new String(ch).substring(start, start + length);
        if (curapk != null && curel != null) {
            if (curel == "version") {
                curapk.version = str;
            } else if (curel == "versioncode") {
                try {
                    curapk.vercode = Integer.parseInt(str);
                } catch (NumberFormatException ex) {
                    curapk.vercode = 0;
                }
            } else if (curel == "size") {
                try {
                    curapk.size = Integer.parseInt(str);
                } catch (NumberFormatException ex) {
                    curapk.size = 0;
                }
            } else if (curel == "hash") {
                curapk.hash = str;
            } else if (curel == "apkname") {
                curapk.apkName = str;
            }
        } else if (curapp != null && curel != null) {
            if (curel == "id") {
                curapp.id = str;
            } else if (curel == "name") {
                curapp.name = str;
            } else if (curel == "icon") {
                curapp.icon = str;
            } else if (curel == "description") {
                curapp.description = str;
            } else if (curel == "summary") {
                curapp.summary = str;
            } else if (curel == "license") {
                curapp.license = str;
            } else if (curel == "source") {
                curapp.sourceURL = str;
            } else if (curel == "web") {
                curapp.webURL = str;
            } else if (curel == "tracker") {
                curapp.trackerURL = str;
            } else if (curel == "marketversion") {
                curapp.marketVersion = str;
            } else if (curel == "marketvercode") {
                try {
                    curapp.marketVercode = Integer.parseInt(str);
                } catch (NumberFormatException ex) {
                    curapp.marketVercode = 0;
                }
            }
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {

        super.endElement(uri, localName, qName);

        if (localName == "application" && curapp != null) {
            Log.d("FDroid", "Repo: Updating application " + curapp.id);
            db.updateApplication(curapp);
            getIcon(curapp);
            curapp = null;
        } else if (localName == "package" && curapk != null && curapp != null) {
            Log.d("FDroid", "Repo: Package added (" + curapk.version + ")");
            curapp.apks.add(curapk);
            curapk = null;
        } else {
            curel = null;
        }

    }

    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {

        super.startElement(uri, localName, qName, attributes);
        if (localName == "application" && curapp == null) {
            Log.d("FDroid", "Repo: Found application at " + mserver);
            curapp = new DB.App();
        } else if (localName == "package" && curapp != null && curapk == null) {
            Log.d("FDroid", "Repo: Found package for " + curapp.id);
            curapk = new DB.Apk();
            curapk.id = curapp.id;
            curapk.server = mserver;
        } else {
            curel = localName;
        }
    }

    private void getIcon(DB.App app) {
        try {

            String destpath = DB.getIconsPath() + app.icon;
            File f = new File(destpath);
            if (f.exists())
                return;

            BufferedInputStream getit = new BufferedInputStream(new URL(mserver
                    + "/icons/" + app.icon).openStream());
            FileOutputStream saveit = new FileOutputStream(destpath);
            BufferedOutputStream bout = new BufferedOutputStream(saveit, 1024);
            byte data[] = new byte[1024];

            int readed = getit.read(data, 0, 1024);
            while (readed != -1) {
                bout.write(data, 0, readed);
                readed = getit.read(data, 0, 1024);
            }
            bout.close();
            getit.close();
            saveit.close();
        } catch (Exception e) {

        }
    }

    private static String LOCAL_PATH = "/sdcard/.fdroid";
    private static String XML_PATH = LOCAL_PATH + "/repotemp.xml";

    public static void doUpdates(DB db) {
        db.beginUpdate();
        Vector<DB.Repo> repos = db.getRepos();
        for (DB.Repo repo : repos) {
            if (repo.inuse) {

                try {

                    File f = new File(XML_PATH);
                    if (f.exists())
                        f.delete();

                    // Download the index file from the repo...
                    BufferedInputStream getit = new BufferedInputStream(
                            new URL(repo.address + "/index.xml").openStream());

                    FileOutputStream saveit = new FileOutputStream(XML_PATH);
                    BufferedOutputStream bout = new BufferedOutputStream(
                            saveit, 1024);
                    byte data[] = new byte[1024];

                    int readed = getit.read(data, 0, 1024);
                    while (readed != -1) {
                        bout.write(data, 0, readed);
                        readed = getit.read(data, 0, 1024);
                    }
                    bout.close();
                    getit.close();
                    saveit.close();

                    // Process the index...
                    SAXParserFactory spf = SAXParserFactory.newInstance();
                    SAXParser sp = spf.newSAXParser();
                    XMLReader xr = sp.getXMLReader();
                    RepoXMLHandler handler = new RepoXMLHandler(repo.address, db);
                    xr.setContentHandler(handler);

                    InputStreamReader isr = new FileReader(new File(XML_PATH));
                    InputSource is = new InputSource(isr);
                    xr.parse(is);
                    File xml_file = new File(XML_PATH);
                    xml_file.delete();

                } catch (Exception e) {
                    Log.d("FDroid", "Exception updating from " + repo.address
                            + " - " + e.getMessage());
                }

            }
        }
        db.endUpdate();

    }

}

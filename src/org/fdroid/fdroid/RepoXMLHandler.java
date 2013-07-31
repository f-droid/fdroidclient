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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.net.ssl.SSLHandshakeException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import android.os.Bundle;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

public class RepoXMLHandler extends DefaultHandler {

    // The repo we're processing.
    private DB.Repo repo;

    private List<DB.App> apps;

    private DB.App curapp = null;
    private DB.Apk curapk = null;
    private StringBuilder curchars = new StringBuilder();

    private String pubkey;
    private String name;
    private String description;
    private String hashType;

    private int progressCounter = 0;
    private ProgressListener progressListener;

    public static final int PROGRESS_TYPE_DOWNLOAD     = 1;
    public static final int PROGRESS_TYPE_PROCESS_XML  = 2;

	public static final String PROGRESS_DATA_REPO = "repo";

    // The date format used in the repo XML file.
    private SimpleDateFormat mXMLDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat logDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private int totalAppCount;

    public RepoXMLHandler(DB.Repo repo, List<DB.App> apps, ProgressListener listener) {
        this.repo = repo;
        this.apps = apps;
        pubkey = null;
        name = null;
        description = null;
        progressListener = listener;
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
            // TODO: Scanning the whole app list like this every time is
            // going to be stupid if the list gets very big!
            boolean merged = false;
            for (DB.App app : apps) {
                if (app.id.equals(curapp.id)) {
                    app.apks.addAll(curapp.apks);
                    merged = true;
                    break;
                }
            }
            if (!merged)
                apps.add(curapp);

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
                    curapk.added = str.length() == 0 ? null : mXMLDateFormat
                            .parse(str);
                } catch (ParseException e) {
                    curapk.added = null;
                }
            } else if (curel.equals("permissions")) {
                curapk.detail_permissions = DB.CommaSeparatedList.make(str);
            } else if (curel.equals("features")) {
                curapk.features = DB.CommaSeparatedList.make(str);
            }
        } else if (curapp != null && str != null) {
            if (curel.equals("id")) {
                curapp.id = str;
            } else if (curel.equals("name")) {
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
            } else if (curel.equals("category")) {
                curapp.category = str;
            } else if (curel.equals("source")) {
                curapp.detail_sourceURL = str;
            } else if (curel.equals("donate")) {
                curapp.detail_donateURL = str;
            } else if (curel.equals("bitcoin")) {
                curapp.detail_bitcoinAddr = str;
            } else if (curel.equals("flattr")) {
                curapp.detail_flattrID = str;
            } else if (curel.equals("web")) {
                curapp.detail_webURL = str;
            } else if (curel.equals("tracker")) {
                curapp.detail_trackerURL = str;
            } else if (curel.equals("added")) {
                try {
                    curapp.added = str.length() == 0 ? null : mXMLDateFormat
                            .parse(str);
                } catch (ParseException e) {
                    curapp.added = null;
                }
            } else if (curel.equals("lastupdated")) {
                try {
                    curapp.lastUpdated = str.length() == 0 ? null
                            : mXMLDateFormat.parse(str);
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
            } else if (curel.equals("antifeatures")) {
                curapp.antiFeatures = DB.CommaSeparatedList.make(str);
            } else if (curel.equals("requirements")) {
                curapp.requirements = DB.CommaSeparatedList.make(str);
            }
        }
    }

	private static Bundle createProgressData(String repoAddress) {
		Bundle data = new Bundle();
		data.putString(PROGRESS_DATA_REPO, repoAddress);
		return data;
	}

    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {
        super.startElement(uri, localName, qName, attributes);
        if (localName.equals("repo")) {
            String pk = attributes.getValue("", "pubkey");
            if (pk != null)
                pubkey = pk;
            String nm = attributes.getValue("", "name");
            if (nm != null)
                name = nm;
            String dc = attributes.getValue("", "description");
            if (dc != null)
                description = dc;
        } else if (localName.equals("application") && curapp == null) {
            curapp = new DB.App();
            curapp.detail_Populated = true;
            Bundle progressData = createProgressData(repo.address);
            progressCounter ++;
            progressListener.onProgress(
                new ProgressListener.Event(
                    RepoXMLHandler.PROGRESS_TYPE_PROCESS_XML, progressCounter,
                    totalAppCount, progressData));
        } else if (localName.equals("package") && curapp != null && curapk == null) {
            curapk = new DB.Apk();
            curapk.id = curapp.id;
            curapk.repo = repo.id;
            hashType = null;
        } else if (localName.equals("hash") && curapk != null) {
            hashType = attributes.getValue("", "type");
        }
        curchars.setLength(0);
    }

    // Get a remote file. Returns the HTTP response code.
    // If 'etag' is not null, it's passed to the server as an If-None-Match
    // header, in which case expect a 304 response if nothing changed.
    // In the event of a 200 response ONLY, 'retag' (which should be passed
    // empty) may contain an etag value for the response, or it may be left
    // empty if none was available.
    private static int getRemoteFile(Context ctx, String url, String dest,
            String etag, StringBuilder retag,
            ProgressListener progressListener,
            ProgressListener.Event progressEvent) throws MalformedURLException,
            IOException {

        long startTime = System.currentTimeMillis();
        URL u = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) u.openConnection();
        if (etag != null)
            connection.setRequestProperty("If-None-Match", etag);
        int code = connection.getResponseCode();
        if (code == 200) {
            // Testing in the emulator for me, showed that figuring out the filesize took about 1 to 1.5 seconds.
            // To put this in context, downloading a repo of:
            //  - 400k takes ~6 seconds
            //  - 5k   takes ~3 seconds
            // on my connection. I think the 1/1.5 seconds is worth it, because as the repo grows, the tradeoff will
            // become more worth it.
            progressEvent.total = connection.getContentLength();
            Log.d("FDroid", "Downloading " + progressEvent.total + " bytes from " + url);
            InputStream input = null;
            OutputStream output = null;
            try {
                input = connection.getInputStream();
                output = ctx.openFileOutput(dest, Context.MODE_PRIVATE);
                Utils.copy(input, output, progressListener, progressEvent);
            } finally {
                Utils.closeQuietly(output);
                Utils.closeQuietly(input);
            }

            String et = connection.getHeaderField("ETag");
            if (et != null)
                retag.append(et);
        }
        Log.d("FDroid", "Fetched " + url + " (" + progressEvent.total +
                " bytes) in " + (System.currentTimeMillis() - startTime) +
                "ms");
        return code;

    }

    // Do an update from the given repo. All applications found, and their
    // APKs, are added to 'apps'. (If 'apps' already contains an app, its
    // APKs are merged into the existing one).
    // Returns null if successful, otherwise an error message to be displayed
    // to the user (if there is an interactive user!)
    // 'newetag' should be passed empty. On success, it may contain an etag
    // value for the index that was successfully processed, or it may contain
    // null if none was available.
    public static String doUpdate(Context ctx, DB.Repo repo,
            List<DB.App> apps, StringBuilder newetag, List<Integer> keeprepos,
            ProgressListener progressListener) {
        try {

            int code = 0;
            if (repo.pubkey != null) {

                // This is a signed repo - we download the jar file,
                // check the signature, and extract the index...
                Log.d("FDroid", "Getting signed index from " + repo.address + " at " + 
                    logDateFormat.format(new Date(System.currentTimeMillis())));
                String address = repo.address + "/index.jar";
                PackageManager pm = ctx.getPackageManager();
                try {
                    PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(), 0);
                    address += "?" + pi.versionName;
                } catch (Exception e) {
                }
                Bundle progressData = createProgressData(repo.address);
                ProgressListener.Event event = new ProgressListener.Event(
                        RepoXMLHandler.PROGRESS_TYPE_DOWNLOAD, progressData);
                code = getRemoteFile(ctx, address, "tempindex.jar",
                        repo.lastetag, newetag, progressListener, event );
                if (code == 200) {
                    String jarpath = ctx.getFilesDir() + "/tempindex.jar";
                    JarFile jar = null;
                    JarEntry je;
                    Certificate[] certs;
                    try {
                        jar = new JarFile(jarpath, true);
                        je = (JarEntry) jar.getEntry("index.xml");
                        File efile = new File(ctx.getFilesDir(),
                                "/tempindex.xml");
                        InputStream input = null;
                        OutputStream output = null;
                        try {
                            input = jar.getInputStream(je);
                            output = new FileOutputStream(efile);
                            Utils.copy(input, output);
                        } finally {
                            Utils.closeQuietly(output);
                            Utils.closeQuietly(input);
                        }
                        certs = je.getCertificates();
                    } catch (SecurityException e) {
                        Log.e("FDroid", "Invalid hash for index file");
                        return "Invalid hash for index file";
                    } finally {
                        if (jar != null) {
                            jar.close();
                        }
                    }
                    if (certs == null) {
                        Log.d("FDroid", "No signature found in index");
                        return "No signature found in index";
                    }
                    Log.d("FDroid", "Index has " + certs.length + " signature"
                            + (certs.length > 1 ? "s." : "."));

                    boolean match = false;
                    for (Certificate cert : certs) {
                        String certdata = Hasher.hex(cert.getEncoded());
                        if (repo.pubkey.equals(certdata)) {
                            match = true;
                            break;
                        }
                    }
                    if (!match) {
                        Log.d("FDroid", "Index signature mismatch");
                        return "Index signature mismatch";
                    }
                }

            } else {

                // It's an old-fashioned unsigned repo...
                Log.d("FDroid", "Getting unsigned index from " + repo.address);
                Bundle eventData = createProgressData(repo.address);
                ProgressListener.Event event = new ProgressListener.Event(
                        RepoXMLHandler.PROGRESS_TYPE_DOWNLOAD, eventData);
                code = getRemoteFile(ctx, repo.address + "/index.xml",
                        "tempindex.xml", repo.lastetag, newetag,
                        progressListener, event);
            }

            if (code == 200) {
                // Process the index...
                SAXParserFactory spf = SAXParserFactory.newInstance();
                SAXParser sp = spf.newSAXParser();
                XMLReader xr = sp.getXMLReader();
                RepoXMLHandler handler = new RepoXMLHandler(repo, apps, progressListener);
                xr.setContentHandler(handler);

                File tempIndex = new File(ctx.getFilesDir() + "/tempindex.xml");
                BufferedReader r = new BufferedReader(new FileReader(tempIndex));

                // A bit of a hack, this might return false positives if an apps description
                // or some other part of the XML file contains this, but it is a pretty good
                // estimate and makes the progress counter more informative.
                // As with asking the server about the size of the index before downloading,
                // this also has a time tradeoff. It takes about three seconds to iterate
                // through the file and count 600 apps on a slow emulator (v17), but if it is
                // taking two minutes to update, the three second wait may be worth it.
                final String APPLICATION = "<application";
                handler.setTotalAppCount(Utils.countSubstringOccurrence(tempIndex, APPLICATION));

                InputSource is = new InputSource(r);
                xr.parse(is);

                if (handler.pubkey != null && repo.pubkey == null) {
                    // We read an unsigned index, but that indicates that
                    // a signed version is now available...
                    Log.d("FDroid",
                            "Public key found - switching to signed repo for future updates");
                    repo.pubkey = handler.pubkey;
                    try {
                        DB db = DB.getDB();
                        db.updateRepoByAddress(repo);
                    } finally {
                        DB.releaseDB();
                    }
                }

            } else if (code == 304) {
                // The index is unchanged since we last read it. We just mark
                // everything that came from this repo as being updated.
                Log.d("FDroid", "Repo index for " + repo.address
                        + " is up to date (by etag)");
                keeprepos.add(repo.id);
                // Make sure we give back the same etag. (The 200 route will
                // have supplied a new one.
                newetag.append(repo.lastetag);

            } else {
                return "Failed to read index - HTTP response "
                        + Integer.toString(code);
            }

        } catch (SSLHandshakeException sslex) {
            Log.e("FDroid", "SSLHandShakeException updating from "
                    + repo.address + ":\n" + Log.getStackTraceString(sslex));
            return "A problem occurred while establishing an SSL connection. If this problem persists, AND you have a very old device, you could try using http instead of https for the repo URL.";
        } catch (Exception e) {
            Log.e("FDroid", "Exception updating from " + repo.address + ":\n"
                    + Log.getStackTraceString(e));
            return "Failed to update - " + e.getMessage();
        } finally {
            ctx.deleteFile("tempindex.xml");
            ctx.deleteFile("tempindex.jar");
        }

        return null;
    }

    public void setTotalAppCount(int totalAppCount) {
        this.totalAppCount = totalAppCount;
    }
}

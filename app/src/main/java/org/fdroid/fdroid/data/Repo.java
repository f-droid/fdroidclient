/*
 * Copyright (C) 2016 Blue Jay Wireless
 * Copyright (C) 2014-2016 Daniel Martí <mvdan@mvdan.cc>
 * Copyright (C) 2014-2016 Hans-Christoph Steiner <hans@eds.org>
 * Copyright (C) 2014-2016 Peter Serwylo <peter@serwylo.com>
 * Copyright (C) 2015 Christian Morgner
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */

package org.fdroid.fdroid.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.fdroid.download.DownloadRequest;
import org.fdroid.download.Mirror;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Schema.RepoTable.Cols;
import org.fdroid.fdroid.net.TreeUriDownloader;

import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import info.guardianproject.netcipher.NetCipher;


/**
 * Represents a the descriptive info and metadata about a given repo, as provided
 * by the repo index.  This also keeps track of the state of the repo.
 * <p>
 * <b>Do not rename these instance variables without careful consideration!</b>
 * They are mapped to JSON field names, the {@code fdroidserver} internal variable
 * names, and the {@code fdroiddata} YAML field names.  Only the instance variables
 * decorated with {@code @JsonIgnore} are not directly mapped.
 *
 * @see <a href="https://gitlab.com/fdroid/fdroiddata">fdroiddata</a>
 * @see <a href="https://gitlab.com/fdroid/fdroidserver">fdroidserver</a>
 */
public class Repo extends ValueObject {

    public static final int VERSION_DENSITY_SPECIFIC_ICONS = 11;

    public static final int PUSH_REQUEST_IGNORE = 0;
    public static final int PUSH_REQUEST_PROMPT = 1;
    public static final int PUSH_REQUEST_ACCEPT_ALWAYS = 2;

    public static final int INT_UNSET_VALUE = -1;
    // these are never set by the Apk/package index metadata
    @JsonIgnore
    protected long id;
    @JsonIgnore
    public boolean inuse;
    @JsonIgnore
    public int priority;
    @JsonIgnore
    public Date lastUpdated;
    @JsonIgnore
    public boolean isSwap;
    /**
     * last etag we updated from, null forces update
     */
    @JsonIgnore
    public String lastetag;
    /**
     * How to treat push requests included in this repo's index XML. This comes
     * from {@code default_repo.xml} or perhaps user input.  It should never be
     * settable from the server-side.
     */
    @JsonIgnore
    public int pushRequests = PUSH_REQUEST_IGNORE;

    /**
     * The canonical URL of the repo.
     */
    public String address;
    public String name;
    public String description;
    public String icon;
    /**
     * index version, i.e. what fdroidserver built it - 0 if not specified
     */
    public int version;
    /**
     * The signing certificate, {@code null} for a newly added repo
     */
    public String signingCertificate;
    /**
     * The SHA1 fingerprint of {@link #signingCertificate}, set to {@code null} when a
     * newly added repo did not include fingerprint. It should never be an
     * empty {@link String}, i.e. {@code ""}
     */
    public String fingerprint;
    /**
     * maximum age of index that will be accepted - 0 for any
     */
    public int maxage;

    public String username;
    public String password;

    /**
     * When the signed repo index was generated, used to protect against replay attacks
     */
    public long timestamp;

    /**
     * Official mirrors of this repo, considered automatically interchangeable
     */
    public String[] mirrors;

    /**
     * Mirrors added by the user, either by UI input or by attaching removeable storage
     */
    @JsonIgnore
    public String[] userMirrors;

    /**
     * Mirrors that have been manually disabled by the user.
     */
    @JsonIgnore
    public String[] disabledMirrors;

    public Repo() {
    }

    public Repo(String address) {
        this.address = address;
    }

    public Repo(Cursor cursor) {

        checkCursorPosition(cursor);

        for (int i = 0; i < cursor.getColumnCount(); i++) {
            switch (cursor.getColumnName(i)) {
                case Cols._ID:
                    id = cursor.getInt(i);
                    break;
                case Cols.LAST_ETAG:
                    lastetag = cursor.getString(i);
                    break;
                case Cols.ADDRESS:
                    address = cursor.getString(i);
                    break;
                case Cols.DESCRIPTION:
                    description = cursor.getString(i);
                    break;
                case Cols.FINGERPRINT:
                    fingerprint = cursor.getString(i);
                    break;
                case Cols.IN_USE:
                    inuse = cursor.getInt(i) == 1;
                    break;
                case Cols.LAST_UPDATED:
                    String dateString = cursor.getString(i);
                    lastUpdated = Utils.parseTime(dateString, Utils.parseDate(dateString, null));
                    break;
                case Cols.MAX_AGE:
                    maxage = cursor.getInt(i);
                    break;
                case Cols.VERSION:
                    version = cursor.getInt(i);
                    break;
                case Cols.NAME:
                    name = cursor.getString(i);
                    break;
                case Cols.SIGNING_CERT:
                    signingCertificate = cursor.getString(i);
                    break;
                case Cols.PRIORITY:
                    priority = cursor.getInt(i);
                    break;
                case Cols.IS_SWAP:
                    isSwap = cursor.getInt(i) == 1;
                    break;
                case Cols.USERNAME:
                    username = cursor.getString(i);
                    break;
                case Cols.PASSWORD:
                    password = cursor.getString(i);
                    break;
                case Cols.TIMESTAMP:
                    timestamp = cursor.getLong(i);
                    break;
                case Cols.ICON:
                    icon = cursor.getString(i);
                    break;
                case Cols.MIRRORS:
                    mirrors = Utils.parseCommaSeparatedString(cursor.getString(i));
                    break;
                case Cols.USER_MIRRORS:
                    userMirrors = Utils.parseCommaSeparatedString(cursor.getString(i));
                    break;
                case Cols.DISABLED_MIRRORS:
                    disabledMirrors = Utils.parseCommaSeparatedString(cursor.getString(i));
                    break;
                case Cols.PUSH_REQUESTS:
                    pushRequests = cursor.getInt(i);
                    break;
            }
        }
    }

    /**
     * @return the database ID to find this repo in the database
     */
    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return address;
    }

    public boolean isSigned() {
        return !TextUtils.isEmpty(this.signingCertificate);
    }

    /**
     * This happens when a repo is configed with a fingerprint, but the client
     * has not connected to it yet to download its signing certificate
     */
    public boolean isSignedButUnverified() {
        return TextUtils.isEmpty(this.signingCertificate) && !TextUtils.isEmpty(this.fingerprint);
    }

    public boolean hasBeenUpdated() {
        return this.lastetag != null;
    }

    /**
     * If we haven't run an update for this repo yet, then the name
     * will be unknown, in which case we will just take a guess at an
     * appropriate name based on the url (e.g. "f-droid.org/archive")
     */
    public static String addressToName(String address) {
        String tempName;
        try {
            URL url = new URL(address);
            tempName = url.getHost() + url.getPath();
        } catch (MalformedURLException e) {
            tempName = address;
        }
        return tempName;
    }

    /**
     * Gets the path relative to the repo root.
     * Can be used to create URLs for use with mirrors.
     * Attention: This does NOT encode for use in URLs.
     */
    public String getPath(String... pathElements) {
        /* Each String in pathElements might contain a /, should keep these as path elements */
        ArrayList<String> elements = new ArrayList<>();
        for (String element : pathElements) {
            Collections.addAll(elements, element.split("/"));
        }
        // build up path WITHOUT encoding the segments, this will happen later when turned into URL
        StringBuilder sb = new StringBuilder();
        for (String element : elements) {
            sb.append(element).append("/");
        }
        sb.deleteCharAt(sb.length() - 1); // remove trailing slash
        return sb.toString();
    }

    @Deprecated // not taking mirrors into account
    public String getFileUrl(String... pathElements) {
        /* Each String in pathElements might contain a /, should keep these as path elements */
        List<String> elements = new ArrayList();
        for (String element : pathElements) {
            for (String elementPart : element.split("/")) {
                elements.add(elementPart);
            }
        }

        /**
         * Storage Access Framework URLs have this wacky URL-encoded path within the URL path.
         *
         * i.e.
         * content://authority/tree/313E-1F1C%3A/document/313E-1F1C%3Aguardianproject.info%2Ffdroid%2Frepo
         *
         * Currently don't know a better way to identify these than by content:// prefix,
         * seems the Android SDK expects apps to consider them as opaque identifiers.
         */
        if (address.startsWith("content://")) {
            StringBuilder result = new StringBuilder(address);
            for (String element : elements) {
                result.append(TreeUriDownloader.ESCAPED_SLASH);
                result.append(element);
            }
            return result.toString();
        } else { // Normal URL
            Uri.Builder result = Uri.parse(address).buildUpon();
            for (String element : elements) {
                result.appendPath(element);
            }
            return result.build().toString();
        }
    }

    public DownloadRequest getDownloadRequest(String path) {
        List<Mirror> mirrors = Mirror.fromStrings(getMirrorList());
        Proxy proxy = NetCipher.getProxy();
        return new DownloadRequest(path, mirrors, proxy, username, password);
    }

    private static int toInt(Integer value) {
        if (value == null) {
            return 0;
        }
        return value;
    }

    public void setValues(ContentValues values) {

        if (values.containsKey(Cols._ID)) {
            id = toInt(values.getAsInteger(Cols._ID));
        }

        if (values.containsKey(Cols.LAST_ETAG)) {
            lastetag = values.getAsString(Cols.LAST_ETAG);
        }

        if (values.containsKey(Cols.ADDRESS)) {
            address = values.getAsString(Cols.ADDRESS);
        }

        if (values.containsKey(Cols.DESCRIPTION)) {
            description = values.getAsString(Cols.DESCRIPTION);
        }

        if (values.containsKey(Cols.FINGERPRINT)) {
            fingerprint = values.getAsString(Cols.FINGERPRINT);
        }

        if (values.containsKey(Cols.IN_USE)) {
            inuse = toInt(values.getAsInteger(Cols.IN_USE)) == 1;
        }

        if (values.containsKey(Cols.LAST_UPDATED)) {
            final String dateString = values.getAsString(Cols.LAST_UPDATED);
            lastUpdated = Utils.parseTime(dateString, Utils.parseDate(dateString, null));
        }

        if (values.containsKey(Cols.MAX_AGE)) {
            maxage = toInt(values.getAsInteger(Cols.MAX_AGE));
        }

        if (values.containsKey(Cols.VERSION)) {
            version = toInt(values.getAsInteger(Cols.VERSION));
        }

        if (values.containsKey(Cols.NAME)) {
            name = values.getAsString(Cols.NAME);
        }

        if (values.containsKey(Cols.SIGNING_CERT)) {
            signingCertificate = values.getAsString(Cols.SIGNING_CERT);
        }

        if (values.containsKey(Cols.PRIORITY)) {
            priority = toInt(values.getAsInteger(Cols.PRIORITY));
        }

        if (values.containsKey(Cols.IS_SWAP)) {
            isSwap = toInt(values.getAsInteger(Cols.IS_SWAP)) == 1;
        }

        if (values.containsKey(Cols.USERNAME)) {
            username = values.getAsString(Cols.USERNAME);
        }

        if (values.containsKey(Cols.PASSWORD)) {
            password = values.getAsString(Cols.PASSWORD);
        }

        if (values.containsKey(Cols.TIMESTAMP)) {
            timestamp = toInt(values.getAsInteger(Cols.TIMESTAMP));
        }

        if (values.containsKey(Cols.ICON)) {
            icon = values.getAsString(Cols.ICON);
        }

        if (values.containsKey(Cols.MIRRORS)) {
            mirrors = Utils.parseCommaSeparatedString(values.getAsString(Cols.MIRRORS));
        }

        if (values.containsKey(Cols.USER_MIRRORS)) {
            userMirrors = Utils.parseCommaSeparatedString(values.getAsString(Cols.USER_MIRRORS));
        }

        if (values.containsKey(Cols.DISABLED_MIRRORS)) {
            disabledMirrors = Utils.parseCommaSeparatedString(values.getAsString(Cols.DISABLED_MIRRORS));
        }

        if (values.containsKey(Cols.PUSH_REQUESTS)) {
            pushRequests = toInt(values.getAsInteger(Cols.PUSH_REQUESTS));
        }
    }

    /**
     * @return {@link List} of valid URLs to reach this repo, including the canonical URL
     */
    public List<String> getMirrorList() {
        final HashSet<String> allMirrors = new HashSet<>();
        if (userMirrors != null) {
            allMirrors.addAll(Arrays.asList(userMirrors));
        }
        if (mirrors != null) {
            allMirrors.addAll(Arrays.asList(mirrors));
        }
        allMirrors.add(address);
        if (disabledMirrors != null) {
            allMirrors.removeAll(Arrays.asList(disabledMirrors));
        }
        return new ArrayList<>(allMirrors);
    }
}

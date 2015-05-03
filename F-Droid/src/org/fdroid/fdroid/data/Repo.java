package org.fdroid.fdroid.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import org.fdroid.fdroid.Utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

public class Repo extends ValueObject {

    public static final int VERSION_DENSITY_SPECIFIC_ICONS = 11;

    protected long id;

    public String address;
    public String name;
    public String description;
    public int version; // index version, i.e. what fdroidserver built it - 0 if not specified
    public boolean inuse;
    public int priority;
    public String pubkey; // null for an unsigned repo
    public String fingerprint; // always null for an unsigned repo
    public int maxage; // maximum age of index that will be accepted - 0 for any
    public String lastetag; // last etag we updated from, null forces update
    public Date lastUpdated;
    public boolean isSwap;

    public Repo() {
    }

    public Repo(Cursor cursor) {

        checkCursorPosition(cursor);

        for (int i = 0; i < cursor.getColumnCount(); i++) {
            switch (cursor.getColumnName(i)) {
            case RepoProvider.DataColumns._ID:
                id = cursor.getInt(i);
                break;
            case RepoProvider.DataColumns.LAST_ETAG:
                lastetag = cursor.getString(i);
                break;
            case RepoProvider.DataColumns.ADDRESS:
                address = cursor.getString(i);
                break;
            case RepoProvider.DataColumns.DESCRIPTION:
                description = cursor.getString(i);
                break;
            case RepoProvider.DataColumns.FINGERPRINT:
                fingerprint = cursor.getString(i);
                break;
            case RepoProvider.DataColumns.IN_USE:
                inuse = cursor.getInt(i) == 1;
                break;
            case RepoProvider.DataColumns.LAST_UPDATED:
                lastUpdated = Utils.parseDate(cursor.getString(i), null);
                break;
            case RepoProvider.DataColumns.MAX_AGE:
                maxage = cursor.getInt(i);
                break;
            case RepoProvider.DataColumns.VERSION:
                version = cursor.getInt(i);
                break;
            case RepoProvider.DataColumns.NAME:
                name = cursor.getString(i);
                break;
            case RepoProvider.DataColumns.PUBLIC_KEY:
                pubkey = cursor.getString(i);
                break;
            case RepoProvider.DataColumns.PRIORITY:
                priority = cursor.getInt(i);
                break;
            case RepoProvider.DataColumns.IS_SWAP:
                isSwap = cursor.getInt(i) == 1;
                break;
            }
        }
    }

    public long getId() { return id; }

    public String getName() { return name; }

    @Override
    public String toString() { return address; }

    public boolean isSigned() {
        return !TextUtils.isEmpty(this.pubkey);
    }

    // this happens when a repo is configed with a fingerprint, but the client
    // has not connected to it yet to download its pubkey
    public boolean isSignedButUnverified() {
        return TextUtils.isEmpty(this.pubkey) && !TextUtils.isEmpty(this.fingerprint);
    }

    public boolean hasBeenUpdated() {
        return this.lastetag != null;
    }
    /**
     * If we haven't run an update for this repo yet, then the name
     * will be unknown, in which case we will just take a guess at an
     * appropriate name based on the url (e.g. "fdroid.org/archive")
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

    private static int toInt(Integer value) {
        if (value == null) {
            return 0;
        }
        return value;
    }

    public void setValues(ContentValues values) {

        if (values.containsKey(RepoProvider.DataColumns._ID)) {
            id = toInt(values.getAsInteger(RepoProvider.DataColumns._ID));
        }

        if (values.containsKey(RepoProvider.DataColumns.LAST_ETAG)) {
            lastetag = values.getAsString(RepoProvider.DataColumns.LAST_ETAG);
        }

        if (values.containsKey(RepoProvider.DataColumns.ADDRESS)) {
            address = values.getAsString(RepoProvider.DataColumns.ADDRESS);
        }

        if (values.containsKey(RepoProvider.DataColumns.DESCRIPTION)) {
            description = values.getAsString(RepoProvider.DataColumns.DESCRIPTION);
        }

        if (values.containsKey(RepoProvider.DataColumns.FINGERPRINT)) {
            fingerprint = values.getAsString(RepoProvider.DataColumns.FINGERPRINT);
        }

        if (values.containsKey(RepoProvider.DataColumns.IN_USE)) {
            inuse = toInt(values.getAsInteger(RepoProvider.DataColumns.IN_USE)) == 1;
        }

        if (values.containsKey(RepoProvider.DataColumns.LAST_UPDATED)) {
            final String dateString = values.getAsString(RepoProvider.DataColumns.LAST_UPDATED);
            lastUpdated = Utils.parseDate(dateString, null);
        }

        if (values.containsKey(RepoProvider.DataColumns.MAX_AGE)) {
            maxage = toInt(values.getAsInteger(RepoProvider.DataColumns.MAX_AGE));
        }

        if (values.containsKey(RepoProvider.DataColumns.VERSION)) {
            version = toInt(values.getAsInteger(RepoProvider.DataColumns.VERSION));
        }

        if (values.containsKey(RepoProvider.DataColumns.NAME)) {
            name = values.getAsString(RepoProvider.DataColumns.NAME);
        }

        if (values.containsKey(RepoProvider.DataColumns.PUBLIC_KEY)) {
            pubkey = values.getAsString(RepoProvider.DataColumns.PUBLIC_KEY);
        }

        if (values.containsKey(RepoProvider.DataColumns.PRIORITY)) {
            priority = toInt(values.getAsInteger(RepoProvider.DataColumns.PRIORITY));
        }

        if (values.containsKey(RepoProvider.DataColumns.IS_SWAP)) {
            isSwap= toInt(values.getAsInteger(RepoProvider.DataColumns.IS_SWAP)) == 1;
        }
    }
}

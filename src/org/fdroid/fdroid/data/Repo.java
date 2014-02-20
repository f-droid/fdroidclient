package org.fdroid.fdroid.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;
import org.fdroid.fdroid.Utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
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

    public Repo() {

    }

    public Repo(Cursor cursor) {

        checkCursorPosition(cursor);

        for(int i = 0; i < cursor.getColumnCount(); i ++ ) {
            String column = cursor.getColumnName(i);
            if (column.equals(RepoProvider.DataColumns._ID)) {
                id = cursor.getInt(i);
            } else if (column.equals(RepoProvider.DataColumns.LAST_ETAG)) {
                lastetag = cursor.getString(i);
            } else if (column.equals(RepoProvider.DataColumns.ADDRESS)) {
                address = cursor.getString(i);
            } else if (column.equals(RepoProvider.DataColumns.DESCRIPTION)) {
                description = cursor.getString(i);
            } else if (column.equals(RepoProvider.DataColumns.FINGERPRINT)) {
                fingerprint = cursor.getString(i);
            } else if (column.equals(RepoProvider.DataColumns.IN_USE)) {
                inuse = cursor.getInt(i) == 1;
            } else if (column.equals(RepoProvider.DataColumns.LAST_UPDATED)) {
                lastUpdated = toDate(cursor.getString(i));
            } else if (column.equals(RepoProvider.DataColumns.MAX_AGE)) {
                maxage = cursor.getInt(i);
            } else if (column.equals(RepoProvider.DataColumns.VERSION)) {
                version = cursor.getInt(i);
            } else if (column.equals(RepoProvider.DataColumns.NAME)) {
                name = cursor.getString(i);
            } else if (column.equals(RepoProvider.DataColumns.PUBLIC_KEY)) {
                pubkey = cursor.getString(i);
            } else if (column.equals(RepoProvider.DataColumns.PRIORITY)) {
                priority = cursor.getInt(i);
            }
        }
    }

    public long getId() { return id; }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return address;
    }

    public boolean isSigned() {
        return this.pubkey != null && this.pubkey.length() > 0;
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
        } else {
            return value;
        }
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
            inuse = toInt(values.getAsInteger(RepoProvider.DataColumns.FINGERPRINT)) == 1;
        }

        if (values.containsKey(RepoProvider.DataColumns.LAST_UPDATED)) {
            String dateString = values.getAsString(RepoProvider.DataColumns.LAST_UPDATED);
            if (dateString != null) {
                try {
                    lastUpdated =  Utils.DATE_FORMAT.parse(dateString);
                } catch (ParseException e) {
                    Log.e("FDroid", "Error parsing date " + dateString);
                }
            }
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
    }
}

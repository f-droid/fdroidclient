package org.fdroid.fdroid.data;

import android.content.ContentProvider;
import android.content.UriMatcher;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

abstract class FDroidProvider extends ContentProvider {

    public static final String AUTHORITY = "org.fdroid.fdroid.data";

    protected static final int CODE_LIST   = 1;
    protected static final int CODE_SINGLE = 2;

    private DBHelper dbHelper;

    abstract protected String getTableName();

    abstract protected String getProviderName();

    @Override
    public boolean onCreate() {
        dbHelper = new DBHelper(getContext());
        return true;
    }

    protected final DBHelper db() {
        return dbHelper;
    }

    protected final SQLiteDatabase read() {
        return db().getReadableDatabase();
    }

    protected final SQLiteDatabase write() {
        return db().getWritableDatabase();
    }

    @Override
    public String getType(Uri uri) {
        String type;
        switch(getMatcher().match(uri)) {
            case CODE_LIST:
                type = "dir";
                break;

            case CODE_SINGLE:
            default:
                type = "item";
                break;

        }
        return "vnd.android.cursor." + type + "/vnd." + AUTHORITY + "." + getProviderName();
    }

    abstract protected UriMatcher getMatcher();

}


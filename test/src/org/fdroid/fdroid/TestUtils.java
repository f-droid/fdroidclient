package org.fdroid.fdroid;

import android.content.*;
import android.net.Uri;
import org.fdroid.fdroid.data.AppProvider;

public class TestUtils {

    public static void insertApp(ContentResolver resolver, String id, String name, ContentValues additionalValues) {

        ContentValues values = new ContentValues();
        values.put(AppProvider.DataColumns.APP_ID, id);
        values.put(AppProvider.DataColumns.NAME, name);

        // Required fields (NOT NULL in the database).
        values.put(AppProvider.DataColumns.SUMMARY, "test summary");
        values.put(AppProvider.DataColumns.DESCRIPTION, "test description");
        values.put(AppProvider.DataColumns.LICENSE, "GPL?");
        values.put(AppProvider.DataColumns.IS_COMPATIBLE, 1);
        values.put(AppProvider.DataColumns.IGNORE_ALLUPDATES, 0);
        values.put(AppProvider.DataColumns.IGNORE_THISUPDATE, 0);

        values.putAll(additionalValues);

        Uri uri = AppProvider.getContentUri();

        resolver.insert(uri, values);
    }

}

package org.fdroid.fdroid.data;

import android.content.ContentValues;
import android.database.AbstractCursor;
import android.database.Cursor;
import android.os.Bundle;

import java.util.Map;

/**
 * In order to keep {@link App#App(Cursor)} and {@link Apk#Apk(Cursor)} as
 * efficient as possible, this wrapper class is used to instantiate {@code App}
 * and {@code Apk} from {@link App#toContentValues()} and
 * {@link Apk#toContentValues()} included as extras {@link Bundle}s in the
 * {@link android.content.Intent} that starts
 * {@link org.fdroid.fdroid.installer.InstallManagerService}
 * <p>
 * This implemented to throw an {@link IllegalArgumentException} if the types
 * do not match what they are expected to be so that things fail fast. So that
 * means only types used in {@link App#toContentValues()} and
 * {@link Apk#toContentValues()} are implemented.
 */
class ContentValuesCursor extends AbstractCursor {

    private final String[] keys;
    private final Object[] values;

    ContentValuesCursor(ContentValues contentValues) {
        super();
        keys = new String[contentValues.size()];
        values = new Object[contentValues.size()];
        int i = 0;
        for (Map.Entry<String, Object> entry : contentValues.valueSet()) {
            keys[i] = entry.getKey();
            values[i] = entry.getValue();
            i++;
        }
        moveToFirst();
    }

    @Override
    public int getCount() {
        return 1;
    }

    @Override
    public String[] getColumnNames() {
        return keys;
    }

    @Override
    public String getString(int i) {
        return (String) values[i];
    }

    @Override
    public int getInt(int i) {
        if (values[i] instanceof Long) {
            return ((Long) values[i]).intValue();
        } else if (values[i] instanceof Integer) {
            return (int) values[i];
        }
        throw new IllegalArgumentException("unimplemented");
    }

    @Override
    public long getLong(int i) {
        if (values[i] instanceof Long) {
            return (Long) values[i];
        }
        throw new IllegalArgumentException("Value is not a Long");
    }

    @Override
    public short getShort(int i) {
        throw new IllegalArgumentException("unimplemented");
    }

    @Override
    public float getFloat(int i) {
        throw new IllegalArgumentException("unimplemented");
    }

    @Override
    public double getDouble(int i) {
        throw new IllegalArgumentException("unimplemented");
    }

    @Override
    public boolean isNull(int i) {
        return values[i] == null;
    }
}

package org.fdroid.fdroid.data;

import android.database.Cursor;
import android.util.Log;
import org.fdroid.fdroid.Utils;

import java.text.ParseException;
import java.util.Date;

abstract class ValueObject {

    protected void checkCursorPosition(Cursor cursor) throws IllegalArgumentException {
        if (cursor.getPosition() == -1) {
            throw new IllegalArgumentException(
                "Cursor position is -1. " +
                "Did you forget to moveToFirst() or move() before passing to the value object?");
        }
    }

    static Date toDate(String string) {
        Date date = null;
        if (string != null) {
            try {
                date = Utils.DATE_FORMAT.parse(string);
            } catch (ParseException e) {
                Log.e("FDroid", "Error parsing date " + string);
            }
        }
        return date;
    }

}

package org.fdroid.fdroid.data;

import android.database.Cursor;

public class ValueObject {

    protected void checkCursorPosition(Cursor cursor) throws IllegalArgumentException {
        if (cursor.getPosition() == -1) {
            throw new IllegalArgumentException(
                "Cursor position is -1. " +
                "Did you forget to moveToFirst() or move() before passing to the value object?");
        }
    }

}

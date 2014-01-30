package org.fdroid.fdroid.data;

import android.util.Log;
import org.fdroid.fdroid.DB;

import java.text.ParseException;
import java.util.Date;

abstract class ValueObject {

    static Date toDate(String string) {
        Date date = null;
        if (string != null) {
            try {
                date = DB.DATE_FORMAT.parse(string);
            } catch (ParseException e) {
                Log.e("FDroid", "Error parsing date " + string);
            }
        }
        return date;
    }

}

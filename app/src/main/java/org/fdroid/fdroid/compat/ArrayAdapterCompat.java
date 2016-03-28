package org.fdroid.fdroid.compat;

import android.annotation.TargetApi;
import android.widget.ArrayAdapter;

import java.util.List;

public class ArrayAdapterCompat extends Compatibility {

    @TargetApi(11)
    public static <T> void addAll(ArrayAdapter<T> adapter, List<T> list) {
        if (hasApi(11)) {
            adapter.addAll(list);
        } else {
            for (T category : list) {
                adapter.add(category);
            }
        }
    }

}

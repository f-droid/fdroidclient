package org.fdroid.fdroid;

import android.os.Bundle;

public interface ProgressListener {

    public void onProgress(Event event);

    // I went a bit overboard with the overloaded constructors, but they all
    // seemed potentially useful and unambiguous, so I just put them in there
    // while I'm here.
    public static class Event {

        public static final int NO_VALUE = Integer.MIN_VALUE;

        public final int type;
        public final Bundle data;

        // These two are not final, so that you can create a template Event,
        // pass it into a function which performs something over time, and
        // that function can initialize "total" and progressively
        // update "progress"
        public int progress;
        public int total;

        public Event(int type) {
            this(type, NO_VALUE, NO_VALUE, null);
        }

        public Event(int type, Bundle data) {
            this(type, NO_VALUE, NO_VALUE, data);
        }

        public Event(int type, int progress) {
            this(type, progress, NO_VALUE, null);
        }

        public Event(int type, int progress, Bundle data) {
            this(type, NO_VALUE, NO_VALUE, data);
        }

        public Event(int type, int progress, int total) {
            this(type, progress, total, null);
        }

        public Event(int type, int progress, int total, Bundle data) {
            this.type = type;
            this.progress = progress;
            this.total = total;
            this.data = data == null ? new Bundle() : data;
        }
    }

}

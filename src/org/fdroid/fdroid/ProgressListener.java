package org.fdroid.fdroid;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public interface ProgressListener {

    public void onProgress(Event event);

    // I went a bit overboard with the overloaded constructors, but they all
    // seemed potentially useful and unambiguous, so I just put them in there
    // while I'm here.
    public static class Event implements Parcelable {

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

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(type);
            dest.writeInt(progress);
            dest.writeInt(total);
            dest.writeBundle(data);
        }

        public static final Parcelable.Creator<Event> CREATOR = new Parcelable.Creator<Event>() {
            @Override
            public Event createFromParcel(Parcel in) {
                return new Event(in.readInt(), in.readInt(), in.readInt(), in.readBundle());
            }

            @Override
            public Event[] newArray(int size) {
                return new Event[size];
            }
        };

    }

}

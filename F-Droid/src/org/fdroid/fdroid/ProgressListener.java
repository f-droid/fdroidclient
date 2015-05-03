package org.fdroid.fdroid;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public interface ProgressListener {

    void onProgress(Event event);

    // I went a bit overboard with the overloaded constructors, but they all
    // seemed potentially useful and unambiguous, so I just put them in there
    // while I'm here.
    class Event implements Parcelable {

        public static final int NO_VALUE = Integer.MIN_VALUE;

        public final String type;
        public final Bundle data;

        // These two are not final, so that you can create a template Event,
        // pass it into a function which performs something over time, and
        // that function can initialize "total" and progressively
        // update "progress"
        public int progress;
        public final int total;

        public Event(String type) {
            this(type, NO_VALUE, NO_VALUE, null);
        }

        public Event(String type, Bundle data) {
            this(type, NO_VALUE, NO_VALUE, data);
        }

        public Event(String type, int progress, int total, Bundle data) {
            this.type = type;
            this.progress = progress;
            this.total = total;
            this.data = (data == null) ? new Bundle() : data;
        }

        @Override
        public int describeContents() { return 0; }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(type);
            dest.writeInt(progress);
            dest.writeInt(total);
            dest.writeBundle(data);
        }

        public static final Parcelable.Creator<Event> CREATOR = new Parcelable.Creator<Event>() {
            @Override
            public Event createFromParcel(Parcel in) {
                return new Event(in.readString(), in.readInt(), in.readInt(), in.readBundle());
            }

            @Override
            public Event[] newArray(int size) {
                return new Event[size];
            }
        };

        /**
         * Can help to provide context to the listener about what process is causing the event.
         * For example, the repo updater uses one listener to listen to multiple downloaders.
         * When it receives an event, it doesn't know which repo download is causing the event,
         * so we pass that through to the downloader when we set the progress listener. This way,
         * we can ask the event for the name of the repo.
         */
        public Bundle getData() { return data; }
    }

}

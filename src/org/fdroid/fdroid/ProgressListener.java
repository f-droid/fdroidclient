
package org.fdroid.fdroid;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public interface ProgressListener {

    public void onProgress(Event event);

    // I went a bit overboard with the overloaded constructors, but they all
    // seemed potentially useful and unambiguous, so I just put them in there
    // while I'm here.
    public static class Event implements Parcelable {

        public static final int NO_VALUE = Integer.MIN_VALUE;
        public static final String PROGRESS_DATA_REPO = "repo";

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

        public Event(int type, String repoAddress) {
            this(type, NO_VALUE, NO_VALUE, repoAddress);
        }

        public Event(int type, int progress, int total, String repoAddress) {
            this.type = type;
            this.progress = progress;
            this.total = total;
            if (TextUtils.isEmpty(repoAddress))
                this.data = new Bundle();
            else
                this.data = createProgressData(repoAddress);
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
                return new Event(in.readInt(), in.readInt(), in.readInt(),
                        in.readBundle().getString(PROGRESS_DATA_REPO));
            }

            @Override
            public Event[] newArray(int size) {
                return new Event[size];
            }
        };

        public String getRepoAddress() {
            return data.getString(PROGRESS_DATA_REPO);
        }

        public static Bundle createProgressData(String repoAddress) {
            Bundle data = new Bundle();
            data.putString(PROGRESS_DATA_REPO, repoAddress);
            return data;
        }

    }

}

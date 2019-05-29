package org.fdroid.fdroid.views.panic;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.R;

public class DestructivePreference extends Preference {
    public DestructivePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public DestructivePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public DestructivePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DestructivePreference(Context context) {
        super(context);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        if (FDroidApp.isAppThemeLight()) {
            holder.itemView.setBackgroundColor(getContext().getResources().getColor(R.color.panic_destructive_light));
        } else {
            holder.itemView.setBackgroundColor(getContext().getResources().getColor(R.color.panic_destructive_dark));
        }
    }
}

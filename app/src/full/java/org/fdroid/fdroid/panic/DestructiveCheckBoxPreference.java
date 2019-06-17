package org.fdroid.fdroid.panic;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.R;

public class DestructiveCheckBoxPreference extends CheckBoxPreference {
    public DestructiveCheckBoxPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public DestructiveCheckBoxPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public DestructiveCheckBoxPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DestructiveCheckBoxPreference(Context context) {
        super(context);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        if (!holder.itemView.isEnabled()) {
            return;
        }
        Resources resources = getContext().getResources();
        if (FDroidApp.isAppThemeLight()) {
            holder.itemView.setBackgroundColor(resources.getColor(R.color.panic_destructive_light));
        } else {
            holder.itemView.setBackgroundColor(resources.getColor(R.color.panic_destructive_dark));
        }
    }
}

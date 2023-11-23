package org.fdroid.fdroid.panic;

import android.content.Context;
import android.util.AttributeSet;

import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreferenceCompat;

import org.fdroid.fdroid.R;

public class DestructiveSwitchPreference extends SwitchPreferenceCompat {
    public DestructiveSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public DestructiveSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public DestructiveSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DestructiveSwitchPreference(Context context) {
        super(context);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        if (!holder.itemView.isEnabled()) {
            return;
        }
        holder.itemView.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.panic_destructive));
    }
}

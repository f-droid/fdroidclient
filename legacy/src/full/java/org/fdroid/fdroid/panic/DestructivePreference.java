package org.fdroid.fdroid.panic;

import android.content.Context;
import android.util.AttributeSet;

import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

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
        holder.itemView.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.panic_destructive));
    }
}

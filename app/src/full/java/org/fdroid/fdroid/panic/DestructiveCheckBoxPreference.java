package org.fdroid.fdroid.panic;

import android.content.Context;
import android.util.AttributeSet;

import androidx.core.content.ContextCompat;
import androidx.preference.CheckBoxPreference;
import androidx.preference.PreferenceViewHolder;

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
        holder.itemView.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.panic_destructive));
    }
}

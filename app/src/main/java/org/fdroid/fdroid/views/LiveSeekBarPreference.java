package org.fdroid.fdroid.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SeekBarPreference;

import org.fdroid.fdroid.R;

public class LiveSeekBarPreference extends SeekBarPreference {
    private SeekBarLiveUpdater seekBarLiveUpdater;
    private boolean trackingTouch;
    private int value = -1;

    @SuppressWarnings("unused")
    public LiveSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @SuppressWarnings("unused")
    public LiveSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @SuppressWarnings("unused")
    public LiveSeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @SuppressWarnings("unused")
    public LiveSeekBarPreference(Context context) {
        super(context);
    }

    @Override
    public void onBindViewHolder(@NonNull final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        View seekBarValue = holder.findViewById(R.id.seekbar_value);
        seekBarValue.setVisibility(View.GONE);

        SeekBarForegroundThumb seekBar = holder.itemView.findViewById(R.id.seekbar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                value = progress;
                if (seekBarLiveUpdater != null) {
                    String message = seekBarLiveUpdater.seekBarUpdated(value);
                    TextView summary = holder.itemView.findViewById(android.R.id.summary);
                    if (summary != null) {
                        summary.setText(message);
                    }
                }
                if (fromUser && !trackingTouch) {
                    persistInt(value);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                trackingTouch = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                trackingTouch = false;
                persistInt(value);
            }
        });
        seekBar.setProgress(value);

        if (isEnabled()) {
            seekBar.setAlpha(1.0f);
        } else {
            seekBar.setAlpha(0.3f);
        }
    }

    @Override
    public void setValue(int value) {
        super.setValue(value);
        this.value = value;
    }

    @Override
    public int getValue() {
        if (value == -1) {
            value = super.getValue();
        }
        return value;
    }

    void setSeekBarLiveUpdater(SeekBarLiveUpdater updater) {
        seekBarLiveUpdater = updater;
    }

    public interface SeekBarLiveUpdater {
        String seekBarUpdated(int position);
    }
}

package org.fdroid.fdroid.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;
import org.fdroid.fdroid.R;

/**
 * SeekBar that does not show the TickMark above the thumb
 * Based on https://stackoverflow.com/a/47727128
 */
public class SeekBarForegroundThumb extends AppCompatSeekBar {
    private Drawable mTickMark;
    private Context context;

    public SeekBarForegroundThumb(Context context) {
        super(context);
        init(context);
    }

    public SeekBarForegroundThumb(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SeekBarForegroundThumb(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        this.context = context;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mTickMark = context.getDrawable(R.drawable.seekbar_tickmark);
        } else {
            mTickMark = context.getResources().getDrawable(R.drawable.seekbar_tickmark);
        }
    }

    private Drawable getThumbCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return getThumb();
        } else {
            return context.getResources().getDrawable(R.drawable.seekbar_thumb);
        }
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawTickMarks(canvas);
    }

    @Override
    public int getThumbOffset() {
        return super.getThumbOffset();
    }

    private void drawTickMarks(Canvas canvas) {
        if (mTickMark != null) {
            final int count = getMax();
            if (count > 1) {
                final int w = mTickMark.getIntrinsicWidth();
                final int h = mTickMark.getIntrinsicHeight();
                final int halfThumbW = getThumbCompat().getIntrinsicWidth() / 2;
                final int halfW = w >= 0 ? w / 2 : 1;
                final int halfH = h >= 0 ? h / 2 : 1;
                mTickMark.setBounds(-halfW, -halfH, halfW, halfH);
                final float spacing = (getWidth() - getPaddingLeft() - getPaddingRight() + getThumbOffset() * 2 - halfThumbW * 2) / (float) count;
                final int saveCount = canvas.save();
                canvas.translate(getPaddingLeft() - getThumbOffset() + halfThumbW, getHeight() / 2);
                for (int i = 0; i <= count; i++) {
                    if(i!=getProgress())
                        mTickMark.draw(canvas);
                    canvas.translate(spacing, 0);
                }
                canvas.restoreToCount(saveCount);
            }
        }
    }
}

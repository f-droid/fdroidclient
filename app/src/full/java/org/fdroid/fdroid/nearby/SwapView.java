package org.fdroid.fdroid.nearby;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.LayoutRes;
import androidx.core.content.ContextCompat;

import org.fdroid.fdroid.R;

/**
 * A {@link android.view.View} that registers to handle the swap events from
 * {@link SwapService}.
 */
public class SwapView extends RelativeLayout {
    public static final String TAG = "SwapView";

    @ColorInt
    public final int toolbarColor;
    public final String toolbarTitle;

    private int layoutResId = -1;

    protected String currentFilterString;

    public SwapView(Context context) {
        this(context, null);
    }

    public SwapView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * In order to support Android < 21, this calls {@code super} rather than
     * {@code this}.  {@link RelativeLayout}'s methods just use a 0 for the
     * fourth argument, just like this used to.
     */
    public SwapView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.SwapView, 0, 0);
        toolbarColor = a.getColor(R.styleable.SwapView_toolbarColor,
                ContextCompat.getColor(context, R.color.swap_blue));
        toolbarTitle = a.getString(R.styleable.SwapView_toolbarTitle);
        a.recycle();
    }

    public SwapView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.SwapView, 0, 0);
        toolbarColor = a.getColor(R.styleable.SwapView_toolbarColor,
                ContextCompat.getColor(context, R.color.swap_blue));
        toolbarTitle = a.getString(R.styleable.SwapView_toolbarTitle);
        a.recycle();
    }

    @LayoutRes
    public int getLayoutResId() {
        return layoutResId;
    }

    public void setLayoutResId(@LayoutRes int layoutResId) {
        this.layoutResId = layoutResId;
    }

    public String getCurrentFilterString() {
        return this.currentFilterString;
    }

    public void setCurrentFilterString(String currentFilterString) {
        this.currentFilterString = currentFilterString;
    }

    public SwapWorkflowActivity getActivity() {
        return (SwapWorkflowActivity) getContext();
    }

    public String getToolbarTitle() {
        return toolbarTitle;
    }
}

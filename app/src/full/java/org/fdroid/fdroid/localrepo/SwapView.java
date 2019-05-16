package org.fdroid.fdroid.localrepo;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.ColorInt;
import android.support.annotation.LayoutRes;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.views.swap.SwapWorkflowActivity;

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

    public SwapView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(21)
    public SwapView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.SwapView, 0, 0);
        toolbarColor = a.getColor(R.styleable.SwapView_toolbarColor,
                getResources().getColor(R.color.swap_blue));
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

    @ColorInt
    public int getToolbarColour() {
        return toolbarColor;
    }

    public String getToolbarTitle() {
        return toolbarTitle;
    }
}

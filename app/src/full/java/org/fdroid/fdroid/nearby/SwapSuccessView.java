package org.fdroid.fdroid.nearby;

import android.content.Context;
import android.util.AttributeSet;

import androidx.compose.ui.platform.ComposeView;

import org.fdroid.R;
import org.fdroid.ui.nearby.SwapSuccessBinder;

/**
 * This is a view that shows a listing of all apps in the swap repo that this
 * just connected to.
 */
public class SwapSuccessView extends SwapView {

    public SwapSuccessView(Context context) {
        super(context);
    }

    public SwapSuccessView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SwapSuccessView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SwapSuccessView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        ComposeView composeView = findViewById(R.id.compose);
        SwapSuccessBinder.bind(composeView, getActivity().viewModel);
    }
}

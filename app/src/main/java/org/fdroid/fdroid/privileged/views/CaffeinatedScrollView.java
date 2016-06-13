/*
**
** Copyright 2012, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

package org.fdroid.fdroid.privileged.views;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.ScrollView;

/**
 * It's a ScrollView that knows how to stay awake.
 */
public class CaffeinatedScrollView extends ScrollView {
    private Runnable fullScrollAction;
    private int bottomSlop;

    public CaffeinatedScrollView(Context context) {
        super(context);
    }

    public CaffeinatedScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Make this visible so we can call it
     */
    @SuppressWarnings("PMD.UselessOverridingMethod")
    @Override
    public boolean awakenScrollBars() {
        return super.awakenScrollBars();
    }

    public void setFullScrollAction(Runnable action) {
        fullScrollAction = action;
        bottomSlop = (int) (4 * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        checkFullScrollAction();
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        checkFullScrollAction();
    }

    private void checkFullScrollAction() {
        if (fullScrollAction != null) {
            int daBottom = getChildAt(0).getBottom();
            int screenBottom = getScrollY() + getHeight() - getPaddingBottom();
            if ((daBottom - screenBottom) < bottomSlop) {
                fullScrollAction.run();
                fullScrollAction = null;
            }
        }
    }
}

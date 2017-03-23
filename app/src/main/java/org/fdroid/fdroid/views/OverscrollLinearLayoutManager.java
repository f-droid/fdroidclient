package org.fdroid.fdroid.views;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

/**
 * This class is like a standard LinearLayoutManager but with an option to add an
 * overscroll listener. This can be used to consume overscrolls, e.g. to draw custom
 * "glows".
 */
public class OverscrollLinearLayoutManager extends LinearLayoutManager {

    /**
     * A listener interface to get overscroll infromation.
     */
    public interface OnOverscrollListener {
        /**
         * Notifies the listener that an overscroll has happened in the x direction.
         * @param overscroll If negative, the recycler view has been scrolled to the "start"
         *                   position. If positive to the "end" position.
         * @return Return the amount of overscroll consumed. Returning 0 will let the
         * recycler view handle this in the default way. Return "overscroll" to consume the
         * whole event.
         */
        int onOverscrollX(int overscroll);

        /**
         * Notifies the listener that an overscroll has happened in the y direction.
         * @param overscroll If negative, the recycler view has been scrolled to the "top"
         *                   position. If positive to the "bottom" position.
         * @return Return the amount of overscroll consumed. Returning 0 will let the
         * recycler view handle this in the default way. Return "overscroll" to consume the
         * whole event.
         */

        int onOverscrollY(int overscroll);
    }

    private OnOverscrollListener overscrollListener = null;

    public OverscrollLinearLayoutManager(Context context) {
        super(context);
    }

    public OverscrollLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    public OverscrollLinearLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * Set the {@link OverscrollLinearLayoutManager.OnOverscrollListener} to get information about
     * when the parent recyclerview is overscrolled.
     *
     * @param listener Listener to add
     * @see OverscrollLinearLayoutManager.OnOverscrollListener
     */
    public void setOnOverscrollListener(OnOverscrollListener listener) {
        overscrollListener = listener;
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        int consumed = super.scrollHorizontallyBy(dx, recycler, state);
        int overscrollX = dx - consumed;
        if (overscrollX != 0) {
            if (overscrollListener != null) {
                int consumedByListener = overscrollListener.onOverscrollX(overscrollX);
                consumed += consumedByListener;
            }
        }
        return consumed;
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        int consumed = super.scrollVerticallyBy(dy, recycler, state);
        int overscrollY = dy - consumed;
        if (overscrollY != 0) {
            if (overscrollListener != null) {
                int consumedByListener = overscrollListener.onOverscrollY(overscrollY);
                consumed += consumedByListener;
            }
        }
        return consumed;
    }
}

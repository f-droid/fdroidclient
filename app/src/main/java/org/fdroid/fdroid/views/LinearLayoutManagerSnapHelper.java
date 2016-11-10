package org.fdroid.fdroid.views;

import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import static android.support.v7.widget.RecyclerView.NO_POSITION;

public class LinearLayoutManagerSnapHelper extends LinearSnapHelper {

    public interface LinearSnapHelperListener {
        /**
         * Tells the listener that we have selected a view to snap to.
         * @param view The selected view (may be null)
         * @param position Adapter position of the snapped to view (or NO_POSITION if none)
         */
        void onSnappedToView(View view, int position);
    };

    private LinearLayoutManager mLlm;
    private OrientationHelper mOrientationHelper;
    private LinearSnapHelperListener mListener;

    public LinearLayoutManagerSnapHelper(LinearLayoutManager llm) {
        this.mLlm = llm;
        this.mOrientationHelper = OrientationHelper.createHorizontalHelper(mLlm);
    };

    public void setLinearSnapHelperListener(LinearSnapHelperListener listener) {
        mListener = listener;
    }

    @Override
    public View findSnapView(RecyclerView.LayoutManager layoutManager) {
        View snappedView = super.findSnapView(layoutManager);
        if (layoutManager.canScrollHorizontally()) {
            if (layoutManager instanceof LinearLayoutManager) {
                int firstChild = ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
                int lastChild = ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
                if (firstChild == 0) {
                    View child = layoutManager.findViewByPosition(firstChild);
                    if (mOrientationHelper.getDecoratedEnd(child) >= mOrientationHelper.getDecoratedMeasurement(child) / 2
                            && mOrientationHelper.getDecoratedEnd(child) > 0) {
                        int dist1 = super.calculateDistanceToFinalSnap(layoutManager, snappedView)[0];
                        int dist2 = mOrientationHelper.getDecoratedStart(child);
                        if (Math.abs(dist1) > Math.abs(dist2)) {
                            snappedView = child;
                        }
                    }
                } else if (lastChild == (mLlm.getItemCount() - 1)) {
                    View child = layoutManager.findViewByPosition(lastChild);
                    if (mOrientationHelper.getDecoratedStart(child) < mOrientationHelper.getTotalSpace() - mOrientationHelper.getDecoratedMeasurement(child) / 2
                            && mOrientationHelper.getDecoratedStart(child) < mOrientationHelper.getTotalSpace()) {
                        int dist1 = super.calculateDistanceToFinalSnap(layoutManager, snappedView)[0];
                        int dist2 = mOrientationHelper.getTotalSpace() - mOrientationHelper.getDecoratedEnd(child);
                        if (Math.abs(dist1) > Math.abs(dist2)) {
                            snappedView = child;
                        }
                    }
                }
            }
        }
        if (mListener != null) {
            int snappedPosition = 0;
            if (snappedView != null)
                snappedPosition = mLlm.getPosition(snappedView);
            mListener.onSnappedToView(snappedView, snappedPosition);
        }
        return snappedView;
    }
}

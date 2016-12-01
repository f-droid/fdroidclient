package org.fdroid.fdroid.views;

import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class LinearLayoutManagerSnapHelper extends LinearSnapHelper {

    private View mLastSavedTarget;
    private int mLastSavedDistance;

    public interface LinearSnapHelperListener {
        /**
         * Tells the listener that we have selected a view to snap to.
         * @param view The selected view (may be null)
         * @param position Adapter position of the snapped to view (or NO_POSITION if none)
         */
        void onSnappedToView(View view, int position);
    }

    private final LinearLayoutManager mLlm;
    private final OrientationHelper mOrientationHelper;
    private LinearSnapHelperListener mListener;

    public LinearLayoutManagerSnapHelper(LinearLayoutManager llm) {
        this.mLlm = llm;
        this.mOrientationHelper = OrientationHelper.createHorizontalHelper(mLlm);
    }

    public void setLinearSnapHelperListener(LinearSnapHelperListener listener) {
        mListener = listener;
    }

    @Override
    public View findSnapView(RecyclerView.LayoutManager layoutManager) {
        View snappedView = super.findSnapView(layoutManager);
        if (snappedView != null && layoutManager.canScrollHorizontally()) {
            if (layoutManager instanceof LinearLayoutManager) {
                mLastSavedTarget = null;

                int distSnap = super.calculateDistanceToFinalSnap(layoutManager, snappedView)[0];

                int firstChild = ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
                int lastChild = ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();

                int idxSnap = -1;
                for (int i = firstChild; i <= lastChild; i++) {
                    View view = ((LinearLayoutManager) layoutManager).findViewByPosition(i);
                    if (view == snappedView) {
                        idxSnap = i;
                        break;
                    }
                }

                int snapPositionFirst = mOrientationHelper.getDecoratedMeasurement(((LinearLayoutManager) layoutManager).findViewByPosition(firstChild)) / 2;
                int snapPositionLast = mOrientationHelper.getTotalSpace() - mOrientationHelper.getDecoratedMeasurement(((LinearLayoutManager) layoutManager).findViewByPosition(lastChild)) / 2;

                int centerSnapPosition = mOrientationHelper.getTotalSpace() / 2;

                if (idxSnap != -1) {
                    int currentSmallestDistance = Integer.MAX_VALUE;
                    View currentSmallestDistanceView = null;
                    for (int i = firstChild; i <= lastChild; i++) {
                        View view = ((LinearLayoutManager) layoutManager).findViewByPosition(i);
                        if (i < idxSnap && firstChild == 0) {
                            int snapPosition = snapPositionFirst + (i - firstChild) * (centerSnapPosition - snapPositionFirst) / (idxSnap - firstChild);
                            int viewPosition = view.getLeft() + view.getWidth() / 2;
                            int dist = snapPosition - viewPosition;
                            if (Math.abs(dist) < Math.abs(currentSmallestDistance) || (Math.abs(dist) == Math.abs(currentSmallestDistance) && distSnap > 0)) {
                                currentSmallestDistance = dist;
                                currentSmallestDistanceView = view;
                            }
                        } else if (i > idxSnap && lastChild == (mLlm.getItemCount() - 1)) {
                            int snapPosition = snapPositionLast - (lastChild - i) * (snapPositionLast - centerSnapPosition) / (lastChild - idxSnap);
                            int viewPosition = view.getLeft() + view.getWidth() / 2;
                            int dist = snapPosition - viewPosition;
                            if (Math.abs(dist) < Math.abs(currentSmallestDistance) || (Math.abs(dist) == Math.abs(currentSmallestDistance) && distSnap < 0)) {
                                currentSmallestDistance = dist;
                                currentSmallestDistanceView = view;
                            }
                        }
                    }
                    if (Math.abs(distSnap) > Math.abs(currentSmallestDistance)) {
                        snappedView = currentSmallestDistanceView;
                        mLastSavedTarget = currentSmallestDistanceView;
                        mLastSavedDistance = -currentSmallestDistance;
                    }
                }
            }
        }
        if (mListener != null) {
            int snappedPosition = 0;
            if (snappedView != null) {
                snappedPosition = mLlm.getPosition(snappedView);
            }
            mListener.onSnappedToView(snappedView, snappedPosition);
        }
        return snappedView;
    }

    @Override
    public int[] calculateDistanceToFinalSnap(@NonNull RecyclerView.LayoutManager layoutManager, @NonNull View targetView) {
        if (targetView == mLastSavedTarget) {
            int[] out = new int[2];
            out[0] = mLastSavedDistance;
            out[1] = 0;
            return out;
        }
        return super.calculateDistanceToFinalSnap(layoutManager, targetView);
    }
}

package org.fdroid.fdroid.views;

import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.view.View;

@SuppressWarnings("LineLength")
public class LinearLayoutManagerSnapHelper extends LinearSnapHelper {

    private View lastSavedTarget;
    private int lastSavedDistance;

    public interface LinearSnapHelperListener {
        /**
         * Tells the listener that we have selected a view to snap to.
         * @param view The selected view (may be null)
         * @param position Adapter position of the snapped to view (or NO_POSITION if none)
         */
        void onSnappedToView(View view, int position);
    }

    private final LinearLayoutManager layoutManager;
    private final OrientationHelper orientationHelper;
    private LinearSnapHelperListener listener;

    public LinearLayoutManagerSnapHelper(LinearLayoutManager layoutManager) {
        this.layoutManager = layoutManager;
        this.orientationHelper = OrientationHelper.createHorizontalHelper(this.layoutManager);
    }

    public void setLinearSnapHelperListener(LinearSnapHelperListener listener) {
        this.listener = listener;
    }

    @Override
    public View findSnapView(RecyclerView.LayoutManager layoutManager) {
        View snappedView = super.findSnapView(layoutManager);
        if (snappedView != null && layoutManager.canScrollHorizontally()) {
            if (layoutManager instanceof LinearLayoutManager) {

                // The super class implementation will always try to snap the center of a view to the
                // center of the screen. This is desired behavior, but will result in that the first
                // and last item will never be fully visible (unless in the special case when they all
                // fit on the screen)
                //
                // We handle this by checking if the first (and/or the last) item is visible, and compare
                // the distance it would take to "snap" this item to the screen edge to the distance
                // needed to snap the "snappedView" to the center of the screen. We always go for the
                // smallest distance, e.g. the closest snap position.
                //
                // To further complicate this, we might have intermediate views in the range 1..idxSnap
                // (and correspondingly idxsnap+1..idxLast-1) that will never be "snapped to". We
                // interpolate the "snap position" for these views (between center screen and screen edge)
                // and then calculate the snap distance for them, again selecting the smallest of them all.
                lastSavedTarget = null;

                int centerSnapPosition = orientationHelper.getTotalSpace() / 2;

                int firstChild = ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
                int lastChild = ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();

                int currentSmallestDistance = Integer.MAX_VALUE;
                View currentSmallestDistanceView = null;

                int snappedViewIndex = ((LinearLayoutManager) layoutManager).getPosition(snappedView);
                if (snappedViewIndex != RecyclerView.NO_POSITION) {

                    int snapPositionFirst = orientationHelper.getDecoratedMeasurement(((LinearLayoutManager) layoutManager).findViewByPosition(firstChild)) / 2;
                    int snapPositionLast = orientationHelper.getTotalSpace() - orientationHelper.getDecoratedMeasurement(((LinearLayoutManager) layoutManager).findViewByPosition(lastChild)) / 2;

                    // If first item not on screen, ignore views 0..snappedViewIndex-1
                    if (firstChild != 0) {
                        firstChild = snappedViewIndex;
                    }

                    // If last item not on screen, ignore views snappedViewIndex+1..N
                    if (lastChild != this.layoutManager.getItemCount() - 1) {
                        lastChild = snappedViewIndex;
                    }

                    for (int i = firstChild; i <= lastChild; i++) {
                        View view = ((LinearLayoutManager) layoutManager).findViewByPosition(i);

                        // Start by interpolating a snap position for (the center of) this view.
                        //
                        int snapPosition;
                        if (i == snappedViewIndex) {
                            snapPosition = centerSnapPosition;
                        } else if (i > snappedViewIndex) {
                            snapPosition = snapPositionLast - (lastChild - i) * (snapPositionLast - centerSnapPosition) / (lastChild - snappedViewIndex);
                        } else {
                            snapPosition = snapPositionFirst + (i - firstChild) * (centerSnapPosition - snapPositionFirst) / (snappedViewIndex - firstChild);
                        }

                        // Get current position of view (center)
                        //
                        int viewPosition = view.getLeft() + view.getWidth() / 2;

                        // Calculate distance and compare to current best candidate
                        //
                        int dist = snapPosition - viewPosition;
                        if (Math.abs(dist) < Math.abs(currentSmallestDistance) || (Math.abs(dist) == Math.abs(currentSmallestDistance))) {
                            currentSmallestDistance = dist;
                            currentSmallestDistanceView = view;
                        }
                    }

                    // Update with best snap candidate
                    snappedView = currentSmallestDistanceView;
                    lastSavedTarget = currentSmallestDistanceView;
                    lastSavedDistance = -currentSmallestDistance;
                }
            }
        }
        if (listener != null) {
            int snappedPosition = 0;
            if (snappedView != null) {
                snappedPosition = this.layoutManager.getPosition(snappedView);
            }
            listener.onSnappedToView(snappedView, snappedPosition);
        }
        return snappedView;
    }

    @Override
    public int[] calculateDistanceToFinalSnap(@NonNull RecyclerView.LayoutManager layoutManager, @NonNull View targetView) {
        if (targetView == lastSavedTarget) {
            // No need to recalc, we already did this when finding the snap candidate
            //
            int[] out = new int[2];
            out[0] = lastSavedDistance;
            out[1] = 0;
            return out;
        }
        return super.calculateDistanceToFinalSnap(layoutManager, targetView);
    }
}

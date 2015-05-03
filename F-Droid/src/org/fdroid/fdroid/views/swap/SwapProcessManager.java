package org.fdroid.fdroid.views.swap;

/**
 * Defines the contract between the {@link org.fdroid.fdroid.views.swap.SwapActivity}
 * and the fragments which live in it. The fragments each have the responsibility of
 * moving to the next stage of the process, and are entitled to stop swapping too
 * (e.g. when a "Cancel" button is pressed).
 */
public interface SwapProcessManager {
    void nextStep();
    void stopSwapping();
}

package org.fdroid.fdroid.views.updates;

import android.support.annotation.Nullable;

/**
 * When dismissing an item from the Updates tab, there is two different things we need to return.
 * This is a dumb data object to represent these things, because a method is only allowed to return one thing.
 */
public class DismissResult {

    @Nullable
    public final CharSequence message;

    public final boolean requiresAdapterRefresh;

    public DismissResult() {
        this(null, false);
    }

    public DismissResult(@Nullable CharSequence message, boolean requiresAdapterRefresh) {
        this.message = message;
        this.requiresAdapterRefresh = requiresAdapterRefresh;
    }

}

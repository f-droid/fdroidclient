package org.fdroid.fdroid.data;

public class AppPrefs extends ValueObject {

    /**
     * True if all updates for this app are to be ignored
     */
    public final boolean ignoreAllUpdates;

    /**
     * True if the current update for this app is to be ignored
     */
    public final int ignoreThisUpdate;

    public AppPrefs(int ignoreThis, boolean ignoreAll) {
        ignoreThisUpdate = ignoreThis;
        ignoreAllUpdates = ignoreAll;
    }

    public static AppPrefs createDefault() {
        return new AppPrefs(0, false);
    }

}

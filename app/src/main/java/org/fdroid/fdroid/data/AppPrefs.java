package org.fdroid.fdroid.data;

public class AppPrefs extends ValueObject {

    /**
     * True if all updates for this app are to be ignored
     */
    public boolean ignoreAllUpdates;

    /**
     * True if the current update for this app is to be ignored
     */
    public int ignoreThisUpdate;

    public AppPrefs(int ignoreThis, boolean ignoreAll) {
        ignoreThisUpdate = ignoreThis;
        ignoreAllUpdates = ignoreAll;
    }

    public static AppPrefs createDefault() {
        return new AppPrefs(0, false);
    }

    @Override
    public boolean equals(Object o) {
        return o != null && o instanceof AppPrefs &&
                ((AppPrefs) o).ignoreAllUpdates == ignoreAllUpdates &&
                ((AppPrefs) o).ignoreThisUpdate == ignoreThisUpdate;
    }

    @Override
    public int hashCode() {
        return (ignoreThisUpdate + "-" + ignoreAllUpdates).hashCode();
    }

    public AppPrefs createClone() {
        return new AppPrefs(ignoreThisUpdate, ignoreAllUpdates);
    }
}

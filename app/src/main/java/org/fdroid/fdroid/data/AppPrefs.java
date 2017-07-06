package org.fdroid.fdroid.data;

public class AppPrefs extends ValueObject {

    /**
     * True if all updates for this app are to be ignored.
     */
    public boolean ignoreAllUpdates;

    /**
     * The version code of the app for which the update should be ignored.
     */
    public int ignoreThisUpdate;

    /**
     * Don't notify of vulnerabilities in this app.
     */
    public boolean ignoreVulnerabilities;

    public AppPrefs(int ignoreThis, boolean ignoreAll, boolean ignoreVulns) {
        ignoreThisUpdate = ignoreThis;
        ignoreAllUpdates = ignoreAll;
        ignoreVulnerabilities = ignoreVulns;
    }

    public static AppPrefs createDefault() {
        return new AppPrefs(0, false, false);
    }

    @Override
    public boolean equals(Object o) {
        return o != null && o instanceof AppPrefs &&
                ((AppPrefs) o).ignoreAllUpdates == ignoreAllUpdates &&
                ((AppPrefs) o).ignoreThisUpdate == ignoreThisUpdate &&
                ((AppPrefs) o).ignoreVulnerabilities == ignoreVulnerabilities;
    }

    @Override
    public int hashCode() {
        return (ignoreThisUpdate + "-" + ignoreAllUpdates + "-" + ignoreVulnerabilities).hashCode();
    }

    public AppPrefs createClone() {
        return new AppPrefs(ignoreThisUpdate, ignoreAllUpdates, ignoreVulnerabilities);
    }
}

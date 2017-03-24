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

    /**
     * While offline, the user asked for this app to be installed when the device goes back online.
     */
    public boolean queueForDownload;

    public AppPrefs(int ignoreThis, boolean ignoreAll, boolean ignoreVulns, boolean queueForDownload) {
        ignoreThisUpdate = ignoreThis;
        ignoreAllUpdates = ignoreAll;
        ignoreVulnerabilities = ignoreVulns;
        this.queueForDownload = queueForDownload;
    }

    public static AppPrefs createDefault() {
        return new AppPrefs(0, false, false, false);
    }

    @Override
    public boolean equals(Object o) {
        return o != null && o instanceof AppPrefs &&
                ((AppPrefs) o).ignoreAllUpdates == ignoreAllUpdates &&
                ((AppPrefs) o).ignoreThisUpdate == ignoreThisUpdate &&
                ((AppPrefs) o).ignoreVulnerabilities == ignoreVulnerabilities &&
                ((AppPrefs) o).queueForDownload == queueForDownload;
    }

    @Override
    public int hashCode() {
        return (ignoreThisUpdate + "-" + ignoreAllUpdates + "-" + ignoreVulnerabilities + "-" + queueForDownload).hashCode();
    }

    public AppPrefs createClone() {
        return new AppPrefs(ignoreThisUpdate, ignoreAllUpdates, ignoreVulnerabilities, queueForDownload);
    }
}

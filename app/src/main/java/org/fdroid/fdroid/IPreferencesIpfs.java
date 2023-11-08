package org.fdroid.fdroid;

import java.util.Set;

/**
 * Interface for accessing IPFS related preferences.
 * <p>
 * Splitting this into a separate interfaces allows for easily injecting data into Compose UI
 * previews. (and maybe unit-tests too)
 */
public interface IPreferencesIpfs {
    boolean isIpfsEnabled();

    void setIpfsEnabled(boolean enabled);

    Set<String> getIpfsGwDisabledDefaults();

    void setIpfsGwDisabledDefaults(Set<String> selectedSet);

    Set<String> getIpfsGwUserList();

    void setIpfsGwUserList(Set<String> selectedSet);
}

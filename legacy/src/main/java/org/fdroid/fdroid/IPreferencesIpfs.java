package org.fdroid.fdroid;

import java.util.List;

/**
 * Interface for accessing IPFS related preferences.
 * <p>
 * Splitting this into a separate interfaces allows for easily injecting data into Compose UI
 * previews. (and maybe unit-tests too)
 */
public interface IPreferencesIpfs {
    boolean isIpfsEnabled();

    void setIpfsEnabled(boolean enabled);

    List<String> getIpfsGwDisabledDefaults();

    void setIpfsGwDisabledDefaults(List<String> selectedSet);

    List<String> getIpfsGwUserList();

    void setIpfsGwUserList(List<String> selectedList);
}

package org.fdroid.fdroid;


import java.util.Set;

/**
 * Interface for accessing IPFS related preferences.
 *
 * Splitting this into a separate interfaces allows for easily injecting data into Compose UI
 * previews. (and maybe unit-tests too)
 */
public interface IPreferencesIpfs {
    public boolean isIpfsEnabled();

    public void setIpfsEnabled(boolean enabled);

    public Set<String> getIpfsGwDisabledDefaults();

    public void setIpfsGwDisabledDefaults(Set<String> selectedSet);

    public Set<String> getIpfsGwUserList();

    public void setIpfsGwUserList(Set<String> selectedSet);
}

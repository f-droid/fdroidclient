package org.fdroid.ui.settings

/**
 * Interface for accessing IPFS related preferences.
 *
 * Splitting this into a separate interfaces allows for easily injecting data into Compose UI
 * previews. (and maybe unit-tests too)
 */
interface IPreferencesIpfs {
    var isIpfsEnabled: Boolean
    var ipfsGwDisabledDefaults: List<String>
    var ipfsGwUserList: List<String>
}

package org.fdroid.ui.settings

import androidx.core.content.edit
import org.fdroid.settings.SettingsManager
import org.json.JSONArray
import org.json.JSONException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IpfsManager @Inject constructor(
    settingsManager: SettingsManager,
) : IPreferencesIpfs {

    companion object {
        val DEFAULT_GATEWAYS = listOf(
            "https://ipfs.io/ipfs/",
        )
        const val PREF_USE_IPFS_GATEWAYS = "useIpfsGateways"
        const val PREF_IPFSGW_DISABLED_DEFAULTS_LIST = "ipfsGwDisabledDefaultsList"
        const val PREF_IPFSGW_USER_LIST = "ipfsGwUserList"
    }

    private val prefs = settingsManager.prefs

    override var isIpfsEnabled: Boolean
        get() = prefs.getBoolean(PREF_USE_IPFS_GATEWAYS, false)
        set(value) {
            prefs.edit {
                putBoolean(PREF_USE_IPFS_GATEWAYS, value)
            }
        }
    override var ipfsGwDisabledDefaults: List<String>
        get() {
            val list = prefs.getString(PREF_IPFSGW_DISABLED_DEFAULTS_LIST, "[]") ?: "[]"
            return parseJsonStringArray(list)
        }
        set(value) {
            prefs.edit {
                putString(PREF_IPFSGW_DISABLED_DEFAULTS_LIST, toJsonStringArray(value))
            }
        }
    override var ipfsGwUserList: List<String>
        get() = parseJsonStringArray(prefs.getString(PREF_IPFSGW_USER_LIST, "[]") ?: "[]")
        set(value) {
            prefs.edit {
                putString(PREF_IPFSGW_USER_LIST, toJsonStringArray(value))
            }
        }

    private fun parseJsonStringArray(json: String): List<String> {
        try {
            val jsonArray = JSONArray(json)
            return MutableList(jsonArray.length()) { i ->
                jsonArray.getString(i)
            }
        } catch (_: JSONException) {
            return emptyList()
        }
    }

    private fun toJsonStringArray(list: List<String>): String = JSONArray().apply {
        for (str in list) put(str)
    }.toString()

}

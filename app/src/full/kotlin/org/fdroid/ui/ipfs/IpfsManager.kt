package org.fdroid.ui.ipfs

import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.fdroid.settings.SettingsManager
import org.json.JSONArray
import org.json.JSONException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IpfsManager @Inject constructor(
    settingsManager: SettingsManager,
) : IpfsActions {

    companion object {
        val DEFAULT_GATEWAYS = listOf(
            "https://ipfs.io/ipfs/",
        )
        const val PREF_USE_IPFS_GATEWAYS = "useIpfsGateways"
        const val PREF_IPFSGW_DISABLED_DEFAULTS_LIST = "ipfsGwDisabledDefaultsList"
        const val PREF_IPFSGW_USER_LIST = "ipfsGwUserList"
    }

    private val prefs = settingsManager.prefs

    private val _preferences = MutableStateFlow(loadPreferences())
    val preferences = _preferences.asStateFlow()

    var enabled: Boolean
        get() = prefs.getBoolean(PREF_USE_IPFS_GATEWAYS, false)
        private set(value) {
            prefs.edit {
                putBoolean(PREF_USE_IPFS_GATEWAYS, value)
            }
        }
    private var disabledDefaultGateways: List<String>
        get() {
            val list = prefs.getString(PREF_IPFSGW_DISABLED_DEFAULTS_LIST, "[]") ?: "[]"
            return parseJsonStringArray(list)
        }
        set(value) {
            prefs.edit {
                putString(PREF_IPFSGW_DISABLED_DEFAULTS_LIST, toJsonStringArray(value))
            }
        }
    private var userGateways: List<String>
        get() = parseJsonStringArray(prefs.getString(PREF_IPFSGW_USER_LIST, "[]") ?: "[]")
        set(value) {
            prefs.edit {
                putString(PREF_IPFSGW_USER_LIST, toJsonStringArray(value))
            }
        }
    val activeGateways: List<String>
        get() = userGateways.toMutableList().apply {
            for (gatewayUrl in DEFAULT_GATEWAYS) {
                if (!disabledDefaultGateways.contains(gatewayUrl)) {
                    add(gatewayUrl)
                }
            }
        }

    private fun loadPreferences() = IpfsPreferences(
        isIpfsEnabled = prefs.getBoolean(PREF_USE_IPFS_GATEWAYS, false),
        disabledDefaultGateways = parseJsonStringArray(
            prefs.getString(PREF_IPFSGW_DISABLED_DEFAULTS_LIST, "[]") ?: "[]"
        ),
        userGateways = parseJsonStringArray(prefs.getString(PREF_IPFSGW_USER_LIST, "[]") ?: "[]"),
    )

    override fun setIpfsEnabled(enabled: Boolean) {
        _preferences.update {
            it.copy(isIpfsEnabled = enabled)
        }
    }

    override fun setDefaultGatewayEnabled(url: String, enabled: Boolean) {
        _preferences.update {
            val newList = it.disabledDefaultGateways.toMutableList()
            if (!enabled) {
                newList.add(url)
            } else {
                newList.remove(url)
            }
            disabledDefaultGateways = newList
            it.copy(disabledDefaultGateways = newList)
        }
    }

    override fun addUserGateway(url: String) {
        // don't allow adding default gateways to the user gateways list
        if (!DEFAULT_GATEWAYS.contains(url)) _preferences.update {
            if (it.userGateways.contains(url)) {
                it // already has URL in list
            } else {
                val newList = it.userGateways.toMutableList().apply {
                    add(url)
                }
                userGateways = newList
                it.copy(userGateways = newList)
            }
        }
    }

    override fun removeUserGateway(url: String) = _preferences.update {
        val newGateways = it.userGateways.toMutableList()
        newGateways.remove(url)
        userGateways = newGateways
        it.copy(userGateways = newGateways)
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

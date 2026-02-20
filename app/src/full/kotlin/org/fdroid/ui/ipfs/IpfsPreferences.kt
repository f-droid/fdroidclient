package org.fdroid.ui.ipfs

interface IpfsActions {
    fun setIpfsEnabled(enabled: Boolean)
    fun setDefaultGatewayEnabled(url: String, enabled: Boolean)
    fun addUserGateway(url: String)
    fun removeUserGateway(url: String)
}

data class IpfsPreferences(
    val isIpfsEnabled: Boolean,
    val disabledDefaultGateways: List<String>,
    val userGateways: List<String>,
)

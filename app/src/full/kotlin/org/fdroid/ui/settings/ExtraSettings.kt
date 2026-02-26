package org.fdroid.ui.settings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import me.zhanghai.compose.preference.preference
import org.fdroid.R
import org.fdroid.ui.ipfs.IpfsGatewaySettingsActivity
import org.fdroid.ui.panic.PanicActivity

fun LazyListScope.extraPrivacySettings(context: Context) {
  preference(
    key = "panic",
    icon = {},
    title = { Text(stringResource(R.string.panic_settings)) },
    summary = { Text(stringResource(R.string.panic_settings_summary)) },
    onClick = {
      val intent = Intent(context, PanicActivity::class.java)
      context.startActivity(intent)
    },
  )
}

fun LazyListScope.extraNetworkSettings(context: Context) {
  preference(
    key = "ipfsGateways",
    icon = {},
    title = { Text(stringResource(R.string.ipfsgw_title)) },
    summary = { Text(stringResource(R.string.ipfsgw_summary_new)) },
    onClick = {
      val intent = Intent(context, IpfsGatewaySettingsActivity::class.java)
      context.startActivity(intent)
    },
  )
}

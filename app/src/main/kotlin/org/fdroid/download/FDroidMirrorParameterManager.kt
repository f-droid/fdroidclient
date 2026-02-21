package org.fdroid.download

import android.content.Context
import android.telephony.TelephonyManager
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import org.fdroid.settings.SettingsConstants
import org.fdroid.settings.SettingsManager
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
@Singleton
class FDroidMirrorParameterManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsManager: SettingsManager,
    private val dnsWithCache: DnsWithCache
) : MirrorParameterManager {

    override fun shouldRetryRequest(mirrorUrl: String): Boolean {
        return dnsWithCache.shouldRetryRequest(mirrorUrl)
    }

    // TODO overhaul default MirrorChooser
    override fun incrementMirrorErrorCount(mirrorUrl: String) {}

    override fun getMirrorErrorCount(mirrorUrl: String): Int = 0

    override fun preferForeignMirrors(): Boolean {
        return settingsManager.mirrorChooser == SettingsConstants.MirrorChooserValues.PreferForeign
    }

    override fun getCurrentLocation(): String {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return tm.simCountryIso ?: tm.networkCountryIso ?: run {
            val localeList = LocaleListCompat.getDefault()
            localeList.get(0)?.country ?: Locale.getDefault().country
        }
    }
}

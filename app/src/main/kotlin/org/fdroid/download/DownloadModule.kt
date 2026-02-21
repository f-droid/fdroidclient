package org.fdroid.download

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.fdroid.BuildConfig
import org.fdroid.settings.SettingsManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DownloadModule {

    private const val USER_AGENT = "F-Droid ${BuildConfig.VERSION_NAME}"

    @Provides
    @Singleton
    fun provideHttpManager(
        settingsManager: SettingsManager,
        dns: DnsWithCache,
        manager: FDroidMirrorParameterManager
    ): HttpManager {
        return HttpManager(
            userAgent = USER_AGENT,
            proxyConfig = settingsManager.proxyConfig,
            customDns = dns,
            mirrorParameterManager = manager
        )
    }

    @Provides
    @Singleton
    fun provideDownloaderFactory(
        downloaderFactoryImpl: DownloaderFactoryImpl,
    ): DownloaderFactory = downloaderFactoryImpl
}

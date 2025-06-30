package org.fdroid.basic.download

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.fdroid.basic.BuildConfig
import org.fdroid.download.DownloaderFactory
import org.fdroid.download.HttpManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DownloadModule {

    private const val USER_AGENT = "F-Droid ${BuildConfig.VERSION_NAME}"

    @Provides
    @Singleton
    fun provideHttpManager(): HttpManager {
        return HttpManager(userAgent = USER_AGENT)
    }

    @Provides
    @Singleton
    fun provideDownloaderFactory(httpManager: HttpManager): DownloaderFactory {
        return DownloaderFactoryImpl(httpManager)
    }
}

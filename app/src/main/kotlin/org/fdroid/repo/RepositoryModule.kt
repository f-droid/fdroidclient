package org.fdroid.repo

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.fdroid.CompatibilityChecker
import org.fdroid.database.FDroidDatabase
import org.fdroid.download.DownloaderFactory
import org.fdroid.download.HttpManager
import org.fdroid.index.RepoManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideRepoManager(
        @ApplicationContext context: Context,
        db: FDroidDatabase,
        downloaderFactory: DownloaderFactory,
        httpManager: HttpManager,
        compatibilityChecker: CompatibilityChecker,
    ): RepoManager = RepoManager(
        context = context,
        db = db,
        downloaderFactory = downloaderFactory,
        httpManager = httpManager,
        compatibilityChecker = compatibilityChecker,
    )
}

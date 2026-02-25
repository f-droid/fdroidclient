package org.fdroid.updates

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.fdroid.CompatibilityChecker
import org.fdroid.CompatibilityCheckerImpl
import org.fdroid.UpdateChecker
import org.fdroid.database.DbAppChecker
import org.fdroid.database.FDroidDatabase

@Module
@InstallIn(SingletonComponent::class)
object UpdatesModule {
  @Provides
  @Singleton
  fun provideCompatibilityChecker(@ApplicationContext context: Context): CompatibilityChecker {
    return CompatibilityCheckerImpl(context.packageManager)
  }

  @Provides
  @Singleton
  fun provideUpdateChecker(compatibilityChecker: CompatibilityChecker): UpdateChecker {
    return UpdateChecker(compatibilityChecker)
  }

  @Provides
  @Singleton
  fun provideDbAppChecker(
    @ApplicationContext context: Context,
    db: FDroidDatabase,
    updateChecker: UpdateChecker,
    compatibilityChecker: CompatibilityChecker,
  ): DbAppChecker {
    return DbAppChecker(db, context, compatibilityChecker, updateChecker)
  }
}

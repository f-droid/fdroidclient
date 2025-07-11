package org.fdroid.basic.updates

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.fdroid.CompatibilityChecker
import org.fdroid.CompatibilityCheckerImpl
import org.fdroid.UpdateChecker
import org.fdroid.database.DbUpdateChecker
import org.fdroid.database.FDroidDatabase
import javax.inject.Singleton

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
    fun provideDbUpdateChecker(
        @ApplicationContext context: Context,
        db: FDroidDatabase,
        updateChecker: UpdateChecker,
        compatibilityChecker: CompatibilityChecker,
    ): DbUpdateChecker {
        return DbUpdateChecker(db, context.packageManager, compatibilityChecker, updateChecker)
    }
}

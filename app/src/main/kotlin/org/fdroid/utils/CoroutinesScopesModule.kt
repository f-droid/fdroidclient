package org.fdroid.utils

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
object CoroutinesScopesModule {
  @Singleton
  @Provides
  @IoDispatcher
  fun providesCoroutineIoScope(): CoroutineScope {
    // Run this code when providing an instance of CoroutineScope
    return CoroutineScope(SupervisorJob() + Dispatchers.IO)
  }
}

@Qualifier @Retention(AnnotationRetention.RUNTIME) annotation class IoDispatcher

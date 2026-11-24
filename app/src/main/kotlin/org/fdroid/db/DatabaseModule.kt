package org.fdroid.db

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.fdroid.BuildConfig
import org.fdroid.database.FDroidDatabase
import org.fdroid.database.FDroidDatabaseHolder

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
  @Provides
  @Singleton
  fun provideFDroidDatabase(
    @ApplicationContext context: Context,
    databaseOpenState: DatabaseOpenState,
    initialData: InitialData,
  ): FDroidDatabase {
    return FDroidDatabaseHolder.getDb(
      context = context,
      name = "fdroid_db",
      fixture = initialData,
      logSlowQueries = BuildConfig.DEBUG,
      onOpenCallback = { databaseOpenState.markAsOpen() },
    )
  }
}

@Singleton
class DatabaseOpenState @Inject constructor() {
  private val _isOpen = MutableStateFlow(false)
  val isOpen = _isOpen.asStateFlow()

  internal fun markAsOpen() {
    _isOpen.value = true
  }
}

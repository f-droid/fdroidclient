package org.fdroid.db

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.fdroid.database.FDroidDatabase
import org.fdroid.database.FDroidDatabaseHolder
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideFDroidDatabase(
        @ApplicationContext context: Context,
        initialData: InitialData,
    ): FDroidDatabase {
        return FDroidDatabaseHolder.getDb(context, "fdroid_db", initialData)
    }
}

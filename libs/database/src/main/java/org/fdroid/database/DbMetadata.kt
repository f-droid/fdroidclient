package org.fdroid.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity
internal data class DbMetadata(
  @PrimaryKey val key: String,
  val value: String,
)

@Dao
internal interface DbMetadataDao {
  @Query("SELECT value FROM DbMetadata WHERE `key` = :key") suspend fun get(key: String): String?

  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun set(vararg entries: DbMetadata)
}

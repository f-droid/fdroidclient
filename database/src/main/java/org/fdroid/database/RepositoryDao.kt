package org.fdroid.database

import androidx.annotation.VisibleForTesting
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.ABORT
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import org.fdroid.index.ReflectionDiffer.applyDiff
import org.fdroid.index.IndexParser.json
import org.fdroid.index.v2.MirrorV2
import org.fdroid.index.v2.RepoV2

public interface RepositoryDao {
    fun insert(repository: RepoV2)
}

@Dao
internal interface RepositoryDaoInt : RepositoryDao {

    @Insert(onConflict = ABORT)
    fun insert(repository: CoreRepository): Long

    @Insert(onConflict = REPLACE)
    fun insertMirrors(mirrors: List<Mirror>)

    @Insert(onConflict = REPLACE)
    fun insertAntiFeatures(repoFeature: List<AntiFeature>)

    @Insert(onConflict = REPLACE)
    fun insertCategories(repoFeature: List<Category>)

    @Insert(onConflict = REPLACE)
    fun insertReleaseChannels(repoFeature: List<ReleaseChannel>)

    @Transaction
    override fun insert(repository: RepoV2) {
        val repoId = insert(repository.toCoreRepository())
        insertMirrors(repository.mirrors.map { it.toMirror(repoId) })
        insertAntiFeatures(repository.antiFeatures.toRepoAntiFeatures(repoId))
        insertCategories(repository.categories.toRepoCategories(repoId))
        insertReleaseChannels(repository.releaseChannels.toRepoReleaseChannel(repoId))
    }

    @Transaction
    @Query("SELECT * FROM CoreRepository WHERE repoId = :repoId")
    fun getRepository(repoId: Long): Repository

    @Transaction
    fun updateRepository(repoId: Long, jsonObject: JsonObject) {
        // get existing repo
        val repo = getRepository(repoId)
        // update repo with JSON diff
        updateRepository(applyDiff(repo.repository, jsonObject))
        // replace mirror list, if it is in the diff
        if (jsonObject.containsKey("mirrors")) {
            val mirrorArray = jsonObject["mirrors"] as JsonArray
            val mirrors = json.decodeFromJsonElement<List<MirrorV2>>(mirrorArray).map {
                it.toMirror(repoId)
            }
            // delete and re-insert mirrors, because it is easier than diffing
            deleteMirrors(repoId)
            insertMirrors(mirrors)
        }
        // diff and update the antiFeatures
        diffAndUpdateTable(
            jsonObject,
            "antiFeatures",
            repo.antiFeatures,
            { name -> AntiFeature(repoId, name, null, emptyMap()) },
            { item -> item.name },
            { deleteAntiFeatures(repoId) },
            { name -> deleteAntiFeature(repoId, name) },
            { list -> insertAntiFeatures(list) },
        )
        // diff and update the categories
        diffAndUpdateTable(
            jsonObject,
            "categories",
            repo.categories,
            { name -> Category(repoId, name, null, emptyMap()) },
            { item -> item.name },
            { deleteCategories(repoId) },
            { name -> deleteCategory(repoId, name) },
            { list -> insertCategories(list) },
        )
        // diff and update the releaseChannels
        diffAndUpdateTable(
            jsonObject,
            "releaseChannels",
            repo.releaseChannels,
            { name -> ReleaseChannel(repoId, name, null, emptyMap()) },
            { item -> item.name },
            { deleteReleaseChannels(repoId) },
            { name -> deleteReleaseChannel(repoId, name) },
            { list -> insertReleaseChannels(list) },
        )
    }

    /**
     * Applies the diff from [JsonObject] identified by the given [key] of the given [jsonObject]
     * to the given [itemList] and updates the DB as needed.
     *
     * @param newItem A function to produce a new [T] which typically contains the primary key(s).
     */
    private fun <T : Any> diffAndUpdateTable(
        jsonObject: JsonObject,
        key: String,
        itemList: List<T>,
        newItem: (String) -> T,
        keyGetter: (T) -> String,
        deleteAll: () -> Unit,
        deleteOne: (String) -> Unit,
        insertReplace: (List<T>) -> Unit,
    ) {
        if (!jsonObject.containsKey(key)) return
        if (jsonObject[key] == JsonNull) {
            deleteAll()
        } else {
            val features = jsonObject[key]?.jsonObject ?: error("no $key object")
            val list = itemList.toMutableList()
            features.entries.forEach { (key, value) ->
                if (value is JsonNull) {
                    list.removeAll { keyGetter(it) == key }
                    deleteOne(key)
                } else {
                    val index = list.indexOfFirst { keyGetter(it) == key }
                    val item = if (index == -1) null else list[index]
                    if (item == null) {
                        list.add(applyDiff(newItem(key), value.jsonObject))
                    } else {
                        list[index] = applyDiff(item, value.jsonObject)
                    }
                }
            }
            insertReplace(list)
        }
    }

    @Update
    fun updateRepository(repo: CoreRepository): Int

    @Transaction
    @Query("SELECT * FROM CoreRepository")
    fun getRepositories(): List<Repository>

    @VisibleForTesting
    @Query("SELECT * FROM Mirror")
    fun getMirrors(): List<Mirror>

    @VisibleForTesting
    @Query("DELETE FROM Mirror WHERE repoId = :repoId")
    fun deleteMirrors(repoId: Long)

    @VisibleForTesting
    @Query("SELECT * FROM AntiFeature")
    fun getAntiFeatures(): List<AntiFeature>

    @VisibleForTesting
    @Query("DELETE FROM AntiFeature WHERE repoId = :repoId")
    fun deleteAntiFeatures(repoId: Long)

    @VisibleForTesting
    @Query("DELETE FROM AntiFeature WHERE repoId = :repoId AND name = :name")
    fun deleteAntiFeature(repoId: Long, name: String)

    @VisibleForTesting
    @Query("SELECT * FROM Category")
    fun getCategories(): List<Category>

    @VisibleForTesting
    @Query("DELETE FROM Category WHERE repoId = :repoId")
    fun deleteCategories(repoId: Long)

    @VisibleForTesting
    @Query("DELETE FROM Category WHERE repoId = :repoId AND name = :name")
    fun deleteCategory(repoId: Long, name: String)

    @VisibleForTesting
    @Query("SELECT * FROM ReleaseChannel")
    fun getReleaseChannels(): List<ReleaseChannel>

    @VisibleForTesting
    @Query("DELETE FROM ReleaseChannel WHERE repoId = :repoId")
    fun deleteReleaseChannels(repoId: Long)

    @VisibleForTesting
    @Query("DELETE FROM ReleaseChannel WHERE repoId = :repoId AND name = :name")
    fun deleteReleaseChannel(repoId: Long, name: String)

    @Delete
    fun removeRepository(repository: CoreRepository)

}

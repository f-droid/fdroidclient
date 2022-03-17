package org.fdroid.database

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import org.fdroid.index.IndexParser.json
import org.fdroid.index.v2.MirrorV2
import org.fdroid.index.v2.ReflectionDiffer.applyDiff
import org.fdroid.index.v2.RepoV2

public interface RepositoryDao {
    /**
     * Use when inserting a new repo for the first time.
     */
    fun insert(repository: RepoV2): Long

    /**
     * Use when replacing an existing repo with a full index.
     * This removes all existing index data associated with this repo from the database.
     */
    fun replace(repoId: Long, repository: RepoV2, certificate: String?)

    fun getRepository(repoId: Long): Repository?
    fun insertEmptyRepo(address:String): Long
    fun deleteRepository(repoId: Long)
    fun getRepositories(): List<Repository>
    fun getLiveRepositories(): LiveData<List<Repository>>
    // FIXME: We probably want unique categories here flattened by repo weight
    fun getLiveCategories(): LiveData<List<Category>>
}

@Dao
internal interface RepositoryDaoInt : RepositoryDao {

    @Insert(onConflict = REPLACE)
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
    override fun insertEmptyRepo(address: String): Long {
        val repo = CoreRepository(
            name = "",
            icon = null,
            address = address,
            timestamp = System.currentTimeMillis(),
            certificate = null,
        )
        return insert(repo)
    }

    @Transaction
    override fun insert(repository: RepoV2): Long {
        val repoId = insert(repository.toCoreRepository())
        insertRepoTables(repoId, repository)
        return repoId
    }

    @Transaction
    override fun replace(repoId: Long, repository: RepoV2, certificate: String?) {
        val newRepoId = insert(repository.toCoreRepository(repoId, certificate))
        require(newRepoId == repoId) { "New repoId $newRepoId did not match old $repoId" }
        insertRepoTables(repoId, repository)
    }

    private fun insertRepoTables(repoId: Long, repository: RepoV2) {
        insertMirrors(repository.mirrors.map { it.toMirror(repoId) })
        insertAntiFeatures(repository.antiFeatures.toRepoAntiFeatures(repoId))
        insertCategories(repository.categories.toRepoCategories(repoId))
        insertReleaseChannels(repository.releaseChannels.toRepoReleaseChannel(repoId))
    }

    @Transaction
    @Query("SELECT * FROM CoreRepository WHERE repoId = :repoId")
    override fun getRepository(repoId: Long): Repository?

    @Transaction
    fun updateRepository(repoId: Long, jsonObject: JsonObject) {
        // get existing repo
        val repo = getRepository(repoId) ?: error("Repo $repoId does not exist")
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
            jsonObject = jsonObject,
            key = "antiFeatures",
            itemList = repo.antiFeatures,
            newItem = { key -> AntiFeature(repoId, key, null, emptyMap(), emptyMap()) },
            keyGetter = { item -> item.id },
            deleteAll = { deleteAntiFeatures(repoId) },
            deleteOne = { key -> deleteAntiFeature(repoId, key) },
            insertReplace = { list -> insertAntiFeatures(list) },
        )
        // diff and update the categories
        diffAndUpdateTable(
            jsonObject = jsonObject,
            key = "categories",
            itemList = repo.categories,
            newItem = { key -> Category(repoId, key, null, emptyMap(), emptyMap()) },
            keyGetter = { item -> item.id },
            deleteAll = { deleteCategories(repoId) },
            deleteOne = { key -> deleteCategory(repoId, key) },
            insertReplace = { list -> insertCategories(list) },
        )
        // diff and update the releaseChannels
        diffAndUpdateTable(
            jsonObject = jsonObject,
            key = "releaseChannels",
            itemList = repo.releaseChannels,
            newItem = { key -> ReleaseChannel(repoId, key, null, emptyMap(), emptyMap()) },
            keyGetter = { item -> item.id },
            deleteAll = { deleteReleaseChannels(repoId) },
            deleteOne = { key -> deleteReleaseChannel(repoId, key) },
            insertReplace = { list -> insertReleaseChannels(list) },
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

    @Query("UPDATE CoreRepository SET certificate = :certificate WHERE repoId = :repoId")
    fun updateRepository(repoId: Long, certificate: String)

    @Transaction
    @Query("SELECT * FROM CoreRepository")
    override fun getRepositories(): List<Repository>

    @Transaction
    @Query("SELECT * FROM CoreRepository")
    override fun getLiveRepositories(): LiveData<List<Repository>>

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
    @Query("DELETE FROM AntiFeature WHERE repoId = :repoId AND id = :id")
    fun deleteAntiFeature(repoId: Long, id: String)

    @VisibleForTesting
    @Query("SELECT * FROM Category")
    fun getCategories(): List<Category>

    @RewriteQueriesToDropUnusedColumns
    @Query("""SELECT * FROM Category
        JOIN RepositoryPreferences AS pref USING (repoId)
        WHERE pref.enabled = 1 GROUP BY id HAVING MAX(pref.weight)""")
    override fun getLiveCategories(): LiveData<List<Category>>

    @VisibleForTesting
    @Query("DELETE FROM Category WHERE repoId = :repoId")
    fun deleteCategories(repoId: Long)

    @VisibleForTesting
    @Query("DELETE FROM Category WHERE repoId = :repoId AND id = :id")
    fun deleteCategory(repoId: Long, id: String)

    @VisibleForTesting
    @Query("SELECT * FROM ReleaseChannel")
    fun getReleaseChannels(): List<ReleaseChannel>

    @VisibleForTesting
    @Query("DELETE FROM ReleaseChannel WHERE repoId = :repoId")
    fun deleteReleaseChannels(repoId: Long)

    @VisibleForTesting
    @Query("DELETE FROM ReleaseChannel WHERE repoId = :repoId AND id = :id")
    fun deleteReleaseChannel(repoId: Long, id: String)

    @Delete
    fun deleteRepository(repository: CoreRepository)

    @Query("DELETE FROM CoreRepository WHERE repoId = :repoId")
    override fun deleteRepository(repoId: Long)

}

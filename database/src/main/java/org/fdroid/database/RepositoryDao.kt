package org.fdroid.database

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import org.fdroid.database.DbDiffUtils.diffAndUpdateListTable
import org.fdroid.database.DbDiffUtils.diffAndUpdateTable
import org.fdroid.index.IndexParser.json
import org.fdroid.index.v2.MirrorV2
import org.fdroid.index.v2.ReflectionDiffer.applyDiff
import org.fdroid.index.v2.RepoV2

public interface RepositoryDao {
    /**
     * Inserts a new [InitialRepository] from a fixture.
     */
    public fun insert(initialRepo: InitialRepository)

    /**
     * Removes all repos and their preferences.
     */
    public fun clearAll()

    public fun getRepository(repoId: Long): Repository?
    public fun insertEmptyRepo(
        address: String,
        username: String? = null,
        password: String? = null,
    ): Long

    public fun deleteRepository(repoId: Long)
    public fun getRepositories(): List<Repository>
    public fun getLiveRepositories(): LiveData<List<Repository>>
    public fun countAppsPerRepository(repoId: Long): Int
    public fun setRepositoryEnabled(repoId: Long, enabled: Boolean)
    public fun updateUserMirrors(repoId: Long, mirrors: List<String>)
    public fun updateUsernameAndPassword(repoId: Long, username: String?, password: String?)
    public fun updateDisabledMirrors(repoId: Long, disabledMirrors: List<String>)
    public fun getLiveCategories(): LiveData<List<Category>>
}

@Dao
internal interface RepositoryDaoInt : RepositoryDao {

    @Insert(onConflict = REPLACE)
    fun insertOrReplace(repository: CoreRepository): Long

    @Update
    fun update(repository: CoreRepository)

    @Insert(onConflict = REPLACE)
    fun insertMirrors(mirrors: List<Mirror>)

    @Insert(onConflict = REPLACE)
    fun insertAntiFeatures(repoFeature: List<AntiFeature>)

    @Insert(onConflict = REPLACE)
    fun insertCategories(repoFeature: List<Category>)

    @Insert(onConflict = REPLACE)
    fun insertReleaseChannels(repoFeature: List<ReleaseChannel>)

    @Insert(onConflict = REPLACE)
    fun insert(repositoryPreferences: RepositoryPreferences)

    @Transaction
    override fun insert(initialRepo: InitialRepository) {
        val repo = CoreRepository(
            name = mapOf("en-US" to initialRepo.name),
            address = initialRepo.address,
            icon = null,
            timestamp = -1,
            version = initialRepo.version,
            formatVersion = null,
            maxAge = null,
            description = mapOf("en-US" to initialRepo.description),
            certificate = initialRepo.certificate,
        )
        val repoId = insertOrReplace(repo)
        val repositoryPreferences = RepositoryPreferences(
            repoId = repoId,
            weight = initialRepo.weight,
            lastUpdated = null,
            enabled = initialRepo.enabled,
        )
        insert(repositoryPreferences)
    }

    @Transaction
    override fun insertEmptyRepo(
        address: String,
        username: String?,
        password: String?,
    ): Long {
        val repo = CoreRepository(
            name = mapOf("en-US" to address),
            icon = null,
            address = address,
            timestamp = -1,
            version = null,
            formatVersion = null,
            maxAge = null,
            certificate = null,
        )
        val repoId = insertOrReplace(repo)
        val currentMaxWeight = getMaxRepositoryWeight()
        val repositoryPreferences = RepositoryPreferences(
            repoId = repoId,
            weight = currentMaxWeight + 1,
            lastUpdated = null,
            username = username,
            password = password,
        )
        insert(repositoryPreferences)
        return repoId
    }

    @Transaction
    @VisibleForTesting
    fun insertOrReplace(repository: RepoV2): Long {
        val repoId = insertOrReplace(repository.toCoreRepository(version = 0))
        insertRepositoryPreferences(repoId)
        insertRepoTables(repoId, repository)
        return repoId
    }

    private fun insertRepositoryPreferences(repoId: Long) {
        val currentMaxWeight = getMaxRepositoryWeight()
        val repositoryPreferences = RepositoryPreferences(repoId, currentMaxWeight + 1)
        insert(repositoryPreferences)
    }

    /**
     * Use when replacing an existing repo with a full index.
     * This removes all existing index data associated with this repo from the database,
     * but does not touch repository preferences.
     * @throws IllegalStateException if no repo with the given [repoId] exists.
     */
    @Transaction
    fun clear(repoId: Long) {
        val repo = getRepository(repoId) ?: error("repo with id $repoId does not exist")
        // this clears all foreign key associated data since the repo gets replaced
        insertOrReplace(repo.repository)
    }

    @Transaction
    override fun clearAll() {
        deleteAllCoreRepositories()
        deleteAllRepositoryPreferences()
    }

    /**
     * Updates an existing repo with new data from a full index update.
     * Call [clear] first to ensure old data was removed.
     */
    @Transaction
    fun update(
        repoId: Long,
        repository: RepoV2,
        version: Long,
        formatVersion: IndexFormatVersion,
        certificate: String?,
    ) {
        update(repository.toCoreRepository(repoId, version, formatVersion, certificate))
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
    fun updateRepository(repoId: Long, version: Long, jsonObject: JsonObject) {
        // get existing repo
        val repo = getRepository(repoId) ?: error("Repo $repoId does not exist")
        // update repo with JSON diff
        updateRepository(applyDiff(repo.repository, jsonObject).copy(version = version))
        // replace mirror list (if it is in the diff)
        diffAndUpdateListTable(
            jsonObject = jsonObject,
            jsonObjectKey = "mirrors",
            listParser = { mirrorArray ->
                json.decodeFromJsonElement<List<MirrorV2>>(mirrorArray).map {
                    it.toMirror(repoId)
                }
            },
            deleteList = { deleteMirrors(repoId) },
            insertNewList = { mirrors -> insertMirrors(mirrors) },
        )
        // diff and update the antiFeatures
        diffAndUpdateTable(
            jsonObject = jsonObject,
            jsonObjectKey = "antiFeatures",
            itemList = repo.antiFeatures,
            itemFinder = { key, item -> item.id == key },
            newItem = { key -> AntiFeature(repoId, key, null, emptyMap(), emptyMap()) },
            deleteAll = { deleteAntiFeatures(repoId) },
            deleteOne = { key -> deleteAntiFeature(repoId, key) },
            insertReplace = { list -> insertAntiFeatures(list) },
        )
        // diff and update the categories
        diffAndUpdateTable(
            jsonObject = jsonObject,
            jsonObjectKey = "categories",
            itemList = repo.categories,
            itemFinder = { key, item -> item.id == key },
            newItem = { key -> Category(repoId, key, null, emptyMap(), emptyMap()) },
            deleteAll = { deleteCategories(repoId) },
            deleteOne = { key -> deleteCategory(repoId, key) },
            insertReplace = { list -> insertCategories(list) },
        )
        // diff and update the releaseChannels
        diffAndUpdateTable(
            jsonObject = jsonObject,
            jsonObjectKey = "releaseChannels",
            itemList = repo.releaseChannels,
            itemFinder = { key, item -> item.id == key },
            newItem = { key -> ReleaseChannel(repoId, key, null, emptyMap(), emptyMap()) },
            deleteAll = { deleteReleaseChannels(repoId) },
            deleteOne = { key -> deleteReleaseChannel(repoId, key) },
            insertReplace = { list -> insertReleaseChannels(list) },
        )
    }

    @Update
    fun updateRepository(repo: CoreRepository): Int

    @Query("UPDATE CoreRepository SET certificate = :certificate WHERE repoId = :repoId")
    fun updateRepository(repoId: Long, certificate: String)

    @Update
    fun updateRepositoryPreferences(preferences: RepositoryPreferences)

    @Query("UPDATE RepositoryPreferences SET enabled = :enabled WHERE repoId = :repoId")
    override fun setRepositoryEnabled(repoId: Long, enabled: Boolean)

    @Query("UPDATE RepositoryPreferences SET userMirrors = :mirrors WHERE repoId = :repoId")
    override fun updateUserMirrors(repoId: Long, mirrors: List<String>)

    @Query("""UPDATE RepositoryPreferences SET username = :username, password = :password
        WHERE repoId = :repoId""")
    override fun updateUsernameAndPassword(repoId: Long, username: String?, password: String?)

    @Query("""UPDATE RepositoryPreferences SET disabledMirrors = :disabledMirrors
        WHERE repoId = :repoId""")
    override fun updateDisabledMirrors(repoId: Long, disabledMirrors: List<String>)

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

    @Query("SELECT * FROM RepositoryPreferences WHERE repoId = :repoId")
    fun getRepositoryPreferences(repoId: Long): RepositoryPreferences?

    @Query("SELECT MAX(weight) FROM RepositoryPreferences")
    fun getMaxRepositoryWeight(): Int

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

    @Query("SELECT COUNT(*) FROM AppMetadata WHERE repoId = :repoId")
    override fun countAppsPerRepository(repoId: Long): Int

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

    @Transaction
    override fun deleteRepository(repoId: Long) {
        deleteCoreRepository(repoId)
        // we don't use cascading delete for preferences,
        // so we can replace index data on full updates
        deleteRepositoryPreferences(repoId)
    }

    @Query("DELETE FROM CoreRepository WHERE repoId = :repoId")
    fun deleteCoreRepository(repoId: Long)

    @Query("DELETE FROM CoreRepository")
    fun deleteAllCoreRepositories()

    @Query("DELETE FROM RepositoryPreferences WHERE repoId = :repoId")
    fun deleteRepositoryPreferences(repoId: Long)

    @Query("DELETE FROM RepositoryPreferences")
    fun deleteAllRepositoryPreferences()

}

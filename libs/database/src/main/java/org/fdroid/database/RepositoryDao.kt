package org.fdroid.database

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import org.fdroid.database.DbDiffUtils.diffAndUpdateListTable
import org.fdroid.database.DbDiffUtils.diffAndUpdateTable
import org.fdroid.index.IndexFormatVersion
import org.fdroid.index.IndexParser.json
import org.fdroid.index.v2.IndexV2Updater
import org.fdroid.index.v2.MirrorV2
import org.fdroid.index.v2.ReflectionDiffer.applyDiff
import org.fdroid.index.v2.RepoV2

public interface RepositoryDao {
    /**
     * Inserts a new [InitialRepository] from a fixture.
     *
     * @return the [Repository.repoId] of the inserted repo.
     */
    public fun insert(initialRepo: InitialRepository): Long

    /**
     * Inserts an empty [Repository] for an initial update.
     *
     * @return the [Repository.repoId] of the inserted repo.
     */
    public fun insertEmptyRepo(
        address: String,
        username: String? = null,
        password: String? = null,
    ): Long

    /**
     * Returns the repository with the given [repoId] or null, if none was found with that ID.
     */
    public fun getRepository(repoId: Long): Repository?

    /**
     * Returns a list of all [Repository]s in the database.
     */
    public fun getRepositories(): List<Repository>

    /**
     * Same as [getRepositories], but does return a [LiveData].
     */
    public fun getLiveRepositories(): LiveData<List<Repository>>

    /**
     * Returns a live data of all categories declared by all [Repository]s.
     */
    public fun getLiveCategories(): LiveData<List<Category>>

    /**
     * Enables or disables the repository with the given [repoId].
     * Data from disabled repositories is ignored in many queries.
     */
    public fun setRepositoryEnabled(repoId: Long, enabled: Boolean)

    /**
     * Updates the user-defined mirrors of the repository with the given [repoId].
     * The existing mirrors get overwritten with the given [mirrors].
     */
    public fun updateUserMirrors(repoId: Long, mirrors: List<String>)

    /**
     * Updates the user name and password (for basic authentication)
     * of the repository with the given [repoId].
     * The existing user name and password get overwritten with the given [username] and [password].
     */
    public fun updateUsernameAndPassword(repoId: Long, username: String?, password: String?)

    /**
     * Updates the disabled mirrors of the repository with the given [repoId].
     * The existing disabled mirrors get overwritten with the given [disabledMirrors].
     */
    public fun updateDisabledMirrors(repoId: Long, disabledMirrors: List<String>)

    /**
     * Removes a [Repository] with the given [repoId] with all associated data from the database.
     */
    public fun deleteRepository(repoId: Long)

    /**
     * Removes all repos and their preferences.
     */
    public fun clearAll()
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
    override fun insert(initialRepo: InitialRepository): Long {
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
        return repoId
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
    fun insertOrReplace(repository: RepoV2, version: Long = 0): Long {
        val repoId = insertOrReplace(repository.toCoreRepository(version = version))
        val currentMaxWeight = getMaxRepositoryWeight()
        val repositoryPreferences = RepositoryPreferences(repoId, currentMaxWeight + 1)
        insert(repositoryPreferences)
        insertRepoTables(repoId, repository)
        return repoId
    }

    @Query("SELECT MAX(weight) FROM ${RepositoryPreferences.TABLE}")
    fun getMaxRepositoryWeight(): Int

    @Transaction
    @Query("SELECT * FROM ${CoreRepository.TABLE} WHERE repoId = :repoId")
    override fun getRepository(repoId: Long): Repository?

    @Transaction
    @Query("SELECT * FROM ${CoreRepository.TABLE}")
    override fun getRepositories(): List<Repository>

    @Transaction
    @Query("SELECT * FROM ${CoreRepository.TABLE}")
    override fun getLiveRepositories(): LiveData<List<Repository>>

    @Query("SELECT * FROM ${RepositoryPreferences.TABLE} WHERE repoId = :repoId")
    fun getRepositoryPreferences(repoId: Long): RepositoryPreferences?

    @RewriteQueriesToDropUnusedColumns
    @Query("""SELECT * FROM ${Category.TABLE}
        JOIN ${RepositoryPreferences.TABLE} AS pref USING (repoId)
        WHERE pref.enabled = 1 GROUP BY id HAVING MAX(pref.weight)""")
    override fun getLiveCategories(): LiveData<List<Category>>

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

    @Update
    fun updateRepository(repo: CoreRepository): Int

    /**
     * Updates the certificate for the [Repository] with the given [repoId].
     * This should be used for V1 index updating where we only get the full cert
     * after reading the entire index file.
     * V2 index should use [update] instead as there the certificate is known
     * before reading full index.
     */
    @Query("UPDATE ${CoreRepository.TABLE} SET certificate = :certificate WHERE repoId = :repoId")
    fun updateRepository(repoId: Long, certificate: String)

    @Update
    fun updateRepositoryPreferences(preferences: RepositoryPreferences)

    /**
     * Used to update an existing repository with a given [jsonObject] JSON diff.
     */
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
            newItem = { key -> AntiFeature(repoId, key, emptyMap(), emptyMap(), emptyMap()) },
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
            newItem = { key -> Category(repoId, key, emptyMap(), emptyMap(), emptyMap()) },
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
            newItem = { key -> ReleaseChannel(repoId, key, emptyMap(), emptyMap(), emptyMap()) },
            deleteAll = { deleteReleaseChannels(repoId) },
            deleteOne = { key -> deleteReleaseChannel(repoId, key) },
            insertReplace = { list -> insertReleaseChannels(list) },
        )
    }

    @Query("UPDATE ${RepositoryPreferences.TABLE} SET enabled = :enabled WHERE repoId = :repoId")
    override fun setRepositoryEnabled(repoId: Long, enabled: Boolean)

    @Query("""UPDATE ${RepositoryPreferences.TABLE} SET userMirrors = :mirrors
        WHERE repoId = :repoId""")
    override fun updateUserMirrors(repoId: Long, mirrors: List<String>)

    @Query("""UPDATE ${RepositoryPreferences.TABLE} SET username = :username, password = :password
        WHERE repoId = :repoId""")
    override fun updateUsernameAndPassword(repoId: Long, username: String?, password: String?)

    @Query("""UPDATE ${RepositoryPreferences.TABLE} SET disabledMirrors = :disabledMirrors
        WHERE repoId = :repoId""")
    override fun updateDisabledMirrors(repoId: Long, disabledMirrors: List<String>)

    @Transaction
    override fun deleteRepository(repoId: Long) {
        deleteCoreRepository(repoId)
        // we don't use cascading delete for preferences,
        // so we can replace index data on full updates
        deleteRepositoryPreferences(repoId)
    }

    @Query("DELETE FROM ${CoreRepository.TABLE} WHERE repoId = :repoId")
    fun deleteCoreRepository(repoId: Long)

    @Query("DELETE FROM ${RepositoryPreferences.TABLE} WHERE repoId = :repoId")
    fun deleteRepositoryPreferences(repoId: Long)

    @Query("DELETE FROM ${CoreRepository.TABLE}")
    fun deleteAllCoreRepositories()

    @Query("DELETE FROM ${RepositoryPreferences.TABLE}")
    fun deleteAllRepositoryPreferences()

    /**
     * Used for diffing.
     */
    @Query("DELETE FROM ${Mirror.TABLE} WHERE repoId = :repoId")
    fun deleteMirrors(repoId: Long)

    /**
     * Used for diffing.
     */
    @Query("DELETE FROM ${AntiFeature.TABLE} WHERE repoId = :repoId")
    fun deleteAntiFeatures(repoId: Long)

    /**
     * Used for diffing.
     */
    @Query("DELETE FROM ${AntiFeature.TABLE} WHERE repoId = :repoId AND id = :id")
    fun deleteAntiFeature(repoId: Long, id: String)

    /**
     * Used for diffing.
     */
    @Query("DELETE FROM ${Category.TABLE} WHERE repoId = :repoId")
    fun deleteCategories(repoId: Long)

    /**
     * Used for diffing.
     */
    @Query("DELETE FROM ${Category.TABLE} WHERE repoId = :repoId AND id = :id")
    fun deleteCategory(repoId: Long, id: String)

    /**
     * Used for diffing.
     */
    @Query("DELETE FROM ${ReleaseChannel.TABLE} WHERE repoId = :repoId")
    fun deleteReleaseChannels(repoId: Long)

    /**
     * Used for diffing.
     */
    @Query("DELETE FROM ${ReleaseChannel.TABLE} WHERE repoId = :repoId AND id = :id")
    fun deleteReleaseChannel(repoId: Long, id: String)

    /**
     * Resets timestamps for *all* repos in the database.
     * This will use a full index instead of diffs
     * when updating the repository via [IndexV2Updater].
     */
    @Query("UPDATE ${CoreRepository.TABLE} SET timestamp = -1")
    fun resetTimestamps()

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

    @VisibleForTesting
    @Query("SELECT COUNT(*) FROM ${Mirror.TABLE}")
    fun countMirrors(): Int

    @VisibleForTesting
    @Query("SELECT COUNT(*) FROM ${AntiFeature.TABLE}")
    fun countAntiFeatures(): Int

    @VisibleForTesting
    @Query("SELECT COUNT(*) FROM ${Category.TABLE}")
    fun countCategories(): Int

    @VisibleForTesting
    @Query("SELECT COUNT(*) FROM ${ReleaseChannel.TABLE}")
    fun countReleaseChannels(): Int

}

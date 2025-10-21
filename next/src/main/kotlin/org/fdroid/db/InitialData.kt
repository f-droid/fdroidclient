package org.fdroid.db

import android.content.Context
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.fdroid.database.FDroidDatabase
import org.fdroid.database.FDroidFixture
import org.fdroid.database.InitialRepository
import org.fdroid.database.RepositoryDao
import org.fdroid.repo.RepoUpdateWorker
import java.io.File

class InitialData(val context: Context) : FDroidFixture {
    override fun prePopulateDb(db: FDroidDatabase) {
        addPreloadedRepositories(db, context.packageName)
        RepoUpdateWorker.updateNow(context)
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun addPreloadedRepositories(db: FDroidDatabase, packageName: String) {
        val defaultRepos = context.assets.open("default_repos.json").use { inputStream ->
            Json.decodeFromStream<List<DefaultRepository>>(inputStream)
        }
        addRepositories(db.getRepositoryDao(), defaultRepos)
        // "system" can be removed when minSdk is 28
        for (root in listOf("/system", "/system_ext", "/product", "/vendor")) {
            val romReposFile = File("$root/etc/$packageName/additional_repos.json")
            if (romReposFile.isFile) {
                val romRepos = romReposFile.inputStream().use { inputStream ->
                    Json.decodeFromStream<List<DefaultRepository>>(inputStream)
                }
                addRepositories(db.getRepositoryDao(), romRepos)
            }
        }
    }

    private fun addRepositories(
        repositoryDao: RepositoryDao,
        repositories: List<DefaultRepository>
    ) {
        repositories.forEach { repository ->
            val initialRepository = InitialRepository(
                name = repository.name,
                address = repository.address,
                mirrors = repository.mirrors,
                description = repository.description,
                certificate = repository.certificate,
                version = 1,
                enabled = repository.enabled,
            )
            repositoryDao.insert(initialRepository)
        }
    }
}

@Serializable
data class DefaultRepository(
    val name: String,
    val address: String,
    val mirrors: List<String> = emptyList(),
    val description: String,
    val certificate: String,
    val enabled: Boolean,
)

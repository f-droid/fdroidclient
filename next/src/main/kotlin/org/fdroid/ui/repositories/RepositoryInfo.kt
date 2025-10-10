package org.fdroid.ui.repositories

interface RepositoryInfo {
    val model: RepositoryModel
    val currentRepositoryId: Long?
    fun onRepositorySelected(repositoryItem: RepositoryItem)
    fun onAddRepo()
    fun onRepositoryMoved(fromIndex: Int, toIndex: Int)
    fun onRepositoriesFinishedMoving(fromRepoId: Long, toRepoId: Long)
}

data class RepositoryModel(
    val repositories: List<RepositoryItem>?,
)

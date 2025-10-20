package org.fdroid.ui.repositories

interface RepositoryInfo {
    val model: RepositoryModel
    val currentRepositoryId: Long?
    fun onOnboardingSeen()
    fun onRepositorySelected(repositoryItem: RepositoryItem)
    fun onRepositoryEnabled(repoId: Long, enabled: Boolean)
    fun onAddRepo()
    fun onRepositoryMoved(fromRepoId: Long, toRepoId: Long)
    fun onRepositoriesFinishedMoving(fromRepoId: Long, toRepoId: Long)
}

data class RepositoryModel(
    val repositories: List<RepositoryItem>?,
    val showOnboarding: Boolean,
)

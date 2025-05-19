package org.fdroid.basic.manager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.fdroid.basic.ui.main.repositories.Repository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RepositoryManager @Inject constructor() {

    private val _repos = MutableStateFlow(
        listOf(
            Repository(
                address = "http://example.org",
                timestamp = System.currentTimeMillis(),
                lastUpdated = null,
                weight = 1,
                enabled = true,
                name = "My first repository",
            ),
            Repository(
                address = "http://example.com",
                timestamp = System.currentTimeMillis(),
                lastUpdated = null,
                weight = 2,
                enabled = true,
                name = "My second repository",
            ),
        )
    )
    val repos = _repos.asStateFlow()

    private val _visibleRepository = MutableStateFlow<Repository?>(null)
    val visibleRepository = _visibleRepository.asStateFlow()

    fun setVisibleRepository(repository: Repository?) {
        _visibleRepository.value = repository
    }

}

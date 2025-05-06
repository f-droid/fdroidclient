package org.fdroid.basic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.cash.molecule.AndroidUiDispatcher
import app.cash.molecule.RecompositionMode.ContextClock
import app.cash.molecule.launchMolecule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.fdroid.basic.ui.main.NUM_ITEMS
import org.fdroid.basic.ui.main.Repository
import org.fdroid.basic.ui.main.Sort
import org.fdroid.basic.ui.main.apps.AppNavigationItem
import org.fdroid.basic.ui.main.apps.FilterModel
import org.fdroid.basic.ui.main.apps.FilterPresenter
import org.fdroid.basic.ui.main.apps.Names
import org.fdroid.basic.ui.main.updates.UpdatableApp

class MainViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        val fakeUpdates = listOf(
            UpdatableApp(
                name = Names.randomName,
                currentVersionName = "1.0.1",
                updateVersionName = "1.1.0",
                size = 123456789,
                whatsNew = "Lots of changes in this version!\nThey are all awesome.\n" +
                    "Only the best changes."
            ),
            UpdatableApp(
                name = Names.randomName,
                currentVersionName = "3.0.1",
                updateVersionName = "3.1.0",
                size = 9876543,
            )
        )
    }

    private val scope = CoroutineScope(viewModelScope.coroutineContext + AndroidUiDispatcher.Main)
    val categories = listOf(
        Pair(app.getString(R.string.category_Internet), R.drawable.category_internet),
        Pair(app.getString(R.string.category_Games), R.drawable.category_games),
        Pair(app.getString(R.string.category_Navigation), R.drawable.category_navigation),
        Pair(app.getString(R.string.category_Multimedia), R.drawable.category_money),
        Pair(app.getString(R.string.category_Security), R.drawable.category_security),
        Pair(app.getString(R.string.category_Reading), R.drawable.category_reading),
        Pair(app.getString(R.string.category_Time), R.drawable.category_theming),
        Pair(app.getString(R.string.category_Money), R.drawable.category_money),
        Pair(app.getString(R.string.category_Theming), R.drawable.category_theming),
        Pair(app.getString(R.string.category_Connectivity), R.drawable.category_connectivity),
        Pair(app.getString(R.string.category_Phone_SMS), R.drawable.category_system),
        Pair(
            app.getString(R.string.category_Science_Education),
            R.drawable.category_science_education
        ),
        Pair(app.getString(R.string.category_Sports_Health), R.drawable.category_security),
        Pair(app.getString(R.string.category_System), R.drawable.category_system),
        Pair(app.getString(R.string.category_Writing), R.drawable.category_writing),
    )

    val initialApps = buildList {
        repeat(NUM_ITEMS) { i ->
            val category = categories.getOrElse(i) { categories.random() }.first
            val navItem = AppNavigationItem(
                packageName = "$i",
                name = Names.randomName,
                summary = "Summary of the app • $category",
                isNew = i > NUM_ITEMS - 4,
            )
            add(navItem)
        }
    }

    private val _onlyInstalledApps = MutableStateFlow(false)
    val onlyInstalledApps = _onlyInstalledApps.asStateFlow<Boolean>()
    private val _sortBy = MutableStateFlow<Sort>(Sort.LATEST)
    val sortBy = _sortBy.asStateFlow<Sort>()
    private val _addedCategories = MutableStateFlow<List<String>>(emptyList())
    val addedCategories = _addedCategories.asStateFlow<List<String>>()

    val filterModel: StateFlow<FilterModel> = scope.launchMolecule(mode = ContextClock) {
        FilterPresenter(
            appsFlow = flow { emit(initialApps) },
            onlyInstalledAppsFlow = onlyInstalledApps,
            sortByFlow = sortBy,
            allCategories = categories.map { it.first },
            addedCategoriesFlow = addedCategories,
        )
    }
    private val _updates = MutableStateFlow(fakeUpdates)
    val updates = _updates.asStateFlow()
    val numUpdates = _updates.map { it.size }
    private val _repos = MutableStateFlow(
        listOf(
            Repository(
                address = "http://example.org",
                timestamp = System.currentTimeMillis(),
                lastUpdated = null,
                weight = 2,
                enabled = true,
                name = "My first repository",
            )
        )
    )
    val repos = _repos.asStateFlow()

    init {
        viewModelScope.launch {
            delay(5000)
            _updates.update {
                it.toMutableList().apply {
                    val app = UpdatableApp(
                        name = Names.randomName,
                        currentVersionName = "4.0.1",
                        updateVersionName = "4.3.0",
                        size = 4561237,
                        whatsNew = "This new version is super fast and aimed at fixing some bugs and enhancing your experience even more. So take the chance to update your app and always enjoy the best of Inter. In addition to the exciting new features in the latest version, we regularly release new versions to improve what you are already using on our app. To keep making your life simpler, keep your app up to date and take advantage of everything we prepare for you. "
                    )
                    add(app)
                }
            }
        }
    }

    fun sortBy(sort: Sort) {
        _sortBy.update { sort }
    }

    fun addCategory(category: String) {
        _addedCategories.update {
            addedCategories.value.toMutableList().apply {
                add(category)
            }
        }
    }

    fun removeCategory(category: String) {
        _addedCategories.update {
            addedCategories.value.toMutableList().apply {
                remove(category)
            }
        }
    }

    fun showOnlyInstalledApps(onlyInstalled: Boolean) {
        _onlyInstalledApps.update { onlyInstalled }
    }

}

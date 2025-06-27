package org.fdroid.basic

import android.app.Application
import androidx.compose.material.icons.Icons.AutoMirrored
import androidx.compose.material.icons.Icons.Default
import androidx.compose.material.icons.automirrored.filled.ChromeReaderMode
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Airplay
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalPlay
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.MusicVideo
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.PermPhoneMsg
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VideoChat
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.cash.molecule.AndroidUiDispatcher
import app.cash.molecule.RecompositionMode.ContextClock
import app.cash.molecule.launchMolecule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import org.fdroid.basic.manager.AppDetailsManager
import org.fdroid.basic.manager.MyAppsManager
import org.fdroid.basic.manager.RepositoryManager
import org.fdroid.basic.ui.Icons
import org.fdroid.basic.ui.Names
import org.fdroid.basic.ui.main.apps.MinimalApp
import org.fdroid.basic.ui.main.discover.AppNavigationItem
import org.fdroid.basic.ui.main.discover.FilterModel
import org.fdroid.basic.ui.main.discover.FilterPresenter
import org.fdroid.basic.ui.main.discover.NUM_ITEMS
import org.fdroid.basic.ui.main.discover.Sort
import org.fdroid.basic.ui.main.lists.AppList
import javax.inject.Inject

data class Category(val name: String, val imageVector: ImageVector)

@HiltViewModel
class MainViewModel @Inject constructor(
    app: Application,
    savedStateHandle: SavedStateHandle,
    val myAppsManager: MyAppsManager,
    private val appDetailsManager: AppDetailsManager,
    val repositoryManager: RepositoryManager,
) : AndroidViewModel(app) {

    private val scope = CoroutineScope(viewModelScope.coroutineContext + AndroidUiDispatcher.Main)

    companion object {
        val categories = listOf(
            Category("App Store & Updater", Default.Storefront),
            Category("Browser", Default.OpenInBrowser),
            Category("Calendar & Agenda", Default.CalendarMonth),
            Category("Cloud Storage & File Sync", Default.Cloud),
            Category("Connectivity", Default.SignalCellularAlt),
            Category("Development", Default.DeveloperMode),
            Category("DNS & Hosts", Default.Dns),
            Category("Email", Default.AlternateEmail),
            Category("File Encryption & Vault", Default.EnhancedEncryption),
            Category("File Transfer", Default.UploadFile),
            Category("Games", Default.Games),
            Category("Graphics", Default.Image),
            Category("Icon Pack", Default.Collections),
            Category("Internet", Default.Language),
            Category("Keyboard & IME", Default.Keyboard),
            Category("Local Media Player", Default.LocalPlay),
            Category("Messaging", AutoMirrored.Default.Message),
            Category("Money", Default.Money),
            Category("Multimedia", Default.MusicVideo),
            Category("Navigation", Default.Navigation),
            Category("News", Default.Newspaper),
            Category("Online Media Player", Default.Airplay),
            Category("Pass Wallet", Default.AccountBalanceWallet),
            Category("Password & 2FA", Default.Password),
            Category("Phone & SMS", Default.PermPhoneMsg),
            Category("Podcast", Default.Podcasts),
            Category("Reading", AutoMirrored.Default.ChromeReaderMode),
            Category("Science & Education", Default.Science),
            Category("Security", Default.Security),
            Category("Social Network", Default.Groups),
            Category("Sports & Health", Default.HealthAndSafety),
            Category("System", Default.Settings),
            Category("Theming", Default.Style),
            Category("Time", Default.AccessTime),
            Category("Translation & Dictionary", Default.Translate),
            Category("Voice & Video Chat", Default.VideoChat),
            Category("VPN & Proxy", Default.VpnLock),
            Category("Wallet", Default.Wallet),
            Category("Wallpaper", Default.Wallpaper),
            Category("Weather", Default.WbSunny),
            Category("Writing", Default.EditNote),
        )
    }

    val initialApps = buildList {
        repeat(NUM_ITEMS) { i ->
            val category = categories.getOrElse(i) { categories.random() }.name
            val navItem = AppNavigationItem(
                packageName = "$i",
                icon = when (i) {
                    0 -> "https://f-droid.org/repo/icons/org.wikimedia.commons.wikimedia.1.png"
                    1 -> "https://f-droid.org/repo/org.videolan.vlc/en-US/icon_yAfSvPRJukZzMMfUzvbYqwaD1XmHXNtiPBtuPVHW-6s=.png"
                    2 -> "https://f-droid.org/repo/net.thunderbird.android/en-US/icon_llBuXRxsJFITCCuDze-ENOPa1J_HFZLudN5K3gU-xiU=.png"
                    3 -> "https://f-droid.org/repo/org.schabi.newpipe/en-US/icon_OHy4y1W-fJCNhHHOBCM9V_cxZNJJgbcNkB-x7UDTY9Q=.png"
                    else -> Icons.randomIcon
                },
                name = Names.randomName,
                summary = "Summary of the app • $category",
                isNew = i > NUM_ITEMS - 4,
            )
            add(navItem)
        }
    }

    private val _currentList = MutableStateFlow<AppList>(AppList.New)
    val currentList = _currentList.asStateFlow()
    private val _showFilters = savedStateHandle.getMutableStateFlow("showFilters", true)
    val showFilters = _showFilters.asStateFlow()
    private val _sortBy = MutableStateFlow<Sort>(Sort.LATEST)
    val sortBy = _sortBy.asStateFlow<Sort>()
    private val _addedCategories = MutableStateFlow<List<String>>(emptyList())
    val addedCategories = _addedCategories.asStateFlow<List<String>>()

    val filterModel: StateFlow<FilterModel> = scope.launchMolecule(mode = ContextClock) {
        FilterPresenter(
            areFiltersShownFlow = showFilters,
            appsFlow = flow { emit(initialApps) },
            sortByFlow = sortBy,
            allCategories = categories.map { it.name },
            addedCategoriesFlow = addedCategories,
        )
    }
    val updates = myAppsManager.updates
    val installed = myAppsManager.installed
    val numUpdates = myAppsManager.numUpdates
    val appDetails = appDetailsManager.appDetails

    fun setAppList(appList: AppList) {
        _currentList.value = appList
    }

    fun setAppDetails(app: MinimalApp) {
        val newApp = filterModel.value.apps.find { it.packageName == app.packageName }
            ?: (updates.value.find { it.packageName == app.packageName }
                ?: installed.value.find { it.packageName == app.packageName })?.let {
                AppNavigationItem(
                    packageName = it.packageName,
                    name = it.name ?: "Unknown app",
                    icon = it.icon,
                    summary = "Summary",
                    isNew = false
                )
            }
        appDetailsManager.setAppDetails(newApp)
    }

    fun toggleListFilterVisibility() {
        _showFilters.update { !it }
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
}

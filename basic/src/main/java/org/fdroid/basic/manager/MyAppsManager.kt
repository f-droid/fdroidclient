package org.fdroid.basic.manager

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.fdroid.basic.ui.main.apps.InstalledApp
import org.fdroid.basic.ui.main.apps.UpdatableApp
import org.fdroid.basic.ui.main.discover.Names
import org.fdroid.basic.ui.main.discover.Sort
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MyAppsManager @Inject constructor() {

    private val _updates = MutableStateFlow<List<UpdatableApp>>(emptyList())
    val updates = _updates.asStateFlow()
    private val _installed = MutableStateFlow<List<InstalledApp>>(installedApps)
    val installed = _installed.asStateFlow()
    val numUpdates = _updates.map { it.size }
    private val _sortBy = MutableStateFlow<Sort>(Sort.NAME)
    val sortBy = _sortBy.asStateFlow()

    companion object {
        val installedApps = listOf(
            InstalledApp(
                packageName = "1000",
                name = Names.randomName,
                versionName = "1.0.1",
            ),
            InstalledApp(
                packageName = "1001",
                name = Names.randomName,
                versionName = "0.1",
            ),
            InstalledApp(
                packageName = "1002",
                name = Names.randomName,
                versionName = "3.0.1",
            ),
            InstalledApp(
                packageName = "1003",
                name = Names.randomName,
                versionName = "0.2.1",
            ),
            InstalledApp(
                packageName = "1004",
                name = Names.randomName,
                versionName = "0.0.1",
            ),
            InstalledApp(
                packageName = "1005",
                name = Names.randomName,
                versionName = "1.1.1",
            ),
            InstalledApp(
                packageName = "1006",
                name = Names.randomName,
                versionName = "2.0.1",
            ),
        ).sortedBy { it.name.lowercase(Locale.getDefault()) }
    }

    init {
        GlobalScope.launch {
            delay(5_000)
            _updates.update {
                listOf(
                    UpdatableApp(
                        packageName = "2000",
                        name = Names.randomName,
                        currentVersionName = "1.0.1",
                        updateVersionName = "1.1.0",
                        size = 123456789,
                        whatsNew = "Lots of changes in this version!\nThey are all awesome.\n" +
                            "Only the best changes."
                    ),
                    UpdatableApp(
                        packageName = "2001",
                        name = Names.randomName,
                        currentVersionName = "3.0.1",
                        updateVersionName = "3.1.0",
                        size = 9876543,
                    ),
                    UpdatableApp(
                        packageName = "2002",
                        name = Names.randomName,
                        currentVersionName = "4.0.1",
                        updateVersionName = "4.3.0",
                        size = 4561237,
                        whatsNew = "This new version is super fast and aimed at fixing some bugs and enhancing your experience even more. So take the chance to update your app and always enjoy the best of Inter. In addition to the exciting new features in the latest version, we regularly release new versions to improve what you are already using on our app. To keep making your life simpler, keep your app up to date and take advantage of everything we prepare for you. "
                    ),
                    UpdatableApp(
                        packageName = "2003",
                        name = Names.randomName,
                        currentVersionName = "3.0.1",
                        updateVersionName = "3.1.0",
                        size = 9876543,
                    ),
                ).sortedBy { it.name.lowercase(Locale.getDefault()) }
            }
        }
    }

    fun sortBy(sort: Sort) {
        when (sort) {
            Sort.NAME -> {
                _updates.update {
                    it.sortedBy { it.name.lowercase(Locale.getDefault()) }
                }
                _installed.update {
                    it.sortedBy { it.name.lowercase(Locale.getDefault()) }
                }
            }
            Sort.LATEST -> {
                _updates.update {
                    it.sortedBy { it.packageName }
                }
                _installed.update {
                    it.sortedBy { it.packageName }
                }
            }
        }
        _sortBy.value = sort
    }

}

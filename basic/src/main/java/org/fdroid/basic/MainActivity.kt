package org.fdroid.basic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import org.fdroid.basic.ui.main.Main
import org.fdroid.basic.ui.main.Sort
import org.fdroid.basic.ui.main.apps.FilterInfo

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val numUpdates = viewModel.numUpdates.collectAsState(0).value
            val updates = viewModel.updates.collectAsState().value
            val filterInfo = object : FilterInfo {
                override val model = viewModel.filterModel.collectAsState().value
                override fun sortBy(sort: Sort) = viewModel.sortBy(sort)
                override fun addCategory(category: String) = viewModel.addCategory(category)
                override fun removeCategory(category: String) = viewModel.removeCategory(category)
                override fun showOnlyInstalledApps(onlyInstalled: Boolean) =
                    viewModel.showOnlyInstalledApps(onlyInstalled)
            }
            Main(numUpdates, updates, filterInfo)
        }
    }
}

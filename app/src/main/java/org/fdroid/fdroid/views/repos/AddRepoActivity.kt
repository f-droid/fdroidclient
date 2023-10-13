package org.fdroid.fdroid.views.repos

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import info.guardianproject.netcipher.NetCipher
import kotlinx.coroutines.launch
import org.fdroid.fdroid.FDroidApp
import org.fdroid.fdroid.Preferences
import org.fdroid.fdroid.UpdateService
import org.fdroid.fdroid.compose.ComposeUtils.FDroidContent
import org.fdroid.fdroid.nearby.SwapService
import org.fdroid.fdroid.views.apps.AppListActivity
import org.fdroid.fdroid.views.apps.AppListActivity.EXTRA_REPO_ID
import org.fdroid.repo.AddRepoError
import org.fdroid.repo.Added

class AddRepoActivity : ComponentActivity() {

    private val repoManager = FDroidApp.getRepoManager(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            repeatOnLifecycle(STARTED) {
                repoManager.addRepoState.collect { state ->
                    if (state is Added) {
                        // update newly added repo
                        UpdateService.updateRepoNow(applicationContext, state.repo.address)
                        // show repo list and close this activity
                        val i = Intent(this@AddRepoActivity, AppListActivity::class.java).apply {
                            putExtra(EXTRA_REPO_ID, state.repo.repoId)
                        }
                        startActivity(i)
                        finish()
                    }
                }
            }
        }
        setContent {
            FDroidContent {
                val state = repoManager.addRepoState.collectAsState().value
                BackHandler(state is AddRepoError) {
                    // reset state when going back on error screen
                    repoManager.abortAddingRepository()
                }
                AddRepoIntroScreen(
                    state = state,
                    onFetchRepo = this::onFetchRepo,
                    onAddRepo = { repoManager.addFetchedRepository() },
                    onBackClicked = { onBackPressedDispatcher.onBackPressed() },
                )
            }
        }
        addOnNewIntentListener { intent ->
            intent.dataString?.let { uri ->
                onFetchRepo(uri)
            }
        }
        intent?.let {
            onNewIntent(it)
            it.setData(null) // avoid this intent from getting re-processed
        }
    }

    override fun onResume() {
        super.onResume()
        FDroidApp.checkStartTor(this, Preferences.get())
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) repoManager.abortAddingRepository()
    }

    private fun onFetchRepo(uriStr: String) {
        val uri = Uri.parse(uriStr)
        if (repoManager.isSwapUri(uri)) {
            val i = Intent(this, SwapService::class.java).apply {
                data = uri
            }
            ContextCompat.startForegroundService(this, i)
        } else {
            repoManager.abortAddingRepository()
            repoManager.fetchRepositoryPreview(uriStr, proxy = NetCipher.getProxy())
        }
    }
}

package org.fdroid.fdroid.views.repos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.MutableCreationExtras
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.fdroid.database.Repository
import org.fdroid.download.Mirror
import org.fdroid.fdroid.FDroidApp
import org.fdroid.fdroid.R
import org.fdroid.fdroid.ui.theme.FDroidContent
import org.fdroid.fdroid.views.apps.AppListActivity

class RepoDetailsActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "RepoDetailsActivity"
        const val ARG_REPO_ID = "repo_id"

        fun launch(context: Context, repoId: Long) {
            val intent = Intent(context, RepoDetailsActivity::class.java).apply {
                putExtra(ARG_REPO_ID, repoId)
            }
            context.startActivity(intent)
        }
    }

    private lateinit var viewModel: RepoDetailsViewModel

    // Only call this once in onCreate()
    private fun initViewModel(repo: Repository) {
        val factory = RepoDetailsViewModel.Factory
        val extras = MutableCreationExtras().apply {
            set(RepoDetailsViewModel.APP_KEY, application)
            set(RepoDetailsViewModel.Companion.REPO_KEY, repo)
        }
        viewModel =
            ViewModelProvider.create(this, factory, extras)[RepoDetailsViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (application as FDroidApp).setSecureWindow(this)
        (application as FDroidApp).applyPureBlackBackgroundInDarkTheme(this)

        val repoId = intent.getLongExtra(ARG_REPO_ID, 0)
        val repo = FDroidApp.getRepoManager(this).getRepository(repoId)
        if (repo == null) {
            // repo must have been deleted just now (maybe slow UI?)
            finish()
            return
        }

        initViewModel(repo)

        setContent {
            val repo by viewModel.repoFlow.collectAsState(repo)
            val archiveState by viewModel.archiveStateFlow.collectAsState(ArchiveState.UNKNOWN)
            val numberOfApps by viewModel.numberAppsFlow.collectAsState(0)

            val r = repo
            if (r == null) {
                finish()
                return@setContent
            }

            FDroidContent {
                RepoDetailsScreen(
                    repo = r,
                    archiveState = archiveState,
                    numberOfApps = numberOfApps,
                    // app bar
                    onBackClicked = { onBackPressedDispatcher.onBackPressed() },
                    onShareClicked = this::onShareClicked,
                    onShowQrCodeClicked = this::onShowQrCodeClicked,
                    onDeleteClicked = this::onDeleteClicked,
                    onInfoClicked = this::onInfoClicked,
                    // other buttons
                    onShowAppsClicked = this::onShowAppsClicked,
                    onToggleArchiveClicked = { enabled ->
                        viewModel.setArchiveRepoEnabled(enabled)
                    },
                    onEditCredentialsClicked = this::onEditCredentialsClicked,
                    // mirrors
                    setMirrorEnabled = { m, enabled -> viewModel.setMirrorEnabled(m, enabled) },
                    onShareMirror = this::onShareMirror,
                    onDeleteMirror = this::onDeleteMirror,
                )
            }
        }
    }

    private fun onShareClicked() {
        val repo = viewModel.repoFlow.value ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, repo.shareUri)
        }
        startActivity(
            Intent.createChooser(intent, getResources().getString(R.string.share_repository))
        )
    }

    private fun onShowQrCodeClicked() {
        viewModel.generateQrCode(this)

        val imageView = ImageView(this)

        lifecycleScope.launch(Dispatchers.Main) {
            val bitmap = viewModel.generateQrCode(this@RepoDetailsActivity)
            imageView.setImageBitmap(bitmap)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.share_repository)
            .setView(imageView)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun onDeleteClicked() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.repo_confirm_delete_title)
            .setMessage(R.string.repo_confirm_delete_body)
            .setPositiveButton(R.string.delete) { dialog, _ ->
                viewModel.deleteRepository()
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun onInfoClicked() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.repo_details)
            .setMessage(R.string.repo_details_info_text)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun onShowAppsClicked() {
        val repo = viewModel.repoFlow.value ?: return
        if (!repo.enabled) {
            return
        }
        val intent = Intent(this, AppListActivity::class.java).apply {
            putExtra(AppListActivity.EXTRA_REPO_ID, repo.repoId)
        }
        startActivity(intent)
    }

    private fun onEditCredentialsClicked() {
        val repo = viewModel.repoFlow.value ?: return

        val view = layoutInflater.inflate(R.layout.login, null, false)
        val usernameInput = view.findViewById<TextInputLayout>(R.id.edit_name)
        val passwordInput = view.findViewById<TextInputLayout>(R.id.edit_password)

        usernameInput.editText?.setText(repo.username ?: "")
        passwordInput.requestFocus()

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.repo_basic_auth_title)
            .setView(view)
            .setPositiveButton(R.string.ok) { dialog, _ ->
                val username = usernameInput.editText?.text.toString()
                val password = passwordInput.editText?.text.toString()

                if (username.isNotBlank()) {
                    viewModel.updateUsernameAndPassword(username, password)
                } else {
                    Toast.makeText(this, R.string.repo_error_empty_username, Toast.LENGTH_LONG)
                        .show()
                }
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun onShareMirror(mirror: Mirror) {
        val repo = viewModel.repoFlow.value ?: return
        val uri = mirror.getFDroidLinkUrl(repo.fingerprint)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, uri)
        }
        startActivity(
            Intent.createChooser(intent, getResources().getString(R.string.share_mirror))
        )
    }

    private fun onDeleteMirror(mirror: Mirror) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.repo_confirm_delete_mirror_title)
            .setMessage(R.string.repo_confirm_delete_mirror_body)
            .setPositiveButton(R.string.delete) { dialog, _ ->
                viewModel.deleteUserMirror(mirror)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}

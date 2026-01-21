package org.fdroid.fdroid

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import io.mockk.verifyOrder
import org.fdroid.database.AppManifest
import org.fdroid.database.AppVersion
import org.fdroid.database.DbUpdateChecker
import org.fdroid.database.Repository
import org.fdroid.database.UpdatableApp
import org.fdroid.download.Downloader
import org.fdroid.fdroid.data.App
import org.fdroid.fdroid.installer.InstallManagerService
import org.fdroid.fdroid.installer.Installer
import org.fdroid.fdroid.installer.InstallerFactory
import org.fdroid.index.RepoManager
import org.fdroid.index.v2.FileV1
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException

class AppUpdateManagerTest {

    private val context: Context = mockk()
    private val repoManager: RepoManager = mockk()
    private val updateChecker: DbUpdateChecker = mockk()
    private val preferences: Preferences = mockk()
    private val downloaderFactory: org.fdroid.download.DownloaderFactory = mockk()
    private val statusManager: AppUpdateStatusManager = mockk()

    private val appUpdateManager =
        AppUpdateManager(context, repoManager, updateChecker, downloaderFactory, statusManager)

    private val downloader: Downloader = mockk()
    private val installer: Installer = mockk()
    private val packageManager: PackageManager = mockk()
    private val installManagerService: InstallManagerService = mockk(relaxed = true)
    private val repoUri: Uri = mockk(relaxed = true)

    private val app1 = App().apply {
        packageName = "org.example.1"
        name = "One"
        repoId = 42
    }
    private val app2 = App().apply {
        packageName = "org.example.2"
        name = "Two"
        repoId = 42
    }
    private val repo: Repository = mockk(relaxed = true) {
        every { repoId } returns app1.repoId
        every { address } returns "https://example.org/repo"
    }
    private val file1 = FileV1("one", "one")
    private val file2 = FileV1("two", "one")
    private val version1: AppVersion = mockk<AppVersion>(relaxed = true) {
        every { repoId } returns app1.repoId
        every { packageName } returns app1.packageName
        every { manifest } returns AppManifest("1", 1)
        every { file } returns file1
    }
    private val version2: AppVersion = mockk<AppVersion>(relaxed = true) {
        every { repoId } returns app2.repoId
        every { packageName } returns app2.packageName
        every { manifest } returns AppManifest("2", 2)
        every { file } returns file2
    }

    // need to mock apps as their constructor is internal
    private val updatableApp1: UpdatableApp = mockk(relaxed = true) {
        every { packageName } returns app1.packageName
        every { name } returns app1.name
        every { summary } returns app1.summary
        every { repoId } returns app1.repoId
        every { update } returns version1
        every { update.file } returns file1
        every { update.manifest } returns AppManifest("1", 1)
        every { update.repoId } returns app1.repoId
        every { update.packageName } returns app1.packageName
        every { installedVersionCode } returns app1.installedVersionCode
    }
    private val updatableApp2: UpdatableApp = mockk(relaxed = true) {
        every { packageName } returns app2.packageName
        every { name } returns app2.name
        every { summary } returns app2.summary
        every { repoId } returns app2.repoId
        every { update } returns version2
        every { update.repoId } returns app2.repoId
        every { update.packageName } returns app2.packageName
        every { update.file } returns file2
        every { update.manifest } returns AppManifest("2", 2)
        every { installedVersionCode } returns app2.installedVersionCode
    }

    init {
        mockkStatic(Preferences::get)
        every { Preferences.get() } returns preferences

        mockkStatic(InstallManagerService::getInstance)
        every { InstallManagerService.getInstance(any()) } returns installManagerService

        mockkStatic(Uri::parse)
        every { Uri.parse(any()) } returns repoUri

        mockkStatic(InstallerFactory::create)
        every { InstallerFactory.create(any(), any(), any()) } returns installer

        every { context.packageManager } returns packageManager
    }

    @Test
    fun testNoUpdates() {
        val updates = emptyList<UpdatableApp>()

        every { preferences.backendReleaseChannels } returns null
        every {
            updateChecker.getUpdatableApps(
                releaseChannels = null,
                onlyFromPreferredRepo = true,
                includeKnownVulnerabilities = false,
            )
        } returns updates
        every { statusManager.addUpdatableApps(updates, false) } just Runs

        assertTrue(appUpdateManager.updateApps())
    }

    @Test
    fun testSomeUpdates() {
        val updates = listOf(updatableApp1, updatableApp2)

        every { preferences.backendReleaseChannels } returns null
        every {
            updateChecker.getUpdatableApps(
                releaseChannels = null,
                onlyFromPreferredRepo = true,
                includeKnownVulnerabilities = false,
            )
        } returns updates
        every { context.packageName } returns null
        every { repoManager.getRepository(app1.repoId) } returns repo
        every { repoManager.getRepository(app2.repoId) } returns repo
        every { statusManager.addUpdatableApps(updates, false) } just Runs
        every { statusManager.addApk(any(), any(), any(), any()) } just Runs
        every {
            packageManager.getPackageInfo(any<String>(), any<Int>())
        } returns getPackageInfo(0)
        every { context.cacheDir } returns File("/tmp/fdroid-app-update-test")
        every { downloaderFactory.create(repo, any(), file1, any()) } returns downloader
        every { downloaderFactory.create(repo, any(), file2, any()) } returns downloader
        every { downloader.setListener(any()) } just Runs
        every { downloader.download() } just Runs
        every { installer.installPackage(any(), any()) } just Runs

        assertTrue(appUpdateManager.updateApps())

        verify(exactly = 2) {
            installManagerService.onDownloadComplete(any())
            installer.installPackage(any(), any())
        }
    }

    @Test
    fun testVersionAlreadyInstalled() {
        val updates = listOf(updatableApp1, updatableApp2)

        every { preferences.backendReleaseChannels } returns null
        every {
            updateChecker.getUpdatableApps(
                releaseChannels = null,
                onlyFromPreferredRepo = true,
                includeKnownVulnerabilities = false,
            )
        } returns updates
        every { context.packageName } returns null
        every { repoManager.getRepository(app1.repoId) } returns repo
        every { repoManager.getRepository(app2.repoId) } returns repo
        every { statusManager.addUpdatableApps(updates, false) } just Runs
        every { statusManager.addApk(any(), any(), any(), any()) } just Runs
        every {
            packageManager.getPackageInfo(app1.packageName, any<Int>())
        } returns getPackageInfo(1)
        every {
            packageManager.getPackageInfo(app2.packageName, any<Int>())
        } returns getPackageInfo(2)

        assertTrue(appUpdateManager.updateApps())

        verify(exactly = 0) {
            installManagerService.onDownloadComplete(any())
            installer.installPackage(any(), any())
        }
    }

    @Test
    fun testOurOwnAppIsUpdatedLast() {
        val installer1: Installer = mockk()
        val installer2: Installer = mockk()
        val updates = listOf(updatableApp1, updatableApp2)
        val updatesSorted = listOf(updatableApp2, updatableApp1)

        every { preferences.backendReleaseChannels } returns null
        every {
            updateChecker.getUpdatableApps(
                releaseChannels = null,
                onlyFromPreferredRepo = true,
                includeKnownVulnerabilities = false,
            )
        } returns updates
        every { context.packageName } returns app1.packageName // we are app1
        every { repoManager.getRepository(app1.repoId) } returns repo
        every { repoManager.getRepository(app2.repoId) } returns repo
        every { statusManager.addUpdatableApps(updatesSorted, false) } just Runs
        every { statusManager.addApk(any(), any(), any(), any()) } just Runs
        every {
            packageManager.getPackageInfo(any<String>(), any<Int>())
        } returns getPackageInfo(0)
        every { context.cacheDir } returns File("/tmp/fdroid-app-update-test")
        every { downloaderFactory.create(repo, any(), file1, any()) } returns downloader
        every { downloaderFactory.create(repo, any(), file2, any()) } returns downloader
        every { downloader.setListener(any()) } just Runs
        every { downloader.download() } just Runs
        every {
            InstallerFactory.create(any(), match { it.packageName == app1.packageName }, any())
        } returns installer1
        every {
            InstallerFactory.create(any(), match { it.packageName == app2.packageName }, any())
        } returns installer2
        every { installer1.installPackage(any(), any()) } just Runs
        every { installer2.installPackage(any(), any()) } just Runs

        assertTrue(appUpdateManager.updateApps())

        verifyOrder {
            // app1 gets installed last, because it is us and updating us kills us
            installer2.installPackage(any(), any())
            installer1.installPackage(any(), any())
        }
    }

    @Test
    fun testFailedDownloadSkipsUpdate() {
        val updates = listOf(updatableApp1)

        every { preferences.backendReleaseChannels } returns null
        every {
            updateChecker.getUpdatableApps(
                releaseChannels = null,
                onlyFromPreferredRepo = true,
                includeKnownVulnerabilities = false,
            )
        } returns updates
        every { context.packageName } returns null
        every { repoManager.getRepository(app1.repoId) } returns repo
        every { statusManager.addUpdatableApps(updates, false) } just Runs
        every { statusManager.addApk(any(), any(), any(), any()) } just Runs
        every {
            packageManager.getPackageInfo(any<String>(), any<Int>())
        } returns getPackageInfo(0)
        every { context.cacheDir } returns File("/tmp/fdroid-app-update-test")
        every { downloaderFactory.create(repo, any(), file1, any()) } returns downloader
        every { downloader.setListener(any()) } just Runs
        every { downloader.download() } throws IOException("foo bar")

        assertFalse(appUpdateManager.updateApps())

        verify(exactly = 0) {
            installer.installPackage(any(), any())
        }
        verify {
            installManagerService.onDownloadFailed(any(), any())
        }
    }

    private fun getPackageInfo(versionCode: Int) = PackageInfo().apply {
        @Suppress("DEPRECATION") // longVersionCode doesn't work
        this.versionCode = versionCode
    }

}

package org.fdroid.fdroid.work

import android.os.Build.VERSION.SDK_INT
import android.text.format.DateUtils
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.work.Configuration
import androidx.work.WorkInfo.State.ENQUEUED
import androidx.work.WorkInfo.State.FAILED
import androidx.work.WorkInfo.State.SUCCEEDED
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.fdroid.fdroid.AppUpdateManager
import org.fdroid.fdroid.FDroidApp
import org.fdroid.fdroid.Preferences
import org.fdroid.fdroid.Preferences.OVER_NETWORK_ALWAYS
import org.fdroid.fdroid.net.ConnectivityMonitorService
import org.fdroid.fdroid.net.ConnectivityMonitorService.FLAG_NET_METERED
import org.fdroid.fdroid.net.ConnectivityMonitorService.FLAG_NET_NO_LIMIT
import org.fdroid.fdroid.net.ConnectivityMonitorService.FLAG_NET_UNAVAILABLE
import org.fdroid.fdroid.work.AppUpdateWorker.Companion.UNIQUE_WORK_NAME_APP_UPDATE
import org.fdroid.fdroid.work.AppUpdateWorker.Companion.UNIQUE_WORK_NAME_AUTO_APP_UPDATE
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.runner.RunWith
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

@RunWith(AndroidJUnit4::class)
class AppUpdateWorkerTest {

    private val context get() = getInstrumentation().targetContext
    private val workManager get() = WorkManager.getInstance(context)
    private val preferences: Preferences by lazy { mockk() }
    private val updateManager: AppUpdateManager by lazy { mockk() }

    @Before
    fun setup() {
        // MockKAgentException: Mocking static is supported starting from Android P
        assumeTrue(SDK_INT >= 28)

        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)

        mockkStatic(FDroidApp::getAppUpdateManager)
        every { FDroidApp.getAppUpdateManager(any()) } returns updateManager
        mockkStatic(Preferences::get)
        every { Preferences.get() } returns preferences
        every { preferences.isLocalRepoHttpsEnabled } returns false
        every { preferences.isOnDemandDownloadAllowed } returns true
        every { preferences.mirrorErrorData } returns emptyMap<String, Int>()
    }

    @Test
    @Throws(Exception::class)
    fun testHappyPath() {
        FDroidApp.networkState = FLAG_NET_NO_LIMIT
        every { updateManager.updateApps() } just Runs

        AppUpdateWorker.updateAppsNow(context)

        verify { updateManager.updateApps() }

        val workInfo = workManager.getWorkInfosForUniqueWork(UNIQUE_WORK_NAME_APP_UPDATE).get()

        assertEquals(1, workInfo.size)
        assertEquals(SUCCEEDED, workInfo[0].state)
    }

    @Test
    @Throws(Exception::class)
    fun testException() {
        every { updateManager.updateApps() } throws IOException("foo bar")

        AppUpdateWorker.updateAppsNow(context)

        val workInfo = workManager.getWorkInfosForUniqueWork(UNIQUE_WORK_NAME_APP_UPDATE).get()

        assertEquals(1, workInfo.size)
        assertEquals(FAILED, workInfo[0].state)
    }

    @Test
    @Throws(Exception::class)
    fun testNotRunningWhenNoNetwork() {
        mockkStatic(ConnectivityMonitorService::getNetworkState)
        every { ConnectivityMonitorService.getNetworkState(any()) } returns FLAG_NET_UNAVAILABLE
        FDroidApp.networkState = FLAG_NET_UNAVAILABLE

        try {
            AppUpdateWorker.updateAppsNow(context)
            fail()
        } catch (e: NullPointerException) {
            // can't send toast from these tests
            assertTrue(e.message?.contains("toast") == true)
        }

        val workInfo = workManager.getWorkInfosForUniqueWork(UNIQUE_WORK_NAME_APP_UPDATE).get()
        assertEquals(0, workInfo.size)
    }

    @Test
    @Throws(Exception::class)
    fun testNotRunningOnMeteredNetwork() {
        FDroidApp.networkState = FLAG_NET_METERED
        every { preferences.isOnDemandDownloadAllowed } returns false

        try {
            AppUpdateWorker.updateAppsNow(context)
            fail()
        } catch (e: NullPointerException) {
            // can't send toast from these tests
            assertTrue(e.message?.contains("toast") == true)
        }

        val workInfo = workManager.getWorkInfosForUniqueWork(UNIQUE_WORK_NAME_APP_UPDATE).get()
        assertEquals(0, workInfo.size)
    }

    @Test
    @Throws(Exception::class)
    fun testPeriodicWork() {
        every { preferences.isAutoDownloadEnabled } returns true
        every { preferences.updateInterval } returns DateUtils.HOUR_IN_MILLIS * 4
        every { preferences.overWifi } returns OVER_NETWORK_ALWAYS
        every { preferences.overData } returns OVER_NETWORK_ALWAYS

        AppUpdateWorker.scheduleOrCancel(context)

        val workInfo = workManager.getWorkInfosForUniqueWork(UNIQUE_WORK_NAME_AUTO_APP_UPDATE).get()
        assertEquals(1, workInfo.size)
        assertEquals(ENQUEUED, workInfo[0].state)
        val id = workInfo[0].id

        every { updateManager.updateApps() } just Runs

        val testDriver = WorkManagerTestInitHelper.getTestDriver(context) ?: fail()
        testDriver.setPeriodDelayMet(id)
        testDriver.setAllConstraintsMet(id)

        verify { updateManager.updateApps() }

        val workInfo2 = workManager.getWorkInfosForUniqueWork(UNIQUE_WORK_NAME_AUTO_APP_UPDATE)
            .get()
        assertEquals(1, workInfo2.size)
        assertEquals(ENQUEUED, workInfo2[0].state) // stays enqueued for next time
    }

}

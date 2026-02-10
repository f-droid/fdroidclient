package org.fdroid.history

import android.content.Context
import android.content.Context.MODE_APPEND
import android.content.Context.MODE_PRIVATE
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.fdroid.settings.SettingsManager
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.FileOutputStream
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

private const val MAX_EVENTS = 10

internal class HistoryManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val context: Context = mockk()
    private val settingsManager: SettingsManager = mockk()
    private val manager = HistoryManager(context, settingsManager, MAX_EVENTS)

    @Test
    fun testAppendGetAndClear() {
        val file = tempFolder.newFile()
        every { context.openFileOutput(any(), MODE_APPEND) } answers {
            FileOutputStream(file, true)
        }
        every { context.openFileInput(any()) } answers {
            file.inputStream()
        }
        every { settingsManager.useInstallHistory } returns true

        val installEvent = InstallEvent(
            time = Random.nextLong(),
            packageName = "foo.bar",
            name = "Foo Bar",
            versionName = "1.0.3",
            oldVersionName = if (Random.nextBoolean()) null else "1.0.1",
        )
        val uninstallEvent = UninstallEvent(
            time = Random.nextLong(),
            packageName = "org.example",
            name = if (Random.nextBoolean()) null else "2.0.3",
        )
        manager.append(installEvent)
        manager.append(installEvent)
        manager.append(uninstallEvent)
        assertEquals(
            listOf(installEvent, installEvent, uninstallEvent),
            manager.getEvents(),
        )

        // delete file
        every { context.deleteFile(any()) } returns true
        manager.clearAll()
        verify { context.deleteFile(any()) }
    }

    @Test
    fun testNoAppendWhenDisabled() {
        val uninstallEvent = UninstallEvent(
            time = Random.nextLong(),
            packageName = "org.example",
            name = if (Random.nextBoolean()) null else "2.0.3",
        )
        every { settingsManager.useInstallHistory } returns false
        manager.append(uninstallEvent)
    }

    @Test
    fun testPrune() {
        val file = tempFolder.newFile()
        every { context.openFileOutput(any(), MODE_APPEND) } answers {
            FileOutputStream(file, true)
        }
        every { context.openFileInput(any()) } answers {
            file.inputStream()
        }
        every { settingsManager.useInstallHistory } returns true

        val installEvent = InstallEvent(
            time = Random.nextLong(),
            packageName = "foo.bar",
            name = "Foo Bar",
            versionName = "1.0.3",
            oldVersionName = if (Random.nextBoolean()) null else "1.0.1",
        )
        val uninstallEvent = UninstallEvent(
            time = Random.nextLong(),
            packageName = "org.example",
            name = if (Random.nextBoolean()) null else "2.0.3",
        )
        repeat((0 until MAX_EVENTS).count()) {
            manager.append(installEvent)
            manager.append(uninstallEvent)
        }
        assertEquals(MAX_EVENTS * 2, manager.getEvents().size)

        every { context.openFileOutput(any(), MODE_PRIVATE) } answers {
            FileOutputStream(file, false)
        }
        manager.pruneEvents()
        assertEquals(MAX_EVENTS, manager.getEvents().size)
    }
}

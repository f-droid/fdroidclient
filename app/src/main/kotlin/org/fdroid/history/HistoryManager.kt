package org.fdroid.history

import android.content.Context
import android.content.Context.MODE_APPEND
import android.content.Context.MODE_PRIVATE
import androidx.annotation.WorkerThread
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.fdroid.settings.SettingsManager
import java.io.FileNotFoundException
import javax.inject.Inject
import javax.inject.Singleton

private const val HISTORY_FILE = "install_history.json"
private const val MAX_EVENTS = 999

@Singleton
class HistoryManager(
    private val context: Context,
    private val settingsManager: SettingsManager,
    private val maxNumEvents: Int = MAX_EVENTS,
) {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        settingsManager: SettingsManager,
    ) : this(context, settingsManager, MAX_EVENTS)

    private val log = KotlinLogging.logger { }

    @Synchronized
    @WorkerThread
    @OptIn(ExperimentalSerializationApi::class)
    fun getEvents(): List<HistoryEvent> {
        return try {
            context.openFileInput(HISTORY_FILE).use { inputStream ->
                // history shouldn't become too large to fit into memory, hopefully...
                val s = inputStream.readBytes().decodeToString().trimEnd(',')
                Json.decodeFromString("[$s]")
            }
        } catch (e: Exception) {
            if (e !is FileNotFoundException) {
                log.error(e) { "Error getting events: " }
                clearAll()
            }
            emptyList()
        }
    }

    @Synchronized
    @WorkerThread
    fun append(event: HistoryEvent) {
        if (!settingsManager.useInstallHistory) return
        try {
            context.openFileOutput(HISTORY_FILE, MODE_APPEND).use { outputStream ->
                val s = Json.encodeToString(event)
                outputStream.write(s.toByteArray())
                outputStream.write(",".toByteArray())
            }
        } catch (e: Exception) {
            log.error(e) { "Error appending $event: " }
        }
    }

    @Synchronized
    @WorkerThread
    fun clearAll() {
        try {
            context.deleteFile(HISTORY_FILE)
        } catch (e: Exception) {
            log.error(e) { "Error deleting file: " }
        }
    }

    @Synchronized
    @WorkerThread
    fun pruneEvents() {
        // only run if enabled and we have more events than we should have
        if (!settingsManager.useInstallHistory) return
        val events = getEvents()
        val overhead = events.size - maxNumEvents
        if (overhead <= 0) return

        try {
            val truncatedEvents = events.drop(overhead)
            val serializedEvents = Json.encodeToString(truncatedEvents)
                .trimStart('[')
                .trimEnd(']')
            context.openFileOutput(HISTORY_FILE, MODE_PRIVATE).use { outputStream ->
                outputStream.write(serializedEvents.toByteArray())
                outputStream.write(",".toByteArray())
            }
        } catch (e: Exception) {
            log.error(e) { "Error pruning $overhead events: " }
        }
    }
}

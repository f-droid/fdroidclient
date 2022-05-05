package org.fdroid.database

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.fdroid.index.v2.IndexV2StreamProcessor
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@RunWith(AndroidJUnit4::class)
internal class UpdateCheckerTest : DbTest() {

    private lateinit var updateChecker: UpdateChecker

    @Before
    override fun createDb() {
        super.createDb()
        // TODO mock packageManager and maybe move to unit tests
        updateChecker = UpdateChecker(db, context.packageManager)
    }

    @Test
    @OptIn(ExperimentalTime::class)
    fun testGetUpdates() {
        db.runInTransaction {
            val repoId = db.getRepositoryDao().insertEmptyRepo("https://f-droid.org/repo")
            val streamReceiver = DbV2StreamReceiver(db, repoId) { true }
            val indexProcessor = IndexV2StreamProcessor(streamReceiver, null)
            assets.open("resources/index-max-v2.json").use { indexStream ->
                indexProcessor.process(42, indexStream)
            }
        }
        val duration = measureTime {
            updateChecker.getUpdatableApps()
        }
        Log.e("TEST", "$duration")
    }

}

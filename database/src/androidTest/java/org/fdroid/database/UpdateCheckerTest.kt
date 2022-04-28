package org.fdroid.database

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.apache.commons.io.input.CountingInputStream
import org.fdroid.index.v1.IndexV1StreamProcessor
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@RunWith(AndroidJUnit4::class)
internal class UpdateCheckerTest : DbTest() {

    private lateinit var context: Context
    private lateinit var updateChecker: UpdateChecker

    @Before
    override fun createDb() {
        super.createDb()
        context = ApplicationProvider.getApplicationContext()
        updateChecker = UpdateChecker(db, context.packageManager)
    }

    @Test
    @OptIn(ExperimentalTime::class)
    fun testGetUpdates() {
        val inputStream = CountingInputStream(context.resources.assets.open("index-v1.json"))
        val repoId = db.getRepositoryDao().insertEmptyRepo("https://f-droid.org/repo")
        val indexProcessor = IndexV1StreamProcessor(DbV1StreamReceiver(db, { true }, repoId), null)

        db.runInTransaction {
            inputStream.use { indexStream ->
                indexProcessor.process(indexStream)
            }
        }

        val duration = measureTime {
            updateChecker.getUpdatableApps()
        }
        Log.e("TEST", "$duration")
    }

}

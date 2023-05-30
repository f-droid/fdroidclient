package org.fdroid.index.v1

import android.content.Context
import android.content.pm.ApplicationInfo.FLAG_SYSTEM
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.fdroid.index.IndexParser
import org.fdroid.test.TestDataMinV1
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
internal class IndexV1CreatorTest {

    @get:Rule
    var tmpFolder: TemporaryFolder = TemporaryFolder()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun test() {
        val repoDir = tmpFolder.newFolder()
        val repo = TestDataMinV1.repo
        val packageNames = context.packageManager.getInstalledPackages(0).filter {
            (it.applicationInfo.flags and FLAG_SYSTEM == 0) and (Random.nextInt(0, 3) == 0)
        }.map { it.packageName }.toSet()
        val indexCreator = IndexV1Creator(context.packageManager, repoDir, packageNames, repo)
        val indexV1 = indexCreator.createRepo()

        val indexFile = File(repoDir, DATA_FILE_NAME)
        assertTrue(indexFile.exists())
        val indexStr = indexFile.readBytes().decodeToString()
        assertEquals(indexV1, IndexParser.parseV1(indexStr))
    }
}

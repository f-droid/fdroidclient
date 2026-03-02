package org.fdroid.index.v2

import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import org.fdroid.index.ASSET_PATH
import org.fdroid.test.TestDataEmptyV2
import org.fdroid.test.TestDataMaxV2
import org.fdroid.test.TestDataMidV2
import org.fdroid.test.TestDataMinV2
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalSerializationApi::class)
internal class IndexV2FullStreamProcessorTest {

  @get:Rule var folder: TemporaryFolder = TemporaryFolder()

  @Test
  fun testEmpty() {
    testStreamProcessing("$ASSET_PATH/index-empty-v2.json", TestDataEmptyV2.index, 0)
  }

  @Test
  fun testMin() {
    testStreamProcessing("$ASSET_PATH/index-min-v2.json", TestDataMinV2.index, 1)
  }

  @Test
  fun testMinReordered() {
    testStreamProcessing("$ASSET_PATH/index-min-reordered-v2.json", TestDataMinV2.index, 1)
  }

  @Test
  fun testMid() {
    testStreamProcessing("$ASSET_PATH/index-mid-v2.json", TestDataMidV2.index, 2)
  }

  @Test
  fun testMax() {
    testStreamProcessing("$ASSET_PATH/index-max-v2.json", TestDataMaxV2.index, 3)
  }

  @Test
  fun testMalformedIndex() {
    // empty dict
    assertFailsWith<IllegalStateException> { testStreamError("{ }") }
      .also { assertContains(it.message!!, "Unexpected startIndex") }

    // garbage input
    assertFailsWith<SerializationException> { testStreamError("{ 23^^%*dfDFG568 }") }

    // repo is a number
    assertFailsWith<SerializationException> {
        testStreamError(
          """
          {
                "repo": 1
            }
          """
            .trimIndent()
        )
      }
      .also { assertContains(it.message!!, "object") }

    // repo is empty
    assertFailsWith<SerializationException> {
        testStreamError(
          """
          {
              "repo": { }
          }
          """
            .trimIndent()
        )
      }
      .also { assertContains(it.message!!, "timestamp") }

    // repo misses address
    assertFailsWith<SerializationException> {
        testStreamError(
          """
          {
              "repo": {
                  "timestamp": 23
              }
          }
          """
            .trimIndent()
        )
      }
      .also { assertContains(it.message!!, "address") }

    // packages is list
    assertFailsWith<SerializationException> {
        testStreamError(
          """
          {
              "repo": {
                  "timestamp": 23,
                  "address": "http://example.com"
              },
              "packages": []
          }
          """
            .trimIndent()
        )
      }
      .also { assertContains(it.message!!, "object") }
  }

  /** Tests that index parsed with a stream receiver is equal to the expected test data. */
  private fun testStreamProcessing(filePath: String, index: IndexV2, expectedNumApps: Int) {
    val file = File(filePath)
    val testStreamReceiver = TestStreamReceiver()
    val streamProcessor = IndexV2FullStreamProcessor(testStreamReceiver)
    var totalApps = 0
    FileInputStream(file).use {
      streamProcessor.process(42, it) { numAppsProcessed -> totalApps = numAppsProcessed }
    }

    assertTrue(testStreamReceiver.calledOnStreamEnded)
    assertEquals(index.repo, testStreamReceiver.repo)
    assertEquals(index.packages, testStreamReceiver.packages)
    assertEquals(expectedNumApps, totalApps)
  }

  private fun testStreamError(str: String) {
    val testStreamReceiver = TestStreamReceiver()
    val streamProcessor = IndexV2FullStreamProcessor(testStreamReceiver)
    var totalApps = 0
    ByteArrayInputStream(str.encodeToByteArray()).use {
      streamProcessor.process(42, it) { numAppsProcessed -> totalApps = numAppsProcessed }
    }

    assertTrue(testStreamReceiver.calledOnStreamEnded)
    assertEquals(0, testStreamReceiver.packages.size)
    assertEquals(0, totalApps)
  }

  private open class TestStreamReceiver : IndexV2StreamReceiver {
    var repo: RepoV2? = null
    val packages = HashMap<String, PackageV2>()
    var calledOnStreamEnded: Boolean = false

    override fun receive(repo: RepoV2, version: Long) {
      this.repo = repo
    }

    override fun receive(packageName: String, p: PackageV2) {
      packages[packageName] = p
    }

    override fun onStreamEnded() {
      if (calledOnStreamEnded) fail()
      calledOnStreamEnded = true
    }
  }
}

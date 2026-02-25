package org.fdroid.download.glide

import java.io.ByteArrayInputStream
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class AutoVerifyingInputStreamTest {

  private val testBytes =
    """
    this is test data for the AutoVerifyingInputStream
    this is test data for the AutoVerifyingInputStream
    this is test data for the AutoVerifyingInputStream
    this is test data for the AutoVerifyingInputStream
    this is test data for the AutoVerifyingInputStream
    this is test data for the AutoVerifyingInputStream
    this is test data for the AutoVerifyingInputStream
    this is test data for the AutoVerifyingInputStream
    this is test data for the AutoVerifyingInputStream
    this is test data for the AutoVerifyingInputStream
    this is test data for the AutoVerifyingInputStream
    this is test data for the AutoVerifyingInputStream
    this is test data for the AutoVerifyingInputStream
    this is test data for the AutoVerifyingInputStream
    this is test data for the AutoVerifyingInputStream
    this is test data for the AutoVerifyingInputStream
    """
      .trimIndent()
      .toByteArray()
  private val testHash = "784973023c9e3f32a750f3bc566bb13ee3f46b3811c2f269e2d11b47f07d1dab"

  @Test
  fun testSuccess() {
    AutoVerifyingInputStream(inputStream = ByteArrayInputStream(testBytes), expectedHash = testHash)
      .use { inputStream -> assertContentEquals(testBytes, inputStream.readBytes()) }
  }

  @Test
  fun testHashMismatch() {
    val e =
      assertFailsWith<IOException> {
        AutoVerifyingInputStream(
            inputStream = ByteArrayInputStream(testBytes),
            expectedHash = "684973023c9e3f32a750f3bc566bb13ee3f46b3811c2f269e2d11b47f07d1dab",
          )
          .use { inputStream -> inputStream.readBytes() }
      }
    assertEquals("Hash not matching.", e.message)
  }

  @Test
  fun testMaxBytesToRead() {
    val e =
      assertFailsWith<IOException> {
        AutoVerifyingInputStream(
            inputStream = ByteArrayInputStream(testBytes),
            expectedHash = testHash,
            maxBytesToRead = 42,
          )
          .use { inputStream -> inputStream.readBytes() }
      }
    assertEquals("Read ${testBytes.size} bytes, above maximum allowed.", e.message)
  }
}

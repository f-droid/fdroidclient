package org.fdroid.repo

import java.io.File
import java.io.InputStream
import java.security.DigestInputStream

internal class SavingInputStream(val inputStream: DigestInputStream, outputFile: File) :
  InputStream() {

  private val outputStream = outputFile.outputStream()

  override fun read(): Int {
    val byte = inputStream.read()
    if (byte != -1) {
      outputStream.write(byte)
    }
    return byte
  }

  override fun read(b: ByteArray): Int {
    val bytesRead = inputStream.read(b)
    if (bytesRead != -1) {
      outputStream.write(b, 0, bytesRead)
    }
    return bytesRead
  }

  override fun read(b: ByteArray, off: Int, len: Int): Int {
    val bytesRead = inputStream.read(b, off, len)
    if (bytesRead != -1) {
      outputStream.write(b, off, bytesRead)
    }
    return bytesRead
  }

  override fun close() {
    outputStream.use { inputStream.close() }
  }
}

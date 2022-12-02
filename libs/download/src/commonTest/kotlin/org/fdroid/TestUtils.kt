package org.fdroid

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.LookAheadSession
import io.ktor.utils.io.LookAheadSuspendSession
import io.ktor.utils.io.ReadSession
import io.ktor.utils.io.SuspendableReadSession
import io.ktor.utils.io.bits.Memory
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.internal.ChunkBuffer
import kotlinx.coroutines.runBlocking
import java.nio.ByteBuffer
import kotlin.random.Random

fun getRandomString(length: Int = Random.nextInt(4, 16)): String {
    val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
    return (1..length)
        .map { allowedChars.random() }
        .joinToString("")
}

fun runSuspend(block: suspend () -> Unit) = runBlocking {
    block()
}

fun get(mockEngine: MockEngine) = object : HttpClientEngineFactory<MockEngineConfig> {
    override fun create(block: MockEngineConfig.() -> Unit): HttpClientEngine {
        return mockEngine
    }
}

internal fun getIndexFile(
    name: String,
    sha256: String? = null,
    size: Long? = null,
    ipfsCidV1: String? = null,
): IndexFile {
    return object : IndexFile {
        override val name: String = name
        override val sha256: String? = sha256
        override val size: Long? = size
        override val ipfsCidV1: String? = ipfsCidV1
        override fun serialize(): String = error("Not yet implemented")
    }
}

@Suppress("OVERRIDE_DEPRECATION", "OverridingDeprecatedMember", "DEPRECATION")
internal abstract class TestByteReadChannel : ByteReadChannel {
    override val closedCause: Throwable? get() = error("Not yet implemented")
    override val isClosedForRead: Boolean get() = error("Not yet implemented")
    override val isClosedForWrite: Boolean get() = error("Not yet implemented")
    override val totalBytesRead: Long get() = error("Not yet implemented")

    override suspend fun awaitContent() = error("Not yet implemented")
    override fun cancel(cause: Throwable?): Boolean = error("Not yet implemented")
    override suspend fun discard(max: Long): Long = error("Not yet implemented")
    override fun <R> lookAhead(visitor: LookAheadSession.() -> R): R = error("Not yet implemented")
    override suspend fun <R> lookAheadSuspend(visitor: suspend LookAheadSuspendSession.() -> R): R =
        error("Not yet implemented")

    override suspend fun peekTo(
        destination: Memory,
        destinationOffset: Long,
        offset: Long,
        min: Long,
        max: Long,
    ): Long = error("Not yet implemented")

    override suspend fun read(min: Int, consumer: (ByteBuffer) -> Unit) =
        error("Not yet implemented")

    override suspend fun readAvailable(dst: ByteBuffer): Int = error("Not yet implemented")
    override suspend fun readAvailable(dst: ByteArray, offset: Int, length: Int): Int =
        error("Not yet implemented")

    override fun readAvailable(min: Int, block: (ByteBuffer) -> Unit): Int =
        error("Not yet implemented")

    override suspend fun readBoolean(): Boolean = error("Not yet implemented")
    override suspend fun readByte(): Byte = error("Not yet implemented")
    override suspend fun readDouble(): Double = error("Not yet implemented")
    override suspend fun readFloat(): Float = error("Not yet implemented")
    override suspend fun readFully(dst: ChunkBuffer, n: Int) = error("Not yet implemented")
    override suspend fun readFully(dst: ByteBuffer): Int = error("Not yet implemented")
    override suspend fun readFully(dst: ByteArray, offset: Int, length: Int) =
        error("Not yet implemented")

    override suspend fun readInt(): Int = error("Not yet implemented")
    override suspend fun readLong(): Long = error("Not yet implemented")
    override suspend fun readPacket(size: Int): ByteReadPacket = error("Not yet implemented")
    override suspend fun readRemaining(limit: Long): ByteReadPacket = error("Not yet implemented")
    override fun readSession(consumer: ReadSession.() -> Unit) = error("Not yet implemented")
    override suspend fun readShort(): Short = error("Not yet implemented")
    override suspend fun readSuspendableSession(
        consumer: suspend SuspendableReadSession.() -> Unit,
    ) = error("Not yet implemented")

    override suspend fun readUTF8Line(limit: Int): String? = error("Not yet implemented")
    override suspend fun <A : Appendable> readUTF8LineTo(out: A, limit: Int): Boolean =
        error("Not yet implemented")
}

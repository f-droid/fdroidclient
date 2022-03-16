package org.fdroid.index.v1

import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.junit.Assume.assumeTrue
import java.io.File
import java.io.FileInputStream
import kotlin.test.Test

internal class StreamTest {

    @Test
    @OptIn(ExperimentalSerializationApi::class)
    fun test() = runBlocking {
        val file = File("src/commonTest/resources/index-v1.json")
        assumeTrue(file.isFile)
        val byteChannel = FileInputStream(file).toByteReadChannel()
        val index = Json.decodeFromStream<IndexV1>(byteChannel.toInputStream())

    }

}

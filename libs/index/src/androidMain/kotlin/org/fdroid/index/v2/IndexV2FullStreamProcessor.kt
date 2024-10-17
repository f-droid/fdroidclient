package org.fdroid.index.v2

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeDecoder.Companion.DECODE_DONE
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.decodeFromStream
import org.fdroid.index.IndexParser
import java.io.InputStream

@OptIn(ExperimentalSerializationApi::class)
public class IndexV2FullStreamProcessor(
    private val indexStreamReceiver: IndexV2StreamReceiver,
    private val json: Json = IndexParser.json,
) : IndexV2StreamProcessor {

    @Throws(SerializationException::class, IllegalStateException::class)
    public override fun process(
        version: Long,
        inputStream: InputStream,
        onAppProcessed: (Int) -> Unit,
    ) {
        json.decodeFromStream(IndexStreamSerializer(version, onAppProcessed), inputStream)
    }

    private inner class IndexStreamSerializer(
        val version: Long,
        private val onAppProcessed: (Int) -> Unit,
    ) : KSerializer<IndexV2?> {
        override val descriptor = IndexV2.serializer().descriptor
        private var appsProcessed: Int = 0

        override fun deserialize(decoder: Decoder): IndexV2? {
            decoder as? JsonDecoder ?: error("Can be deserialized only by JSON")

            decoder.beginStructure(descriptor)
            val repoIndex = descriptor.getElementIndex("repo")
            val packagesIndex = descriptor.getElementIndex("packages")

            when (val startIndex = decoder.decodeElementIndex(descriptor)) {
                repoIndex -> {
                    deserializeRepo(decoder, startIndex)
                    val index = decoder.decodeElementIndex(descriptor)
                    if (index == packagesIndex) deserializePackages(decoder, index)
                }
                packagesIndex -> {
                    deserializePackages(decoder, startIndex)
                    val index = decoder.decodeElementIndex(descriptor)
                    if (index == repoIndex) deserializeRepo(decoder, index)
                }
                else -> error("Unexpected startIndex: $startIndex")
            }
            var currentIndex = 0
            while (currentIndex != DECODE_DONE) {
                currentIndex = decoder.decodeElementIndex(descriptor)
            }
            decoder.endStructure(descriptor)
            indexStreamReceiver.onStreamEnded()
            return null
        }

        private fun deserializeRepo(decoder: JsonDecoder, index: Int) {
            require(index == descriptor.getElementIndex("repo"))
            val repo = decoder.decodeSerializableValue(RepoV2.serializer())
            indexStreamReceiver.receive(repo, version)
        }

        private fun deserializePackages(decoder: JsonDecoder, index: Int) {
            require(index == descriptor.getElementIndex("packages"))
            val mapDescriptor = descriptor.getElementDescriptor(index)
            val compositeDecoder = decoder.beginStructure(mapDescriptor)
            while (true) {
                val packageIndex = compositeDecoder.decodeElementIndex(descriptor)
                if (packageIndex == DECODE_DONE) break
                readMapEntry(compositeDecoder, packageIndex)
                appsProcessed += 1
                onAppProcessed(appsProcessed)
            }
            compositeDecoder.endStructure(mapDescriptor)
        }

        private fun readMapEntry(decoder: CompositeDecoder, index: Int) {
            val packageName = decoder.decodeStringElement(descriptor, index)
            decoder.decodeElementIndex(descriptor)
            val packageV2 = decoder.decodeSerializableElement(
                descriptor = descriptor,
                index = index + 1,
                deserializer = PackageV2.serializer(),
            )
            indexStreamReceiver.receive(packageName, packageV2)
        }

        override fun serialize(encoder: Encoder, value: IndexV2?) {
            error("Not implemented")
        }
    }

}

package org.fdroid.index.v2

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.decodeFromStream
import org.fdroid.index.IndexParser
import java.io.InputStream

@OptIn(ExperimentalSerializationApi::class)
public class IndexStreamProcessor(
    private val indexStreamReceiver: IndexStreamReceiver,
    private val certificate: String?,
    private val json: Json = IndexParser.json,
    private val getAndLogReadBytes: () -> Long? = { null },
) {

    public fun process(repoId: Long, version: Int, inputStream: InputStream) {
        json.decodeFromStream(IndexStreamSerializer(repoId, version), inputStream)
        getAndLogReadBytes()
    }

    private inner class IndexStreamSerializer(
        val repoId: Long,
        val version: Int,
    ) : KSerializer<IndexV2?> {
        override val descriptor = IndexV2.serializer().descriptor

        override fun deserialize(decoder: Decoder): IndexV2? {
            getAndLogReadBytes()
            decoder as? JsonDecoder ?: error("Can be deserialized only by JSON")

            decoder.beginStructure(descriptor)
            val repoIndex = descriptor.getElementIndex("repo")
            val packagesIndex = descriptor.getElementIndex("packages")

            when (val startIndex = decoder.decodeElementIndex(descriptor)) {
                repoIndex -> {
                    deserializeRepo(decoder, startIndex, repoId)
                    val index = decoder.decodeElementIndex(descriptor)
                    deserializePackages(decoder, index, repoId)
                }
                packagesIndex -> {
                    deserializePackages(decoder, startIndex, repoId)
                    val index = decoder.decodeElementIndex(descriptor)
                    deserializeRepo(decoder, index, repoId)
                }
                else -> error("Unexpected startIndex: $startIndex")
            }
            decoder.endStructure(descriptor)
            indexStreamReceiver.onStreamEnded(repoId)
            return null
        }

        private fun deserializeRepo(decoder: JsonDecoder, index: Int, repoId: Long) {
            require(index == descriptor.getElementIndex("repo"))
            val repo = decoder.decodeSerializableValue(RepoV2.serializer())
            // TODO this replaces the index and thus removes all data, not good when repo is second
            indexStreamReceiver.receive(repoId, repo, version, certificate)
        }

        private fun deserializePackages(decoder: JsonDecoder, index: Int, repoId: Long) {
            require(index == descriptor.getElementIndex("packages"))
            val mapDescriptor = descriptor.getElementDescriptor(index)
            val compositeDecoder = decoder.beginStructure(mapDescriptor)
            while (true) {
                getAndLogReadBytes()
                val packageIndex = compositeDecoder.decodeElementIndex(descriptor)
                if (packageIndex == CompositeDecoder.DECODE_DONE) break
                readMapEntry(compositeDecoder, packageIndex, repoId)
            }
            compositeDecoder.endStructure(mapDescriptor)
        }

        private fun readMapEntry(decoder: CompositeDecoder, index: Int, repoId: Long) {
            val packageName = decoder.decodeStringElement(descriptor, index)
            decoder.decodeElementIndex(descriptor)
            val packageV2 = decoder.decodeSerializableElement(
                descriptor, index + 1, PackageV2.serializer()
            )
            indexStreamReceiver.receive(repoId, packageName, packageV2)
        }

        override fun serialize(encoder: Encoder, value: IndexV2?) {
            error("Not implemented")
        }
    }

}

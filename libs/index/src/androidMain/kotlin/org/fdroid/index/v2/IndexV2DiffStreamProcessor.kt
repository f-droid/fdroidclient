package org.fdroid.index.v2

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.jsonObject
import org.fdroid.index.IndexParser
import java.io.InputStream

@OptIn(ExperimentalSerializationApi::class)
public class IndexV2DiffStreamProcessor(
    private val indexStreamReceiver: IndexV2DiffStreamReceiver,
    private val json: Json = IndexParser.json,
) : IndexV2StreamProcessor {

    public override fun process(
        version: Long,
        inputStream: InputStream,
        onAppProcessed: (Int) -> Unit,
    ) {
        json.decodeFromStream(IndexStreamSerializer(version, onAppProcessed), inputStream)
    }

    private inner class IndexStreamSerializer(
        private val version: Long,
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
                    diffRepo(version, decoder, startIndex)
                    val index = decoder.decodeElementIndex(descriptor)
                    if (index == packagesIndex) diffPackages(decoder, index)
                }

                packagesIndex -> {
                    diffPackages(decoder, startIndex)
                    val index = decoder.decodeElementIndex(descriptor)
                    if (index == repoIndex) diffRepo(version, decoder, index)
                }

                else -> error("Unexpected startIndex: $startIndex")
            }
            var currentIndex = 0
            while (currentIndex != CompositeDecoder.DECODE_DONE) {
                currentIndex = decoder.decodeElementIndex(descriptor)
            }
            decoder.endStructure(descriptor)
            indexStreamReceiver.onStreamEnded()
            return null
        }

        private fun diffRepo(version: Long, decoder: JsonDecoder, index: Int) {
            require(index == descriptor.getElementIndex("repo"))
            val repo = decoder.decodeJsonElement().jsonObject
            indexStreamReceiver.receiveRepoDiff(version, repo)
        }

        private fun diffPackages(decoder: JsonDecoder, index: Int) {
            require(index == descriptor.getElementIndex("packages"))
            val mapDescriptor = descriptor.getElementDescriptor(index)
            val compositeDecoder = decoder.beginStructure(mapDescriptor)
            while (true) {
                val packageIndex = compositeDecoder.decodeElementIndex(descriptor)
                if (packageIndex == CompositeDecoder.DECODE_DONE) break
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
                deserializer = JsonElement.serializer(),
            )
            if (packageV2 is JsonNull) {
                // delete app and existing metadata
                indexStreamReceiver.receivePackageMetadataDiff(packageName, null)
                return
            }
            // diff package metadata
            val metadata = packageV2.jsonObject["metadata"]
            if (metadata is JsonNull) {
                // delete app and existing metadata
                indexStreamReceiver.receivePackageMetadataDiff(packageName, null)
            } else if (metadata is JsonObject) {
                // if it is null, the diff doesn't change it, so only call receiver if not null
                indexStreamReceiver.receivePackageMetadataDiff(packageName, metadata)
            }
            // diff package versions
            if (packageV2.jsonObject["versions"] is JsonNull) {
                // delete all versions of this app
                indexStreamReceiver.receiveVersionsDiff(packageName, null)
            } else {
                val versions = packageV2.jsonObject["versions"]?.jsonObject?.mapValues {
                    if (it.value is JsonNull) null else it.value.jsonObject
                }
                if (versions != null) {
                    // if it is null, the diff doesn't change it, so only call receiver if not null
                    indexStreamReceiver.receiveVersionsDiff(packageName, versions)
                }
            }
        }

        override fun serialize(encoder: Encoder, value: IndexV2?) {
            error("Not implemented")
        }
    }

}

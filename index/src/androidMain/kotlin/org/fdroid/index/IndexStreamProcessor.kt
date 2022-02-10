package org.fdroid.index

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.jsonObject
import org.fdroid.index.v2.IndexStreamReceiver
import org.fdroid.index.v2.IndexV2
import org.fdroid.index.v2.PackageV2
import org.fdroid.index.v2.RepoV2
import java.io.InputStream

@OptIn(ExperimentalSerializationApi::class)
public class IndexStreamProcessor(
    private val indexStreamReceiver: IndexStreamReceiver,
    private val json: Json = Json,
) {
    public fun process(inputStream: InputStream) {
        json.decodeFromStream(IndexStreamSerializer(), inputStream)
    }

    private inner class IndexStreamSerializer : KSerializer<IndexV2?> {
        override val descriptor = IndexV2.serializer().descriptor

        override fun deserialize(decoder: Decoder): IndexV2? {
            val jsonInput = decoder as? JsonDecoder ?: error("Can be deserialized only by JSON")
            val jsonObject = jsonInput.decodeJsonElement().jsonObject

            val jsonRepo = jsonObject["repo"] ?: throw SerializationException()
            val repo = json.decodeFromJsonElement<RepoV2>(jsonRepo)
            val repoId = indexStreamReceiver.receive(null, repo)
            val packages = jsonObject["packages"]?.jsonObject ?: throw SerializationException()
            // TODO check that this isn't reading the entire stream already
            packages.entries.forEach { (packageId, jsonPackage) ->
                val p = json.decodeFromJsonElement<PackageV2>(jsonPackage)
                indexStreamReceiver.receive(repoId, packageId, p)
            }
            return null
        }

        override fun serialize(encoder: Encoder, value: IndexV2?) {
            error("Not implemented")
        }
    }

}


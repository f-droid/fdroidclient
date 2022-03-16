package org.fdroid.index.v1

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encoding.CompositeDecoder.Companion.DECODE_DONE
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.decodeFromStream
import org.fdroid.index.DEFAULT_LOCALE
import org.fdroid.index.IndexParser
import org.fdroid.index.DEFAULT_LOCALE
import org.fdroid.index.RELEASE_CHANNEL_BETA
import org.fdroid.index.getV1ReleaseChannels
import org.fdroid.index.mapInto
import org.fdroid.index.mapValuesNotNull
import org.fdroid.index.v2.AntiFeatureV2
import org.fdroid.index.v2.CategoryV2
import org.fdroid.index.v2.LocalizedTextV2
import org.fdroid.index.v2.PackageVersionV2
import java.io.InputStream

@Suppress("DEPRECATION")
@OptIn(ExperimentalSerializationApi::class)
public class IndexV1StreamProcessor(
    private val indexStreamReceiver: IndexV1StreamReceiver,
    private val certificate: String?,
    private val locale: String = DEFAULT_LOCALE,
    private val json: Json = IndexParser.json,
    private val getAndLogReadBytes: () -> Long? = { null },
) {

    public fun process(repoId: Long, inputStream: InputStream) {
        json.decodeFromStream(IndexStreamSerializer(repoId), inputStream)
        getAndLogReadBytes()
    }

    private inner class IndexStreamSerializer(val repoId: Long) : KSerializer<IndexV1?> {
        override val descriptor = IndexV1.serializer().descriptor

        override fun deserialize(decoder: Decoder): IndexV1? {
            getAndLogReadBytes()
            decoder as? JsonDecoder ?: error("Can be deserialized only by JSON")

            decoder.beginStructure(descriptor)
            var index = decoder.decodeElementIndex(descriptor)
            deserializeRepo(decoder, index, repoId)
            index = decoder.decodeElementIndex(descriptor)
            deserializeRequests(decoder, index, repoId)
            index = decoder.decodeElementIndex(descriptor)
            val appDataMap = deserializeApps(decoder, index, repoId)
            index = decoder.decodeElementIndex(descriptor)
            deserializePackages(decoder, index, repoId, appDataMap)
            decoder.endStructure(descriptor)

            updateRepoData(appDataMap)
            return null
        }

        private fun deserializeRepo(decoder: JsonDecoder, index: Int, repoId: Long) {
            require(index == descriptor.getElementIndex("repo"))
            val repo = decoder.decodeSerializableValue(RepoV1.serializer())
            val repoV2 = repo.toRepoV2(
                locale = DEFAULT_LOCALE,
                antiFeatures = emptyMap(),
                categories = emptyMap(),
                releaseChannels = emptyMap()
            )
            indexStreamReceiver.receive(repoId, repoV2, repo.version, certificate)
        }

        private fun deserializeRequests(decoder: JsonDecoder, index: Int, repoId: Long) {
            require(index == descriptor.getElementIndex("requests"))
            decoder.decodeSerializableValue(Requests.serializer())
            // we ignore the requests here, don't act on them
        }

        private fun deserializeApps(
            decoder: JsonDecoder,
            index: Int,
            repoId: Long,
        ): Map<String, AppData> {
            require(index == descriptor.getElementIndex("apps"))
            val appDataMap = HashMap<String, AppData>()
            val mapDescriptor = descriptor.getElementDescriptor(index)
            val compositeDecoder = decoder.beginStructure(mapDescriptor)
            while (true) {
                getAndLogReadBytes()
                val packageIndex = compositeDecoder.decodeElementIndex(descriptor)
                if (packageIndex == DECODE_DONE) break
                val appV1 =
                    decoder.decodeSerializableElement(descriptor, packageIndex, AppV1.serializer())
                val appV2 = appV1.toMetadataV2(null, locale)
                indexStreamReceiver.receive(repoId, appV1.packageName, appV2)
                appDataMap[appV1.packageName] = AppData(
                    antiFeatures = appV1.antiFeatures.associateWith { emptyMap() },
                    whatsNew = appV1.localized?.mapValuesNotNull { it.value.whatsNew },
                    suggestedVersionCode = appV1.suggestedVersionCode?.toLongOrNull(),
                    categories = appV1.categories,
                )
            }
            compositeDecoder.endStructure(mapDescriptor)
            return appDataMap
        }

        private fun deserializePackages(
            decoder: JsonDecoder,
            index: Int,
            repoId: Long,
            appDataMap: Map<String, AppData>,
        ) {
            require(index == descriptor.getElementIndex("packages"))
            val mapDescriptor = descriptor.getElementDescriptor(index)
            val compositeDecoder = decoder.beginStructure(mapDescriptor)
            while (true) {
                getAndLogReadBytes()
                val packageIndex = compositeDecoder.decodeElementIndex(descriptor)
                if (packageIndex == DECODE_DONE) break
                readPackageMapEntry(
                    decoder = compositeDecoder as JsonDecoder,
                    index = packageIndex,
                    repoId = repoId,
                    appDataMap = appDataMap,
                )
            }
            compositeDecoder.endStructure(mapDescriptor)
        }

        private fun readPackageMapEntry(
            decoder: JsonDecoder,
            index: Int,
            repoId: Long,
            appDataMap: Map<String, AppData>,
        ) {
            val packageName = decoder.decodeStringElement(descriptor, index)
            decoder.decodeElementIndex(descriptor)
            val versions = HashMap<String, PackageVersionV2>()

            val listDescriptor = ListSerializer(PackageV1.serializer()).descriptor
            val compositeDecoder = decoder.beginStructure(listDescriptor)
            var isFirstVersion = true
            while (true) {
                getAndLogReadBytes()
                val packageIndex = compositeDecoder.decodeElementIndex(descriptor)
                if (packageIndex == DECODE_DONE) break
                val packageVersionV1 = decoder.decodeSerializableElement(
                    descriptor, index + 1, PackageV1.serializer()
                )
                val versionCode = packageVersionV1.versionCode ?: 0
                val suggestedVersionCode =
                    appDataMap[packageName]?.suggestedVersionCode ?: 0
                val releaseChannels = if (versionCode > suggestedVersionCode)
                    listOf(RELEASE_CHANNEL_BETA) else emptyList()
                val packageVersionV2 = packageVersionV1.toPackageVersionV2(
                    releaseChannels = releaseChannels,
                    appAntiFeatures = appDataMap[packageName]?.antiFeatures ?: emptyMap(),
                    whatsNew = if (isFirstVersion) appDataMap[packageName]?.whatsNew else null
                )
                if (isFirstVersion) {
                    indexStreamReceiver.updateAppMetadata(
                        repoId,
                        packageName,
                        packageVersionV1.signer
                    )
                }
                isFirstVersion = false
                val versionId = packageVersionV2.file.sha256
                versions[versionId] = packageVersionV2
            }
            indexStreamReceiver.receive(repoId, packageName, versions)
            compositeDecoder.endStructure(listDescriptor)
        }

        private fun updateRepoData(appDataMap: Map<String, AppData>) {
            val antiFeatures = HashMap<String, AntiFeatureV2>()
            val categories = HashMap<String, CategoryV2>()
            appDataMap.values.forEach { appData ->
                appData.antiFeatures.keys.mapInto(
                    antiFeatures,
                    AntiFeatureV2(name = emptyMap(), description = emptyMap())
                )
                appData.categories.mapInto(
                    categories,
                    CategoryV2(name = emptyMap(), description = emptyMap())
                )
            }
            val releaseChannels = getV1ReleaseChannels()
            indexStreamReceiver.updateRepo(repoId, antiFeatures, categories, releaseChannels)
        }

        override fun serialize(encoder: Encoder, value: IndexV1?) {
            error("Not implemented")
        }
    }

}

private class AppData(
    val antiFeatures: Map<String, LocalizedTextV2>,
    val whatsNew: LocalizedTextV2?,
    val suggestedVersionCode: Long?,
    val categories: List<String>,
)

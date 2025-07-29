package org.fdroid.appsearch

import androidx.appsearch.annotation.Document
import androidx.appsearch.app.AppSearchSchema.LongPropertyConfig
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES
import androidx.appsearch.app.StringSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import org.fdroid.index.IndexParser.json
import org.fdroid.index.v2.FileV2
import org.fdroid.index.v2.LocalizedTextV2

@Document
data class AppDocument(
    @Document.Namespace
    val namespace: String = "app",
    @Document.Id
    val id: String,
    @Document.CreationTimestampMillis
    val lastUpdated: Long,
    @Document.LongProperty(indexingType = LongPropertyConfig.INDEXING_TYPE_RANGE)
    val repoId: Long,

    @Document.StringProperty(
        indexingType = INDEXING_TYPE_PREFIXES,
        serializer = LocalizedTextV2Serializer::class,
    )
    val name: LocalizedTextV2?,
    @Document.StringProperty(
        indexingType = INDEXING_TYPE_PREFIXES,
        serializer = LocalizedTextV2Serializer::class,
    )
    val summary: LocalizedTextV2?,
    @Document.StringProperty(
        indexingType = INDEXING_TYPE_PREFIXES,
        serializer = LocalizedTextV2Serializer::class,
    )
    val description: LocalizedTextV2?,
    @Document.StringProperty(indexingType = INDEXING_TYPE_PREFIXES)
    val packageName: String,
    @Document.StringProperty(indexingType = INDEXING_TYPE_PREFIXES)
    val authorName: String?,

    @Document.StringProperty(
        indexingType = INDEXING_TYPE_NONE,
        serializer = FileV2Serializer::class,
    )
    val icon: FileV2?,
) : AppSearchDoc

private val localizedTextV2Serializer = MapSerializer(String.serializer(), String.serializer())

class LocalizedTextV2Serializer : StringSerializer<LocalizedTextV2> {
    override fun serialize(instance: LocalizedTextV2): String {
        return json.encodeToString(localizedTextV2Serializer, instance)
    }

    @Override
    override fun deserialize(string: String): LocalizedTextV2? {
        return json.decodeFromString(localizedTextV2Serializer, string)
    }
}

class FileV2Serializer : StringSerializer<FileV2> {
    override fun serialize(instance: FileV2): String {
        return json.encodeToString(instance)
    }

    @Override
    override fun deserialize(string: String): FileV2? {
        return FileV2.deserialize(string)
    }
}

package org.fdroid.appsearch

import androidx.appsearch.annotation.Document
import androidx.appsearch.app.AppSearchSchema.LongPropertyConfig
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES
import androidx.appsearch.app.StringSerializer
import org.fdroid.index.IndexParser
import org.fdroid.index.v2.FileV2

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

    @Document.StringProperty(indexingType = INDEXING_TYPE_PREFIXES)
    val name: String?,
    @Document.StringProperty(indexingType = INDEXING_TYPE_PREFIXES)
    val summary: String?,
    @Document.StringProperty(indexingType = INDEXING_TYPE_PREFIXES)
    val description: String?,
    @Document.StringProperty(indexingType = INDEXING_TYPE_PREFIXES)
    val packageName: String,
    @Document.StringProperty(indexingType = INDEXING_TYPE_PREFIXES)
    val authorName: String?,

    @Document.StringProperty(
        indexingType = INDEXING_TYPE_NONE,
        serializer = FileV2Serializer::class,
    )
    val icon: FileV2?,
)

class FileV2Serializer : StringSerializer<FileV2> {
    override fun serialize(instance: FileV2): String {
        return IndexParser.json.encodeToString(instance)
    }

    @Override
    override fun deserialize(string: String): FileV2? {
        return FileV2.deserialize(string)
    }
}

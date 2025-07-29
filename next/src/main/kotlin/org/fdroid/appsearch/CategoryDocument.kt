package org.fdroid.appsearch

import androidx.appsearch.annotation.Document
import androidx.appsearch.app.AppSearchSchema.LongPropertyConfig
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES
import org.fdroid.index.v2.FileV2

@Document
data class CategoryDocument(
    @Document.Namespace
    val namespace: String = "category",
    @Document.Id
    val id: String,
    @Document.LongProperty(indexingType = LongPropertyConfig.INDEXING_TYPE_RANGE)
    val repoId: Long,
    @Document.StringProperty(indexingType = INDEXING_TYPE_PREFIXES)
    val name: String?,
    @Document.StringProperty(indexingType = INDEXING_TYPE_PREFIXES)
    val description: String?,

    @Document.StringProperty(
        indexingType = INDEXING_TYPE_NONE,
        serializer = FileV2Serializer::class,
    )
    val icon: FileV2?,
) : AppSearchDoc

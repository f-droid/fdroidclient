package org.fdroid.database

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import org.fdroid.index.IndexParser.json
import org.fdroid.index.v2.FileV2
import org.fdroid.index.v2.LocalizedFileV2
import org.fdroid.index.v2.LocalizedTextV2

internal object Converters {

    private val localizedTextV2Serializer = MapSerializer(String.serializer(), String.serializer())
    private val localizedFileV2Serializer = MapSerializer(String.serializer(), FileV2.serializer())
    private val mapOfLocalizedTextV2Serializer =
        MapSerializer(String.serializer(), localizedTextV2Serializer)

    @TypeConverter
    fun fromStringToLocalizedTextV2(value: String?): LocalizedTextV2? {
        return value?.let { json.decodeFromString(localizedTextV2Serializer, it) }
    }

    @TypeConverter
    fun localizedTextV2toString(text: LocalizedTextV2?): String? {
        return text?.let { json.encodeToString(localizedTextV2Serializer, it) }
    }

    @TypeConverter
    fun fromStringToLocalizedFileV2(value: String?): LocalizedFileV2? {
        return value?.let { json.decodeFromString(localizedFileV2Serializer, it) }
    }

    @TypeConverter
    fun localizedFileV2toString(file: LocalizedFileV2?): String? {
        return file?.let { json.encodeToString(localizedFileV2Serializer, it) }
    }

    @TypeConverter
    fun fromStringToMapOfLocalizedTextV2(value: String?): Map<String, LocalizedTextV2>? {
        return value?.let { json.decodeFromString(mapOfLocalizedTextV2Serializer, it) }
    }

    @TypeConverter
    fun mapOfLocalizedTextV2toString(text: Map<String, LocalizedTextV2>?): String? {
        return text?.let { json.encodeToString(mapOfLocalizedTextV2Serializer, it) }
    }

    @TypeConverter
    fun fromStringToListString(value: String?): List<String> {
        return value?.split(',')?.filter { it.isNotEmpty() } ?: emptyList()
    }

    @TypeConverter
    fun listStringToString(text: List<String>?): String? {
        if (text.isNullOrEmpty()) return null
        return text.joinToString(
            prefix = ",",
            separator = ",",
            postfix = ",",
        ) { it.replace(',', '_') }
    }
}

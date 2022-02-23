package org.fdroid.database

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import org.fdroid.index.IndexParser.json
import org.fdroid.index.v2.LocalizedTextV2

internal class Converters {

    private val localizedTextV2Serializer = MapSerializer(String.serializer(), String.serializer())

    @TypeConverter
    fun fromStringToLocalizedTextV2(value: String?): LocalizedTextV2? {
        return value?.let { json.decodeFromString(localizedTextV2Serializer, it) }
    }

    @TypeConverter
    fun localizedTextV2toString(text: LocalizedTextV2?): String? {
        return text?.let { json.encodeToString(localizedTextV2Serializer, it) }
    }
}

package org.fdroid.index

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.fdroid.index.v1.IndexV1
import org.fdroid.index.v2.Entry
import org.fdroid.index.v2.IndexV2

public object IndexParser {

    @Volatile
    private var jsonInstance: Json? = null

    /**
     * Initializing [Json] is expensive, so using this method is preferable as it keeps returning
     * a single instance with the recommended settings.
     */
    public val json: Json
        @JvmStatic
        get() {
            return jsonInstance ?: synchronized(this) {
                Json {
                    ignoreUnknownKeys = true
                }
            }
        }

    @JvmStatic
    public fun parseV1(str: String): IndexV1 {
        return json.decodeFromString(str)
    }

    @JvmStatic
    public fun parseV2(str: String): IndexV2 {
        return json.decodeFromString(str)
    }

    @JvmStatic
    public fun parseEntry(str: String): Entry {
        return json.decodeFromString(str)
    }

}

package org.fdroid.test

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.fdroid.index.v2.MetadataV2
import org.fdroid.index.v2.PackageVersionV2
import org.fdroid.index.v2.RepoV2
import kotlin.random.Random

object DiffUtils {

    /**
     * Create a map diff by adding or removing keys. Note that this does not change keys.
     */
    fun <T> Map<String, T?>.randomDiff(factory: () -> T): Map<String, T?> = buildMap {
        if (this@randomDiff.isNotEmpty()) {
            // remove random keys
            while (Random.nextBoolean()) put(this@randomDiff.keys.random(), null)
            // Note: we don't replace random keys, because we can't easily diff inside T
        }
        // add random keys
        while (Random.nextBoolean()) put(TestUtils.getRandomString(), factory())
    }

    /**
     * Removes keys from a JSON object representing a [RepoV2] which need special handling.
     */
    fun JsonObject.cleanRepo(): JsonObject {
        val keysToFilter = listOf("mirrors", "antiFeatures", "categories", "releaseChannels")
        val newMap = filterKeys { it !in keysToFilter }
        return JsonObject(newMap)
    }

    fun RepoV2.clean() = copy(
        mirrors = emptyList(),
        antiFeatures = emptyMap(),
        categories = emptyMap(),
        releaseChannels = emptyMap(),
    )

    /**
     * Removes keys from a JSON object representing a [MetadataV2] which need special handling.
     */
    fun JsonObject.cleanMetadata(): JsonObject {
        val keysToFilter = listOf("icon", "featureGraphic", "promoGraphic", "tvBanner",
            "screenshots")
        val newMap = filterKeys { it !in keysToFilter }
        return JsonObject(newMap)
    }

    fun MetadataV2.clean() = copy(
        icon = null,
        featureGraphic = null,
        promoGraphic = null,
        tvBanner = null,
        screenshots = null,
    )

    /**
     * Removes keys from a JSON object representing a [PackageVersionV2] which need special handling.
     */
    fun JsonObject.cleanVersion(): JsonObject {
        if (!containsKey("manifest")) return this
        val keysToFilter = listOf("features", "usesPermission", "usesPermissionSdk23")
        val newMap = toMutableMap()
        val filteredManifest = newMap["manifest"]!!.jsonObject.filterKeys { it !in keysToFilter }
        newMap["manifest"] = JsonObject(filteredManifest)
        return JsonObject(newMap)
    }

    fun PackageVersionV2.clean() = copy(
        manifest = manifest.copy(
            features = emptyList(),
            usesPermission = emptyList(),
            usesPermissionSdk23 = emptyList(),
        ),
    )

    fun <T> Map<String, T>.applyDiff(diff: Map<String, T?>): Map<String, T> =
        toMutableMap().apply {
            diff.entries.forEach { (key, value) ->
                if (value == null) remove(key)
                else set(key, value)
            }
        }

}

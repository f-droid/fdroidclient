package org.fdroid.index

import org.fdroid.index.v1.IndexV1
import org.fdroid.index.v1.Localized
import org.fdroid.index.v2.AntiFeatureV2
import org.fdroid.index.v2.CategoryV2
import org.fdroid.index.v2.IndexV2
import org.fdroid.index.v2.LocalizedTextV2
import org.fdroid.index.v2.PackageV2
import org.fdroid.index.v2.ReleaseChannelV2

public const val RELEASE_CHANNEL_BETA: String = "Beta"
internal const val DEFAULT_LOCALE = "en-US"

public class IndexConverter(
    private val defaultLocale: String = DEFAULT_LOCALE,
) {

    public fun toIndexV2(v1: IndexV1): IndexV2 {
        val antiFeatures = HashMap<String, AntiFeatureV2>()
        val categories = HashMap<String, CategoryV2>()
        val packagesV2 = HashMap<String, PackageV2>(v1.apps.size)
        v1.apps.forEach { app ->
            val versions = v1.packages[app.packageName]
            val preferredSigner = versions?.get(0)?.signer
            val appAntiFeatures: Map<String, LocalizedTextV2> =
                app.antiFeatures.associateWith { emptyMap() }
            val whatsNew: LocalizedTextV2? = app.localized?.mapValuesNotNull { it.value.whatsNew }
            var isFirstVersion = true
            val packageV2 = PackageV2(
                metadata = app.toMetadataV2(preferredSigner, defaultLocale),
                versions = versions?.associate {
                    val versionCode = it.versionCode ?: 0
                    val suggestedVersionCode = app.suggestedVersionCode?.toLongOrNull() ?: 0
                    val versionReleaseChannels = if (versionCode > suggestedVersionCode)
                        listOf(RELEASE_CHANNEL_BETA) else emptyList()
                    val wn = if (isFirstVersion) whatsNew else null
                    isFirstVersion = false
                    it.hash to it.toPackageVersionV2(versionReleaseChannels, appAntiFeatures, wn)
                } ?: emptyMap(),
            )
            appAntiFeatures.keys.mapInto(
                antiFeatures,
                AntiFeatureV2(name = emptyMap(), description = emptyMap())
            )
            app.categories.mapInto(
                categories,
                CategoryV2(name = emptyMap(), description = emptyMap())
            )
            packagesV2[app.packageName] = packageV2
        }
        return IndexV2(
            repo = v1.repo.toRepoV2(
                locale = defaultLocale,
                antiFeatures = antiFeatures,
                categories = categories,
                releaseChannels = getV1ReleaseChannels(),
            ),
            packages = packagesV2,
        )
    }

}

internal fun <T> Collection<String>.mapInto(map: HashMap<String, T>, value: T) {
    forEach { key ->
        if (!map.containsKey(key)) map[key] = value
    }
}

internal fun getV1ReleaseChannels() = mapOf(
    RELEASE_CHANNEL_BETA to ReleaseChannelV2(emptyMap(), emptyMap())
)

internal fun <T> Map<String, Localized>.mapValuesNotNull(
    transform: (Map.Entry<String, Localized>) -> T?,
): Map<String, T>? {
    val map = LinkedHashMap<String, T>(size)
    for (element in this) {
        val value = transform(element)
        if (value != null) map[element.key] = value
    }
    return map.takeIf { map.isNotEmpty() }
}

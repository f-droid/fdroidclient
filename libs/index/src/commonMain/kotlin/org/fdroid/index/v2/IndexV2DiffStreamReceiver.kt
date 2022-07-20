package org.fdroid.index.v2

import kotlinx.serialization.json.JsonObject

public interface IndexV2DiffStreamReceiver {

    /**
     * Receives the diff for the [RepoV2] from the index stream.
     */
    public fun receiveRepoDiff(version: Long, repoJsonObject: JsonObject)

    /**
     * Receives one diff for a [MetadataV2] from the index stream.
     * This is called once for each package in the index diff.
     *
     * If the given [packageJsonObject] is null, the package should be removed.
     */
    public fun receivePackageMetadataDiff(packageName: String, packageJsonObject: JsonObject?)

    /**
     * Receives the diff for all versions of the give n [packageName]
     * as a map of versions IDs to the diff [JsonObject].
     * This is called once for each package in the index diff (if versions have changed).
     *
     * If an entry in the given [versionsDiffMap] is null,
     * the version with that ID should be removed.
     */
    public fun receiveVersionsDiff(packageName: String, versionsDiffMap: Map<String, JsonObject?>?)

    /**
     * Called when the stream has been processed to its end.
     */
    public fun onStreamEnded()

}

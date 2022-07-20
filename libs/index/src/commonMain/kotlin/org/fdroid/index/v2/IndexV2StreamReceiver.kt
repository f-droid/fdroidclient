package org.fdroid.index.v2

public interface IndexV2StreamReceiver {

    /**
     * Receives the [RepoV2] from the index stream.
     * Attention: This might get called after receiving packages.
     */
    public fun receive(repo: RepoV2, version: Long, certificate: String)

    /**
     * Receives one [PackageV2] from the index stream.
     * This is called once for each package in the index.
     */
    public fun receive(packageName: String, p: PackageV2)

    /**
     * Called when the stream has been processed to its end.
     */
    public fun onStreamEnded()

}

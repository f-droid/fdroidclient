package org.fdroid.index.v2

public interface IndexStreamReceiver {

    public fun receive(repoId: Long, repo: RepoV2)
    public fun receive(repoId: Long, packageId: String, p: PackageV2)

}

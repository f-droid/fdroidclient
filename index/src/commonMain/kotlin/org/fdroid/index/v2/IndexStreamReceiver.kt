package org.fdroid.index.v2

public interface IndexStreamReceiver {

    public fun receive(repoId: Long? = null, repo: RepoV2): Long
    public fun receive(repoId: Long, packageId: String, p: PackageV2)

}

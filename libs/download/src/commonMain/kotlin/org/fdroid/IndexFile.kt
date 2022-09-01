package org.fdroid

public interface IndexFile {
    public val name: String
    public val sha256: String?
    public val size: Long?
    public val ipfsCidV1: String?

    public fun serialize(): String
}

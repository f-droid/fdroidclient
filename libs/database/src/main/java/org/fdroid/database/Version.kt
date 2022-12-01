package org.fdroid.database

import androidx.core.os.LocaleListCompat
import androidx.room.DatabaseView
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Relation
import org.fdroid.LocaleChooser.getBestLocale
import org.fdroid.database.VersionedStringType.PERMISSION
import org.fdroid.database.VersionedStringType.PERMISSION_SDK_23
import org.fdroid.index.v2.ANTI_FEATURE_KNOWN_VULNERABILITY
import org.fdroid.index.v2.FileV1
import org.fdroid.index.v2.FileV2
import org.fdroid.index.v2.LocalizedTextV2
import org.fdroid.index.v2.ManifestV2
import org.fdroid.index.v2.PackageManifest
import org.fdroid.index.v2.PackageVersion
import org.fdroid.index.v2.PackageVersionV2
import org.fdroid.index.v2.PermissionV2
import org.fdroid.index.v2.SignerV2
import org.fdroid.index.v2.UsesSdkV2

/**
 * A database table entity representing the version of an [App]
 * identified by its [versionCode] and [signer].
 * This holds the data of [PackageVersionV2].
 */
@Entity(
    tableName = Version.TABLE,
    primaryKeys = ["repoId", "packageName", "versionId"],
    foreignKeys = [ForeignKey(
        entity = AppMetadata::class,
        parentColumns = ["repoId", "packageName"],
        childColumns = ["repoId", "packageName"],
        onDelete = ForeignKey.CASCADE,
    )],
)
internal data class Version(
    val repoId: Long,
    val packageName: String,
    val versionId: String,
    val added: Long,
    @Embedded(prefix = "file_") val file: FileV1,
    @Embedded(prefix = "src_") val src: FileV2? = null,
    @Embedded(prefix = "manifest_") val manifest: AppManifest,
    override val releaseChannels: List<String>? = emptyList(),
    val antiFeatures: Map<String, LocalizedTextV2>? = null,
    val whatsNew: LocalizedTextV2? = null,
    val isCompatible: Boolean,
) : PackageVersion {
    internal companion object {
        const val TABLE = "Version"
    }

    override val versionCode: Long get() = manifest.versionCode
    override val signer: SignerV2? get() = manifest.signer
    override val packageManifest: PackageManifest get() = manifest
    override val hasKnownVulnerability: Boolean
        get() = antiFeatures?.contains(ANTI_FEATURE_KNOWN_VULNERABILITY) == true

    internal fun toAppVersion(versionedStrings: List<VersionedString>): AppVersion = AppVersion(
        version = this,
        versionedStrings = versionedStrings,
    )
}

internal fun PackageVersionV2.toVersion(
    repoId: Long,
    packageName: String,
    versionId: String,
    isCompatible: Boolean,
) = Version(
    repoId = repoId,
    packageName = packageName,
    versionId = versionId,
    added = added,
    file = file,
    src = src,
    manifest = manifest.toManifest(),
    releaseChannels = releaseChannels,
    antiFeatures = antiFeatures,
    whatsNew = whatsNew,
    isCompatible = isCompatible,
)

/**
 * A version of an [App] identified by [AppManifest.versionCode] and [AppManifest.signer].
 */
public data class AppVersion internal constructor(
    @Embedded internal val version: Version,
    @Relation(
        parentColumn = "versionId",
        entityColumn = "versionId",
    )
    internal val versionedStrings: List<VersionedString>?,
) {
    public val repoId: Long get() = version.repoId
    public val packageName: String get() = version.packageName
    public val added: Long get() = version.added
    public val isCompatible: Boolean get() = version.isCompatible
    public val manifest: AppManifest get() = version.manifest
    public val file: FileV1 get() = version.file
    public val src: FileV2? get() = version.src
    public val usesPermission: List<PermissionV2>
        get() = versionedStrings?.getPermissions(version) ?: emptyList()
    public val usesPermissionSdk23: List<PermissionV2>
        get() = versionedStrings?.getPermissionsSdk23(version) ?: emptyList()
    public val featureNames: List<String> get() = version.manifest.features ?: emptyList()
    public val nativeCode: List<String> get() = version.manifest.nativecode ?: emptyList()
    public val releaseChannels: List<String> get() = version.releaseChannels ?: emptyList()
    public val antiFeatureKeys: List<String>
        get() = version.antiFeatures?.map { it.key } ?: emptyList()

    public fun getWhatsNew(localeList: LocaleListCompat): String? =
        version.whatsNew.getBestLocale(localeList)

    public fun getAntiFeatureReason(antiFeatureKey: String, localeList: LocaleListCompat): String? {
        return version.antiFeatures?.get(antiFeatureKey)?.getBestLocale(localeList)
    }
}

/**
 * The manifest information of an [AppVersion].
 */
public data class AppManifest(
    public val versionName: String,
    public val versionCode: Long,
    @Embedded(prefix = "usesSdk_") public val usesSdk: UsesSdkV2? = null,
    public override val maxSdkVersion: Int? = null,
    @Embedded(prefix = "signer_") public val signer: SignerV2? = null,
    public override val nativecode: List<String>? = emptyList(),
    public val features: List<String>? = emptyList(),
) : PackageManifest {
    public override val minSdkVersion: Int? get() = usesSdk?.minSdkVersion
    public override val featureNames: List<String>? get() = features
}

internal fun ManifestV2.toManifest() = AppManifest(
    versionName = versionName,
    versionCode = versionCode,
    usesSdk = usesSdk,
    maxSdkVersion = maxSdkVersion,
    signer = signer,
    nativecode = nativecode,
    features = features.map { it.name },
)

@DatabaseView(viewName = HighestVersion.TABLE,
    value = """SELECT repoId, packageName, antiFeatures FROM ${Version.TABLE}
    GROUP BY repoId, packageName HAVING MAX(manifest_versionCode)""")
internal class HighestVersion(
    val repoId: Long,
    val packageName: String,
    val antiFeatures: Map<String, LocalizedTextV2>? = null,
) {
    internal companion object {
        const val TABLE = "HighestVersion"
    }
}

internal enum class VersionedStringType {
    PERMISSION,
    PERMISSION_SDK_23,
}

@Entity(
    tableName = VersionedString.TABLE,
    primaryKeys = ["repoId", "packageName", "versionId", "type", "name"],
    foreignKeys = [ForeignKey(
        entity = Version::class,
        parentColumns = ["repoId", "packageName", "versionId"],
        childColumns = ["repoId", "packageName", "versionId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
internal data class VersionedString(
    val repoId: Long,
    val packageName: String,
    val versionId: String,
    val type: VersionedStringType,
    val name: String,
    val version: Int? = null,
) {
    internal companion object {
        const val TABLE = "VersionedString"
    }
}

internal fun List<PermissionV2>.toVersionedString(
    version: Version,
    type: VersionedStringType,
) = map { permission ->
    VersionedString(
        repoId = version.repoId,
        packageName = version.packageName,
        versionId = version.versionId,
        type = type,
        name = permission.name,
        version = permission.maxSdkVersion,
    )
}

internal fun ManifestV2.getVersionedStrings(version: Version): List<VersionedString> {
    return usesPermission.toVersionedString(version, PERMISSION) +
        usesPermissionSdk23.toVersionedString(version, PERMISSION_SDK_23)
}

internal fun List<VersionedString>.getPermissions(version: Version) = mapNotNull { v ->
    v.map(version, PERMISSION) {
        PermissionV2(
            name = v.name,
            maxSdkVersion = v.version,
        )
    }
}

internal fun List<VersionedString>.getPermissionsSdk23(version: Version) = mapNotNull { v ->
    v.map(version, PERMISSION_SDK_23) {
        PermissionV2(
            name = v.name,
            maxSdkVersion = v.version,
        )
    }
}

private fun <T> VersionedString.map(
    v: Version,
    wantedType: VersionedStringType,
    factory: () -> T,
): T? {
    return if (repoId != v.repoId || packageName != v.packageName || versionId != v.versionId ||
        type != wantedType
    ) null
    else factory()
}

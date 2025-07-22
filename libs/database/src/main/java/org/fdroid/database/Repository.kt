package org.fdroid.database

import android.net.Uri
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.core.os.LocaleListCompat
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.Relation
import org.fdroid.LocaleChooser.getBestLocale
import org.fdroid.index.IndexFormatVersion
import org.fdroid.index.IndexUtils.getFingerprint
import org.fdroid.index.v2.AntiFeatureV2
import org.fdroid.index.v2.CategoryV2
import org.fdroid.index.v2.FileV2
import org.fdroid.index.v2.LocalizedFileV2
import org.fdroid.index.v2.LocalizedTextV2
import org.fdroid.index.v2.MirrorV2
import org.fdroid.index.v2.ReleaseChannelV2
import org.fdroid.index.v2.RepoV2
import java.util.concurrent.TimeUnit

private const val TAG = "Repository"

@Entity(tableName = CoreRepository.TABLE)
internal data class CoreRepository(
    @PrimaryKey(autoGenerate = true) val repoId: Long = 0,
    val name: LocalizedTextV2 = emptyMap(),
    val icon: LocalizedFileV2?,
    val address: String,
    val webBaseUrl: String? = null,
    val timestamp: Long,
    val version: Long?,
    val formatVersion: IndexFormatVersion?,
    val maxAge: Int?,
    val description: LocalizedTextV2 = emptyMap(),
    val certificate: String,
) {
    internal companion object {
        const val TABLE = "CoreRepository"
    }

    init {
        // TODO comment in some time after #2662 had time to resolve itself
//        validateCertificate(certificate)
    }
}

internal fun RepoV2.toCoreRepository(
    repoId: Long = 0,
    version: Long,
    formatVersion: IndexFormatVersion? = null,
    certificate: String,
) = CoreRepository(
    repoId = repoId,
    name = name,
    icon = icon,
    address = address,
    webBaseUrl = webBaseUrl,
    timestamp = timestamp,
    version = version,
    formatVersion = formatVersion,
    maxAge = null,
    description = description,
    certificate = certificate,
)

public data class Repository internal constructor(
    @Embedded internal val repository: CoreRepository,
    @Relation(
        parentColumn = "repoId",
        entityColumn = "repoId",
    )
    internal val mirrors: List<Mirror>,
    @Relation(
        parentColumn = "repoId",
        entityColumn = "repoId",
    )
    internal val antiFeatures: List<AntiFeature>,
    @Relation(
        parentColumn = "repoId",
        entityColumn = "repoId",
    )
    internal val categories: List<Category>,
    @Relation(
        parentColumn = "repoId",
        entityColumn = "repoId",
    )
    internal val releaseChannels: List<ReleaseChannel>,
    @Relation(
        parentColumn = "repoId",
        entityColumn = "repoId",
    )
    internal val preferences: RepositoryPreferences,
) {
    /**
     * Used to create a minimal version of a [Repository].
     */
    @JvmOverloads
    public constructor(
        repoId: Long,
        address: String,
        timestamp: Long,
        formatVersion: IndexFormatVersion,
        certificate: String,
        version: Long,
        weight: Int,
        lastUpdated: Long,
        username: String? = null,
        password: String? = null,
    ) : this(
        repository = CoreRepository(
            repoId = repoId,
            icon = null,
            address = address,
            timestamp = timestamp,
            formatVersion = formatVersion,
            maxAge = 42,
            certificate = certificate,
            version = version,
        ),
        mirrors = emptyList(),
        antiFeatures = emptyList(),
        categories = emptyList(),
        releaseChannels = emptyList(),
        preferences = RepositoryPreferences(
            repoId = repoId,
            weight = weight,
            lastUpdated = lastUpdated,
            username = username,
            password = password,
        )
    )

    public val repoId: Long get() = repository.repoId
    public val address: String get() = repository.address
    public val webBaseUrl: String? get() = repository.webBaseUrl
    public val timestamp: Long get() = repository.timestamp
    public val version: Long get() = repository.version ?: 0
    public val formatVersion: IndexFormatVersion? get() = repository.formatVersion
    public val certificate: String get() = repository.certificate

    /**
     * True if this repository is an archive repo.
     * It is suggested to not show archive repos in the list of repos in the UI.
     */
    public val isArchiveRepo: Boolean
        get() = repository.address.trimEnd('/').endsWith("/archive")

    public fun getName(localeList: LocaleListCompat): String? =
        repository.name.getBestLocale(localeList)

    public fun getDescription(localeList: LocaleListCompat): String? =
        repository.description.getBestLocale(localeList)

    public fun getIcon(localeList: LocaleListCompat): FileV2? =
        repository.icon.getBestLocale(localeList)

    public fun getAntiFeatures(): Map<String, AntiFeature> {
        return antiFeatures.associateBy { antiFeature -> antiFeature.id }
    }

    public fun getCategories(): Map<String, Category> {
        return categories.associateBy { category -> category.id }
    }

    public fun getReleaseChannels(): Map<String, ReleaseChannel> {
        return releaseChannels.associateBy { releaseChannel -> releaseChannel.id }
    }

    public val weight: Int get() = preferences.weight
    public val enabled: Boolean get() = preferences.enabled
    public val lastUpdated: Long? get() = preferences.lastUpdated
    public val userMirrors: List<String> get() = preferences.userMirrors ?: emptyList()
    public val disabledMirrors: List<String> get() = preferences.disabledMirrors ?: emptyList()
    public val username: String? get() = preferences.username
    public val password: String? get() = preferences.password

    @Suppress("DEPRECATION")
    @Deprecated("Only used for v1 index", ReplaceWith(""))
    public val lastETag: String?
        get() = preferences.lastETag

    /**
     * The fingerprint for the [certificate].
     * This gets calculated on first call and is an expensive operation.
     * Subsequent calls re-use the
     */
    @delegate:Ignore
    public val fingerprint: String? by lazy {
        certificate?.let { getFingerprint(it) }
    }

    /**
     * Returns official and user-added mirrors without the [disabledMirrors].
     */
    public fun getMirrors(): List<org.fdroid.download.Mirror> {
        return getAllMirrors(true).filter {
            !disabledMirrors.contains(it.baseUrl)
        }.ifEmpty { listOf(org.fdroid.download.Mirror(address)) }
    }

    public val allUserMirrors: List<org.fdroid.download.Mirror>
        get() = userMirrors.map { org.fdroid.download.Mirror(it) }

    public val allOfficialMirrors: List<org.fdroid.download.Mirror>
        get() = getAllMirrors(false)

    /**
     * Returns all mirrors, including [disabledMirrors].
     */
    @JvmOverloads
    public fun getAllMirrors(includeUserMirrors: Boolean = true): List<org.fdroid.download.Mirror> {
        val all = mirrors.map {
            it.toDownloadMirror()
        } + if (includeUserMirrors) userMirrors.map {
            org.fdroid.download.Mirror(it)
        } else emptyList()
        // whether or not the repo address is part of the mirrors is not yet standardized,
        // so we may need to add it to the list ourselves
        val hasCanonicalMirror = all.find { it.baseUrl == address } != null
        return if (hasCanonicalMirror) all else all.toMutableList().apply {
            add(0, org.fdroid.download.Mirror(address))
        }
    }

    val shareUri: String
        @WorkerThread
        get() {
            var uri = Uri.parse(address)
            fingerprint?.let {
                try {
                    uri = uri.buildUpon().appendQueryParameter("fingerprint", it).build()
                } catch (e: UnsupportedOperationException) {
                    Log.e(TAG, "Failed to append fingerprint to URI: $e")
                }
            }
            return uri.toString()
        }
}

// Dummy repo to use in Compose Previews and in tests
public val DUMMY_TEST_REPO: Repository = Repository(
    repoId = 1L,
    address = "https://example.com/fdroid/repo",
    timestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2),
    formatVersion = IndexFormatVersion.TWO,
    certificate = "abc",
    version = 1L,
    weight = 1,
    lastUpdated = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1),
)

/**
 * A database table to store repository mirror information.
 */
@Entity(
    tableName = Mirror.TABLE,
    primaryKeys = ["repoId", "url"],
    foreignKeys = [ForeignKey(
        entity = CoreRepository::class,
        parentColumns = ["repoId"],
        childColumns = ["repoId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
internal data class Mirror(
    val repoId: Long,
    val url: String,
    val countryCode: String? = null,
) {
    internal companion object {
        const val TABLE = "Mirror"
    }

    fun toDownloadMirror(): org.fdroid.download.Mirror = org.fdroid.download.Mirror(
        baseUrl = url,
        countryCode = countryCode,
    )
}

internal fun MirrorV2.toMirror(repoId: Long) = Mirror(
    repoId = repoId,
    url = url,
    countryCode = countryCode,
)

internal fun List<MirrorV2>.toMirrors(repoId: Long): List<Mirror> {
    return this.map { it.toMirror(repoId) }
}

/**
 * An attribute belonging to a [Repository].
 */
public abstract class RepoAttribute {
    public abstract val icon: LocalizedFileV2
    internal abstract val name: LocalizedTextV2
    internal abstract val description: LocalizedTextV2

    public fun getIcon(localeList: LocaleListCompat): FileV2? =
        icon.getBestLocale(localeList)

    public fun getName(localeList: LocaleListCompat): String? =
        name.getBestLocale(localeList)

    public fun getDescription(localeList: LocaleListCompat): String? =
        description.getBestLocale(localeList)
}

/**
 * An anti-feature belonging to a [Repository].
 */
@Entity(
    tableName = AntiFeature.TABLE,
    primaryKeys = ["repoId", "id"],
    foreignKeys = [ForeignKey(
        entity = CoreRepository::class,
        parentColumns = ["repoId"],
        childColumns = ["repoId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
public data class AntiFeature internal constructor(
    internal val repoId: Long,
    internal val id: String,
    override val icon: LocalizedFileV2 = emptyMap(),
    override val name: LocalizedTextV2,
    override val description: LocalizedTextV2 = emptyMap(),
) : RepoAttribute() {
    internal companion object {
        const val TABLE = "AntiFeature"
    }
}

internal fun Map<String, AntiFeatureV2>.toRepoAntiFeatures(repoId: Long) = map {
    AntiFeature(
        repoId = repoId,
        id = it.key,
        icon = it.value.icon,
        name = it.value.name,
        description = it.value.description,
    )
}

/**
 * A category of apps belonging to a [Repository].
 */
@Entity(
    tableName = Category.TABLE,
    primaryKeys = ["repoId", "id"],
    foreignKeys = [ForeignKey(
        entity = CoreRepository::class,
        parentColumns = ["repoId"],
        childColumns = ["repoId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
public data class Category internal constructor(
    public val repoId: Long,
    public val id: String,
    override val icon: LocalizedFileV2 = emptyMap(),
    override val name: LocalizedTextV2,
    override val description: LocalizedTextV2 = emptyMap(),
) : RepoAttribute() {
    internal companion object {
        const val TABLE = "Category"
    }
}

internal fun Map<String, CategoryV2>.toRepoCategories(repoId: Long) = map {
    Category(
        repoId = repoId,
        id = it.key,
        icon = it.value.icon,
        name = it.value.name,
        description = it.value.description,
    )
}

/**
 * A release-channel for apps belonging to a [Repository].
 */
@Entity(
    tableName = ReleaseChannel.TABLE,
    primaryKeys = ["repoId", "id"],
    foreignKeys = [ForeignKey(
        entity = CoreRepository::class,
        parentColumns = ["repoId"],
        childColumns = ["repoId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
public data class ReleaseChannel(
    internal val repoId: Long,
    internal val id: String,
    override val icon: LocalizedFileV2 = emptyMap(),
    override val name: LocalizedTextV2,
    override val description: LocalizedTextV2 = emptyMap(),
) : RepoAttribute() {
    internal companion object {
        const val TABLE = "ReleaseChannel"
    }
}

internal fun Map<String, ReleaseChannelV2>.toRepoReleaseChannel(repoId: Long) = map {
    ReleaseChannel(
        repoId = repoId,
        id = it.key,
        name = it.value.name,
        description = it.value.description,
    )
}

@Entity(tableName = RepositoryPreferences.TABLE)
internal data class RepositoryPreferences(
    @PrimaryKey internal val repoId: Long,
    val weight: Int,
    val enabled: Boolean = true,
    val lastUpdated: Long? = System.currentTimeMillis(),
    @Deprecated("Only used for indexV1") val lastETag: String? = null,
    val userMirrors: List<String>? = null,
    val disabledMirrors: List<String>? = null,
    val username: String? = null,
    val password: String? = null,
) {
    internal companion object {
        const val TABLE = "RepositoryPreferences"
    }
}

/**
 * A reduced version of [Repository] used to pre-populate the [FDroidDatabase].
 */
public data class InitialRepository @JvmOverloads constructor(
    val name: String,
    val address: String,
    val mirrors: List<String> = emptyList(),
    val description: String,
    val certificate: String,
    val version: Long,
    val enabled: Boolean,
    @Deprecated("This is automatically assigned now and can be safely removed.")
    val weight: Int = 0, // still used for testing, could be made internal or tests migrate away
) {
    init {
        validateCertificate(certificate)
    }
}

@Throws(IllegalArgumentException::class)
private fun validateCertificate(certificate: String?) {
    if (certificate != null) require(certificate.length % 2 == 0 &&
        certificate.chunked(2).find { it.toIntOrNull(16) == null } == null
    ) { "Invalid certificate: $certificate" }
}

/**
 * A reduced version of [Repository] used to add new repositories.
 */
public data class NewRepository(
    val name: LocalizedTextV2,
    val icon: LocalizedFileV2,
    val address: String,
    val formatVersion: IndexFormatVersion?,
    val certificate: String,
    val username: String? = null,
    val password: String? = null,
)
